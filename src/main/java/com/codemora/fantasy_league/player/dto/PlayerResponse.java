package com.codemora.fantasy_league.player.dto;

import com.codemora.fantasy_league.common.Position;

public record PlayerResponse(
        Long id,
        Long teamId,
        String name,
        Position position,
        Integer marketValue) {
}
