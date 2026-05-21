package com.animetracker.recommendation;

import com.animetracker.anime.Anime;
import com.animetracker.anime.AnimeSearchService;
import com.animetracker.llm.LlmService;
import com.animetracker.recommendation.internal.RecommendationCache;
import com.animetracker.recommendation.internal.RecommendationCacheRepository;
import com.animetracker.recommendation.internal.RecommendationProducer;
import com.animetracker.recommendation.internal.RecommendationRequest;
import com.animetracker.recommendation.internal.RecommendationRequestRepository;
import com.animetracker.tracking.TrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private static final int POPULAR_FALLBACK_SIZE = 10;

    private final RecommendationCacheRepository cacheRepository;
    private final RecommendationRequestRepository requestRepository;
    private final AnimeSearchService animeSearchService;
    private final TrackingService trackingService;
    private final LlmService llmService;
    private final RecommendationProducer producer;

    public boolean hasCachedRecommendations(Long userId) {
        return cacheRepository.existsByUserId(userId);
    }

    public List<Anime> getCachedRecommendations(Long userId) {
        List<RecommendationCache> cached = cacheRepository.findByUserIdOrderByRankPositionAsc(userId);
        if (cached.isEmpty()) return null;
        List<Long> ids = cached.stream().map(RecommendationCache::getAnimeId).collect(Collectors.toList());
        return animeSearchService.findByIds(ids);
    }

    public boolean hasWatchedAnime(Long userId) {
        return trackingService.hasWatchedAnime(userId);
    }

    public boolean isAlreadyProcessing(Long userId) {
        return requestRepository.existsByUserIdAndStatus(userId, "PROCESSING");
    }

    public List<Anime> getPopularFallback() {
        return animeSearchService.findTopPopular(POPULAR_FALLBACK_SIZE);
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

    @Transactional
    public void processRecommendation(RecommendationRequestEvent event) {
        Long userId = event.getTelegramId();
        log.info("Processing recommendation for user {}", userId);

        Optional<RecommendationRequest> reqOpt = requestRepository
                .findTopByUserIdAndStatusOrderByCreatedAtDesc(userId, "PROCESSING");

        try {
            List<Object[]> watchedAnime = trackingService.getWatchedAnimeData(userId);

            if (watchedAnime.isEmpty()) {
                fail(event, reqOpt);
                return;
            }

            List<String> recommendedTitles = llmService.getRecommendations(watchedAnime);

            if (recommendedTitles.isEmpty()) {
                fail(event, reqOpt);
                return;
            }

            List<Anime> matchedAnime = new ArrayList<>();
            for (String title : recommendedTitles) {
                List<Anime> found = animeSearchService.searchByTitle(title);
                if (!found.isEmpty()) {
                    matchedAnime.add(found.get(0));
                }
            }

            if (matchedAnime.isEmpty()) {
                fail(event, reqOpt);
                return;
            }

            cacheRepository.deleteByUserId(userId);
            long requestDbId = reqOpt.map(RecommendationRequest::getId).orElse(0L);
            for (int i = 0; i < matchedAnime.size(); i++) {
                RecommendationCache cache = new RecommendationCache();
                cache.setUserId(userId);
                cache.setRequestId(requestDbId);
                cache.setAnimeId(matchedAnime.get(i).getId());
                cache.setRankPosition(i + 1);
                cacheRepository.save(cache);
            }

            updateRequestStatus(reqOpt, "COMPLETED");
            sendResult(event.getRequestId(), userId, "COMPLETED");
            log.info("Recommendations saved for user {}", userId);
        } catch (Exception e) {
            log.error("Error processing recommendations for user {}: {}", userId, e.getMessage());
            fail(event, reqOpt);
        }
    }

    @Transactional
    public void invalidateCache(Long userId) {
        if (cacheRepository.existsByUserId(userId)) {
            cacheRepository.deleteByUserId(userId);
            log.info("Invalidated recommendation cache for user {}", userId);
        }
    }

    private void fail(RecommendationRequestEvent event, Optional<RecommendationRequest> reqOpt) {
        updateRequestStatus(reqOpt, "FAILED");
        sendResult(event.getRequestId(), event.getTelegramId(), "FAILED");
    }

    private void sendResult(String requestId, Long userId, String status) {
        producer.sendRecommendationResult(new RecommendationResultEvent(requestId, userId, status));
    }

    private void updateRequestStatus(Optional<RecommendationRequest> reqOpt, String status) {
        reqOpt.ifPresent(req -> {
            req.setStatus(status);
            requestRepository.save(req);
        });
    }
}
