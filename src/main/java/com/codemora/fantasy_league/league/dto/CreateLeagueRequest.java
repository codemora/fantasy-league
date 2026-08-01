package com.codemora.fantasy_league.league.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateLeagueRequest(@NotBlank @Size(max = 128) String name) {
}
