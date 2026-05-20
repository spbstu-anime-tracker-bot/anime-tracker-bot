package com.animetracker.module;

import com.animetracker.entity.Anime;
import com.animetracker.entity.ListToView;
import com.animetracker.entity.ListViewed;
import com.animetracker.repository.AnimeRepository;
import com.animetracker.repository.ListToViewRepository;
import com.animetracker.repository.ListViewedRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DisplayUserListsModule {

    private final ListViewedRepository listViewedRepository;
    private final ListToViewRepository listToViewRepository;
    private final AnimeRepository animeRepository;

    public List<Anime> getViewedList(Long userId) {
        List<Long> ids = listViewedRepository.findByUserId(userId)
                .stream().map(ListViewed::getAnimeId).collect(Collectors.toList());
        if (ids.isEmpty()) return List.of();
        return animeRepository.findByIdsOrderedByPopularity(ids);
    }

    public List<Anime> getToViewList(Long userId) {
        List<Long> ids = listToViewRepository.findByUserId(userId)
                .stream().map(ListToView::getAnimeId).collect(Collectors.toList());
        if (ids.isEmpty()) return List.of();
        return animeRepository.findByIdsOrderedByPopularity(ids);
    }
}
