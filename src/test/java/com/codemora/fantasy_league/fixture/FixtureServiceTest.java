package com.codemora.fantasy_league.fixture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.codemora.fantasy_league.common.error.ConflictException;
import com.codemora.fantasy_league.common.error.NotFoundException;
import com.codemora.fantasy_league.fixture.dto.GenerateFixturesResponse;
import com.codemora.fantasy_league.gameweek.Gameweek;
import com.codemora.fantasy_league.gameweek.GameweekRepository;
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

    // Real instance: pure, deterministic, already covered by its own thorough
    // test -- exercising it for real here is more meaningful than mocking it.
    private final RoundRobinScheduler roundRobinScheduler = new RoundRobinScheduler();

    private FixtureService fixtureService() {
        return new FixtureService(seasonRepository, seasonEntrantRepository, gameweekRepository, fixtureRepository, roundRobinScheduler);
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
}
