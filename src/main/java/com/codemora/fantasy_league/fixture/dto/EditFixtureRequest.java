package com.codemora.fantasy_league.fixture.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;

public record EditFixtureRequest(@NotNull LocalDateTime startDateTime) {
}
