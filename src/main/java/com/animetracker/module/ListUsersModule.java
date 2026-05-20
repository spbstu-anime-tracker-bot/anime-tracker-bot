package com.animetracker.module;

import com.animetracker.dto.UserStatsDto;
import com.animetracker.entity.User;
import com.animetracker.repository.ListToViewRepository;
import com.animetracker.repository.ListViewedRepository;
import com.animetracker.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ListUsersModule {

    private final UserRepository userRepository;
    private final ListViewedRepository listViewedRepository;
    private final ListToViewRepository listToViewRepository;

    public List<UserStatsDto> getAllUsersStats() {
        return userRepository.findAll().stream().map(user -> {
            long viewedCount = listViewedRepository.countByUserId(user.getId());
            long toViewCount = listToViewRepository.countByUserId(user.getId());
            long ratedCount = listViewedRepository.countRatedByUserId(user.getId());
            return new UserStatsDto(user.getId(), user.getName(), viewedCount, toViewCount, ratedCount);
        }).collect(Collectors.toList());
    }
}
