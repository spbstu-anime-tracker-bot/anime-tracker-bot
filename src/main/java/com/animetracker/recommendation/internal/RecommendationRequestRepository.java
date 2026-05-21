package com.animetracker.recommendation.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RecommendationRequestRepository extends JpaRepository<RecommendationRequest, Long> {

    Optional<RecommendationRequest> findTopByUserIdAndStatusOrderByCreatedAtDesc(Long userId, String status);

    boolean existsByUserIdAndStatus(Long userId, String status);
}
