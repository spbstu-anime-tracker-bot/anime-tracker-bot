package com.animetracker.bot;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.generics.TelegramClient;

import java.util.List;

@Slf4j
@Component
public class TelegramBot implements LongPollingUpdateConsumer {

    private static final String TEMPORARILY_DISABLED =
            "Команда временно отключена";

    private final TelegramClient telegramClient;
    private final String botToken;

    public TelegramBot(@Value("${telegram.bot.token}") String botToken) {
        this.botToken = botToken;
        this.telegramClient = new OkHttpTelegramClient(botToken);
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
        Long chatId = message.getChatId();
        String text = message.getText().trim();
        String firstName = message.getFrom().getFirstName();

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

        if (text.startsWith("/")) {
            send(chatId, TEMPORARILY_DISABLED);
            return;
        }

        send(chatId, "Пожалуйста, используйте команды. Введите /start для помощи.");
    }

    private void handleCallback(CallbackQuery callbackQuery) {
        answerCallback(callbackQuery.getId(), TEMPORARILY_DISABLED);
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
