package com.animetracker.module;

import com.animetracker.recommendation.RecommendationRequestEvent;
import com.animetracker.recommendation.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RecommendationWorker {

    private final RecommendationService recommendationService;

    @KafkaListener(
            topics = "${kafka.topics.recommendation-requests}",
            groupId = "anime-tracker-worker",
            containerFactory = "requestListenerFactory"
    )
    public void processRecommendation(RecommendationRequestEvent event) {
        recommendationService.processRecommendation(event);
    }
}
