package com.codemora.fantasy_league.transfer.dto;

import jakarta.validation.constraints.NotNull;

public record MakeTransferRequest(
        @NotNull Long playerOutId,
        @NotNull Long playerInId) {
}
