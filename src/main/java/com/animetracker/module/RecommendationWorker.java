package com.animetracker.module;

import com.animetracker.dto.RecommendationRequestEvent;
import com.animetracker.dto.RecommendationResultEvent;
import com.animetracker.entity.Anime;
import com.animetracker.entity.RecommendationCache;
import com.animetracker.entity.RecommendationRequest;
import com.animetracker.kafka.RecommendationProducer;
import com.animetracker.repository.AnimeRepository;
import com.animetracker.repository.ListViewedRepository;
import com.animetracker.repository.RecommendationCacheRepository;
import com.animetracker.repository.RecommendationRequestRepository;
import com.animetracker.service.OllamaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecommendationWorker {

    private final OllamaService ollamaService;
    private final ListViewedRepository listViewedRepository;
    private final AnimeRepository animeRepository;
    private final RecommendationCacheRepository cacheRepository;
    private final RecommendationRequestRepository requestRepository;
    private final RecommendationProducer producer;

    @KafkaListener(
            topics = "${kafka.topics.recommendation-requests}",
            groupId = "anime-tracker-worker",
            containerFactory = "requestListenerFactory"
    )
    @Transactional
    public void processRecommendation(RecommendationRequestEvent event) {
        Long userId = event.getTelegramId();
        log.info("Processing recommendation for user {}", userId);

        Optional<RecommendationRequest> reqOpt = requestRepository
                .findTopByUserIdAndStatusOrderByCreatedAtDesc(userId, "PROCESSING");

        try {
            List<Object[]> watchedAnime = listViewedRepository.findAllWatchedAnimeByUserId(userId);

            if (watchedAnime.isEmpty()) {
                sendResult(event.getRequestId(), userId, "FAILED");
                updateRequestStatus(reqOpt, "FAILED");
                return;
            }

            List<String> recommendedTitles = ollamaService.getRecommendations(watchedAnime);

            if (recommendedTitles.isEmpty()) {
                sendResult(event.getRequestId(), userId, "FAILED");
                updateRequestStatus(reqOpt, "FAILED");
                return;
            }

            
            List<Anime> matchedAnime = new ArrayList<>();
            for (String title : recommendedTitles) {
                List<Anime> found = animeRepository.searchByTitle(title);
                if (!found.isEmpty()) {
                    matchedAnime.add(found.get(0));
                }
            }

            if (matchedAnime.isEmpty()) {
                sendResult(event.getRequestId(), userId, "FAILED");
                updateRequestStatus(reqOpt, "FAILED");
                return;
            }

            
            cacheRepository.deleteByUserId(userId);

            long requestId = reqOpt.map(RecommendationRequest::getId).orElse(0L);
            for (int i = 0; i < matchedAnime.size(); i++) {
                RecommendationCache cache = new RecommendationCache();
                cache.setUserId(userId);
                cache.setRequestId(requestId);
                cache.setAnimeId(matchedAnime.get(i).getId());
                cache.setRankPosition(i + 1);
                cacheRepository.save(cache);
            }

            updateRequestStatus(reqOpt, "COMPLETED");
            sendResult(event.getRequestId(), userId, "COMPLETED");
            log.info("Recommendations saved for user {}", userId);

        } catch (Exception e) {
            log.error("Error processing recommendations for user {}: {}", userId, e.getMessage());
            updateRequestStatus(reqOpt, "FAILED");
            sendResult(event.getRequestId(), userId, "FAILED");
        }
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
