package com.codemora.fantasy_league.points;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.codemora.fantasy_league.common.Position;
import com.codemora.fantasy_league.lineup.Formation;

/**
 * FPL-style auto-substitution (#39): a starter who records 0 minutes is
 * replaced by the first bench player, in bench order, whose own minutes are
 * greater than 0 and whose replacement keeps the starting formation legal
 * (README Squad Rules / {@link Formation}). The reserve goalkeeper only ever
 * covers the starting goalkeeper, never an outfield starter, and vice versa --
 * a squad always carries exactly one reserve GK, so no ordering question
 * arises there. A starter with no valid replacement simply scores 0, same as
 * v1. There's no vice-captain concept in this codebase: if the captain is
 * subbed out, the doubled armband is simply lost for the gameweek rather than
 * passed to anyone else.
 */
public class BenchSubstitutionResolver {

    public record Participant(Long playerId, Position position, boolean played) {
    }

    public record Result(Set<Long> subbedOutPlayerIds, Set<Long> subbedInPlayerIds) {
        static final Result NONE = new Result(Set.of(), Set.of());
    }

    public Result resolve(List<Participant> starters, List<Participant> benchInOrder) {
        Set<Long> subbedOut = new HashSet<>();
        Set<Long> subbedIn = new HashSet<>();

        resolveGoalkeeper(starters, benchInOrder, subbedOut, subbedIn);
        resolveOutfield(starters, benchInOrder, subbedOut, subbedIn);

        return subbedOut.isEmpty() ? Result.NONE : new Result(subbedOut, subbedIn);
    }

    private void resolveGoalkeeper(List<Participant> starters, List<Participant> bench, Set<Long> subbedOut, Set<Long> subbedIn) {
        starters.stream()
                .filter(p -> p.position() == Position.GK && !p.played())
                .findFirst()
                .ifPresent(startingGk -> bench.stream()
                        .filter(p -> p.position() == Position.GK && p.played())
                        .findFirst()
                        .ifPresent(benchGk -> {
                            subbedOut.add(startingGk.playerId());
                            subbedIn.add(benchGk.playerId());
                        }));
    }

    private void resolveOutfield(List<Participant> starters, List<Participant> bench, Set<Long> subbedOut, Set<Long> subbedIn) {
        Map<Position, Integer> counts = new EnumMap<>(Position.class);
        starters.stream()
                .filter(p -> p.position() != Position.GK)
                .forEach(p -> counts.merge(p.position(), 1, Integer::sum));

        List<Participant> availableStarters = new ArrayList<>(starters.stream()
                .filter(p -> p.position() != Position.GK && !p.played())
                .toList());

        for (Participant benchPlayer : bench) {
            if (benchPlayer.position() == Position.GK || !benchPlayer.played()) {
                continue;
            }
            Iterator<Participant> it = availableStarters.iterator();
            while (it.hasNext()) {
                Participant starter = it.next();
                if (canSwap(counts, starter.position(), benchPlayer.position())) {
                    counts.merge(starter.position(), -1, Integer::sum);
                    counts.merge(benchPlayer.position(), 1, Integer::sum);
                    subbedOut.add(starter.playerId());
                    subbedIn.add(benchPlayer.playerId());
                    it.remove();
                    break;
                }
            }
        }
    }

    /** Only the two positions actually changing count can newly violate their range. */
    private boolean canSwap(Map<Position, Integer> counts, Position out, Position in) {
        if (out == in) {
            return true;
        }
        int[] outRange = Formation.RANGE.get(out);
        int[] inRange = Formation.RANGE.get(in);
        int newOutCount = counts.getOrDefault(out, 0) - 1;
        int newInCount = counts.getOrDefault(in, 0) + 1;
        return newOutCount >= outRange[0] && newInCount <= inRange[1];
    }
}
