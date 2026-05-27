package com.animetracker;

import com.animetracker.bot.TelegramBot;
import com.animetracker.config.AppConfig;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.modulith.Modulithic;
import org.telegram.telegrambots.longpolling.TelegramBotsLongPollingApplication;

import java.util.concurrent.CountDownLatch;

@Modulithic
public class AnimeTrackerApplication {

    public static void main(String[] args) throws Exception {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);
        TelegramBot bot = context.getBean(TelegramBot.class);
        TelegramBotsLongPollingApplication telegram = new TelegramBotsLongPollingApplication();

        telegram.registerBot(bot.getBotToken(), bot);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                telegram.close();
            } catch (Exception ignored) {
            }
            context.close();
        }));

        System.out.println("Anime Tracker Bot started without Spring Boot");
        new CountDownLatch(1).await();
    }
}
