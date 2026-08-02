package com.codemora.fantasy_league.leaderboard.dto;

public record LeaderboardRowResponse(
        int rank,
        Long squadId,
        Long userId,
        String username,
        int totalPoints,
        int transferPointsCost) {
}
