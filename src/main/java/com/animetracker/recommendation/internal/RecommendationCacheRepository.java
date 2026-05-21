package com.animetracker.recommendation.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface RecommendationCacheRepository extends JpaRepository<RecommendationCache, Long> {

    List<RecommendationCache> findByUserIdOrderByRankPositionAsc(Long userId);

    @Transactional
    void deleteByUserId(Long userId);

    boolean existsByUserId(Long userId);
}
