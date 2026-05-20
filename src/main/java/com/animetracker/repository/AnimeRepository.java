package com.animetracker.repository;

import com.animetracker.entity.Anime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnimeRepository extends JpaRepository<Anime, Long> {

    @Query(value = """
        SELECT * FROM anime.anime
        WHERE (LOWER(title) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(title_english) LIKE LOWER(CONCAT('%', :query, '%')))
        ORDER BY
            CASE WHEN score IS NOT NULL AND scored_by IS NOT NULL AND scored_by > 0
                 THEN score * LOG(scored_by::numeric) ELSE 0 END DESC
        """, nativeQuery = true)
    List<Anime> searchByTitle(@Param("query") String query);

    @Query(value = """
        SELECT a.* FROM anime.anime a
        INNER JOIN anime."animeGenres" ag ON a.id = ag.anime_id
        INNER JOIN anime.genre g ON ag.genre_id = g.id
        WHERE (:genre IS NULL OR LOWER(g.name) = LOWER(CAST(:genre AS TEXT)))
          AND (:type IS NULL OR LOWER(a.type) = LOWER(CAST(:type AS TEXT)))
          AND (:year IS NULL OR a.year = CAST(:year AS INTEGER))
        ORDER BY
            CASE WHEN a.score IS NOT NULL AND a.scored_by IS NOT NULL AND a.scored_by > 0
                 THEN a.score * LOG(a.scored_by::numeric) ELSE 0 END DESC
        """, nativeQuery = true)
    List<Anime> searchByFilters(
            @Param("genre") String genre,
            @Param("type") String type,
            @Param("year") Integer year
    );

    @Query(value = """
        SELECT * FROM anime.anime WHERE id IN :ids
        ORDER BY
            CASE WHEN score IS NOT NULL AND scored_by IS NOT NULL AND scored_by > 0
                 THEN score * LOG(scored_by::numeric) ELSE 0 END DESC
        """, nativeQuery = true)
    List<Anime> findByIdsOrderedByPopularity(@Param("ids") List<Long> ids);
}
