package com.animetracker.module;

import com.animetracker.anime.Anime;
import com.animetracker.recommendation.RecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdviseModule {

    private final RecommendationService recommendationService;

    public boolean hasCachedRecommendations(Long userId) {
        return recommendationService.hasCachedRecommendations(userId);
    }

    public List<Anime> getCachedRecommendations(Long userId) {
        return recommendationService.getCachedRecommendations(userId);
    }

    public boolean hasWatchedAnime(Long userId) {
        return recommendationService.hasWatchedAnime(userId);
    }

    public boolean isAlreadyProcessing(Long userId) {
        return recommendationService.isAlreadyProcessing(userId);
    }

    public List<Anime> getPopularFallback() {
        return recommendationService.getPopularFallback();
    }

    public boolean requestNewRecommendations(Long userId) {
        return recommendationService.requestNewRecommendations(userId);
    }
}
