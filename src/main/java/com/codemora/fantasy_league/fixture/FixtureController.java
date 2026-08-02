package com.codemora.fantasy_league.fixture;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.codemora.fantasy_league.fixture.dto.AddFixtureResultRequest;
import com.codemora.fantasy_league.fixture.dto.EditFixtureRequest;
import com.codemora.fantasy_league.fixture.dto.FixtureResponse;
import com.codemora.fantasy_league.fixture.dto.GenerateFixturesResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/leagues/{leagueId}/seasons/{seasonId}/fixtures")
@Tag(name = "Fixtures")
public class FixtureController {

    private final FixtureService fixtureService;

    public FixtureController(FixtureService fixtureService) {
        this.fixtureService = fixtureService;
    }

    @PostMapping("/generate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Generate the season's fixture schedule",
            description = "ADMIN only. A round-robin schedule grouped into Gameweeks with deadlines. "
                    + "409 if fixtures already exist for this season, or fewer than 2 teams are entered.")
    public ResponseEntity<GenerateFixturesResponse> generate(
            @PathVariable Long leagueId, @PathVariable Long seasonId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(fixtureService.generate(leagueId, seasonId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Edit a fixture's kickoff time", description = "ADMIN only. 409 if the fixture already has a "
            + "recorded result or its gameweek's deadline has passed.")
    public ResponseEntity<FixtureResponse> update(
            @PathVariable Long leagueId, @PathVariable Long seasonId, @PathVariable Long id,
            @Valid @RequestBody EditFixtureRequest request) {
        return ResponseEntity.ok(fixtureService.update(leagueId, seasonId, id, request));
    }

    @PostMapping("/{id}/result")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Record a fixture's result", description = "ADMIN only. Can be called again to correct a "
            + "previously recorded result.")
    public ResponseEntity<FixtureResponse> addResult(
            @PathVariable Long leagueId, @PathVariable Long seasonId, @PathVariable Long id,
            @Valid @RequestBody AddFixtureResultRequest request) {
        return ResponseEntity.ok(fixtureService.addResult(leagueId, seasonId, id, request));
    }
}
