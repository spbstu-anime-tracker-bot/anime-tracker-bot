package com.animetracker.config;

import com.animetracker.bot.TelegramBot;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import java.util.Properties;

@Configuration
@ComponentScan(basePackageClasses = TelegramBot.class)
public class AppConfig {

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
        Properties properties = new Properties();
        properties.setProperty("telegram.bot.token", env("TELEGRAM_BOT_TOKEN", "your-token-here"));
        properties.setProperty("telegram.bot.username", env("TELEGRAM_BOT_USERNAME", "your-bot-username"));

        PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
        configurer.setProperties(properties);
        return configurer;
    }

    private static String env(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }
}
