package com.animetracker.tracking;

import com.animetracker.anime.Anime;
import com.animetracker.anime.AnimeSearchService;
import com.animetracker.tracking.internal.ListToView;
import com.animetracker.tracking.internal.ListToViewRepository;
import com.animetracker.tracking.internal.ListViewed;
import com.animetracker.tracking.internal.ListViewedRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TrackingService {

    private final ListViewedRepository listViewedRepository;
    private final ListToViewRepository listToViewRepository;
    private final AnimeSearchService animeSearchService;

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

    @Transactional
    public String rate(Long userId, Long animeId, int score) {
        if (score < 1 || score > 10) return "Оценка должна быть от 1 до 10.";
        ListViewed entry = listViewedRepository.findByUserIdAndAnimeId(userId, animeId)
                .orElseGet(() -> {
                    ListViewed e = new ListViewed();
                    e.setUserId(userId);
                    e.setAnimeId(animeId);
                    return e;
                });
        entry.setUserScore(score);
        listViewedRepository.save(entry);
        return "⭐ Оценка " + score + "/10 сохранена.";
    }

    public boolean isInViewed(Long userId, Long animeId) {
        return listViewedRepository.existsByUserIdAndAnimeId(userId, animeId);
    }

    public boolean isInToView(Long userId, Long animeId) {
        return listToViewRepository.existsByUserIdAndAnimeId(userId, animeId);
    }

    public Integer getUserScore(Long userId, Long animeId) {
        return listViewedRepository.findByUserIdAndAnimeId(userId, animeId)
                .map(ListViewed::getUserScore)
                .orElse(null);
    }

    public List<Anime> getViewedList(Long userId) {
        List<Long> ids = listViewedRepository.findByUserId(userId)
                .stream().map(ListViewed::getAnimeId).collect(Collectors.toList());
        return animeSearchService.findByIds(ids);
    }

    public List<Anime> getToViewList(Long userId) {
        List<Long> ids = listToViewRepository.findByUserId(userId)
                .stream().map(ListToView::getAnimeId).collect(Collectors.toList());
        return animeSearchService.findByIds(ids);
    }

    public boolean hasWatchedAnime(Long userId) {
        return listViewedRepository.countByUserId(userId) > 0;
    }

    public long countViewed(Long userId) {
        return listViewedRepository.countByUserId(userId);
    }

    public long countToView(Long userId) {
        return listToViewRepository.countByUserId(userId);
    }

    public long countRated(Long userId) {
        return listViewedRepository.countRatedByUserId(userId);
    }

    public List<Object[]> getWatchedAnimeData(Long userId) {
        return listViewedRepository.findAllWatchedAnimeByUserId(userId);
    }
}
