package com.codemora.fantasy_league.fantasysquad.dto;

import java.util.List;

public record FantasySquadResponse(
        Long id,
        Long userId,
        Long seasonId,
        Integer bankBalance,
        Integer freeTransfers,
        List<SquadPlayerResponse> players) {
}
