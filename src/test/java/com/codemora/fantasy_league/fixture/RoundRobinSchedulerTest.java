package com.codemora.fantasy_league.fixture;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

class RoundRobinSchedulerTest {

    private final RoundRobinScheduler scheduler = new RoundRobinScheduler();

    @Test
    void evenTeamsSingleLeg_everyPairPlaysExactlyOnce() {
        List<Long> teams = List.of(1L, 2L, 3L, 4L);

        List<List<TeamPairing>> rounds = scheduler.generateRounds(teams, false);

        assertThat(rounds).hasSize(3); // n - 1 rounds
        rounds.forEach(round -> assertThat(round).hasSize(2)); // n / 2 fixtures per round

        Set<Set<Long>> uniquePairs = allPairsAsUnorderedSets(rounds);
        assertThat(uniquePairs).hasSize(6); // C(4,2)
        assertEveryTeamPlaysEveryOtherTeamOnce(teams, uniquePairs);
    }

    @Test
    void oddTeamsSingleLeg_everyTeamGetsExactlyOneByePerRound() {
        List<Long> teams = List.of(1L, 2L, 3L, 4L, 5L);

        List<List<TeamPairing>> rounds = scheduler.generateRounds(teams, false);

        assertThat(rounds).hasSize(5); // n rounds once padded with a bye
        rounds.forEach(round -> assertThat(round).hasSize(2)); // (n-1)/2 fixtures per round, one team byes

        Set<Set<Long>> uniquePairs = allPairsAsUnorderedSets(rounds);
        assertThat(uniquePairs).hasSize(10); // C(5,2)
        assertEveryTeamPlaysEveryOtherTeamOnce(teams, uniquePairs);
    }

    @Test
    void doubleLeg_everyTeamPlaysEveryOtherTeamOnceHomeAndOnceAway() {
        List<Long> teams = List.of(1L, 2L, 3L, 4L);

        List<List<TeamPairing>> rounds = scheduler.generateRounds(teams, true);

        assertThat(rounds).hasSize(6); // 2 * (n - 1)
        List<TeamPairing> allFixtures = rounds.stream().flatMap(List::stream).toList();
        assertThat(allFixtures).hasSize(12); // n * (n - 1) ordered pairs

        Set<TeamPairing> uniqueOrderedPairs = new HashSet<>(allFixtures);
        assertThat(uniqueOrderedPairs).hasSize(12); // no fixture repeated with the same home/away

        // Every ordered pair (a home vs b) has exactly one mirror (b home vs a).
        for (TeamPairing pairing : allFixtures) {
            assertThat(allFixtures).contains(new TeamPairing(pairing.awayTeamId(), pairing.homeTeamId()));
        }
    }

    @Test
    void noFixtureHasATeamPlayingItself() {
        List<Long> teams = List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L);

        List<List<TeamPairing>> rounds = scheduler.generateRounds(teams, true);

        rounds.stream().flatMap(List::stream).forEach(pairing ->
                assertThat(pairing.homeTeamId()).isNotEqualTo(pairing.awayTeamId()));
    }

    private Set<Set<Long>> allPairsAsUnorderedSets(List<List<TeamPairing>> rounds) {
        Set<Set<Long>> pairs = new HashSet<>();
        rounds.forEach(round -> round.forEach(pairing ->
                pairs.add(Set.of(pairing.homeTeamId(), pairing.awayTeamId()))));
        return pairs;
    }

    private void assertEveryTeamPlaysEveryOtherTeamOnce(List<Long> teams, Set<Set<Long>> uniquePairs) {
        for (Long a : teams) {
            for (Long b : teams) {
                if (!a.equals(b)) {
                    assertThat(uniquePairs).contains(Set.of(a, b));
                }
            }
        }
    }
}
