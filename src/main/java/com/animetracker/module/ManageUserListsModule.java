package com.animetracker.module;

import com.animetracker.entity.ListToView;
import com.animetracker.entity.ListViewed;
import com.animetracker.repository.ListToViewRepository;
import com.animetracker.repository.ListViewedRepository;
import com.animetracker.repository.RecommendationCacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ManageUserListsModule {

    private final ListViewedRepository listViewedRepository;
    private final ListToViewRepository listToViewRepository;
    private final RecommendationCacheRepository recommendationCacheRepository;

    @Transactional
    public String addToViewed(Long userId, Long animeId) {
        if (listViewedRepository.existsByUserIdAndAnimeId(userId, animeId)) {
            return "Аниме уже в списке просмотренных.";
        }
        ListViewed entry = new ListViewed();
        entry.setUserId(userId);
        entry.setAnimeId(animeId);
        listViewedRepository.save(entry);
        listToViewRepository.findByUserIdAndAnimeId(userId, animeId)
                .ifPresent(listToViewRepository::delete);
        if (recommendationCacheRepository.existsByUserId(userId)) {
            recommendationCacheRepository.deleteByUserId(userId);
            log.info("Invalidated recommendation cache for user {} after adding to viewed", userId);
        }
        return "✅ Добавлено в просмотренные.";
    }

    @Transactional
    public String removeFromViewed(Long userId, Long animeId) {
        return listViewedRepository.findByUserIdAndAnimeId(userId, animeId).map(entry -> {
            listViewedRepository.delete(entry);
            return "🗑 Удалено из просмотренных.";
        }).orElse("Аниме не найдено в списке просмотренных.");
    }

    @Transactional
    public String addToToView(Long userId, Long animeId) {
        if (listViewedRepository.existsByUserIdAndAnimeId(userId, animeId)) {
            return "Аниме уже в списке просмотренных. Сначала удалите его оттуда.";
        }
        if (listToViewRepository.existsByUserIdAndAnimeId(userId, animeId)) {
            return "Аниме уже в списке отслеживаемых.";
        }
        ListToView entry = new ListToView();
        entry.setUserId(userId);
        entry.setAnimeId(animeId);
        listToViewRepository.save(entry);
        return "📌 Добавлено в отслеживаемые.";
    }

    @Transactional
    public String removeFromToView(Long userId, Long animeId) {
        return listToViewRepository.findByUserIdAndAnimeId(userId, animeId).map(entry -> {
            listToViewRepository.delete(entry);
            return "🗑 Удалено из отслеживаемых.";
        }).orElse("Аниме не найдено в списке отслеживаемых.");
    }

    public boolean isInViewed(Long userId, Long animeId) {
        return listViewedRepository.existsByUserIdAndAnimeId(userId, animeId);
    }

    public boolean isInToView(Long userId, Long animeId) {
        return listToViewRepository.existsByUserIdAndAnimeId(userId, animeId);
    }

    public Integer getUserScore(Long userId, Long animeId) {
        return listViewedRepository.findByUserIdAndAnimeId(userId, animeId)
                .map(lv -> lv.getUserScore())
                .orElse(null);
    }
}
