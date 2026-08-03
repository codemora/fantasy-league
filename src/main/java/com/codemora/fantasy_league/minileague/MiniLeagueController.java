package com.codemora.fantasy_league.minileague;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.codemora.fantasy_league.leaderboard.dto.LeaderboardRowResponse;
import com.codemora.fantasy_league.minileague.dto.CreateMiniLeagueRequest;
import com.codemora.fantasy_league.minileague.dto.MiniLeagueResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Mini-Leagues")
public class MiniLeagueController {

    private final MiniLeagueService miniLeagueService;

    public MiniLeagueController(MiniLeagueService miniLeagueService) {
        this.miniLeagueService = miniLeagueService;
    }

    @PostMapping("/leagues/{leagueId}/seasons/{seasonId}/mini-leagues")
    @Operation(summary = "Create a private mini-league for a season",
            description = "You must already have a fantasy squad for this season. You're automatically added as "
                    + "the first member; share the returned inviteCode with friends so they can join.")
    public ResponseEntity<MiniLeagueResponse> create(
            @PathVariable Long leagueId, @PathVariable Long seasonId,
            @Valid @RequestBody CreateMiniLeagueRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(miniLeagueService.create(leagueId, seasonId, request));
    }

    @GetMapping("/leagues/{leagueId}/seasons/{seasonId}/mini-leagues")
    @Operation(summary = "View the mini-leagues you're a member of for a season")
    public ResponseEntity<List<MiniLeagueResponse>> findMine(
            @PathVariable Long leagueId, @PathVariable Long seasonId) {
        return ResponseEntity.ok(miniLeagueService.findMine(leagueId, seasonId));
    }

    @GetMapping("/leagues/{leagueId}/seasons/{seasonId}/mini-leagues/{miniLeagueId}/leaderboard")
    @Operation(summary = "View a mini-league's private leaderboard",
            description = "Same ranking as the season-wide leaderboard, restricted to this mini-league's members. "
                    + "404 if you're not a member -- a private mini-league's existence isn't visible to non-members.")
    public ResponseEntity<List<LeaderboardRowResponse>> leaderboard(
            @PathVariable Long leagueId, @PathVariable Long seasonId, @PathVariable Long miniLeagueId) {
        return ResponseEntity.ok(miniLeagueService.leaderboard(leagueId, seasonId, miniLeagueId));
    }

    @PostMapping("/mini-leagues/{inviteCode}/join")
    @Operation(summary = "Join a private mini-league via its invite code",
            description = "You must already have a fantasy squad for the mini-league's season.")
    public ResponseEntity<MiniLeagueResponse> join(@PathVariable String inviteCode) {
        return ResponseEntity.status(HttpStatus.CREATED).body(miniLeagueService.join(inviteCode));
    }
}
