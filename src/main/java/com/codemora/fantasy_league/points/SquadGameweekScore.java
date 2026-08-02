package com.codemora.fantasy_league.points;

import java.util.List;

import com.codemora.fantasy_league.points.dto.PlayerPointsResponse;

/**
 * One squad's outcome for one gameweek. {@code totalPoints} is what counts
 * towards the season: player points earned, less any hits taken on transfers
 * made in that gameweek.
 */
public record SquadGameweekScore(
        List<PlayerPointsResponse> players,
        int playerPoints,
        int transferPointsCost,
        int totalPoints) {

    static SquadGameweekScore of(List<PlayerPointsResponse> players, int playerPoints, int transferPointsCost) {
        return new SquadGameweekScore(players, playerPoints, transferPointsCost, playerPoints - transferPointsCost);
    }

    /** A squad that never submitted a lineup for the gameweek still pays for any transfers it made. */
    static SquadGameweekScore withoutLineup(int transferPointsCost) {
        return of(List.of(), 0, transferPointsCost);
    }
}
