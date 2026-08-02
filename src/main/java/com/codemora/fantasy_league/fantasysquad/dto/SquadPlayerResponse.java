package com.codemora.fantasy_league.fantasysquad.dto;

import java.time.LocalDateTime;

import com.codemora.fantasy_league.common.Position;

public record SquadPlayerResponse(
        Long playerId,
        String playerName,
        Position position,
        Long teamId,
        Integer purchasePrice,
        LocalDateTime addedAt) {
}
