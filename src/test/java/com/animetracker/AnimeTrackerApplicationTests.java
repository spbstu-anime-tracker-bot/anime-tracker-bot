package com.animetracker;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "telegram.bot.token=test-token",
        "telegram.bot.username=test-bot",
        "gemini.api.key=test-key",
        "spring.datasource.url=jdbc:h2:mem:testdb",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.kafka.bootstrap-servers=localhost:9092"
})
class AnimeTrackerApplicationTests {

    @Test
    void contextLoads() {
        
    }
}
