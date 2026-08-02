package com.codemora.fantasy_league.lineup.dto;

import java.util.List;

public record GameweekLineupResponse(
        Long id,
        Long squadId,
        Long gameweekId,
        Long captainPlayerId,
        List<LineupSlotResponse> starters,
        List<LineupSlotResponse> bench) {
}
