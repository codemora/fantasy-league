package com.codemora.fantasy_league.player;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class PlayerNameGeneratorTest {

    private final PlayerNameGenerator generator = new PlayerNameGenerator();

    @Test
    void candidateNamesFarExceedTheFifteenPlayersASquadNeeds() {
        List<String> names = generator.candidateNames();

        assertThat(names.size()).isGreaterThan(15);
    }

    @Test
    void candidateNamesAreNonBlank() {
        assertThat(generator.candidateNames()).allSatisfy(name -> assertThat(name).isNotBlank());
    }
}
