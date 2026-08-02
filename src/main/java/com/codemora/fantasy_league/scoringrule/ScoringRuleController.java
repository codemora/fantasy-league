package com.codemora.fantasy_league.scoringrule;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.codemora.fantasy_league.common.Position;
import com.codemora.fantasy_league.scoringrule.dto.ScoringRuleResponse;
import com.codemora.fantasy_league.scoringrule.dto.UpdateScoringRuleRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/leagues/{leagueId}/seasons/{seasonId}/scoring-rules")
@Tag(name = "Scoring Rules")
public class ScoringRuleController {

    private final ScoringRuleService scoringRuleService;

    public ScoringRuleController(ScoringRuleService scoringRuleService) {
        this.scoringRuleService = scoringRuleService;
    }

    @GetMapping
    @Operation(summary = "View a season's scoring rules", description = "One row per position (GK/DEF/MID/FWD).")
    public ResponseEntity<List<ScoringRuleResponse>> findBySeason(
            @PathVariable Long leagueId, @PathVariable Long seasonId) {
        return ResponseEntity.ok(scoringRuleService.findBySeason(leagueId, seasonId));
    }

    @PutMapping("/{position}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Edit a position's scoring rule",
            description = "ADMIN only. Only affects points calculated from now on, never rewrites past gameweeks.")
    public ResponseEntity<ScoringRuleResponse> update(
            @PathVariable Long leagueId, @PathVariable Long seasonId, @PathVariable Position position,
            @Valid @RequestBody UpdateScoringRuleRequest request) {
        return ResponseEntity.ok(scoringRuleService.update(leagueId, seasonId, position, request));
    }
}
