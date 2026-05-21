package com.animetracker.module;

import com.animetracker.anime.Anime;
import com.animetracker.anime.AnimeSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SearchAnimeModule {

    private final AnimeSearchService animeSearchService;

    public List<Anime> searchByTitle(String query) {
        return animeSearchService.searchByTitle(query);
    }

    public SearchResult searchByFilters(String params) {
        AnimeSearchService.SearchResult result = animeSearchService.searchByFilters(params);
        return new SearchResult(result.anime(), result.errorMessage());
    }

    public record SearchResult(List<Anime> anime, String errorMessage) {
        public boolean isError() {
            return errorMessage != null;
        }
    }
}
