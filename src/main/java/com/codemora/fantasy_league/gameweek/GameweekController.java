package com.codemora.fantasy_league.gameweek;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.codemora.fantasy_league.gameweek.dto.GameweekResponse;
import com.codemora.fantasy_league.gameweek.dto.UpdateGameweekStatusRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/leagues/{leagueId}/seasons/{seasonId}/gameweeks")
@Tag(name = "Gameweeks")
public class GameweekController {

    private final GameweekService gameweekService;

    public GameweekController(GameweekService gameweekService) {
        this.gameweekService = gameweekService;
    }

    @PatchMapping("/{gameweekId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Advance a gameweek's status",
            description = "Moves the gameweek one step along UPCOMING -> LOCKED -> IN_PROGRESS -> COMPLETE. "
                    + "The lifecycle only moves forward: rewinding would reopen a passed deadline. Points for a "
                    + "gameweek are only official once it reaches COMPLETE.")
    public ResponseEntity<GameweekResponse> updateStatus(
            @PathVariable Long leagueId, @PathVariable Long seasonId, @PathVariable Long gameweekId,
            @Valid @RequestBody UpdateGameweekStatusRequest request) {
        return ResponseEntity.ok(gameweekService.updateStatus(leagueId, seasonId, gameweekId, request));
    }

    @GetMapping
    @Operation(summary = "View a season's gameweeks", description = "Each entry flags whether it's the next "
            + "upcoming deadline, and how many minutes remain until it (negative if already passed).")
    public ResponseEntity<List<GameweekResponse>> findBySeason(
            @PathVariable Long leagueId, @PathVariable Long seasonId) {
        return ResponseEntity.ok(gameweekService.findBySeason(leagueId, seasonId));
    }
}
