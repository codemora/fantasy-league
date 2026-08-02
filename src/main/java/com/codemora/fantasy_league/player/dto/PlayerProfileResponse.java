package com.codemora.fantasy_league.player.dto;

import com.codemora.fantasy_league.common.Position;

public record PlayerProfileResponse(
        Long id,
        Long teamId,
        String teamName,
        String name,
        Position position,
        Integer marketValue,
        int appearances,
        int goals,
        int assists,
        int cleanSheets) {
}
