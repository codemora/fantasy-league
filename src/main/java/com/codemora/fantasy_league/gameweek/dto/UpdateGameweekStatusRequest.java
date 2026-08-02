package com.codemora.fantasy_league.gameweek.dto;

import com.codemora.fantasy_league.gameweek.GameweekStatus;

import jakarta.validation.constraints.NotNull;

public record UpdateGameweekStatusRequest(@NotNull GameweekStatus status) {
}
