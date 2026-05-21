package com.animetracker;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;




















class ModulithStructureTest {

    private final ApplicationModules modules = ApplicationModules.of(AnimeTrackerApplication.class);

    @Test
    void verifyModularity() {

        modules.verify();
    }

    @Test
    void printModuleStructure() {

        modules.forEach(System.out::println);
    }
}
