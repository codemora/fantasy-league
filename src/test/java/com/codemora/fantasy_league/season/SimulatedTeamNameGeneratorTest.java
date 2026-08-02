package com.codemora.fantasy_league.season;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

class SimulatedTeamNameGeneratorTest {

    private final SimulatedTeamNameGenerator generator = new SimulatedTeamNameGenerator();

    @Test
    void candidateNamesAreAllUniqueAndFarExceedAnyRealisticTeamLimit() {
        List<String> names = generator.candidateNames();
        Set<String> unique = new HashSet<>(names);

        assertThat(unique).hasSize(names.size());
        assertThat(names.size()).isGreaterThan(100);
    }

    @Test
    void candidateNamesAreNonBlank() {
        assertThat(generator.candidateNames()).allSatisfy(name -> assertThat(name).isNotBlank());
    }
}
