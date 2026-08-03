package com.codemora.fantasy_league.chip;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.codemora.fantasy_league.chip.dto.ChipResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/leagues/{leagueId}/seasons/{seasonId}")
@Tag(name = "Chips")
public class ChipController {

    private final ChipService chipService;

    public ChipController(ChipService chipService) {
        this.chipService = chipService;
    }

    @PostMapping("/gameweeks/{gameweekId}/chips/{chipType}")
    @Operation(summary = "Play a chip for a gameweek",
            description = "WILDCARD, TRIPLE_CAPTAIN, and BENCH_BOOST can each be played at most once per season, "
                    + "and only one chip can be active in any given gameweek. WILDCARD makes that gameweek's "
                    + "transfers free without touching your banked free transfers. TRIPLE_CAPTAIN triples (instead "
                    + "of doubles) your captain's points for the gameweek. BENCH_BOOST counts your bench's points "
                    + "toward the gameweek total instead of scoring them as 0.")
    public ResponseEntity<ChipResponse> activate(
            @PathVariable Long leagueId, @PathVariable Long seasonId, @PathVariable Long gameweekId,
            @PathVariable ChipType chipType) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(chipService.activate(leagueId, seasonId, gameweekId, chipType));
    }

    @GetMapping("/chips")
    @Operation(summary = "View your chip usage for a season",
            description = "Which chips you've played and in which gameweek, oldest first.")
    public ResponseEntity<List<ChipResponse>> findHistory(@PathVariable Long leagueId, @PathVariable Long seasonId) {
        return ResponseEntity.ok(chipService.findHistory(leagueId, seasonId));
    }
}
