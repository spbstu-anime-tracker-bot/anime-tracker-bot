package com.animetracker.bot;

import com.animetracker.anime.Anime;
import com.animetracker.module.AuthUserModule;
import com.animetracker.module.DisplayCardsModule;
import com.animetracker.module.SearchAnimeModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class TelegramBot implements LongPollingUpdateConsumer {

    private static final String TEMPORARILY_DISABLED =
            "Команда временно отключена";

    private final TelegramClient telegramClient;
    private final String botToken;
    private final AuthUserModule authUserModule;
    private final SearchAnimeModule searchAnimeModule;
    private final DisplayCardsModule displayCardsModule;
    private final UserSessionService sessionService;

    public TelegramBot(
            @Value("${telegram.bot.token}") String botToken,
            AuthUserModule authUserModule,
            SearchAnimeModule searchAnimeModule,
            DisplayCardsModule displayCardsModule,
            UserSessionService sessionService) {
        this.botToken = botToken;
        this.telegramClient = new OkHttpTelegramClient(botToken);
        this.authUserModule = authUserModule;
        this.searchAnimeModule = searchAnimeModule;
        this.displayCardsModule = displayCardsModule;
        this.sessionService = sessionService;
    }

    public String getBotToken() {
        return botToken;
    }

    @Override
    public void consume(List<Update> updates) {
        updates.forEach(this::handleUpdate);
    }

    private void handleUpdate(Update update) {
        try {
            if (update.hasCallbackQuery()) {
                handleCallback(update.getCallbackQuery());
            } else if (update.hasMessage() && update.getMessage().hasText()) {
                handleMessage(update.getMessage());
            }
        } catch (Exception e) {
            log.error("Error handling update: {}", e.getMessage(), e);
        }
    }

    private void handleMessage(Message message) {
        Long userId = message.getFrom().getId();
        Long chatId = message.getChatId();
        String text = message.getText().trim();
        String firstName = message.getFrom().getFirstName();

        authUserModule.registerOrGet(userId, firstName);

        if (text.startsWith("/start")) {
            send(chatId,
                    "👋 Привет, " + firstName + "! Добро пожаловать в Anime Tracker Bot.\n\n" +
                            "Команды:\n" +
                            "/search <название> — поиск аниме\n" +
                            "/search_by <параметры> — расширенный поиск (жанр, тип, год)\n" +
                            "/list_viewed — список просмотренных\n" +
                            "/list_to_view — список отслеживаемых\n" +
                            "/advise — получить рекомендации");
            return;
        }

        if (text.startsWith("/search_by")) {
            send(chatId, TEMPORARILY_DISABLED);
            return;
        }

        if (text.startsWith("/search")) {
            String query = text.substring("/search".length()).trim();
            if (query.isEmpty()) {
                send(chatId, "Использование: /search <название>");
                return;
            }
            List<Anime> results = searchAnimeModule.searchByTitle(query);
            sendResults(userId, chatId, results);
            return;
        }

        if (text.startsWith("/")) {
            send(chatId, TEMPORARILY_DISABLED);
            return;
        }

        send(chatId, "Пожалуйста, используйте команды. Введите /start для помощи.");
    }

    private void handleCallback(CallbackQuery callbackQuery) {
        Long userId = callbackQuery.getFrom().getId();
        Long chatId = callbackQuery.getMessage().getChatId();
        String data = callbackQuery.getData();

        if ("noop".equals(data)) {
            answerCallback(callbackQuery.getId(), "");
            return;
        }
        if ("np".equals(data)) {
            navigatePage(userId, chatId, true);
            answerCallback(callbackQuery.getId(), "");
            return;
        }
        if ("pp".equals(data)) {
            navigatePage(userId, chatId, false);
            answerCallback(callbackQuery.getId(), "");
            return;
        }

        answerCallback(callbackQuery.getId(), TEMPORARILY_DISABLED);
    }

    private void navigatePage(Long userId, Long chatId, boolean next) {
        UserSession session = sessionService.get(userId);
        if (session == null) return;

        if (next && session.hasNextPage()) {
            session.setCurrentPage(session.getCurrentPage() + 1);
        } else if (!next && session.hasPrevPage()) {
            session.setCurrentPage(session.getCurrentPage() - 1);
        } else {
            return;
        }

        List<Anime> pageItems = session.getCurrentPageItems();
        List<Integer> cardIds = new ArrayList<>(session.getCardMessageIds());
        int oldSize = cardIds.size();
        int newSize = pageItems.size();

        for (int i = 0; i < Math.min(newSize, oldSize); i++) {
            try {
                telegramClient.execute(EditMessageText.builder()
                        .chatId(chatId)
                        .messageId(cardIds.get(i))
                        .text(displayCardsModule.buildCardText(pageItems.get(i), null))
                        .parseMode("Markdown")
                        .build());
            } catch (Exception e) {
                log.warn("Could not edit card: {}", e.getMessage());
            }
        }

        for (int i = oldSize - 1; i >= newSize; i--) {
            try {
                telegramClient.execute(DeleteMessage.builder()
                        .chatId(chatId)
                        .messageId(cardIds.get(i))
                        .build());
                cardIds.remove(i);
            } catch (Exception e) {
                log.warn("Could not delete card: {}", e.getMessage());
            }
        }

        for (int i = oldSize; i < newSize; i++) {
            try {
                Message sent = telegramClient.execute(SendMessage.builder()
                        .chatId(chatId)
                        .text(displayCardsModule.buildCardText(pageItems.get(i), null))
                        .parseMode("Markdown")
                        .build());
                cardIds.add(sent.getMessageId());
            } catch (Exception e) {
                log.error("Error sending new card: {}", e.getMessage());
            }
        }

        session.setCardMessageIds(cardIds);

        if (session.getNavMessageId() != null) {
            try {
                telegramClient.execute(EditMessageReplyMarkup.builder()
                        .chatId(chatId)
                        .messageId(session.getNavMessageId())
                        .replyMarkup(displayCardsModule.buildNavKeyboard(
                                session.hasPrevPage(), session.hasNextPage(),
                                session.getCurrentPage(), session.getTotalPages()))
                        .build());
            } catch (Exception e) {
                log.warn("Could not edit nav: {}", e.getMessage());
            }
        }
    }

    private void sendResults(Long userId, Long chatId, List<Anime> results) {
        if (results == null || results.isEmpty()) {
            send(chatId, "Аниме не найдены");
            return;
        }

        sessionService.setResults(userId, results, chatId);
        UserSession session = sessionService.get(userId);
        List<Integer> cardMessageIds = new ArrayList<>();

        for (Anime anime : session.getCurrentPageItems()) {
            try {
                Message sent = telegramClient.execute(SendMessage.builder()
                        .chatId(chatId)
                        .text(displayCardsModule.buildCardText(anime, null))
                        .parseMode("Markdown")
                        .build());
                cardMessageIds.add(sent.getMessageId());
            } catch (Exception e) {
                log.error("Error sending card: {}", e.getMessage());
            }
        }
        session.setCardMessageIds(cardMessageIds);

        if (session.getTotalPages() > 1) {
            try {
                Message sentNav = telegramClient.execute(SendMessage.builder()
                        .chatId(chatId)
                        .text("Страница " + (session.getCurrentPage() + 1) + " из " + session.getTotalPages())
                        .replyMarkup(displayCardsModule.buildNavKeyboard(
                                session.hasPrevPage(), session.hasNextPage(),
                                session.getCurrentPage(), session.getTotalPages()))
                        .build());
                session.setNavMessageId(sentNav.getMessageId());
            } catch (Exception e) {
                log.error("Error sending nav: {}", e.getMessage());
            }
        }
    }

    public void send(Long chatId, String text) {
        try {
            telegramClient.execute(SendMessage.builder()
                    .chatId(chatId)
                    .text(text)
                    .build());
        } catch (Exception e) {
            log.error("Error sending message: {}", e.getMessage());
        }
    }

    private void answerCallback(String callbackId, String text) {
        try {
            telegramClient.execute(AnswerCallbackQuery.builder()
                    .callbackQueryId(callbackId)
                    .text(text)
                    .showAlert(false)
                    .build());
        } catch (Exception e) {
            log.warn("Error answering callback: {}", e.getMessage());
        }
    }
}
