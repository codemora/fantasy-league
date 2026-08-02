package com.codemora.fantasy_league.points.dto;

import java.util.List;

public record GameweekPointsResponse(
        Long gameweekId,
        Integer gameweekNumber,
        List<PlayerPointsResponse> players,
        int playerPoints,
        int transferPointsCost,
        int totalPoints) {
}
