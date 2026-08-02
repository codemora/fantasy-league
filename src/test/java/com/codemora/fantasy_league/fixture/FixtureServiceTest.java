package com.codemora.fantasy_league.fixture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.codemora.fantasy_league.common.Position;
import com.codemora.fantasy_league.common.error.ConflictException;
import com.codemora.fantasy_league.common.error.NotFoundException;
import com.codemora.fantasy_league.fixture.dto.AddFixtureResultRequest;
import com.codemora.fantasy_league.fixture.dto.EditFixtureRequest;
import com.codemora.fantasy_league.fixture.dto.FixtureResponse;
import com.codemora.fantasy_league.fixture.dto.GenerateFixturesResponse;
import com.codemora.fantasy_league.fixture.dto.SimulateFixturesResponse;
import com.codemora.fantasy_league.gameweek.Gameweek;
import com.codemora.fantasy_league.gameweek.GameweekRepository;
import com.codemora.fantasy_league.gameweek.GameweekStatus;
import com.codemora.fantasy_league.player.Player;
import com.codemora.fantasy_league.player.PlayerPerformance;
import com.codemora.fantasy_league.player.PlayerPerformanceGenerator;
import com.codemora.fantasy_league.player.PlayerPerformanceRepository;
import com.codemora.fantasy_league.player.PlayerRepository;
import com.codemora.fantasy_league.season.Season;
import com.codemora.fantasy_league.season.SeasonEntrant;
import com.codemora.fantasy_league.season.SeasonEntrantRepository;
import com.codemora.fantasy_league.season.SeasonRepository;

@ExtendWith(MockitoExtension.class)
class FixtureServiceTest {

    @Mock
    private SeasonRepository seasonRepository;
    @Mock
    private SeasonEntrantRepository seasonEntrantRepository;
    @Mock
    private GameweekRepository gameweekRepository;
    @Mock
    private FixtureRepository fixtureRepository;
    @Mock
    private PlayerRepository playerRepository;
    @Mock
    private PlayerPerformanceRepository playerPerformanceRepository;

    // Real instances: pure, deterministic, already covered by their own thorough
    // tests -- exercising them for real here is more meaningful than mocking them.
    private final RoundRobinScheduler roundRobinScheduler = new RoundRobinScheduler();
    private final MatchScoreSimulator matchScoreSimulator = new MatchScoreSimulator();
    private final PlayerPerformanceGenerator playerPerformanceGenerator = new PlayerPerformanceGenerator();

    private FixtureService fixtureService() {
        return new FixtureService(
                seasonRepository, seasonEntrantRepository, gameweekRepository, fixtureRepository, roundRobinScheduler,
                playerRepository, playerPerformanceRepository, matchScoreSimulator, playerPerformanceGenerator);
    }

    private Season fourTeamSeason() {
        return Season.builder()
                .id(10L)
                .leagueId(1L)
                .period("2025-26")
                .teamLimit(4)
                .startingBudget(1000)
                .doubleLeg(false)
                .startDate(LocalDate.of(2025, 8, 1))
                .build();
    }

    private List<SeasonEntrant> fourEntrants() {
        return List.of(
                SeasonEntrant.builder().id(1L).seasonId(10L).teamId(101L).build(),
                SeasonEntrant.builder().id(2L).seasonId(10L).teamId(102L).build(),
                SeasonEntrant.builder().id(3L).seasonId(10L).teamId(103L).build(),
                SeasonEntrant.builder().id(4L).seasonId(10L).teamId(104L).build());
    }

