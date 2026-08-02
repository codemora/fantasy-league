package com.codemora.fantasy_league.gameweek.dto;

import java.time.LocalDateTime;

import com.codemora.fantasy_league.gameweek.GameweekStatus;

public record GameweekResponse(
        Long id,
        Long seasonId,
        Integer number,
        LocalDateTime deadlineDateTime,
        GameweekStatus status,
        boolean isNext,
        long minutesUntilDeadline) {
}
