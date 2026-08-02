package com.codemora.fantasy_league.player;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.codemora.fantasy_league.common.Position;

class PlayerPerformanceGeneratorTest {

    private final PlayerPerformanceGenerator generator = new PlayerPerformanceGenerator();

    private List<Player> roster(Long teamId) {
        List<Player> players = new ArrayList<>();
        long id = teamId * 100;
        for (int i = 0; i < 2; i++) players.add(Player.builder().id(id++).teamId(teamId).createdByUserId(7L).name("GK" + i).position(Position.GK).marketValue(45).build());
        for (int i = 0; i < 5; i++) players.add(Player.builder().id(id++).teamId(teamId).createdByUserId(7L).name("DEF" + i).position(Position.DEF).marketValue(50).build());
        for (int i = 0; i < 5; i++) players.add(Player.builder().id(id++).teamId(teamId).createdByUserId(7L).name("MID" + i).position(Position.MID).marketValue(60).build());
        for (int i = 0; i < 3; i++) players.add(Player.builder().id(id++).teamId(teamId).createdByUserId(7L).name("FWD" + i).position(Position.FWD).marketValue(70).build());
        return players;
    }

    @Test
    void producesOnePerformanceRowPerRosterPlayer() {
        List<Player> home = roster(1L);
        List<Player> away = roster(2L);

        List<PlayerPerformance> performances = generator.generate(500L, home, away, 2, 1, new Random(1L));

        assertThat(performances).hasSize(home.size() + away.size());
    }

    @Test
    void sumOfIndividualGoalsMatchesEachTeamsScore() {
        List<Player> home = roster(1L);
        List<Player> away = roster(2L);

        List<PlayerPerformance> performances = generator.generate(500L, home, away, 3, 2, new Random(7L));

        List<Long> homeIds = home.stream().map(Player::getId).toList();
        List<Long> awayIds = away.stream().map(Player::getId).toList();
        int homeGoalsSum = performances.stream().filter(p -> homeIds.contains(p.getPlayerId())).mapToInt(PlayerPerformance::getGoals).sum();
        int awayGoalsSum = performances.stream().filter(p -> awayIds.contains(p.getPlayerId())).mapToInt(PlayerPerformance::getGoals).sum();

        assertThat(homeGoalsSum).isEqualTo(3);
        assertThat(awayGoalsSum).isEqualTo(2);
    }

    @Test
    void onlyStartersGetMinutesAndOnlyStartersCanHaveACleanSheet() {
        List<Player> home = roster(1L);
        List<Player> away = roster(2L);

        List<PlayerPerformance> performances = generator.generate(500L, home, away, 0, 0, new Random(3L));

        long starters = performances.stream().filter(p -> p.getMinutesPlayed() == 90).count();
        long bench = performances.stream().filter(p -> p.getMinutesPlayed() == 0).count();
        assertThat(starters).isEqualTo(11 + 11); // 4-4-2 + GK, both teams
        assertThat(bench).isEqualTo(performances.size() - starters);
        assertThat(performances.stream().filter(PlayerPerformance::isCleanSheet)).allSatisfy(p -> assertThat(p.getMinutesPlayed()).isEqualTo(90));
        // 0-0 means both sides conceded zero -- every starter on both teams keeps a clean sheet
        assertThat(performances.stream().filter(p -> p.getMinutesPlayed() == 90)).allMatch(PlayerPerformance::isCleanSheet);
    }

    @Test
    void concedingATeamMeansNoCleanSheetForItsStarters() {
        List<Player> home = roster(1L);
        List<Player> away = roster(2L);

        // home scores 2 (so away concedes 2) -- away's starters should have no clean sheet
        List<PlayerPerformance> performances = generator.generate(500L, home, away, 2, 0, new Random(5L));

        List<Long> awayIds = away.stream().map(Player::getId).toList();
        assertThat(performances.stream().filter(p -> awayIds.contains(p.getPlayerId())).filter(p -> p.getMinutesPlayed() == 90))
                .allMatch(p -> !p.isCleanSheet());
    }

    @Test
    void sameSeedProducesIdenticalOutput() {
        List<Player> home = roster(1L);
        List<Player> away = roster(2L);

        List<PlayerPerformance> first = generator.generate(500L, home, away, 2, 1, new Random(99L));
        List<PlayerPerformance> second = generator.generate(500L, home, away, 2, 1, new Random(99L));

        for (int i = 0; i < first.size(); i++) {
            assertThat(first.get(i).getPlayerId()).isEqualTo(second.get(i).getPlayerId());
            assertThat(first.get(i).getGoals()).isEqualTo(second.get(i).getGoals());
            assertThat(first.get(i).getAssists()).isEqualTo(second.get(i).getAssists());
            assertThat(first.get(i).getMinutesPlayed()).isEqualTo(second.get(i).getMinutesPlayed());
        }
    }

    @Test
    void emptyRosterProducesNoPerformancesForThatTeam() {
        List<Player> away = roster(2L);

        List<PlayerPerformance> performances = generator.generate(500L, List.of(), away, 0, 1, new Random(1L));

        assertThat(performances).hasSize(away.size());
    }
}
