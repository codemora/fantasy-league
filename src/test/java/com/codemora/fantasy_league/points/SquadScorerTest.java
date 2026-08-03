package com.codemora.fantasy_league.points;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.codemora.fantasy_league.chip.ChipType;
import com.codemora.fantasy_league.chip.SquadChip;
import com.codemora.fantasy_league.chip.SquadChipRepository;
import com.codemora.fantasy_league.common.Position;
import com.codemora.fantasy_league.fixture.Fixture;
import com.codemora.fantasy_league.fixture.FixtureRepository;
import com.codemora.fantasy_league.lineup.GameweekLineup;
import com.codemora.fantasy_league.lineup.GameweekLineupRepository;
import com.codemora.fantasy_league.lineup.LineupRole;
import com.codemora.fantasy_league.lineup.LineupSlot;
import com.codemora.fantasy_league.lineup.LineupSlotRepository;
import com.codemora.fantasy_league.player.Player;
import com.codemora.fantasy_league.player.PlayerPerformance;
import com.codemora.fantasy_league.player.PlayerPerformanceRepository;
import com.codemora.fantasy_league.player.PlayerRepository;
import com.codemora.fantasy_league.points.dto.PlayerPointsResponse;
import com.codemora.fantasy_league.scoringrule.ScoringRule;
import com.codemora.fantasy_league.transfer.Transfer;
import com.codemora.fantasy_league.transfer.TransferRepository;

@ExtendWith(MockitoExtension.class)
class SquadScorerTest {

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
    private TransferRepository transferRepository;
    @Mock
    private SquadChipRepository squadChipRepository;

    private static final Long SQUAD_ID = 500L;
    private static final Long GW = 20L;

    private SquadScorer scorer() {
        return new SquadScorer(gameweekLineupRepository, lineupSlotRepository, playerRepository,
                playerPerformanceRepository, fixtureRepository, transferRepository, squadChipRepository);
    }

    private ScoringRule rule(Position position, int pointsPerGoal) {
        return ScoringRule.builder().seasonId(10L).createdByUserId(1L).position(position)
                .pointsPerGoal(pointsPerGoal).pointsPerAssist(3).pointsPerCleanSheet(4)
                .pointsPerAppearance60(2).pointsPerAppearance1to59(1).pointsPerGoalsConcededPerThree(-1)
                .pointsPerPenaltySave(5).pointsPerPenaltyMiss(-2).pointsPerYellowCard(-1)
                .pointsPerRedCard(-3).pointsPerOwnGoal(-2).build();
    }

    private Map<Position, ScoringRule> rules() {
        return Map.of(Position.GK, rule(Position.GK, 10), Position.MID, rule(Position.MID, 5));
    }

    private Player player(long id, Position position) {
        return Player.builder().id(id).teamId(100L).createdByUserId(1L)
                .name("Player" + id).position(position).marketValue(50).build();
    }

    private SquadScorer.GameweekContext contextWith(Map<Long, PlayerPerformance> performances) {
        return new SquadScorer.GameweekContext(GW, performances);
    }

    private void stubLineup(List<LineupSlot> slots, List<Player> players, long captainId) {
        when(gameweekLineupRepository.findBySquadIdAndGameweekId(SQUAD_ID, GW)).thenReturn(Optional.of(
                GameweekLineup.builder().id(900L).squadId(SQUAD_ID).gameweekId(GW).captainPlayerId(captainId).build()));
        when(lineupSlotRepository.findByLineupId(900L)).thenReturn(slots);
        when(playerRepository.findAllById(anyCollection())).thenReturn(players);
    }

