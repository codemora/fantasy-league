package com.codemora.fantasy_league.lineup;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.codemora.fantasy_league.lineup.dto.GameweekLineupResponse;
import com.codemora.fantasy_league.lineup.dto.SubmitLineupRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/leagues/{leagueId}/seasons/{seasonId}/gameweeks/{gameweekId}/lineup")
@Tag(name = "Gameweek Lineups")
public class LineupController {

    private final LineupService lineupService;

    public LineupController(LineupService lineupService) {
        this.lineupService = lineupService;
    }

    @PutMapping
    @Operation(summary = "Submit your starting XI, bench, and captain for a gameweek",
            description = "11 starters in a valid formation (1 GK, 3-5 DEF, 2-5 MID, 1-3 FWD) and 4 bench players, "
                    + "all drawn from your fantasy squad. The captain must be one of the starters. Resubmitting "
                    + "for the same gameweek replaces the previous selection.")
    public ResponseEntity<GameweekLineupResponse> submit(
            @PathVariable Long leagueId, @PathVariable Long seasonId, @PathVariable Long gameweekId,
            @Valid @RequestBody SubmitLineupRequest request) {
        return ResponseEntity.ok(lineupService.submit(leagueId, seasonId, gameweekId, request));
    }

    @GetMapping
    @Operation(summary = "View your submitted lineup for a gameweek")
    public ResponseEntity<GameweekLineupResponse> findCurrent(
            @PathVariable Long leagueId, @PathVariable Long seasonId, @PathVariable Long gameweekId) {
        return ResponseEntity.ok(lineupService.findCurrent(leagueId, seasonId, gameweekId));
    }
}
