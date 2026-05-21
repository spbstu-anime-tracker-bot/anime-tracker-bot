package com.animetracker.tracking.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ListViewedRepository extends JpaRepository<ListViewed, Long> {

    List<ListViewed> findByUserId(Long userId);

    Optional<ListViewed> findByUserIdAndAnimeId(Long userId, Long animeId);

    boolean existsByUserIdAndAnimeId(Long userId, Long animeId);

    @Query("SELECT COUNT(lv) FROM ListViewed lv WHERE lv.userId = :userId AND lv.userScore IS NOT NULL")
    long countRatedByUserId(@Param("userId") Long userId);

    long countByUserId(Long userId);

    @Query(value = """
        SELECT lv.anime_id, lv.user_score, a.title FROM "user"."listViewed" lv
        JOIN anime.anime a ON lv.anime_id = a.id
        WHERE lv.user_id = :userId
        """, nativeQuery = true)
    List<Object[]> findAllWatchedAnimeByUserId(@Param("userId") Long userId);
}
