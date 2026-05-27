package com.animetracker;

import com.animetracker.bot.TelegramBot;
import com.animetracker.config.AppConfig;
import com.animetracker.config.EmbeddedWebServer;
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
        EmbeddedWebServer webServer = EmbeddedWebServer.start(context, serverPort());

        telegram.registerBot(bot.getBotToken(), bot);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                telegram.close();
            } catch (Exception ignored) {
            }
            try {
                webServer.close();
            } catch (Exception ignored) {
            }
            context.close();
        }));

        System.out.println("Anime Tracker Bot started without Spring Boot");
        new CountDownLatch(1).await();
    }

    private static int serverPort() {
        String value = System.getenv("SERVER_PORT");
        if (value == null || value.isBlank()) {
            return 8080;
        }
        return Integer.parseInt(value);
    }
}
