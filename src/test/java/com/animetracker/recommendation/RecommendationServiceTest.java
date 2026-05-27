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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    private RecommendationCacheRepository cacheRepository;

    @Mock
    private RecommendationRequestRepository requestRepository;

    @Mock
    private AnimeSearchService animeSearchService;

    @Mock
    private TrackingService trackingService;

    @Mock
    private LlmService llmService;

    @Mock
    private RecommendationProducer producer;

    @InjectMocks
    private RecommendationService recommendationService;

    @Test
    void requestNewRecommendationsCreatesRequestAndSendsKafkaEvent() {
        List<String> savedStatuses = new ArrayList<>();
        when(requestRepository.save(any(RecommendationRequest.class)))
                .thenAnswer(invocation -> {
                    RecommendationRequest request = invocation.getArgument(0);
                    savedStatuses.add(request.getStatus());
                    return request;
                });

        boolean result = recommendationService.requestNewRecommendations(100L);

        assertTrue(result);
        verify(producer).sendRecommendationRequest(argThat(event ->
                event.getTelegramId().equals(100L)
                        && event.getRequestId() != null
                        && event.getType().equals("GENERATE_RECOMMENDATIONS")
        ));

        verify(requestRepository, org.mockito.Mockito.times(2)).save(any(RecommendationRequest.class));
        assertEquals(List.of("CREATED", "PROCESSING"), savedStatuses);
    }

    @Test
    void processRecommendationUsesLlmMatchesAnimeAndSavesCache() {
        RecommendationRequest request = new RecommendationRequest();
        request.setId(42L);
        request.setUserId(100L);
        request.setStatus("PROCESSING");

        RecommendationRequestEvent event = new RecommendationRequestEvent(
                "request-1",
                100L,
                LocalDateTime.now(),
                "GENERATE_RECOMMENDATIONS"
        );

        Anime naruto = anime(20L, "Naruto");
        Anime onePiece = anime(21L, "One Piece");

        when(requestRepository.findTopByUserIdAndStatusOrderByCreatedAtDesc(100L, "PROCESSING"))
                .thenReturn(Optional.of(request));
        when(trackingService.getWatchedAnimeData(100L))
                .thenReturn(List.<Object[]>of(new Object[]{1L, 10, "Cowboy Bebop"}));
        when(llmService.getRecommendations(any()))
                .thenReturn(List.of("Naruto", "One Piece"));
        when(animeSearchService.searchByTitle("Naruto")).thenReturn(List.of(naruto));
        when(animeSearchService.searchByTitle("One Piece")).thenReturn(List.of(onePiece));

        recommendationService.processRecommendation(event);

        verify(cacheRepository).deleteByUserId(100L);

        ArgumentCaptor<RecommendationCache> cacheCaptor =
                ArgumentCaptor.forClass(RecommendationCache.class);
        verify(cacheRepository, org.mockito.Mockito.times(2)).save(cacheCaptor.capture());

        List<RecommendationCache> cached = cacheCaptor.getAllValues();
        assertEquals(List.of(20L, 21L), cached.stream().map(RecommendationCache::getAnimeId).toList());
        assertEquals(List.of(1, 2), cached.stream().map(RecommendationCache::getRankPosition).toList());
        assertEquals(List.of(42L, 42L), cached.stream().map(RecommendationCache::getRequestId).toList());

        assertEquals("COMPLETED", request.getStatus());
        verify(requestRepository).save(request);
        verify(producer).sendRecommendationResult(argThat(result ->
                result.getRequestId().equals("request-1")
                        && result.getTelegramId().equals(100L)
                        && result.getStatus().equals("COMPLETED")
        ));
    }

    @Test
    void processRecommendationMarksRequestAsFailedWhenLlmReturnsNoTitles() {
        RecommendationRequest request = new RecommendationRequest();
        request.setId(43L);
        request.setUserId(100L);
        request.setStatus("PROCESSING");

        RecommendationRequestEvent event = new RecommendationRequestEvent(
                "request-2",
                100L,
                LocalDateTime.now(),
                "GENERATE_RECOMMENDATIONS"
        );

        when(requestRepository.findTopByUserIdAndStatusOrderByCreatedAtDesc(100L, "PROCESSING"))
                .thenReturn(Optional.of(request));
        when(trackingService.getWatchedAnimeData(100L))
                .thenReturn(List.<Object[]>of(new Object[]{1L, 10, "Cowboy Bebop"}));
        when(llmService.getRecommendations(any())).thenReturn(List.of());

        recommendationService.processRecommendation(event);

        assertEquals("FAILED", request.getStatus());
        verify(requestRepository).save(request);
        verify(producer).sendRecommendationResult(argThat(result ->
                result.getRequestId().equals("request-2")
                        && result.getTelegramId().equals(100L)
                        && result.getStatus().equals("FAILED")
        ));
    }

    private Anime anime(Long id, String title) {
        Anime anime = new Anime();
        anime.setId(id);
        anime.setTitle(title);
        return anime;
    }
}
