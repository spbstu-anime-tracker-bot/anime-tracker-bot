package com.animetracker.recommendation.internal;

import com.animetracker.recommendation.RecommendationReadyEvent;
import com.animetracker.recommendation.RecommendationResultEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
class RecommendationResultConsumer {

    private final ApplicationEventPublisher eventPublisher;

    @KafkaListener(
            topics = "${kafka.topics.recommendation-results}",
            groupId = "anime-tracker-group",
            containerFactory = "resultListenerFactory"
    )
    public void onResult(RecommendationResultEvent event) {
        log.info("Received recommendation result for user {}: {}", event.getTelegramId(), event.getStatus());
        eventPublisher.publishEvent(
                new RecommendationReadyEvent(this, event.getTelegramId(), event.getStatus()));
    }
}
