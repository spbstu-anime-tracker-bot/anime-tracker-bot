package com.animetracker.tracking.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ListToViewRepository extends JpaRepository<ListToView, Long> {

    List<ListToView> findByUserId(Long userId);

    Optional<ListToView> findByUserIdAndAnimeId(Long userId, Long animeId);

    boolean existsByUserIdAndAnimeId(Long userId, Long animeId);

    long countByUserId(Long userId);
}
