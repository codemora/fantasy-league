package com.codemora.fantasy_league.transfer;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.codemora.fantasy_league.transfer.dto.MakeTransferRequest;
import com.codemora.fantasy_league.transfer.dto.MakeTransferResponse;
import com.codemora.fantasy_league.transfer.dto.TransferResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/leagues/{leagueId}/seasons/{seasonId}")
@Tag(name = "Transfers")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    @PostMapping("/gameweeks/{gameweekId}/transfers")
    @Operation(summary = "Transfer a player into your squad for a gameweek",
            description = "Like-for-like swap (same position) that must keep the squad within budget and within "
                    + "3 players per real team. Covered by a free transfer if you have one banked, otherwise it "
                    + "costs 4 points against that gameweek's total.")
    public ResponseEntity<MakeTransferResponse> makeTransfer(
            @PathVariable Long leagueId, @PathVariable Long seasonId, @PathVariable Long gameweekId,
            @Valid @RequestBody MakeTransferRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transferService.makeTransfer(leagueId, seasonId, gameweekId, request));
    }

    @GetMapping("/transfers")
    @Operation(summary = "View your transfer history for a season",
            description = "Every transfer made to your squad, oldest first, with the gameweek, both players, "
                    + "and the points it cost.")
    public ResponseEntity<List<TransferResponse>> findHistory(
            @PathVariable Long leagueId, @PathVariable Long seasonId) {
        return ResponseEntity.ok(transferService.findHistory(leagueId, seasonId));
    }
}
