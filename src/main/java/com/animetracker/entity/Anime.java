package com.animetracker.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "anime", schema = "anime")
@Getter
@Setter
public class Anime {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "title")
    private String title;

    @Column(name = "title_english")
    private String titleEnglish;

    @Column(name = "type")
    private String type;

    @Column(name = "episodes")
    private Integer episodes;

    @Column(name = "status")
    private String status;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "year")
    private Integer year;

    @Column(name = "score")
    private Double score;

    @Column(name = "scored_by")
    private Long scoredBy;

    @Column(name = "popularity_rank")
    private Integer popularityRank;

    @Column(name = "synopsis", columnDefinition = "TEXT")
    private String synopsis;

    @OneToMany(mappedBy = "anime", fetch = FetchType.LAZY)
    private List<AnimeGenre> animeGenres;

    public double getPopularityMetric() {
        if (score == null || scoredBy == null || scoredBy <= 0) return 0.0;
        return score * Math.log10(scoredBy);
    }
}
