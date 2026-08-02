package com.codemora.fantasy_league.standings;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.codemora.fantasy_league.standings.dto.LeagueTableRowResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/leagues/{leagueId}/seasons/{seasonId}/standings")
@Tag(name = "Standings")
public class StandingsController {

    private final StandingsService standingsService;

    public StandingsController(StandingsService standingsService) {
        this.standingsService = standingsService;
    }

    @GetMapping
    @Operation(summary = "View the league table",
            description = "Ranked by points, with goal difference then goals for as tie-breakers.")
    public ResponseEntity<List<LeagueTableRowResponse>> table(
            @PathVariable Long leagueId, @PathVariable Long seasonId) {
        return ResponseEntity.ok(standingsService.getTable(leagueId, seasonId));
    }

    @GetMapping("/teams/{teamId}")
    @Operation(summary = "View a team's position in the league table")
    public ResponseEntity<LeagueTableRowResponse> teamPosition(
            @PathVariable Long leagueId, @PathVariable Long seasonId, @PathVariable Long teamId) {
        return ResponseEntity.ok(standingsService.getTeamPosition(leagueId, seasonId, teamId));
    }

    @GetMapping("/winner")
    @Operation(summary = "View the season's winner", description = "The top-ranked team in the league table.")
    public ResponseEntity<LeagueTableRowResponse> winner(
            @PathVariable Long leagueId, @PathVariable Long seasonId) {
        return ResponseEntity.ok(standingsService.getWinner(leagueId, seasonId));
    }

    @GetMapping("/top-teams")
    @Operation(summary = "View the top 4 teams", description = "Fixed v1 assumption of a 20-team season.")
    public ResponseEntity<List<LeagueTableRowResponse>> topTeams(
            @PathVariable Long leagueId, @PathVariable Long seasonId) {
        return ResponseEntity.ok(standingsService.getTopTeams(leagueId, seasonId));
    }

    @GetMapping("/relegated")
    @Operation(summary = "View the bottom 3 (relegated) teams", description = "Fixed v1 assumption of a 20-team season.")
    public ResponseEntity<List<LeagueTableRowResponse>> relegated(
            @PathVariable Long leagueId, @PathVariable Long seasonId) {
        return ResponseEntity.ok(standingsService.getRelegatedTeams(leagueId, seasonId));
    }
}
