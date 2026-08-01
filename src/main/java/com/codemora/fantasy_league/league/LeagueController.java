package com.codemora.fantasy_league.league;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.codemora.fantasy_league.league.dto.CreateLeagueRequest;
import com.codemora.fantasy_league.league.dto.LeagueResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/leagues")
public class LeagueController {

    private final LeagueService leagueService;

    public LeagueController(LeagueService leagueService) {
        this.leagueService = leagueService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LeagueResponse> create(@Valid @RequestBody CreateLeagueRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(leagueService.create(request));
    }
}
