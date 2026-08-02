package com.codemora.fantasy_league.transfer.dto;

/** The transfer just made, plus the squad state it left behind. */
public record MakeTransferResponse(
        TransferResponse transfer,
        Integer bankBalance,
        int freeTransfersRemaining) {
}
