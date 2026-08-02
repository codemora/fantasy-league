package com.codemora.fantasy_league.fantasysquad.dto;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateFantasySquadRequest(
        @NotNull @Size(min = 15, max = 15) List<Long> playerIds) {
}
