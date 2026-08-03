package com.codemora.fantasy_league.minileague.dto;

import java.time.LocalDateTime;

public record MiniLeagueResponse(
        Long id,
        Long seasonId,
        String name,
        /** Share this with friends so they can join -- see POST /api/v1/mini-leagues/{inviteCode}/join. */
        String inviteCode,
        int memberCount,
        LocalDateTime createdAt) {
}
