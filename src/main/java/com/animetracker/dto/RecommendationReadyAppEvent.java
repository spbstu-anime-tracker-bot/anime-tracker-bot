package com.animetracker.dto;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class RecommendationReadyAppEvent extends ApplicationEvent {
    private final Long telegramId;
    private final String status;

    public RecommendationReadyAppEvent(Object source, Long telegramId, String status) {
        super(source);
        this.telegramId = telegramId;
        this.status = status;
    }
}
