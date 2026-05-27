package com.animetracker;

import com.animetracker.config.AppConfig;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

class AnimeTrackerApplicationTests {

    @Test
    void contextLoads() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class)) {
            context.getBean("telegramBot");
        }
    }
}
