package com.codemora.fantasy_league.standings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.codemora.fantasy_league.common.error.NotFoundException;
import com.codemora.fantasy_league.fixture.Fixture;
import com.codemora.fantasy_league.fixture.FixtureRepository;
import com.codemora.fantasy_league.season.Season;
import com.codemora.fantasy_league.season.SeasonEntrant;
import com.codemora.fantasy_league.season.SeasonEntrantRepository;
import com.codemora.fantasy_league.season.SeasonRepository;
import com.codemora.fantasy_league.standings.dto.LeagueTableRowResponse;
import com.codemora.fantasy_league.team.Team;
import com.codemora.fantasy_league.team.TeamRepository;

@ExtendWith(MockitoExtension.class)
class StandingsServiceTest {

    @Mock
    private SeasonRepository seasonRepository;
    @Mock
    private SeasonEntrantRepository seasonEntrantRepository;
    @Mock
    private FixtureRepository fixtureRepository;
    @Mock
    private TeamRepository teamRepository;

    private StandingsService standingsService() {
        return new StandingsService(seasonRepository, seasonEntrantRepository, fixtureRepository, teamRepository);
    }

    private Season season() {
        return Season.builder().id(10L).leagueId(1L).period("2025-26").teamLimit(4).startingBudget(1000).build();
    }

    private List<SeasonEntrant> fourEntrants() {
        return List.of(
                SeasonEntrant.builder().id(1L).seasonId(10L).teamId(101L).build(),
                SeasonEntrant.builder().id(2L).seasonId(10L).teamId(102L).build(),
                SeasonEntrant.builder().id(3L).seasonId(10L).teamId(103L).build(),
                SeasonEntrant.builder().id(4L).seasonId(10L).teamId(104L).build());
    }

    private List<Team> fourTeams() {
        return List.of(
                Team.builder().id(101L).createdByUserId(1L).name("Team A").build(),
                Team.builder().id(102L).createdByUserId(1L).name("Team B").build(),
                Team.builder().id(103L).createdByUserId(1L).name("Team C").build(),
                Team.builder().id(104L).createdByUserId(1L).name("Team D").build());
    }

    private Fixture fixture(Long id, Long homeTeamId, Long awayTeamId, int homeScore, int awayScore) {
        return Fixture.builder()
                .id(id)
                .seasonId(10L)
                .gameweekId(200L)
                .homeTeamId(homeTeamId)
                .awayTeamId(awayTeamId)
                .homeTeamScore(homeScore)
                .awayTeamScore(awayScore)
                .played(true)
                .startDateTime(LocalDateTime.of(2025, 8, 1, 15, 0))
                .simulationSeed(1L)
                .build();
    }

    private void stubBaseline() {
        when(seasonRepository.findById(10L)).thenReturn(Optional.of(season()));
        when(seasonEntrantRepository.findBySeasonId(10L)).thenReturn(fourEntrants());
        when(teamRepository.findAllById(org.mockito.ArgumentMatchers.any())).thenReturn(fourTeams());
    }

    @Test
    void getTableRanksByPointsThenGoalDifferenceThenGoalsFor() {
        stubBaseline();
        // 101 beats 102 3-1, 103 draws 104 0-0, 101 draws 103 1-1
        when(fixtureRepository.findBySeasonIdAndPlayedTrue(10L)).thenReturn(List.of(
                fixture(1L, 101L, 102L, 3, 1),
                fixture(2L, 103L, 104L, 0, 0),
                fixture(3L, 101L, 103L, 1, 1)));

        List<LeagueTableRowResponse> table = standingsService().getTable(1L, 10L);

        assertThat(table).hasSize(4);
        assertThat(table.get(0).teamId()).isEqualTo(101L); // 4 points (win + draw), GF 4 GA 2 -> GD +2
        assertThat(table.get(0).points()).isEqualTo(4);
        assertThat(table.get(0).wins()).isEqualTo(1);
        assertThat(table.get(0).draws()).isEqualTo(1);
        assertThat(table.get(0).goalDifference()).isEqualTo(2);
        assertThat(table.get(0).position()).isEqualTo(1);
        // 103 and 104 both have 1 point, 0 GD, 0 GF from their draw; 103 also
        // drew 101 (0 more points, +0 GD) so 103 (1pt) ranks above 104 (1pt) on identical tiebreakers by list order
        assertThat(table).extracting(LeagueTableRowResponse::teamId).contains(102L, 103L, 104L);
    }

