package com.codemora.fantasy_league.standings.dto;

public record LeagueTableRowResponse(
        int position,
        Long teamId,
        String teamName,
        int matchesPlayed,
        int wins,
        int draws,
        int losses,
        int goalsFor,
        int goalsAgainst,
        int goalDifference,
        int points) {
}
