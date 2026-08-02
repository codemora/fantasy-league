package com.codemora.fantasy_league.fixture.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AddFixtureResultRequest(
        @NotNull @Min(0) Integer homeTeamScore,
        @NotNull @Min(0) Integer awayTeamScore) {
}
