package com.codemora.fantasy_league.player;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.codemora.fantasy_league.player.dto.PlayerResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/teams/{teamId}/players")
@Tag(name = "Players")
public class PlayerController {

    private final PlayerService playerService;

    public PlayerController(PlayerService playerService) {
        this.playerService = playerService;
    }

    @PostMapping("/generate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Generate a full simulated squad for a team",
            description = "ADMIN only. Creates 2 GK, 5 DEF, 5 MID, 3 FWD. 409 if the team already has players.")
    public ResponseEntity<List<PlayerResponse>> generate(@PathVariable Long teamId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(playerService.generateSquad(teamId));
    }

    @GetMapping
    @Operation(summary = "View a team's player roster")
    public ResponseEntity<List<PlayerResponse>> findByTeam(@PathVariable Long teamId) {
        return ResponseEntity.ok(playerService.findByTeam(teamId));
    }
}
