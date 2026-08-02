package com.codemora.fantasy_league.fixture;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Random;

import org.junit.jupiter.api.Test;

class MatchScoreSimulatorTest {

    private final MatchScoreSimulator simulator = new MatchScoreSimulator();

    @Test
    void sameSeedProducesAnIdenticalScore() {
        MatchScore first = simulator.simulate(70, 50, 60, 55, new Random(42L));
        MatchScore second = simulator.simulate(70, 50, 60, 55, new Random(42L));

        assertThat(first).isEqualTo(second);
    }

    @Test
    void differentSeedsCanProduceDifferentScores() {
        MatchScore first = simulator.simulate(70, 50, 60, 55, new Random(1L));
        MatchScore second = simulator.simulate(70, 50, 60, 55, new Random(999999L));

        // Not a strict guarantee for any two seeds, but true often enough that a
        // failure here is a useful smoke signal something's badly wrong (e.g.
        // the Random isn't actually being consumed).
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void goalsAreNeverNegative() {
        for (long seed = 0; seed < 200; seed++) {
            MatchScore score = simulator.simulate(65, 65, 65, 65, new Random(seed));
            assertThat(score.homeGoals()).isGreaterThanOrEqualTo(0);
            assertThat(score.awayGoals()).isGreaterThanOrEqualTo(0);
        }
    }

    @Test
    void strongerAttackAgainstWeakerDefenseScoresMoreOnAverage() {
        double strongTotal = 0;
        double weakTotal = 0;
        int trials = 500;
        for (long seed = 0; seed < trials; seed++) {
            strongTotal += simulator.simulate(120, 65, 65, 65, new Random(seed)).homeGoals();
            weakTotal += simulator.simulate(40, 65, 65, 65, new Random(seed)).homeGoals();
        }

        assertThat(strongTotal / trials).isGreaterThan(weakTotal / trials);
    }
}
