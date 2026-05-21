package com.animetracker.llm;

import java.util.List;


public interface LlmService {

    List<String> getRecommendations(List<Object[]> watchedAnime);

    boolean isAvailable();
}
