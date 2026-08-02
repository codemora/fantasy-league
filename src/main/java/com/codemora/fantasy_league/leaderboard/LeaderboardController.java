package com.codemora.fantasy_league.leaderboard;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.codemora.fantasy_league.leaderboard.dto.LeaderboardRowResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/leagues/{leagueId}/seasons/{seasonId}/leaderboard")
@Tag(name = "Fantasy Leaderboard")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    public LeaderboardController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    @GetMapping
    @Operation(summary = "View the season's fantasy leaderboard",
            description = "Every squad in the season ranked by points accumulated so far, highest first. "
                    + "Tied squads share a rank and the next distinct total skips ahead (1, 2, 2, 4). "
                    + "The table is live: gameweeks still in progress are included.")
    public ResponseEntity<List<LeaderboardRowResponse>> findBySeason(
            @PathVariable Long leagueId, @PathVariable Long seasonId) {
        return ResponseEntity.ok(leaderboardService.findBySeason(leagueId, seasonId));
    }
}
