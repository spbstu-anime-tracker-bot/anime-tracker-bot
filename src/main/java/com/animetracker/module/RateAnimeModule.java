package com.animetracker.module;

import com.animetracker.entity.ListViewed;
import com.animetracker.repository.ListViewedRepository;
import com.animetracker.repository.RecommendationCacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RateAnimeModule {

    private final ListViewedRepository listViewedRepository;
    private final RecommendationCacheRepository recommendationCacheRepository;

    @Transactional
    public String rate(Long userId, Long animeId, int score) {
        if (score < 1 || score > 10) return "Оценка должна быть от 1 до 10.";

        ListViewed entry = listViewedRepository.findByUserIdAndAnimeId(userId, animeId)
                .orElseGet(() -> {
                    ListViewed e = new ListViewed();
                    e.setUserId(userId);
                    e.setAnimeId(animeId);
                    return e;
                });

        entry.setUserScore(score);
        listViewedRepository.save(entry);

        
        if (recommendationCacheRepository.existsByUserId(userId)) {
            recommendationCacheRepository.deleteByUserId(userId);
            log.info("Invalidated recommendation cache for user {}", userId);
        }

        return "⭐ Оценка " + score + "/10 сохранена.";
    }
}