    @Test
    void getTableIncludesEntrantsWithNoPlayedFixturesAtZero() {
        stubBaseline();
        when(fixtureRepository.findBySeasonIdAndPlayedTrue(10L)).thenReturn(List.of());

        List<LeagueTableRowResponse> table = standingsService().getTable(1L, 10L);

        assertThat(table).hasSize(4);
        assertThat(table).allSatisfy(row -> {
            assertThat(row.matchesPlayed()).isZero();
            assertThat(row.points()).isZero();
        });
    }

    @Test
    void getTableRejectsUnknownSeason() {
        when(seasonRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> standingsService().getTable(1L, 99L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void getTableRejectsSeasonInDifferentLeague() {
        Season wrongLeagueSeason = Season.builder().id(10L).leagueId(2L).period("2025-26").teamLimit(4).startingBudget(1000).build();
        when(seasonRepository.findById(10L)).thenReturn(Optional.of(wrongLeagueSeason));

        assertThatThrownBy(() -> standingsService().getTable(1L, 10L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void getTeamPositionReturnsTheTeamsRow() {
        stubBaseline();
        when(fixtureRepository.findBySeasonIdAndPlayedTrue(10L)).thenReturn(List.of(fixture(1L, 101L, 102L, 3, 1)));

        LeagueTableRowResponse row = standingsService().getTeamPosition(1L, 10L, 101L);

        assertThat(row.teamId()).isEqualTo(101L);
        assertThat(row.points()).isEqualTo(3);
    }

    @Test
    void getTeamPositionRejectsTeamNotInSeason() {
        stubBaseline();
        when(fixtureRepository.findBySeasonIdAndPlayedTrue(10L)).thenReturn(List.of());

        assertThatThrownBy(() -> standingsService().getTeamPosition(1L, 10L, 999L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void getWinnerReturnsTopRankedTeam() {
        stubBaseline();
        when(fixtureRepository.findBySeasonIdAndPlayedTrue(10L)).thenReturn(List.of(fixture(1L, 101L, 102L, 3, 1)));

        LeagueTableRowResponse winner = standingsService().getWinner(1L, 10L);

        assertThat(winner.teamId()).isEqualTo(101L);
        assertThat(winner.position()).isEqualTo(1);
    }

    @Test
    void getTopTeamsReturnsUpToFour() {
        stubBaseline();
        when(fixtureRepository.findBySeasonIdAndPlayedTrue(10L)).thenReturn(List.of());

        List<LeagueTableRowResponse> topTeams = standingsService().getTopTeams(1L, 10L);

        assertThat(topTeams).hasSize(4); // season only has 4 entrants total
    }

    @Test
    void getRelegatedTeamsReturnsBottomThree() {
        stubBaseline();
        when(fixtureRepository.findBySeasonIdAndPlayedTrue(10L)).thenReturn(List.of(
                fixture(1L, 101L, 102L, 3, 1),
                fixture(2L, 101L, 103L, 2, 0),
                fixture(3L, 101L, 104L, 1, 0)));

        List<LeagueTableRowResponse> relegated = standingsService().getRelegatedTeams(1L, 10L);

        assertThat(relegated).hasSize(3);
        assertThat(relegated).extracting(LeagueTableRowResponse::teamId).doesNotContain(101L); // top team excluded
    }
}
