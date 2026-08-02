package com.codemora.fantasy_league.points;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.codemora.fantasy_league.points.dto.GameweekPointsResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/leagues/{leagueId}/seasons/{seasonId}/gameweeks/{gameweekId}/points")
@Tag(name = "Gameweek Points")
public class GameweekPointsController {

    private final GameweekPointsService gameweekPointsService;

    public GameweekPointsController(GameweekPointsService gameweekPointsService) {
        this.gameweekPointsService = gameweekPointsService;
    }

    @GetMapping
    @Operation(summary = "View your fantasy points breakdown for a gameweek",
            description = "Points earned by each of your 15 squad players for the gameweek (bench players always "
                    + "score 0), the captain's points doubled, plus the gameweek total. 404 if you haven't "
                    + "submitted a lineup for this gameweek.")
    public ResponseEntity<GameweekPointsResponse> findPoints(
            @PathVariable Long leagueId, @PathVariable Long seasonId, @PathVariable Long gameweekId) {
        return ResponseEntity.ok(gameweekPointsService.findPoints(leagueId, seasonId, gameweekId));
    }
}
