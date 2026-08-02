package com.codemora.fantasy_league.lineup.dto;

import com.codemora.fantasy_league.common.Position;

public record LineupSlotResponse(Long playerId, String playerName, Position position, Integer benchOrder) {
}