    @Test
    void doublesTheCaptainAndScoresNothingForTheBench() {
        stubLineup(
                List.of(LineupSlot.builder().id(1L).lineupId(900L).playerId(1L).role(LineupRole.STARTER).build(),
                        LineupSlot.builder().id(2L).lineupId(900L).playerId(2L).role(LineupRole.BENCH).benchOrder(1).build()),
                List.of(player(1L, Position.GK), player(2L, Position.MID)),
                1L);
        when(transferRepository.findBySquadIdAndGameweekId(SQUAD_ID, GW)).thenReturn(List.of());

        SquadGameweekScore score = scorer().score(SQUAD_ID, contextWith(Map.of(
                1L, PlayerPerformance.builder().playerId(1L).fixtureId(9000L).goals(1).minutesPlayed(90).build(),
                2L, PlayerPerformance.builder().playerId(2L).fixtureId(9000L).goals(2).minutesPlayed(90).build())),
                rules());

        // captain GK: (1*10 + 2 appearance) = 12, doubled = 24
        // bench MID: raw 2*5 + 2 = 12 but benched, so 0
        assertThat(score.playerPoints()).isEqualTo(24);
        assertThat(score.totalPoints()).isEqualTo(24);
        PlayerPointsResponse bench = score.players().stream()
                .filter(p -> p.role() == LineupRole.BENCH).findFirst().orElseThrow();
        assertThat(bench.rawPoints()).isEqualTo(12);
        assertThat(bench.points()).isZero();
    }

    @Test
    void substitutesANonPlayingStarterWithTheFirstEligibleBenchPlayerInBenchOrder() {
        stubLineup(
                List.of(LineupSlot.builder().id(1L).lineupId(900L).playerId(1L).role(LineupRole.STARTER).build(),
                        LineupSlot.builder().id(2L).lineupId(900L).playerId(2L).role(LineupRole.BENCH).benchOrder(1).build(),
                        LineupSlot.builder().id(3L).lineupId(900L).playerId(3L).role(LineupRole.BENCH).benchOrder(2).build()),
                List.of(player(1L, Position.MID), player(2L, Position.MID), player(3L, Position.MID)),
                1L); // captain is the starter who won't play -- the armband isn't transferred to the sub
        when(transferRepository.findBySquadIdAndGameweekId(SQUAD_ID, GW)).thenReturn(List.of());

        // player 1 has no recorded performance at all -- didn't feature
        SquadGameweekScore score = scorer().score(SQUAD_ID, contextWith(Map.of(
                2L, PlayerPerformance.builder().playerId(2L).fixtureId(9000L).goals(1).minutesPlayed(90).build(),
                3L, PlayerPerformance.builder().playerId(3L).fixtureId(9000L).goals(1).minutesPlayed(90).build())),
                rules());

        PlayerPointsResponse starter = playerResponse(score, 1L);
        PlayerPointsResponse subIn = playerResponse(score, 2L);
        PlayerPointsResponse stillBenched = playerResponse(score, 3L);

        assertThat(starter.points()).isZero();
        assertThat(starter.substitutedOut()).isTrue();
        // MID: 1*5 + appearance60(2) = 7, not doubled despite being captain -- the armband is lost, not passed on
        assertThat(subIn.points()).isEqualTo(7);
        assertThat(subIn.substitutedIn()).isTrue();
        assertThat(stillBenched.points()).isZero();
        assertThat(stillBenched.substitutedIn()).isFalse();
        assertThat(score.playerPoints()).isEqualTo(7);
    }

    @Test
    void substitutesTheReserveGoalkeeperWhenTheStartingGoalkeeperDoesNotPlay() {
        stubLineup(
                List.of(LineupSlot.builder().id(1L).lineupId(900L).playerId(1L).role(LineupRole.STARTER).build(),
                        LineupSlot.builder().id(2L).lineupId(900L).playerId(2L).role(LineupRole.BENCH).benchOrder(1).build()),
                List.of(player(1L, Position.GK), player(2L, Position.GK)),
                99L); // captain isn't in the lineup here, so no doubling to worry about
        when(transferRepository.findBySquadIdAndGameweekId(SQUAD_ID, GW)).thenReturn(List.of());

        SquadGameweekScore score = scorer().score(SQUAD_ID, contextWith(Map.of(
                2L, PlayerPerformance.builder().playerId(2L).fixtureId(9000L).minutesPlayed(90).build())),
                rules());

        PlayerPointsResponse startingGk = playerResponse(score, 1L);
        PlayerPointsResponse reserveGk = playerResponse(score, 2L);

        assertThat(startingGk.points()).isZero();
        assertThat(startingGk.substitutedOut()).isTrue();
        assertThat(reserveGk.substitutedIn()).isTrue();
        assertThat(reserveGk.points()).isEqualTo(2); // GK: 0 goals + appearance60(2)
    }

