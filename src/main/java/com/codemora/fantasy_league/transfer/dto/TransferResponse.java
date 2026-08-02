package com.codemora.fantasy_league.transfer.dto;

import java.time.LocalDateTime;

import com.codemora.fantasy_league.common.Position;

public record TransferResponse(
        Long id,
        Long gameweekId,
        Integer gameweekNumber,
        Long playerOutId,
        String playerOutName,
        Long playerInId,
        String playerInName,
        Position position,
        Integer pointsCost,
        LocalDateTime timestamp) {
}
