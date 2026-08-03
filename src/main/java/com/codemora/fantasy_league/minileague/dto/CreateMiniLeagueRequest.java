package com.codemora.fantasy_league.minileague.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateMiniLeagueRequest(@NotBlank @Size(max = 128) String name) {
}
