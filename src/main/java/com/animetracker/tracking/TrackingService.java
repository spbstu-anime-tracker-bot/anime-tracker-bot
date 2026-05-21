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
            return "РђРЅРёРјРµ СѓР¶Рµ РІ СЃРїРёСЃРєРµ РїСЂРѕСЃРјРѕС‚СЂРµРЅРЅС‹С….";
        }
        ListViewed entry = new ListViewed();
        entry.setUserId(userId);
        entry.setAnimeId(animeId);
        listViewedRepository.save(entry);
        listToViewRepository.findByUserIdAndAnimeId(userId, animeId)
                .ifPresent(listToViewRepository::delete);
        return "вњ… Р”РѕР±Р°РІР»РµРЅРѕ РІ РїСЂРѕСЃРјРѕС‚СЂРµРЅРЅС‹Рµ.";
    }

    @Transactional
    public String removeFromViewed(Long userId, Long animeId) {
        return listViewedRepository.findByUserIdAndAnimeId(userId, animeId).map(entry -> {
            listViewedRepository.delete(entry);
            return "рџ—‘ РЈРґР°Р»РµРЅРѕ РёР· РїСЂРѕСЃРјРѕС‚СЂРµРЅРЅС‹С….";
        }).orElse("РђРЅРёРјРµ РЅРµ РЅР°Р№РґРµРЅРѕ РІ СЃРїРёСЃРєРµ РїСЂРѕСЃРјРѕС‚СЂРµРЅРЅС‹С….");
    }

    @Transactional
    public String addToToView(Long userId, Long animeId) {
        if (listViewedRepository.existsByUserIdAndAnimeId(userId, animeId)) {
            return "РђРЅРёРјРµ СѓР¶Рµ РІ СЃРїРёСЃРєРµ РїСЂРѕСЃРјРѕС‚СЂРµРЅРЅС‹С…. РЎРЅР°С‡Р°Р»Р° СѓРґР°Р»РёС‚Рµ РµРіРѕ РѕС‚С‚СѓРґР°.";
        }
        if (listToViewRepository.existsByUserIdAndAnimeId(userId, animeId)) {
            return "РђРЅРёРјРµ СѓР¶Рµ РІ СЃРїРёСЃРєРµ РѕС‚СЃР»РµР¶РёРІР°РµРјС‹С….";
        }
        ListToView entry = new ListToView();
        entry.setUserId(userId);
        entry.setAnimeId(animeId);
        listToViewRepository.save(entry);
        return "рџ“Њ Р”РѕР±Р°РІР»РµРЅРѕ РІ РѕС‚СЃР»РµР¶РёРІР°РµРјС‹Рµ.";
    }

    @Transactional
    public String removeFromToView(Long userId, Long animeId) {
        return listToViewRepository.findByUserIdAndAnimeId(userId, animeId).map(entry -> {
            listToViewRepository.delete(entry);
            return "рџ—‘ РЈРґР°Р»РµРЅРѕ РёР· РѕС‚СЃР»РµР¶РёРІР°РµРјС‹С….";
        }).orElse("РђРЅРёРјРµ РЅРµ РЅР°Р№РґРµРЅРѕ РІ СЃРїРёСЃРєРµ РѕС‚СЃР»РµР¶РёРІР°РµРјС‹С….");
    }

    @Transactional
    public String rate(Long userId, Long animeId, int score) {
        if (score < 1 || score > 10) return "РћС†РµРЅРєР° РґРѕР»Р¶РЅР° Р±С‹С‚СЊ РѕС‚ 1 РґРѕ 10.";
        ListViewed entry = listViewedRepository.findByUserIdAndAnimeId(userId, animeId)
                .orElseGet(() -> {
                    ListViewed e = new ListViewed();
                    e.setUserId(userId);
                    e.setAnimeId(animeId);
                    return e;
                });
        entry.setUserScore(score);
        listViewedRepository.save(entry);
        return "в­ђ РћС†РµРЅРєР° " + score + "/10 СЃРѕС…СЂР°РЅРµРЅР°.";
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
