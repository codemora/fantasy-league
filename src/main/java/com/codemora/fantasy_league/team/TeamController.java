package com.codemora.fantasy_league.team;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.codemora.fantasy_league.common.PageResponse;
import com.codemora.fantasy_league.fixture.FixtureService;
import com.codemora.fantasy_league.fixture.dto.FixtureResponse;
import com.codemora.fantasy_league.team.dto.CreateTeamRequest;
import com.codemora.fantasy_league.team.dto.TeamResponse;
import com.codemora.fantasy_league.team.dto.UpdateTeamRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/teams")
@Tag(name = "Teams")
public class TeamController {

    private final TeamService teamService;
    private final FixtureService fixtureService;

    public TeamController(TeamService teamService, FixtureService fixtureService) {
        this.teamService = teamService;
        this.fixtureService = fixtureService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a team", description = "ADMIN only. Team names must be unique.")
    public ResponseEntity<TeamResponse> create(@Valid @RequestBody CreateTeamRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(teamService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Edit a team", description = "ADMIN only. Team names must remain unique.")
    public ResponseEntity<TeamResponse> update(@PathVariable Long id, @Valid @RequestBody UpdateTeamRequest request) {
        return ResponseEntity.ok(teamService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a team", description = "ADMIN only. Fails with 409 if the team has been entered into a season, has players, or has fixtures.")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        teamService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "View a team's details")
    public ResponseEntity<TeamResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(teamService.findById(id));
    }

    @GetMapping
    @Operation(summary = "Search teams", description = "Optionally filter by a case-insensitive, partial match on name.")
    public ResponseEntity<PageResponse<TeamResponse>> search(
            @RequestParam(required = false) String name,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(teamService.search(name, pageable));
    }

    @GetMapping("/{id}/fixtures")
    @Operation(summary = "View a team's fixtures", description = "Optionally filter with status=played or status=upcoming.")
    public ResponseEntity<List<FixtureResponse>> fixtures(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "all") String status) {
        return ResponseEntity.ok(fixtureService.findByTeam(id, status));
    }
}
