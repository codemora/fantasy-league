package com.codemora.fantasy_league.season.dto;

import jakarta.validation.constraints.NotNull;

public record AddSeasonEntrantRequest(@NotNull Long teamId) {
}
