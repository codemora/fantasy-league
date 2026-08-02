package com.codemora.fantasy_league.fixture;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

/**
 * Classic "circle method" round-robin scheduling. Pure and deterministic given
 * its inputs -- no randomness, no persistence -- so it's independently
 * testable without a database (see ADR 0001's reasoning for behavioral logic
 * living outside entities/services where practical).
 */
@Component
public class RoundRobinScheduler {

    /**
     * One round per gameweek; each round is the list of fixtures played that
     * gameweek. For an odd number of teams, one team has a bye each round (no
     * fixture). With doubleLeg, the whole single-leg schedule is repeated with
     * home/away swapped, so every team plays every other team once at home and
     * once away.
     */
    public List<List<TeamPairing>> generateRounds(List<Long> teamIds, boolean doubleLeg) {
        List<Long> teams = new ArrayList<>(teamIds);
        if (teams.size() % 2 != 0) {
            teams.add(null); // bye
        }
        int n = teams.size();
        int roundsCount = n - 1;

        List<List<TeamPairing>> firstLeg = new ArrayList<>();
        List<Long> rotating = new ArrayList<>(teams);
        for (int round = 0; round < roundsCount; round++) {
            List<TeamPairing> pairings = new ArrayList<>();
            for (int i = 0; i < n / 2; i++) {
                Long a = rotating.get(i);
                Long b = rotating.get(n - 1 - i);
                if (a == null || b == null) {
                    continue; // bye
                }
                // Alternate home/away by round parity so it's not always the
                // same side of the pairing at home across the whole schedule.
                pairings.add(round % 2 == 0 ? new TeamPairing(a, b) : new TeamPairing(b, a));
            }
            firstLeg.add(pairings);

            Long last = rotating.remove(rotating.size() - 1);
            rotating.add(1, last);
        }

        if (!doubleLeg) {
            return firstLeg;
        }

        List<List<TeamPairing>> allRounds = new ArrayList<>(firstLeg);
        for (List<TeamPairing> round : firstLeg) {
            List<TeamPairing> mirrored = new ArrayList<>();
            for (TeamPairing pairing : round) {
                mirrored.add(new TeamPairing(pairing.awayTeamId(), pairing.homeTeamId()));
            }
            allRounds.add(mirrored);
        }
        return allRounds;
    }
}
