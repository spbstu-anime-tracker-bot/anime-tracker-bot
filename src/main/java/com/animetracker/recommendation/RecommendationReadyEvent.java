package com.animetracker.recommendation;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;


@Getter
public class RecommendationReadyEvent extends ApplicationEvent {

    private final Long telegramId;
    private final String status;

    public RecommendationReadyEvent(Object source, Long telegramId, String status) {
        super(source);
        this.telegramId = telegramId;
        this.status = status;
    }
}
