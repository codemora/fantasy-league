package com.codemora.fantasy_league.gameweek;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.codemora.fantasy_league.gameweek.dto.GameweekResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/leagues/{leagueId}/seasons/{seasonId}/gameweeks")
@Tag(name = "Gameweeks")
public class GameweekController {

    private final GameweekService gameweekService;

    public GameweekController(GameweekService gameweekService) {
        this.gameweekService = gameweekService;
    }

    @GetMapping
    @Operation(summary = "View a season's gameweeks", description = "Each entry flags whether it's the next "
            + "upcoming deadline, and how many minutes remain until it (negative if already passed).")
    public ResponseEntity<List<GameweekResponse>> findBySeason(
            @PathVariable Long leagueId, @PathVariable Long seasonId) {
        return ResponseEntity.ok(gameweekService.findBySeason(leagueId, seasonId));
    }
}
