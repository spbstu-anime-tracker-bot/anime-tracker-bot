package com.animetracker.module;

import com.animetracker.recommendation.RecommendationService;
import com.animetracker.tracking.TrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ManageUserListsModule {

    private final TrackingService trackingService;
    private final RecommendationService recommendationService;

    @Transactional
    public String addToViewed(Long userId, Long animeId) {
        String result = trackingService.addToViewed(userId, animeId);
        recommendationService.invalidateCache(userId);
        return result;
    }

    @Transactional
    public String removeFromViewed(Long userId, Long animeId) {
        String result = trackingService.removeFromViewed(userId, animeId);
        recommendationService.invalidateCache(userId);
        return result;
    }

    public String addToToView(Long userId, Long animeId) {
        return trackingService.addToToView(userId, animeId);
    }

    public String removeFromToView(Long userId, Long animeId) {
        return trackingService.removeFromToView(userId, animeId);
    }

    public boolean isInViewed(Long userId, Long animeId) {
        return trackingService.isInViewed(userId, animeId);
    }

    public boolean isInToView(Long userId, Long animeId) {
        return trackingService.isInToView(userId, animeId);
    }

    public Integer getUserScore(Long userId, Long animeId) {
        return trackingService.getUserScore(userId, animeId);
    }
}
