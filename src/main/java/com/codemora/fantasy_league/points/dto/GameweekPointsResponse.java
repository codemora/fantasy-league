package com.codemora.fantasy_league.points.dto;

import java.util.List;

import com.codemora.fantasy_league.chip.ChipType;

public record GameweekPointsResponse(
        Long gameweekId,
        Integer gameweekNumber,
        List<PlayerPointsResponse> players,
        int playerPoints,
        int transferPointsCost,
        int totalPoints,
        /** False until the gameweek reaches COMPLETE; until then these points can still move (#36). */
        boolean official,
        /** Null unless WILDCARD, TRIPLE_CAPTAIN, or BENCH_BOOST was played for this gameweek (#40). */
        ChipType activeChip) {
}
