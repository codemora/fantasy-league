package com.codemora.fantasy_league.player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import org.springframework.stereotype.Component;

import com.codemora.fantasy_league.common.Position;

/**
 * Pure, deterministic given the same roster + goal counts + Random -- callers
 * (FixtureService#simulate) drive it with a Random seeded from the fixture's
 * simulationSeed so results are reproducible, per #26's acceptance criteria.
 *
 * No persisted "starting XI" concept exists for real matches (unlike fantasy
 * GameweekLineup), so this picks a fixed 4-4-2 (1 GK, 4 DEF, 4 MID, 2 FWD)
 * from each team's generated 15-player roster (2 GK/5 DEF/5 MID/3 FWD, see
 * PlayerService), capped by whatever's actually available. Starters play the
 * full 90 minutes; the rest of the squad doesn't feature at all (0 minutes,
 * no substitutions -- mirrors the "no auto-substitutions in v1" rule already
 * used for fantasy squads).
 */
@Component
public class PlayerPerformanceGenerator {

    private static final int STARTING_DEF = 4;
    private static final int STARTING_MID = 4;
    private static final int STARTING_FWD = 2;
    private static final int STARTING_GK = 1;
    private static final int FULL_MATCH_MINUTES = 90;

    private static final double ASSIST_CHANCE = 0.8;
    private static final double YELLOW_CARD_CHANCE = 0.08;
    private static final double RED_CARD_CHANCE = 0.015;

    public List<PlayerPerformance> generate(
            Long fixtureId, List<Player> homeRoster, List<Player> awayRoster, int homeGoals, int awayGoals, Random random) {
        List<PlayerPerformance> performances = new ArrayList<>();
        performances.addAll(generateForTeam(fixtureId, homeRoster, homeGoals, awayGoals, random));
        performances.addAll(generateForTeam(fixtureId, awayRoster, awayGoals, homeGoals, random));
        return performances;
    }

    private List<PlayerPerformance> generateForTeam(
            Long fixtureId, List<Player> roster, int goalsScored, int goalsConceded, Random random) {
        if (roster.isEmpty()) {
            return List.of();
        }
        List<Player> starters = pickStartingLineup(roster, random);
        List<Player> outfieldStarters = starters.stream().filter(p -> p.getPosition() != Position.GK).toList();
        boolean cleanSheet = goalsConceded == 0;

        List<PlayerPerformance> performances = new ArrayList<>();
        for (Player player : roster) {
            boolean starting = starters.contains(player);
            performances.add(PlayerPerformance.builder()
                    .playerId(player.getId())
                    .fixtureId(fixtureId)
                    .minutesPlayed(starting ? FULL_MATCH_MINUTES : 0)
                    .cleanSheet(starting && cleanSheet)
                    .goalsConceded(starting ? goalsConceded : 0)
                    .build());
        }

        if (!outfieldStarters.isEmpty()) {
            attributeGoalsAndAssists(performances, outfieldStarters, goalsScored, random);
        }
        attributeCards(performances, starters, random);
        return performances;
    }

    private List<Player> pickStartingLineup(List<Player> roster, Random random) {
        List<Player> gk = shuffledByPosition(roster, Position.GK, random);
        List<Player> def = shuffledByPosition(roster, Position.DEF, random);
        List<Player> mid = shuffledByPosition(roster, Position.MID, random);
        List<Player> fwd = shuffledByPosition(roster, Position.FWD, random);

        List<Player> starters = new ArrayList<>();
        starters.addAll(gk.subList(0, Math.min(STARTING_GK, gk.size())));
        starters.addAll(def.subList(0, Math.min(STARTING_DEF, def.size())));
        starters.addAll(mid.subList(0, Math.min(STARTING_MID, mid.size())));
        starters.addAll(fwd.subList(0, Math.min(STARTING_FWD, fwd.size())));
        return starters;
    }

    private List<Player> shuffledByPosition(List<Player> roster, Position position, Random random) {
        List<Player> players = new ArrayList<>(roster.stream()
                .filter(p -> p.getPosition() == position)
                .sorted(Comparator.comparing(Player::getId))
                .toList());
        Collections.shuffle(players, random);
        return players;
    }

    /** FWD weighted heaviest, then MID, then DEF -- matches the README's attacking-position weighting. */
    private int weightFor(Position position) {
        return switch (position) {
            case FWD -> 3;
            case MID -> 2;
            case DEF -> 1;
            case GK -> 0;
        };
    }

    private void attributeGoalsAndAssists(
            List<PlayerPerformance> performances, List<Player> eligibleScorers, int goalsToAttribute, Random random) {
        for (int i = 0; i < goalsToAttribute; i++) {
            Player scorer = weightedPick(eligibleScorers, random);
            findPerformance(performances, scorer.getId()).setGoals(findPerformance(performances, scorer.getId()).getGoals() + 1);

            if (eligibleScorers.size() > 1 && random.nextDouble() < ASSIST_CHANCE) {
                List<Player> assistCandidates = eligibleScorers.stream().filter(p -> !p.getId().equals(scorer.getId())).toList();
                Player assister = weightedPick(assistCandidates, random);
                findPerformance(performances, assister.getId()).setAssists(findPerformance(performances, assister.getId()).getAssists() + 1);
            }
        }
    }

    private void attributeCards(List<PlayerPerformance> performances, List<Player> starters, Random random) {
        for (Player player : starters) {
            PlayerPerformance performance = findPerformance(performances, player.getId());
            if (random.nextDouble() < YELLOW_CARD_CHANCE) {
                performance.setYellowCards(1);
            }
            if (random.nextDouble() < RED_CARD_CHANCE) {
                performance.setRedCards(1);
            }
        }
    }

    private Player weightedPick(List<Player> candidates, Random random) {
        int totalWeight = candidates.stream().mapToInt(p -> weightFor(p.getPosition())).sum();
        if (totalWeight <= 0) {
            return candidates.get(random.nextInt(candidates.size()));
        }
        int roll = random.nextInt(totalWeight);
        int cumulative = 0;
        for (Player candidate : candidates) {
            cumulative += weightFor(candidate.getPosition());
            if (roll < cumulative) {
                return candidate;
            }
        }
        return candidates.get(candidates.size() - 1);
    }

    private PlayerPerformance findPerformance(List<PlayerPerformance> performances, Long playerId) {
        return performances.stream().filter(p -> p.getPlayerId().equals(playerId)).findFirst().orElseThrow();
    }
}
