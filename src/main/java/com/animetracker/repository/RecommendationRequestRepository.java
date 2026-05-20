package com.animetracker.repository;

import com.animetracker.entity.RecommendationRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RecommendationRequestRepository extends JpaRepository<RecommendationRequest, Long> {

    Optional<RecommendationRequest> findTopByUserIdAndStatusOrderByCreatedAtDesc(Long userId, String status);

    Optional<RecommendationRequest> findTopByUserIdOrderByCreatedAtDesc(Long userId);
}
