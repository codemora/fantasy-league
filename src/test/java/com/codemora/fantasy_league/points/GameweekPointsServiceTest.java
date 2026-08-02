package com.codemora.fantasy_league.points;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.codemora.fantasy_league.common.Position;
import com.codemora.fantasy_league.common.error.NotFoundException;
import com.codemora.fantasy_league.config.CurrentUserProvider;
import com.codemora.fantasy_league.fantasysquad.FantasySquad;
import com.codemora.fantasy_league.fantasysquad.FantasySquadRepository;
import com.codemora.fantasy_league.fixture.Fixture;
import com.codemora.fantasy_league.fixture.FixtureRepository;
import com.codemora.fantasy_league.gameweek.Gameweek;
import com.codemora.fantasy_league.gameweek.GameweekRepository;
import com.codemora.fantasy_league.gameweek.GameweekStatus;
import com.codemora.fantasy_league.lineup.GameweekLineup;
import com.codemora.fantasy_league.lineup.GameweekLineupRepository;
import com.codemora.fantasy_league.lineup.LineupRole;
import com.codemora.fantasy_league.lineup.LineupSlot;
import com.codemora.fantasy_league.lineup.LineupSlotRepository;
import com.codemora.fantasy_league.player.Player;
import com.codemora.fantasy_league.player.PlayerPerformance;
import com.codemora.fantasy_league.player.PlayerPerformanceRepository;
import com.codemora.fantasy_league.player.PlayerRepository;
import com.codemora.fantasy_league.points.dto.GameweekPointsResponse;
import com.codemora.fantasy_league.points.dto.PlayerPointsResponse;
import com.codemora.fantasy_league.scoringrule.ScoringRule;
import com.codemora.fantasy_league.scoringrule.ScoringRuleRepository;
import com.codemora.fantasy_league.season.Season;
import com.codemora.fantasy_league.season.SeasonRepository;

@ExtendWith(MockitoExtension.class)
class GameweekPointsServiceTest {

    @Mock
    private SeasonRepository seasonRepository;
    @Mock
    private GameweekRepository gameweekRepository;
    @Mock
    private FantasySquadRepository fantasySquadRepository;
    @Mock
    private GameweekLineupRepository gameweekLineupRepository;
    @Mock
    private LineupSlotRepository lineupSlotRepository;
    @Mock
    private PlayerRepository playerRepository;
    @Mock
    private PlayerPerformanceRepository playerPerformanceRepository;
    @Mock
    private FixtureRepository fixtureRepository;
    @Mock
    private ScoringRuleRepository scoringRuleRepository;
    @Mock
    private CurrentUserProvider currentUserProvider;

    private GameweekPointsService service() {
        return new GameweekPointsService(seasonRepository, gameweekRepository, fantasySquadRepository,
                gameweekLineupRepository, lineupSlotRepository, playerRepository, playerPerformanceRepository,
                fixtureRepository, scoringRuleRepository, currentUserProvider);
    }

    private Season season() {
        return Season.builder().id(10L).leagueId(1L).period("2025-26").teamLimit(20).startingBudget(1000).build();
    }

    private Gameweek gameweek() {
        return Gameweek.builder().id(20L).seasonId(10L).number(3)
                .deadlineDateTime(LocalDateTime.of(2025, 8, 1, 12, 0)).status(GameweekStatus.IN_PROGRESS).build();
    }

    private void stubSeasonGameweekSquadLineup(List<LineupSlot> slots) {
        when(seasonRepository.findById(10L)).thenReturn(Optional.of(season()));
        when(gameweekRepository.findById(20L)).thenReturn(Optional.of(gameweek()));
        when(currentUserProvider.getUserId()).thenReturn(7L);
        FantasySquad squad = FantasySquad.builder().id(500L).userId(7L).seasonId(10L).bankBalance(100).freeTransfers(1).build();
        when(fantasySquadRepository.findByUserIdAndSeasonId(7L, 10L)).thenReturn(Optional.of(squad));
        GameweekLineup lineup = GameweekLineup.builder().id(900L).squadId(500L).gameweekId(20L).captainPlayerId(1L).build();
        when(gameweekLineupRepository.findBySquadIdAndGameweekId(500L, 20L)).thenReturn(Optional.of(lineup));
        when(lineupSlotRepository.findByLineupId(900L)).thenReturn(slots);
    }

    private ScoringRule rule(Position position, int pointsPerGoal, int pointsPerAssist, int pointsPerCleanSheet) {
        return ScoringRule.builder().seasonId(10L).createdByUserId(1L).position(position)
                .pointsPerGoal(pointsPerGoal).pointsPerAssist(pointsPerAssist).pointsPerCleanSheet(pointsPerCleanSheet)
                .pointsPerAppearance60(2).pointsPerAppearance1to59(1).pointsPerGoalsConcededPerThree(-1)
                .pointsPerPenaltySave(5).pointsPerPenaltyMiss(-2).pointsPerYellowCard(-1).pointsPerRedCard(-3).pointsPerOwnGoal(-2)
                .build();
    }

