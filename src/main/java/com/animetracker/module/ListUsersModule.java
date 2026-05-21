package com.animetracker.module;

import com.animetracker.admin.UserStatsDto;
import com.animetracker.tracking.TrackingService;
import com.animetracker.user.UserRegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ListUsersModule {

    private final UserRegistrationService userRegistrationService;
    private final TrackingService trackingService;

    public List<UserStatsDto> getAllUsersStats() {
        return userRegistrationService.findAll().stream()
                .map(user -> new UserStatsDto(
                        user.getId(),
                        user.getName(),
                        trackingService.countViewed(user.getId()),
                        trackingService.countToView(user.getId()),
                        trackingService.countRated(user.getId())
                ))
                .toList();
    }
}