    @Test
    void generateCreatesOneGameweekPerRoundAndOneFixturePerPairing() {
        when(seasonRepository.findById(10L)).thenReturn(Optional.of(fourTeamSeason()));
        when(seasonRepository.hasAnyFixtures(10L)).thenReturn(false);
        when(seasonEntrantRepository.findBySeasonId(10L)).thenReturn(fourEntrants());
        when(gameweekRepository.save(any(Gameweek.class))).thenAnswer(invocation -> {
            Gameweek gw = invocation.getArgument(0);
            gw.setId((long) (100 + gw.getNumber()));
            return gw;
        });
        when(fixtureRepository.save(any(Fixture.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GenerateFixturesResponse response = fixtureService().generate(1L, 10L);

        assertThat(response.gameweeksCreated()).isEqualTo(3); // n - 1 for 4 teams, single leg
        assertThat(response.fixturesCreated()).isEqualTo(6); // C(4,2)
        verify(gameweekRepository, times(3)).save(any());
        verify(fixtureRepository, times(6)).save(any());
    }

    @Test
    void generateRejectsUnknownSeason() {
        when(seasonRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fixtureService().generate(1L, 99L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void generateRejectsSeasonInDifferentLeague() {
        Season wrongLeagueSeason = Season.builder().id(10L).leagueId(2L).period("2025-26").teamLimit(4).startingBudget(1000).build();
        when(seasonRepository.findById(10L)).thenReturn(Optional.of(wrongLeagueSeason));

        assertThatThrownBy(() -> fixtureService().generate(1L, 10L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void generateRejectsWhenFixturesAlreadyExist() {
        when(seasonRepository.findById(10L)).thenReturn(Optional.of(fourTeamSeason()));
        when(seasonRepository.hasAnyFixtures(10L)).thenReturn(true);

        assertThatThrownBy(() -> fixtureService().generate(1L, 10L)).isInstanceOf(ConflictException.class);
        verify(fixtureRepository, never()).save(any());
    }

    @Test
    void generateRejectsFewerThanTwoEntrants() {
        when(seasonRepository.findById(10L)).thenReturn(Optional.of(fourTeamSeason()));
        when(seasonRepository.hasAnyFixtures(10L)).thenReturn(false);
        when(seasonEntrantRepository.findBySeasonId(10L)).thenReturn(
                List.of(SeasonEntrant.builder().id(1L).seasonId(10L).teamId(101L).build()));

        assertThatThrownBy(() -> fixtureService().generate(1L, 10L)).isInstanceOf(ConflictException.class);
    }

    private Fixture unplayedFixture() {
        return Fixture.builder()
                .id(500L)
                .seasonId(10L)
                .gameweekId(200L)
                .homeTeamId(101L)
                .awayTeamId(102L)
                .played(false)
                .startDateTime(LocalDateTime.of(2025, 8, 1, 15, 0))
                .simulationSeed(42L)
                .build();
    }

    private Gameweek upcomingGameweek() {
        return Gameweek.builder()
                .id(200L)
                .seasonId(10L)
                .number(1)
                .deadlineDateTime(LocalDateTime.now().plusDays(3))
                .status(GameweekStatus.UPCOMING)
                .build();
    }

    @Test
    void updateSavesNewStartDateTime() {
        LocalDateTime newTime = LocalDateTime.of(2025, 8, 2, 17, 30);
        when(seasonRepository.findById(10L)).thenReturn(Optional.of(fourTeamSeason()));
        when(fixtureRepository.findById(500L)).thenReturn(Optional.of(unplayedFixture()));
        when(gameweekRepository.findById(200L)).thenReturn(Optional.of(upcomingGameweek()));
        when(fixtureRepository.save(any(Fixture.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FixtureResponse response = fixtureService().update(1L, 10L, 500L, new EditFixtureRequest(newTime));

        assertThat(response.startDateTime()).isEqualTo(newTime);
    }

    @Test
    void updateRejectsUnknownFixture() {
        when(seasonRepository.findById(10L)).thenReturn(Optional.of(fourTeamSeason()));
        when(fixtureRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fixtureService().update(1L, 10L, 999L, new EditFixtureRequest(LocalDateTime.now())))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateRejectsFixtureInADifferentSeason() {
        Fixture otherSeasonFixture = unplayedFixture().toBuilder().seasonId(99L).build();
        when(seasonRepository.findById(10L)).thenReturn(Optional.of(fourTeamSeason()));
        when(fixtureRepository.findById(500L)).thenReturn(Optional.of(otherSeasonFixture));

        assertThatThrownBy(() -> fixtureService().update(1L, 10L, 500L, new EditFixtureRequest(LocalDateTime.now())))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateRejectsAlreadyPlayedFixture() {
        Fixture playedFixture = unplayedFixture().toBuilder().played(true).build();
        when(seasonRepository.findById(10L)).thenReturn(Optional.of(fourTeamSeason()));
        when(fixtureRepository.findById(500L)).thenReturn(Optional.of(playedFixture));

        assertThatThrownBy(() -> fixtureService().update(1L, 10L, 500L, new EditFixtureRequest(LocalDateTime.now())))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void updateRejectsOnceGameweekDeadlineHasPassed() {
        Gameweek pastGameweek = upcomingGameweek().toBuilder().deadlineDateTime(LocalDateTime.now().minusDays(1)).build();
        when(seasonRepository.findById(10L)).thenReturn(Optional.of(fourTeamSeason()));
        when(fixtureRepository.findById(500L)).thenReturn(Optional.of(unplayedFixture()));
        when(gameweekRepository.findById(200L)).thenReturn(Optional.of(pastGameweek));

        assertThatThrownBy(() -> fixtureService().update(1L, 10L, 500L, new EditFixtureRequest(LocalDateTime.now())))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void addResultRecordsScoresAndMarksPlayed() {
        when(seasonRepository.findById(10L)).thenReturn(Optional.of(fourTeamSeason()));
        when(fixtureRepository.findById(500L)).thenReturn(Optional.of(unplayedFixture()));
        when(fixtureRepository.save(any(Fixture.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FixtureResponse response = fixtureService().addResult(1L, 10L, 500L, new AddFixtureResultRequest(3, 1));

        assertThat(response.homeTeamScore()).isEqualTo(3);
        assertThat(response.awayTeamScore()).isEqualTo(1);
        assertThat(response.played()).isTrue();
    }

    @Test
    void addResultCanCorrectAnAlreadyPlayedFixture() {
        Fixture playedFixture = unplayedFixture().toBuilder().played(true).homeTeamScore(1).awayTeamScore(1).build();
        when(seasonRepository.findById(10L)).thenReturn(Optional.of(fourTeamSeason()));
        when(fixtureRepository.findById(500L)).thenReturn(Optional.of(playedFixture));
        when(fixtureRepository.save(any(Fixture.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FixtureResponse response = fixtureService().addResult(1L, 10L, 500L, new AddFixtureResultRequest(2, 1));

        assertThat(response.homeTeamScore()).isEqualTo(2);
    }

    @Test
    void addResultRejectsUnknownFixture() {
        when(seasonRepository.findById(10L)).thenReturn(Optional.of(fourTeamSeason()));
        when(fixtureRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fixtureService().addResult(1L, 10L, 999L, new AddFixtureResultRequest(1, 0)))
                .isInstanceOf(NotFoundException.class);
    }

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
    void simulateMarksUnplayedFixturesAsPlayedAndGeneratesPlayerPerformances() {
        when(seasonRepository.findById(10L)).thenReturn(Optional.of(fourTeamSeason()));
        when(fixtureRepository.findBySeasonIdAndPlayedFalse(10L)).thenReturn(List.of(unplayedFixture()));
        when(playerRepository.findByTeamId(101L)).thenReturn(roster(101L));
        when(playerRepository.findByTeamId(102L)).thenReturn(roster(102L));
        when(fixtureRepository.save(any(Fixture.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SimulateFixturesResponse response = fixtureService().simulate(1L, 10L, null);

        assertThat(response.fixturesSimulated()).isEqualTo(1);
        verify(fixtureRepository).save(argThat(f -> f.isPlayed() && f.getHomeTeamScore() != null && f.getAwayTeamScore() != null));
        verify(playerPerformanceRepository).saveAll(argThat((List<PlayerPerformance> performances) -> performances.size() == 30));
    }

    @Test
    void simulateFiltersByGameweekWhenGiven() {
        when(seasonRepository.findById(10L)).thenReturn(Optional.of(fourTeamSeason()));
        when(fixtureRepository.findBySeasonIdAndGameweekIdAndPlayedFalse(10L, 200L)).thenReturn(List.of(unplayedFixture()));
        when(playerRepository.findByTeamId(101L)).thenReturn(roster(101L));
        when(playerRepository.findByTeamId(102L)).thenReturn(roster(102L));
        when(fixtureRepository.save(any(Fixture.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SimulateFixturesResponse response = fixtureService().simulate(1L, 10L, 200L);

        assertThat(response.fixturesSimulated()).isEqualTo(1);
        verify(fixtureRepository, never()).findBySeasonIdAndPlayedFalse(any());
    }

    @Test
    void simulateIsANoOpWhenNothingIsUnplayed() {
        when(seasonRepository.findById(10L)).thenReturn(Optional.of(fourTeamSeason()));
        when(fixtureRepository.findBySeasonIdAndPlayedFalse(10L)).thenReturn(List.of());

        SimulateFixturesResponse response = fixtureService().simulate(1L, 10L, null);

        assertThat(response.fixturesSimulated()).isZero();
        verify(fixtureRepository, never()).save(any());
    }

    @Test
    void simulateRejectsUnknownSeason() {
        when(seasonRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fixtureService().simulate(1L, 99L, null)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void simulateRejectsSeasonInDifferentLeague() {
        Season wrongLeagueSeason = Season.builder().id(10L).leagueId(2L).period("2025-26").teamLimit(4).startingBudget(1000).build();
        when(seasonRepository.findById(10L)).thenReturn(Optional.of(wrongLeagueSeason));

        assertThatThrownBy(() -> fixtureService().simulate(1L, 10L, null)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void simulateProducesTheSameResultForTheSameSeed() {
        when(seasonRepository.findById(10L)).thenReturn(Optional.of(fourTeamSeason()));
        when(fixtureRepository.findBySeasonIdAndPlayedFalse(10L)).thenReturn(List.of(unplayedFixture()));
        when(playerRepository.findByTeamId(101L)).thenReturn(roster(101L));
        when(playerRepository.findByTeamId(102L)).thenReturn(roster(102L));

        List<Fixture> firstRun = new ArrayList<>();
        doAnswer(invocation -> {
            firstRun.add(invocation.getArgument(0));
            return invocation.getArgument(0);
        }).when(fixtureRepository).save(any(Fixture.class));
        fixtureService().simulate(1L, 10L, null);

        when(fixtureRepository.findBySeasonIdAndPlayedFalse(10L)).thenReturn(List.of(unplayedFixture()));
        List<Fixture> secondRun = new ArrayList<>();
        doAnswer(invocation -> {
            secondRun.add(invocation.getArgument(0));
            return invocation.getArgument(0);
        }).when(fixtureRepository).save(any(Fixture.class));
        fixtureService().simulate(1L, 10L, null);

        assertThat(firstRun.get(0).getHomeTeamScore()).isEqualTo(secondRun.get(0).getHomeTeamScore());
        assertThat(firstRun.get(0).getAwayTeamScore()).isEqualTo(secondRun.get(0).getAwayTeamScore());
    }
}
