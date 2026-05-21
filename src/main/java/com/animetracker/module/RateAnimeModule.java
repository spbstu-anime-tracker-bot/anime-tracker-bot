package com.animetracker.module;

import com.animetracker.recommendation.RecommendationService;
import com.animetracker.tracking.TrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RateAnimeModule {

    private final TrackingService trackingService;
    private final RecommendationService recommendationService;

    @Transactional
    public String rate(Long userId, Long animeId, int score) {
        String result = trackingService.rate(userId, animeId, score);
        recommendationService.invalidateCache(userId);
        return result;
    }
}
