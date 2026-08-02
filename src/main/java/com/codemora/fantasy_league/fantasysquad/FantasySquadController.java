package com.codemora.fantasy_league.fantasysquad;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.codemora.fantasy_league.fantasysquad.dto.CreateFantasySquadRequest;
import com.codemora.fantasy_league.fantasysquad.dto.FantasySquadResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/leagues/{leagueId}/seasons/{seasonId}/squad")
@Tag(name = "Fantasy Squads")
public class FantasySquadController {

    private final FantasySquadService fantasySquadService;

    public FantasySquadController(FantasySquadService fantasySquadService) {
        this.fantasySquadService = fantasySquadService;
    }

    @PostMapping
    @Operation(summary = "Create your fantasy squad for a season",
            description = "Exactly 15 players (2 GK, 5 DEF, 5 MID, 3 FWD), max 3 per real team, within the "
                    + "season's starting budget. 409 if you already have a squad for this season.")
    public ResponseEntity<FantasySquadResponse> create(
            @PathVariable Long leagueId, @PathVariable Long seasonId, @Valid @RequestBody CreateFantasySquadRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(fantasySquadService.create(leagueId, seasonId, request));
    }

    @GetMapping
    @Operation(summary = "View your fantasy squad for a season",
            description = "Players grouped by position, with purchase price, bank balance, and free transfers.")
    public ResponseEntity<FantasySquadResponse> findMine(
            @PathVariable Long leagueId, @PathVariable Long seasonId) {
        return ResponseEntity.ok(fantasySquadService.findMine(leagueId, seasonId));
    }
}
