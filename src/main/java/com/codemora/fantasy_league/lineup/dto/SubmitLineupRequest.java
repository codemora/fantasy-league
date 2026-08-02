package com.codemora.fantasy_league.lineup.dto;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SubmitLineupRequest(
        @NotNull @Size(min = 11, max = 11) List<Long> starterPlayerIds,
        @NotNull @Size(min = 4, max = 4) List<Long> benchPlayerIds,
        @NotNull Long captainPlayerId) {
}