    @Test
    void doublesTheCaptainsPointsAndZeroesOutBenchPoints() {
        // Player 1: GK, starter, captain, team 100. Player 2: DEF, starter, team 100. Player 3: MID, bench, team 300 (not in the gameweek's fixture).
        Player player1 = Player.builder().id(1L).teamId(100L).createdByUserId(1L).name("Keeper").position(Position.GK).marketValue(50).build();
        Player player2 = Player.builder().id(2L).teamId(100L).createdByUserId(1L).name("Defender").position(Position.DEF).marketValue(50).build();
        Player player3 = Player.builder().id(3L).teamId(300L).createdByUserId(1L).name("Bencher").position(Position.MID).marketValue(50).build();
        List<LineupSlot> slots = List.of(
                LineupSlot.builder().id(1001L).lineupId(900L).playerId(1L).role(LineupRole.STARTER).build(),
                LineupSlot.builder().id(1002L).lineupId(900L).playerId(2L).role(LineupRole.STARTER).build(),
                LineupSlot.builder().id(1003L).lineupId(900L).playerId(3L).role(LineupRole.BENCH).benchOrder(1).build());
        stubSeasonGameweekSquadLineup(slots);
        when(playerRepository.findAllById(anyCollection())).thenReturn(List.of(player1, player2, player3));
        when(fixtureRepository.findByGameweekId(20L)).thenReturn(List.of(
                Fixture.builder().id(9000L).seasonId(10L).gameweekId(20L).homeTeamId(100L).awayTeamId(200L)
                        .played(true).startDateTime(LocalDateTime.now()).simulationSeed(1L).build()));
        when(scoringRuleRepository.findBySeasonId(10L)).thenReturn(List.of(
                rule(Position.GK, 10, 3, 4), rule(Position.DEF, 6, 3, 4), rule(Position.MID, 5, 3, 1)));
        when(playerPerformanceRepository.findByPlayerIdAndFixtureId(1L, 9000L)).thenReturn(Optional.of(
                PlayerPerformance.builder().playerId(1L).fixtureId(9000L).goals(1).minutesPlayed(90).cleanSheet(true).build()));
        when(playerPerformanceRepository.findByPlayerIdAndFixtureId(2L, 9000L)).thenReturn(Optional.of(
                PlayerPerformance.builder().playerId(2L).fixtureId(9000L).assists(1).minutesPlayed(90).cleanSheet(true).build()));

        GameweekPointsResponse response = service().findPoints(1L, 10L, 20L);

        // player1 (captain, GK): raw = 1*10 + 4 + 2 = 16, doubled = 32
        // player2 (DEF): raw = 1*3 + 4 + 2 = 9, not captain, starter = 9
        // player3 (bench): no fixture found for team 300 -> raw = 0, bench never counts = 0
        assertThat(response.totalPoints()).isEqualTo(41);
        assertThat(response.players()).extracting(PlayerPointsResponse::playerId).containsExactly(1L, 2L, 3L);
        PlayerPointsResponse captainBreakdown = response.players().get(0);
        assertThat(captainBreakdown.captain()).isTrue();
        assertThat(captainBreakdown.rawPoints()).isEqualTo(16);
        assertThat(captainBreakdown.points()).isEqualTo(32);
        PlayerPointsResponse benchBreakdown = response.players().get(2);
        assertThat(benchBreakdown.role()).isEqualTo(LineupRole.BENCH);
        assertThat(benchBreakdown.points()).isEqualTo(0);
    }

    @Test
    void rejectsWhenNoLineupSubmittedForTheGameweek() {
        when(seasonRepository.findById(10L)).thenReturn(Optional.of(season()));
        when(gameweekRepository.findById(20L)).thenReturn(Optional.of(gameweek()));
        when(currentUserProvider.getUserId()).thenReturn(7L);
        FantasySquad squad = FantasySquad.builder().id(500L).userId(7L).seasonId(10L).bankBalance(100).freeTransfers(1).build();
        when(fantasySquadRepository.findByUserIdAndSeasonId(7L, 10L)).thenReturn(Optional.of(squad));
        when(gameweekLineupRepository.findBySquadIdAndGameweekId(500L, 20L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().findPoints(1L, 10L, 20L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void rejectsWhenUserHasNoSquad() {
        when(seasonRepository.findById(10L)).thenReturn(Optional.of(season()));
        when(gameweekRepository.findById(20L)).thenReturn(Optional.of(gameweek()));
        when(currentUserProvider.getUserId()).thenReturn(7L);
        when(fantasySquadRepository.findByUserIdAndSeasonId(7L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().findPoints(1L, 10L, 20L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void rejectsUnknownSeason() {
        when(seasonRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().findPoints(1L, 99L, 20L)).isInstanceOf(NotFoundException.class);
    }
}
