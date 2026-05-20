package com.animetracker.module;

import com.animetracker.dto.RecommendationRequestEvent;
import com.animetracker.entity.Anime;
import com.animetracker.entity.RecommendationCache;
import com.animetracker.entity.RecommendationRequest;
import com.animetracker.kafka.RecommendationProducer;
import com.animetracker.repository.AnimeRepository;
import com.animetracker.repository.ListViewedRepository;
import com.animetracker.repository.RecommendationCacheRepository;
import com.animetracker.repository.RecommendationRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdviseModule {

    private static final int POPULAR_FALLBACK_SIZE = 10;

    private final RecommendationCacheRepository cacheRepository;
    private final RecommendationRequestRepository requestRepository;
    private final AnimeRepository animeRepository;
    private final ListViewedRepository listViewedRepository;
    private final RecommendationProducer producer;

    public boolean hasCachedRecommendations(Long userId) {
        return cacheRepository.existsByUserId(userId);
    }

    public List<Anime> getCachedRecommendations(Long userId) {
        List<RecommendationCache> cached = cacheRepository.findByUserIdOrderByRankPositionAsc(userId);
        if (cached.isEmpty()) return null;
        List<Long> ids = cached.stream().map(RecommendationCache::getAnimeId).collect(Collectors.toList());
        return animeRepository.findByIdsOrderedByPopularity(ids);
    }

    public boolean hasWatchedAnime(Long userId) {
        return listViewedRepository.countByUserId(userId) > 0;
    }

    public boolean isAlreadyProcessing(Long userId) {
        return requestRepository.existsByUserIdAndStatus(userId, "PROCESSING");
    }

    public List<Anime> getPopularFallback() {
        return animeRepository.findTopPopular(POPULAR_FALLBACK_SIZE);
    }

    @Transactional
    public boolean requestNewRecommendations(Long userId) {
        RecommendationRequest req = new RecommendationRequest();
        req.setUserId(userId);
        req.setStatus("CREATED");
        requestRepository.save(req);

        String requestId = UUID.randomUUID().toString();
        try {
            RecommendationRequestEvent event = new RecommendationRequestEvent(
                    requestId, userId, LocalDateTime.now(), "GENERATE_RECOMMENDATIONS"
            );
            producer.sendRecommendationRequest(event);
            req.setStatus("PROCESSING");
            requestRepository.save(req);
            return true;
        } catch (Exception e) {
            log.error("Failed to send to Kafka: {}", e.getMessage());
            req.setStatus("FAILED_TO_SEND");
            requestRepository.save(req);
            return false;
        }
    }
}
