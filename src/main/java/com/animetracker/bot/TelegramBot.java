package com.animetracker.bot;

import com.animetracker.anime.Anime;
import com.animetracker.module.AdviseModule;
import com.animetracker.module.AuthUserModule;
import com.animetracker.module.DisplayCardsModule;
import com.animetracker.module.DisplayUserListsModule;
import com.animetracker.module.ManageUserListsModule;
import com.animetracker.module.RateAnimeModule;
import com.animetracker.module.SearchAnimeModule;
import com.animetracker.recommendation.RecommendationReadyEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
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
    private final ManageUserListsModule manageUserListsModule;
    private final DisplayUserListsModule displayUserListsModule;
    private final RateAnimeModule rateAnimeModule;
    private final AdviseModule adviseModule;
    private final UserSessionService sessionService;

    public TelegramBot(
            @Value("${telegram.bot.token}") String botToken,
            AuthUserModule authUserModule,
            SearchAnimeModule searchAnimeModule,
            DisplayCardsModule displayCardsModule,
            ManageUserListsModule manageUserListsModule,
            DisplayUserListsModule displayUserListsModule,
            RateAnimeModule rateAnimeModule,
            AdviseModule adviseModule,
            UserSessionService sessionService) {
        this.botToken = botToken;
        this.telegramClient = new OkHttpTelegramClient(botToken);
        this.authUserModule = authUserModule;
        this.searchAnimeModule = searchAnimeModule;
        this.displayCardsModule = displayCardsModule;
        this.manageUserListsModule = manageUserListsModule;
        this.displayUserListsModule = displayUserListsModule;
        this.rateAnimeModule = rateAnimeModule;
        this.adviseModule = adviseModule;
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

        if (text.equals("/list_viewed")) {
            List<Anime> results = displayUserListsModule.getViewedList(userId);
            if (results.isEmpty()) {
                send(chatId, "Ваш список просмотренных пуст.");
            } else {
                sendResults(userId, chatId, results);
            }
            return;
        }

        if (text.equals("/list_to_view")) {
            List<Anime> results = displayUserListsModule.getToViewList(userId);
            if (results.isEmpty()) {
                send(chatId, "Ваш список отслеживаемых пуст.");
            } else {
                sendResults(userId, chatId, results);
            }
            return;
        }

        if (text.equals("/advise")) {
            handleAdvise(userId, chatId);
            return;
        }

        if (text.startsWith("/")) {
            send(chatId, TEMPORARILY_DISABLED);
            return;
        }

        send(chatId, "Пожалуйста, используйте команды. Введите /start для помощи.");
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

        if (!adviseModule.hasWatchedAnime(userId)) {
            send(chatId, "У вас пока нет просмотренных аниме. Показываем самые популярные:");
            sendResults(userId, chatId, adviseModule.getPopularFallback());
            return;
        }

        if (adviseModule.isAlreadyProcessing(userId)) {
            send(chatId, "⏳ Рекомендации уже формируются. Пожалуйста, подождите.");
            return;
        }

        send(chatId, "⏳ Формируем рекомендации на основе ваших оценок... Это займёт немного времени.");
        boolean sent = adviseModule.requestNewRecommendations(userId);
        if (!sent) {
            send(chatId, "Функция временно недоступна. Попробуйте повторить запрос через некоторое время");
        }
    }

    @EventListener
    public void onRecommendationReady(RecommendationReadyEvent event) {
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
                telegramClient.execute(EditMessageReplyMarkup.builder()
                        .chatId(chatId)
                        .messageId(messageId)
                        .replyMarkup(displayCardsModule.buildRatingKeyboard(animeId))
                        .build());
            } catch (Exception e) {
                log.warn("Error showing rating keyboard: {}", e.getMessage());
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

        answerCallback(callbackQuery.getId(), TEMPORARILY_DISABLED);
    }

    private void refreshCard(Long userId, Long chatId, Integer messageId, Long animeId) {
        UserSession session = sessionService.get(userId);
        if (session == null) return;
        Anime anime = session.getResults().stream()
                .filter(a -> a.getId().equals(animeId))
                .findFirst()
                .orElse(null);
        if (anime == null) return;

        boolean inViewed = manageUserListsModule.isInViewed(userId, animeId);
        boolean inToView = manageUserListsModule.isInToView(userId, animeId);
        Integer userScore = manageUserListsModule.getUserScore(userId, animeId);
        try {
            telegramClient.execute(EditMessageText.builder()
                    .chatId(chatId)
                    .messageId(messageId)
                    .text(displayCardsModule.buildCardText(anime, userScore))
                    .parseMode("Markdown")
                    .replyMarkup(displayCardsModule.buildCardKeyboard(anime, inViewed, inToView, userScore))
                    .build());
        } catch (Exception e) {
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

        List<Anime> pageItems = session.getCurrentPageItems();
        List<Integer> cardIds = new ArrayList<>(session.getCardMessageIds());
        int oldSize = cardIds.size();
        int newSize = pageItems.size();

        for (int i = 0; i < Math.min(newSize, oldSize); i++) {
            try {
                telegramClient.execute(EditMessageText.builder()
                        .chatId(chatId)
                        .messageId(cardIds.get(i))
                        .text(buildCardText(userId, pageItems.get(i)))
                        .parseMode("Markdown")
                        .replyMarkup(buildCardKeyboard(userId, pageItems.get(i)))
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
                        .text(buildCardText(userId, pageItems.get(i)))
                        .parseMode("Markdown")
                        .replyMarkup(buildCardKeyboard(userId, pageItems.get(i)))
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
                        .text(buildCardText(userId, anime))
                        .parseMode("Markdown")
                        .replyMarkup(buildCardKeyboard(userId, anime))
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

    private String buildCardText(Long userId, Anime anime) {
        Integer userScore = manageUserListsModule.getUserScore(userId, anime.getId());
        return displayCardsModule.buildCardText(anime, userScore);
    }

    private org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup buildCardKeyboard(
            Long userId,
            Anime anime) {
        boolean inViewed = manageUserListsModule.isInViewed(userId, anime.getId());
        boolean inToView = manageUserListsModule.isInToView(userId, anime.getId());
        Integer userScore = manageUserListsModule.getUserScore(userId, anime.getId());
        return displayCardsModule.buildCardKeyboard(anime, inViewed, inToView, userScore);
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
