package com.animetracker.module;

import com.animetracker.service.GeminiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class HealthCheckModule {

    private final JdbcTemplate jdbcTemplate;
    private final GeminiService geminiService;

    @Value("${spring.kafka.bootstrap-servers}")
    private String kafkaBootstrapServers;

    public Map<String, String> checkHealth() {
        String postgresql = checkPostgres();
        String kafka = checkKafka();
        String gemini = checkGemini();

        boolean allUp = "UP".equals(postgresql) && "UP".equals(kafka) && "UP".equals(gemini);
        String overall = allUp ? "UP" : "DEGRADED";

        return Map.of(
                "status", overall,
                "postgresql", postgresql,
                "kafka", kafka,
                "gemini", gemini
        );
    }

    private String checkPostgres() {
        try {
            jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            return "UP";
        } catch (Exception e) {
            log.warn("PostgreSQL check failed: {}", e.getMessage());
            return "DOWN";
        }
    }

    private String checkKafka() {
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers);
        props.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, "3000");
        try (AdminClient client = AdminClient.create(props)) {
            client.listTopics().names().get(3, TimeUnit.SECONDS);
            return "UP";
        } catch (Exception e) {
            log.warn("Kafka check failed: {}", e.getMessage());
            return "DOWN";
        }
    }

    private String checkGemini() {
        try {
            return geminiService.isAvailable() ? "UP" : "DOWN";
        } catch (Exception e) {
            return "DOWN";
        }
    }
}
