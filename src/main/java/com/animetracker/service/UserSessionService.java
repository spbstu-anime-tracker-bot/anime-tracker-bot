package com.animetracker.service;

import com.animetracker.bot.UserSession;
import com.animetracker.entity.Anime;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserSessionService {

    private final Map<Long, UserSession> sessions = new ConcurrentHashMap<>();

    public UserSession getOrCreate(Long userId) {
        return sessions.computeIfAbsent(userId, id -> new UserSession());
    }

    public UserSession get(Long userId) {
        return sessions.get(userId);
    }

    public void setResults(Long userId, List<Anime> results, Long chatId) {
        UserSession session = getOrCreate(userId);
        session.setResults(results);
        session.setCurrentPage(0);
        session.setChatId(chatId);
        session.getCardMessageIds().clear();
        session.setNavMessageId(null);
        session.setAwaitingRatingForAnimeId(null);
    }

    public void setAwaitingRating(Long userId, String animeId) {
        UserSession session = getOrCreate(userId);
        session.setAwaitingRatingForAnimeId(animeId);
    }

    public void clearRatingAwait(Long userId) {
        UserSession session = get(userId);
        if (session != null) session.setAwaitingRatingForAnimeId(null);
    }
}
