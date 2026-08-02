package com.codemora.fantasy_league.fixture;

import java.util.Random;

import org.springframework.stereotype.Component;

/**
 * Pure and deterministic given the same ratings + Random -- team/player
 * ratings feed a Poisson-distributed goal count per side, per the README
 * Match Simulation section. Re-running with a Random seeded from the same
 * value (Fixture.simulationSeed) always reproduces the same score.
 */
@Component
public class MatchScoreSimulator {

    /** Roughly the average goals-per-team in a real match. */
    private static final double BASE_LAMBDA = 1.3;

    private static final double HOME_ADVANTAGE = 1.1;

    public MatchScore simulate(double homeAttack, double homeDefense, double awayAttack, double awayDefense, Random random) {
        double homeLambda = BASE_LAMBDA * (homeAttack / awayDefense) * HOME_ADVANTAGE;
        double awayLambda = BASE_LAMBDA * (awayAttack / homeDefense);
        return new MatchScore(samplePoisson(homeLambda, random), samplePoisson(awayLambda, random));
    }

    /** Knuth's algorithm: draws a Poisson-distributed integer with mean lambda. */
    private int samplePoisson(double lambda, Random random) {
        double threshold = Math.exp(-lambda);
        int k = 0;
        double p = 1.0;
        do {
            k++;
            p *= random.nextDouble();
        } while (p > threshold);
        return k - 1;
    }
}
