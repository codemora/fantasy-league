package com.codemora.fantasy_league.season.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record UpdateSeasonRequest(
        @NotBlank @Size(max = 32) String period,
        @NotNull @Positive Integer teamLimit,
        @NotNull @Positive Integer startingBudget,
        boolean doubleLeg,
        LocalDate startDate,
        LocalDate endDate) {
}
