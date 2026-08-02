package com.codemora.fantasy_league.season;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.codemora.fantasy_league.season.dto.CreateSeasonRequest;
import com.codemora.fantasy_league.season.dto.SeasonResponse;
import com.codemora.fantasy_league.season.dto.UpdateSeasonRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/leagues/{leagueId}/seasons")
@Tag(name = "Seasons")
public class SeasonController {

    private final SeasonService seasonService;

    public SeasonController(SeasonService seasonService) {
        this.seasonService = seasonService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a season", description = "ADMIN only. 404 if the league doesn't exist.")
    public ResponseEntity<SeasonResponse> create(
            @PathVariable Long leagueId, @Valid @RequestBody CreateSeasonRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(seasonService.create(leagueId, request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Edit a season", description = "ADMIN only. 409 if team_limit would drop below the current number of entered teams.")
    public ResponseEntity<SeasonResponse> update(
            @PathVariable Long leagueId, @PathVariable Long id, @Valid @RequestBody UpdateSeasonRequest request) {
        return ResponseEntity.ok(seasonService.update(leagueId, id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a season", description = "ADMIN only. Fails with 409 if the season has entered teams, fixtures, or fantasy squads.")
    public ResponseEntity<Void> delete(@PathVariable Long leagueId, @PathVariable Long id) {
        seasonService.delete(leagueId, id);
        return ResponseEntity.noContent().build();
    }
}
