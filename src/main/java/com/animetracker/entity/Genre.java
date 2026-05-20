package com.animetracker.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "genre", schema = "anime")
@Getter
@Setter
public class Genre {

    @Id
    @Column(name = "id")
    private Long id;

    @Column(name = "name")
    private String name;
}
