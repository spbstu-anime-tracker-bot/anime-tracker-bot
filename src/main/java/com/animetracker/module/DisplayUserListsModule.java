package com.animetracker.module;

import com.animetracker.anime.Anime;
import com.animetracker.tracking.TrackingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DisplayUserListsModule {

    private final TrackingService trackingService;

    public List<Anime> getViewedList(Long userId) {
        return trackingService.getViewedList(userId);
    }

    public List<Anime> getToViewList(Long userId) {
        return trackingService.getToViewList(userId);
    }
}
