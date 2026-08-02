package com.codemora.fantasy_league.season.dto;

import java.time.LocalDate;

public record SeasonResponse(
        Long id,
        Long leagueId,
        String period,
        Integer teamLimit,
        Integer startingBudget,
        boolean doubleLeg,
        LocalDate startDate,
        LocalDate endDate) {
}
