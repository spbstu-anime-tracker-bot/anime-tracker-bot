package com.animetracker.recommendation.internal;

import com.animetracker.recommendation.RecommendationRequestEvent;
import com.animetracker.recommendation.RecommendationResultEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecommendationProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.recommendation-requests}")
    private String requestsTopic;

    @Value("${kafka.topics.recommendation-results}")
    private String resultsTopic;

    public void sendRecommendationRequest(RecommendationRequestEvent event) {
        log.info("Sending recommendation request for user {}", event.getTelegramId());
        kafkaTemplate.send(requestsTopic, event.getRequestId(), event);
    }

    public void sendRecommendationResult(RecommendationResultEvent event) {
        log.info("Sending recommendation result for user {}, status={}", event.getTelegramId(), event.getStatus());
        kafkaTemplate.send(resultsTopic, event.getRequestId(), event);
    }
}
