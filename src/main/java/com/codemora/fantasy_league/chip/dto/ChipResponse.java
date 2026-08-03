package com.codemora.fantasy_league.chip.dto;

import java.time.LocalDateTime;

import com.codemora.fantasy_league.chip.ChipType;

public record ChipResponse(
        Long id,
        ChipType chipType,
        Long gameweekId,
        Integer gameweekNumber,
        LocalDateTime activatedAt) {
}
