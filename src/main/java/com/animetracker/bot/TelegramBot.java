package com.animetracker.bot;

import com.animetracker.dto.RecommendationReadyAppEvent;
import com.animetracker.entity.Anime;
import com.animetracker.module.*;
import com.animetracker.service.UserSessionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

    private final AuthUserModule authUserModule;
    private final SearchAnimeModule searchAnimeModule;
    private final DisplayCardsModule displayCardsModule;
    private final ManageUserListsModule manageUserListsModule;
    private final DisplayUserListsModule displayUserListsModule;
    private final RateAnimeModule rateAnimeModule;
    private final AdviseModule adviseModule;
    private final UserSessionService sessionService;

    private final String botUsername;

    public TelegramBot(
            @Value("${telegram.bot.token}") String token,
            @Value("${telegram.bot.username}") String botUsername,
            AuthUserModule authUserModule,
            SearchAnimeModule searchAnimeModule,
            DisplayCardsModule displayCardsModule,
            ManageUserListsModule manageUserListsModule,
            DisplayUserListsModule displayUserListsModule,
            RateAnimeModule rateAnimeModule,
            AdviseModule adviseModule,
            UserSessionService sessionService) {
        super(token);
        this.botUsername = botUsername;
        this.authUserModule = authUserModule;
        this.searchAnimeModule = searchAnimeModule;
        this.displayCardsModule = displayCardsModule;
        this.manageUserListsModule = manageUserListsModule;
        this.displayUserListsModule = displayUserListsModule;
        this.rateAnimeModule = rateAnimeModule;
        this.adviseModule = adviseModule;
        this.sessionService = sessionService;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public void onUpdateReceived(Update update) {
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

        // Ensure user is registered
        authUserModule.registerOrGet(userId, firstName);

        // Check if user is awaiting rating input (handled via buttons, but just in case)
        UserSession session = sessionService.get(userId);

        if (text.startsWith("/start")) {
            send(chatId, "👋 Привет, " + firstName + "! Добро пожаловать в Anime Tracker Bot.\n\n" +
                    "Команды:\n" +
                    "/search <название> — поиск аниме\n" +
                    "/search_by <параметры> — расширенный поиск (жанр, тип, год)\n" +
                    "/list_viewed — список просмотренных\n" +
                    "/list_to_view — список отслеживаемых\n" +
                    "/advise — получить рекомендации");

        } else if (text.startsWith("/search_by")) {
            String params = text.substring("/search_by".length()).trim();
            if (params.isEmpty()) {
                send(chatId, "Использование: /search_by <жанр/тип/год>, ...\nПример: /search_by Action, TV, 2020");
                return;
            }
            SearchAnimeModule.SearchResult result = searchAnimeModule.searchByFilters(params);
            if (result.isError()) {
                send(chatId, result.errorMessage());
            } else {
                sendResults(userId, chatId, result.anime());
            }

        } else if (text.startsWith("/search")) {
            String query = text.substring("/search".length()).trim();
            if (query.isEmpty()) {
                send(chatId, "Использование: /search <название>");
                return;
            }
            List<Anime> results = searchAnimeModule.searchByTitle(query);
            sendResults(userId, chatId, results);

        } else if (text.equals("/list_viewed")) {
            List<Anime> results = displayUserListsModule.getViewedList(userId);
            if (results.isEmpty()) {
                send(chatId, "Ваш список просмотренных пуст.");
            } else {
                sendResults(userId, chatId, results);
            }

        } else if (text.equals("/list_to_view")) {
            List<Anime> results = displayUserListsModule.getToViewList(userId);
            if (results.isEmpty()) {
                send(chatId, "Ваш список отслеживаемых пуст.");
            } else {
                sendResults(userId, chatId, results);
            }

        } else if (text.equals("/advise")) {
            handleAdvise(userId, chatId);

        } else if (text.startsWith("/")) {
            send(chatId, "❓ Неизвестная команда. Введите /start для просмотра доступных команд.");

        } else {
            send(chatId, "ℹ️ Пожалуйста, используйте команды. Введите /start для помощи.");
        }
    }

    private void handleAdvise(Long userId, Long chatId) {
        if (adviseModule.hasCachedRecommendations(userId)) {
            List<Anime> cached = adviseModule.getCachedRecommendations(userId);
            if (cached != null && !cached.isEmpty()) {
                send(chatId, "📋 Ваши рекомендации:");
                sendResults(userId, chatId, cached);
                return;
            }
        }

        send(chatId, "⏳ Формируем рекомендации... Это займет немного времени.");
        boolean sent = adviseModule.requestNewRecommendations(userId);
        if (!sent) {
            send(chatId, "Функция временно недоступна. Попробуйте повторить запрос через некоторое время");
        }
    }

    @EventListener
    public void onRecommendationReady(RecommendationReadyAppEvent event) {
        Long userId = event.getTelegramId();
        UserSession session = sessionService.get(userId);
        Long chatId = session != null ? session.getChatId() : userId;

        if ("COMPLETED".equals(event.getStatus())) {
            List<Anime> recommendations = adviseModule.getCachedRecommendations(userId);
            if (recommendations != null && !recommendations.isEmpty()) {
                send(chatId, "✅ Рекомендации готовы!");
                sendResults(userId, chatId, recommendations);
            } else {
                send(chatId, "Функция временно недоступна. Попробуйте повторить запрос через некоторое время");
            }
        } else {
            send(chatId, "Функция временно недоступна. Попробуйте повторить запрос через некоторое время");
        }
    }

    private void handleCallback(CallbackQuery callbackQuery) {
        Long userId = callbackQuery.getFrom().getId();
        Long chatId = callbackQuery.getMessage().getChatId();
        Integer messageId = callbackQuery.getMessage().getMessageId();
        String data = callbackQuery.getData();

        authUserModule.registerOrGet(userId, callbackQuery.getFrom().getFirstName());

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

        if (data.startsWith("av:")) {
            Long animeId = Long.parseLong(data.substring(3));
            String result = manageUserListsModule.addToViewed(userId, animeId);
            answerCallback(callbackQuery.getId(), result);
            refreshCard(userId, chatId, messageId, animeId);
            return;
        }

        if (data.startsWith("rv:")) {
            Long animeId = Long.parseLong(data.substring(3));
            String result = manageUserListsModule.removeFromViewed(userId, animeId);
            answerCallback(callbackQuery.getId(), result);
            refreshCard(userId, chatId, messageId, animeId);
            return;
        }

        if (data.startsWith("at:")) {
            Long animeId = Long.parseLong(data.substring(3));
            String result = manageUserListsModule.addToToView(userId, animeId);
            answerCallback(callbackQuery.getId(), result);
            refreshCard(userId, chatId, messageId, animeId);
            return;
        }

        if (data.startsWith("rt:")) {
            Long animeId = Long.parseLong(data.substring(3));
            String result = manageUserListsModule.removeFromToView(userId, animeId);
            answerCallback(callbackQuery.getId(), result);
            refreshCard(userId, chatId, messageId, animeId);
            return;
        }

        if (data.startsWith("ra:")) {
            Long animeId = Long.parseLong(data.substring(3));
            if (!manageUserListsModule.isInViewed(userId, animeId)) {
                answerCallback(callbackQuery.getId(), "Сначала добавьте аниме в просмотренные.");
                return;
            }
            sessionService.setAwaitingRating(userId, data.substring(3));
            try {
                EditMessageReplyMarkup edit = EditMessageReplyMarkup.builder()
                        .chatId(chatId.toString())
                        .messageId(messageId)
                        .replyMarkup(displayCardsModule.buildRatingKeyboard(animeId))
                        .build();
                execute(edit);
            } catch (TelegramApiException e) {
                log.error("Error showing rating keyboard: {}", e.getMessage());
            }
            answerCallback(callbackQuery.getId(), "Выберите оценку:");
            return;
        }

        if (data.startsWith("rs:")) {
            String[] parts = data.substring(3).split(":");
            Long animeId = Long.parseLong(parts[0]);
            int score = Integer.parseInt(parts[1]);
            String result = rateAnimeModule.rate(userId, animeId, score);
            answerCallback(callbackQuery.getId(), result);
            sessionService.clearRatingAwait(userId);
            refreshCard(userId, chatId, messageId, animeId);
            return;
        }

        answerCallback(callbackQuery.getId(), "");
    }

    private void refreshCard(Long userId, Long chatId, Integer messageId, Long animeId) {
        UserSession session = sessionService.get(userId);
        if (session == null) return;

        Anime anime = session.getResults().stream()
                .filter(a -> a.getId().equals(animeId))
                .findFirst().orElse(null);
        if (anime == null) return;

        boolean inViewed = manageUserListsModule.isInViewed(userId, animeId);
        boolean inToView = manageUserListsModule.isInToView(userId, animeId);

        try {
            EditMessageText edit = EditMessageText.builder()
                    .chatId(chatId.toString())
                    .messageId(messageId)
                    .text(displayCardsModule.buildCardText(anime))
                    .parseMode("Markdown")
                    .replyMarkup(displayCardsModule.buildCardKeyboard(anime, inViewed, inToView))
                    .build();
            execute(edit);
        } catch (TelegramApiException e) {
            log.warn("Could not edit message: {}", e.getMessage());
        }
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

        // Edit existing card messages
        List<Anime> pageItems = session.getCurrentPageItems();
        List<Integer> cardIds = session.getCardMessageIds();

        for (int i = 0; i < cardIds.size(); i++) {
            try {
                if (i < pageItems.size()) {
                    Anime anime = pageItems.get(i);
                    boolean inViewed = manageUserListsModule.isInViewed(userId, anime.getId());
                    boolean inToView = manageUserListsModule.isInToView(userId, anime.getId());

                    EditMessageText edit = EditMessageText.builder()
                            .chatId(chatId.toString())
                            .messageId(cardIds.get(i))
                            .text(displayCardsModule.buildCardText(anime))
                            .parseMode("Markdown")
                            .replyMarkup(displayCardsModule.buildCardKeyboard(anime, inViewed, inToView))
                            .build();
                    execute(edit);
                } else {
                    // Fewer items on this page — clear the message
                    EditMessageText edit = EditMessageText.builder()
                            .chatId(chatId.toString())
                            .messageId(cardIds.get(i))
                            .text("—")
                            .build();
                    execute(edit);
                }
            } catch (TelegramApiException e) {
                log.warn("Could not edit card message: {}", e.getMessage());
            }
        }

        // Edit nav message
        if (session.getNavMessageId() != null) {
            try {
                EditMessageReplyMarkup editNav = EditMessageReplyMarkup.builder()
                        .chatId(chatId.toString())
                        .messageId(session.getNavMessageId())
                        .replyMarkup(displayCardsModule.buildNavKeyboard(
                                session.hasPrevPage(), session.hasNextPage(),
                                session.getCurrentPage(), session.getTotalPages()))
                        .build();
                execute(editNav);
            } catch (TelegramApiException e) {
                log.warn("Could not edit nav message: {}", e.getMessage());
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

        List<Anime> pageItems = session.getCurrentPageItems();
        List<Integer> cardMessageIds = new ArrayList<>();

        for (Anime anime : pageItems) {
            boolean inViewed = manageUserListsModule.isInViewed(userId, anime.getId());
            boolean inToView = manageUserListsModule.isInToView(userId, anime.getId());

            SendMessage msg = SendMessage.builder()
                    .chatId(chatId.toString())
                    .text(displayCardsModule.buildCardText(anime))
                    .parseMode("Markdown")
                    .replyMarkup(displayCardsModule.buildCardKeyboard(anime, inViewed, inToView))
                    .build();

            try {
                Message sent = execute(msg);
                cardMessageIds.add(sent.getMessageId());
            } catch (TelegramApiException e) {
                log.error("Error sending card: {}", e.getMessage());
            }
        }

        session.setCardMessageIds(cardMessageIds);

        // Send navigation message if more than one page
        if (session.getTotalPages() > 1) {
            SendMessage navMsg = SendMessage.builder()
                    .chatId(chatId.toString())
                    .text("Страница " + (session.getCurrentPage() + 1) + " из " + session.getTotalPages())
                    .replyMarkup(displayCardsModule.buildNavKeyboard(
                            session.hasPrevPage(), session.hasNextPage(),
                            session.getCurrentPage(), session.getTotalPages()))
                    .build();
            try {
                Message sentNav = execute(navMsg);
                session.setNavMessageId(sentNav.getMessageId());
            } catch (TelegramApiException e) {
                log.error("Error sending nav: {}", e.getMessage());
            }
        }
    }

    private void send(Long chatId, String text) {
        try {
            execute(SendMessage.builder()
                    .chatId(chatId.toString())
                    .text(text)
                    .build());
        } catch (TelegramApiException e) {
            log.error("Error sending message: {}", e.getMessage());
        }
    }

    private void answerCallback(String callbackId, String text) {
        try {
            execute(org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery.builder()
                    .callbackQueryId(callbackId)
                    .text(text)
                    .showAlert(false)
                    .build());
        } catch (TelegramApiException e) {
            log.warn("Error answering callback: {}", e.getMessage());
        }
    }
}