    private PlayerPointsResponse playerResponse(SquadGameweekScore score, long playerId) {
        return score.players().stream().filter(p -> p.playerId().equals(playerId)).findFirst().orElseThrow();
    }

    private void stubChip(ChipType chipType) {
        when(squadChipRepository.findBySquadIdAndGameweekId(SQUAD_ID, GW)).thenReturn(Optional.of(
                SquadChip.builder().id(1L).squadId(SQUAD_ID).gameweekId(GW).chipType(chipType)
                        .activatedAt(LocalDateTime.now()).build()));
    }

    @Test
    void tripleCaptainTriplesTheCaptainsPointsInsteadOfDoubling() {
        stubLineup(
                List.of(LineupSlot.builder().id(1L).lineupId(900L).playerId(1L).role(LineupRole.STARTER).build()),
                List.of(player(1L, Position.GK)),
                1L);
        when(transferRepository.findBySquadIdAndGameweekId(SQUAD_ID, GW)).thenReturn(List.of());
        stubChip(ChipType.TRIPLE_CAPTAIN);

        SquadGameweekScore score = scorer().score(SQUAD_ID, contextWith(Map.of(
                1L, PlayerPerformance.builder().playerId(1L).fixtureId(9000L).goals(1).minutesPlayed(90).build())),
                rules());

        // captain GK: (1*10 + 2 appearance) = 12, tripled = 36 -- not doubled
        assertThat(score.playerPoints()).isEqualTo(36);
        assertThat(score.activeChip()).isEqualTo(ChipType.TRIPLE_CAPTAIN);
    }

    @Test
    void benchBoostCountsBenchPlayersPointsInsteadOfScoringThemZero() {
        stubLineup(
                List.of(LineupSlot.builder().id(1L).lineupId(900L).playerId(1L).role(LineupRole.STARTER).build(),
                        LineupSlot.builder().id(2L).lineupId(900L).playerId(2L).role(LineupRole.BENCH).benchOrder(1).build()),
                List.of(player(1L, Position.GK), player(2L, Position.MID)),
                1L);
        when(transferRepository.findBySquadIdAndGameweekId(SQUAD_ID, GW)).thenReturn(List.of());
        stubChip(ChipType.BENCH_BOOST);

        SquadGameweekScore score = scorer().score(SQUAD_ID, contextWith(Map.of(
                1L, PlayerPerformance.builder().playerId(1L).fixtureId(9000L).goals(1).minutesPlayed(90).build(),
                2L, PlayerPerformance.builder().playerId(2L).fixtureId(9000L).goals(1).minutesPlayed(90).build())),
                rules());

        PlayerPointsResponse bench = playerResponse(score, 2L);
        // MID: 1*5 + appearance60(2) = 7, counted in full despite being on the bench
        assertThat(bench.points()).isEqualTo(7);
        assertThat(bench.substitutedIn()).isFalse(); // no substitution needed -- bench boost already counts everyone
        assertThat(score.activeChip()).isEqualTo(ChipType.BENCH_BOOST);
    }

