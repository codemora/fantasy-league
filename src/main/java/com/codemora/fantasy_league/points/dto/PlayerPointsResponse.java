package com.codemora.fantasy_league.points.dto;

import com.codemora.fantasy_league.common.Position;
import com.codemora.fantasy_league.lineup.LineupRole;

public record PlayerPointsResponse(
        Long playerId,
        String playerName,
        Position position,
        LineupRole role,
        boolean captain,
        int rawPoints,
        int points) {
}
