package com.animetracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
@EnableKafka
public class AnimeTrackerApplication {
    public static void main(String[] args) {
        SpringApplication.run(AnimeTrackerApplication.class, args);
    }
}
