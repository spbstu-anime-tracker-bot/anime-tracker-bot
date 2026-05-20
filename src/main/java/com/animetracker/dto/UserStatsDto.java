package com.animetracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserStatsDto {
    private Long telegramId;
    private String name;
    private long viewedCount;
    private long toViewCount;
    private long ratedCount;
}
