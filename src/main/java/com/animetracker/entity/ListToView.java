package com.animetracker.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "listToView", schema = "user")
@Getter
@Setter
public class ListToView {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "anime_id", nullable = false)
    private Long animeId;

    @Column(name = "added_at")
    private LocalDateTime addedAt;

    @PrePersist
    void prePersist() {
        if (addedAt == null) addedAt = LocalDateTime.now();
    }
}