    @Test
    void aPlayerWithNoRecordedPerformanceScoresNothing() {
        stubLineup(
                List.of(LineupSlot.builder().id(1L).lineupId(900L).playerId(1L).role(LineupRole.STARTER).build()),
                List.of(player(1L, Position.GK)),
                1L);
        when(transferRepository.findBySquadIdAndGameweekId(SQUAD_ID, GW)).thenReturn(List.of());

        // empty context: team had no fixture, or no performance was recorded
        SquadGameweekScore score = scorer().score(SQUAD_ID, contextWith(Map.of()), rules());

        assertThat(score.playerPoints()).isZero();
    }

    @Test
    void deductsTransferHitsFromTheTotalButNotFromPlayerPoints() {
        stubLineup(
                List.of(LineupSlot.builder().id(1L).lineupId(900L).playerId(1L).role(LineupRole.STARTER).build()),
                List.of(player(1L, Position.GK)),
                99L); // captain isn't in the lineup here, so no doubling
        when(transferRepository.findBySquadIdAndGameweekId(SQUAD_ID, GW)).thenReturn(List.of(
                Transfer.builder().id(1L).squadId(SQUAD_ID).gameweekId(GW).playerOutId(5L).playerInId(6L)
                        .pointsCost(4).timestamp(LocalDateTime.now()).build(),
                Transfer.builder().id(2L).squadId(SQUAD_ID).gameweekId(GW).playerOutId(7L).playerInId(8L)
                        .pointsCost(4).timestamp(LocalDateTime.now()).build()));

        SquadGameweekScore score = scorer().score(SQUAD_ID, contextWith(Map.of(
                1L, PlayerPerformance.builder().playerId(1L).fixtureId(9000L).goals(1).minutesPlayed(90).build())),
                rules());

        assertThat(score.playerPoints()).isEqualTo(12);
        assertThat(score.transferPointsCost()).isEqualTo(8);
        assertThat(score.totalPoints()).isEqualTo(4);
    }

    @Test
    void aSquadWithNoLineupStillPaysForItsTransfers() {
        when(gameweekLineupRepository.findBySquadIdAndGameweekId(SQUAD_ID, GW)).thenReturn(Optional.empty());
        when(transferRepository.findBySquadIdAndGameweekId(SQUAD_ID, GW)).thenReturn(List.of(
                Transfer.builder().id(1L).squadId(SQUAD_ID).gameweekId(GW).playerOutId(5L).playerInId(6L)
                        .pointsCost(4).timestamp(LocalDateTime.now()).build()));

        SquadGameweekScore score = scorer().score(SQUAD_ID, contextWith(Map.of()), rules());

        assertThat(score.players()).isEmpty();
        assertThat(score.playerPoints()).isZero();
        assertThat(score.totalPoints()).isEqualTo(-4);
    }

    @Test
    void loadContextIndexesEveryPerformanceInTheGameweekByPlayer() {
        when(fixtureRepository.findByGameweekId(GW)).thenReturn(List.of(
                Fixture.builder().id(9000L).seasonId(10L).gameweekId(GW).homeTeamId(100L).awayTeamId(200L)
                        .played(true).startDateTime(LocalDateTime.now()).simulationSeed(1L).build(),
                Fixture.builder().id(9001L).seasonId(10L).gameweekId(GW).homeTeamId(300L).awayTeamId(400L)
                        .played(true).startDateTime(LocalDateTime.now()).simulationSeed(2L).build()));
        when(playerPerformanceRepository.findByFixtureId(9000L)).thenReturn(List.of(
                PlayerPerformance.builder().playerId(1L).fixtureId(9000L).goals(1).build()));
        when(playerPerformanceRepository.findByFixtureId(9001L)).thenReturn(List.of(
                PlayerPerformance.builder().playerId(2L).fixtureId(9001L).goals(2).build()));

        SquadScorer.GameweekContext context = scorer().loadContext(GW);

        assertThat(context.gameweekId()).isEqualTo(GW);
        assertThat(context.performanceByPlayerId()).containsOnlyKeys(1L, 2L);
    }
}
