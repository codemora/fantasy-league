package com.codemora.fantasy_league.lineup;

import java.util.Map;

import com.codemora.fantasy_league.common.Position;

/**
 * Per the README Squad Rules: exactly 11 starters (1 GK, 3-5 DEF, 2-5 MID,
 * 1-3 FWD) and 4 bench. Shared between lineup submission validation and
 * gameweek-scoring auto-substitution (#39), which both need to know whether a
 * given starting XI shape is legal.
 */
public final class Formation {

    public static final int STARTER_COUNT = 11;
    public static final int BENCH_COUNT = 4;

    /** [min, max] starters allowed per position. */
    public static final Map<Position, int[]> RANGE = Map.of(
            Position.GK, new int[] {1, 1},
            Position.DEF, new int[] {3, 5},
            Position.MID, new int[] {2, 5},
            Position.FWD, new int[] {1, 3});

    private Formation() {
    }
}
