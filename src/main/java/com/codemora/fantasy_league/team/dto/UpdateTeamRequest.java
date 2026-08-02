package com.codemora.fantasy_league.team.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateTeamRequest(
        @NotBlank @Size(max = 128) String name,
        @Size(max = 255) String slogan) {
}
