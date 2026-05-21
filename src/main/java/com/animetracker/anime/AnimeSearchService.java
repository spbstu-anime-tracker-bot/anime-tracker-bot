package com.animetracker.anime;

import com.animetracker.anime.internal.AnimeRepository;
import com.animetracker.anime.internal.GenreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AnimeSearchService {

    private static final Set<String> KNOWN_TYPES = Set.of(
            "tv", "movie", "ova", "ona", "special", "music"
    );

    private final AnimeRepository animeRepository;
    private final GenreRepository genreRepository;

    public List<Anime> searchByTitle(String query) {
        return animeRepository.searchByTitle(query.trim());
    }

    public SearchResult searchByFilters(String params) {
        String[] parts = Arrays.stream(params.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toArray(String[]::new);

        String genre = null;
        String type = null;
        Integer year = null;

        for (String part : parts) {
            if (isYear(part)) {
                year = Integer.parseInt(part);
            } else if (isType(part)) {
                type = part.toUpperCase();
            } else if (isGenre(part)) {
                genre = part;
            } else {
                return SearchResult.error("Введён несуществующий критерий — '" + part + "'");
            }
        }

        return SearchResult.success(animeRepository.searchByFilters(genre, type, year));
    }

    public List<Anime> findByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) return List.of();
        return animeRepository.findByIdsOrderedByPopularity(ids);
    }

    public List<Anime> findTopPopular(int limit) {
        return animeRepository.findTopPopular(limit);
    }

    private boolean isYear(String s) {
        try {
            int y = Integer.parseInt(s);
            return y >= 1900 && y <= 2030;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean isType(String s) {
        return KNOWN_TYPES.contains(s.toLowerCase());
    }

    private boolean isGenre(String s) {
        return genreRepository.findByNameIgnoreCase(s).isPresent();
    }

    public record SearchResult(List<Anime> anime, String errorMessage) {
        public static SearchResult success(List<Anime> anime) {
            return new SearchResult(anime, null);
        }

        public static SearchResult error(String message) {
            return new SearchResult(null, message);
        }

        public boolean isError() {
            return errorMessage != null;
        }
    }
}
