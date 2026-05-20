package com.animetracker.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationResultEvent {
    private String requestId;
    private Long telegramId;
    private String status; 
}
