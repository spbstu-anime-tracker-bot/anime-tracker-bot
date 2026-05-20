package com.animetracker.repository;

import com.animetracker.entity.RecommendationCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecommendationCacheRepository extends JpaRepository<RecommendationCache, Long> {

    List<RecommendationCache> findByUserIdOrderByRankPositionAsc(Long userId);

    void deleteByUserId(Long userId);

    boolean existsByUserId(Long userId);
}
