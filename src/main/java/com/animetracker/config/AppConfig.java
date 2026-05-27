package com.animetracker.config;

import com.animetracker.anime.AnimeSearchService;
import com.animetracker.bot.TelegramBot;
import com.animetracker.module.SearchAnimeModule;
import com.animetracker.recommendation.RecommendationService;
import com.animetracker.tracking.TrackingService;
import com.animetracker.user.UserRegistrationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import java.util.Properties;

@Configuration
@Import({DatabaseConfig.class, JacksonConfig.class})
@ComponentScan(
        basePackageClasses = {
                TelegramBot.class,
                AnimeSearchService.class,
                UserRegistrationService.class,
                TrackingService.class,
                RecommendationService.class,
                SearchAnimeModule.class
        },
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.REGEX,
                pattern = {
                        "com\\.animetracker\\.llm\\..*",
                        "com\\.animetracker\\.module\\.AdviseModule",
                        "com\\.animetracker\\.module\\.HealthCheckModule",
                        "com\\.animetracker\\.module\\.RecommendationWorker",
                        "com\\.animetracker\\.recommendation\\.internal\\.RecommendationProducer",
                        "com\\.animetracker\\.recommendation\\.internal\\.RecommendationResultConsumer"
                }
        )
)
public class AppConfig {

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        Properties properties = new Properties();
        properties.setProperty("telegram.bot.token", env("TELEGRAM_BOT_TOKEN", "your-token-here"));
        properties.setProperty("telegram.bot.username", env("TELEGRAM_BOT_USERNAME", "your-bot-username"));
        properties.setProperty("spring.datasource.url", env("DB_URL", "jdbc:postgresql://localhost:5432/postgres"));
        properties.setProperty("spring.datasource.username", env("DB_USERNAME", "postgres"));
        properties.setProperty("spring.datasource.password", env("DB_PASSWORD", "postgres"));
        properties.setProperty("spring.datasource.driver-class-name", "org.postgresql.Driver");
        properties.setProperty("spring.jpa.hibernate.ddl-auto", "none");
        properties.setProperty("spring.jpa.show-sql", "false");

        PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
        configurer.setProperties(properties);
        return configurer;
    }

    private static String env(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }
}
