package com.animetracker.bot;

import com.animetracker.anime.Anime;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class UserSession {
    private List<Anime> results = new ArrayList<>();
    private int currentPage = 0;
    private List<Integer> cardMessageIds = new ArrayList<>();
    private Integer navMessageId;
    private Long chatId;
    private String awaitingRatingForAnimeId;

    public static final int PAGE_SIZE = 5;

    public int getTotalPages() {
        return (int) Math.ceil((double) results.size() / PAGE_SIZE);
    }

    public List<Anime> getCurrentPageItems() {
        int from = currentPage * PAGE_SIZE;
        int to = Math.min(from + PAGE_SIZE, results.size());
        if (from >= results.size()) return List.of();
        return results.subList(from, to);
    }

    public boolean hasPrevPage() {
        return currentPage > 0;
    }

    public boolean hasNextPage() {
        return currentPage < getTotalPages() - 1;
    }
}
