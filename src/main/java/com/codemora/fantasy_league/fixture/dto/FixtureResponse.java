package com.codemora.fantasy_league.fixture.dto;

import java.time.LocalDateTime;

public record FixtureResponse(
        Long id,
        Long seasonId,
        Long gameweekId,
        Long homeTeamId,
        Long awayTeamId,
        Integer homeTeamScore,
        Integer awayTeamScore,
        boolean played,
        LocalDateTime startDateTime) {
}
