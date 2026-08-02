package com.codemora.fantasy_league.season;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.codemora.fantasy_league.common.error.ConflictException;
import com.codemora.fantasy_league.common.error.NotFoundException;
import com.codemora.fantasy_league.league.LeagueRepository;
import com.codemora.fantasy_league.season.dto.AddSeasonEntrantRequest;
import com.codemora.fantasy_league.season.dto.CreateSeasonRequest;
import com.codemora.fantasy_league.season.dto.SeasonEntrantResponse;
import com.codemora.fantasy_league.season.dto.SeasonResponse;
import com.codemora.fantasy_league.season.dto.UpdateSeasonRequest;
import com.codemora.fantasy_league.team.TeamRepository;

@ExtendWith(MockitoExtension.class)
class SeasonServiceTest {

    @Mock
    private SeasonRepository seasonRepository;
    @Mock
    private LeagueRepository leagueRepository;
    @Mock
    private SeasonEntrantRepository seasonEntrantRepository;
    @Mock
    private TeamRepository teamRepository;

    private SeasonService seasonService() {
        return new SeasonService(seasonRepository, leagueRepository, seasonEntrantRepository, teamRepository);
    }

    @Test
    void createSavesSeasonUnderTheGivenLeague() {
        when(leagueRepository.existsById(1L)).thenReturn(true);
        when(seasonRepository.save(any(Season.class))).thenAnswer(invocation -> {
            Season s = invocation.getArgument(0);
            s.setId(10L);
            return s;
        });

        SeasonResponse response = seasonService().create(
                1L, new CreateSeasonRequest("2025-26", 20, 1000, true, LocalDate.of(2025, 8, 1), LocalDate.of(2026, 5, 24)));

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.leagueId()).isEqualTo(1L);
        assertThat(response.period()).isEqualTo("2025-26");
        assertThat(response.teamLimit()).isEqualTo(20);
        assertThat(response.startingBudget()).isEqualTo(1000);
        assertThat(response.doubleLeg()).isTrue();
    }

    @Test
    void createRejectsUnknownLeague() {
        when(leagueRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> seasonService().create(99L, new CreateSeasonRequest("2025-26", 20, 1000, false, null, null)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateSavesNewDetails() {
        Season existing = Season.builder().id(10L).leagueId(1L).period("2025-26").teamLimit(20).startingBudget(1000).build();
        when(seasonRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(seasonRepository.countEntrants(10L)).thenReturn(5L);
        when(seasonRepository.save(any(Season.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SeasonResponse response = seasonService().update(
                1L, 10L, new UpdateSeasonRequest("2025-26 (revised)", 18, 1100, true, null, null));

        assertThat(response.period()).isEqualTo("2025-26 (revised)");
        assertThat(response.teamLimit()).isEqualTo(18);
        assertThat(response.startingBudget()).isEqualTo(1100);
        assertThat(response.doubleLeg()).isTrue();
    }

    @Test
    void updateRejectsUnknownId() {
        when(seasonRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> seasonService().update(1L, 99L, new UpdateSeasonRequest("2025-26", 20, 1000, false, null, null)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateRejectsSeasonBelongingToADifferentLeague() {
        Season existing = Season.builder().id(10L).leagueId(2L).period("2025-26").teamLimit(20).startingBudget(1000).build();
        when(seasonRepository.findById(10L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> seasonService().update(1L, 10L, new UpdateSeasonRequest("2025-26", 20, 1000, false, null, null)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateRejectsTeamLimitBelowCurrentEntrantCount() {
        Season existing = Season.builder().id(10L).leagueId(1L).period("2025-26").teamLimit(20).startingBudget(1000).build();
        when(seasonRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(seasonRepository.countEntrants(10L)).thenReturn(15L);

        assertThatThrownBy(() -> seasonService().update(1L, 10L, new UpdateSeasonRequest("2025-26", 10, 1000, false, null, null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void deleteRemovesAnUnusedSeason() {
        Season existing = Season.builder().id(10L).leagueId(1L).period("2025-26").teamLimit(20).startingBudget(1000).build();
        when(seasonRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(seasonRepository.countEntrants(10L)).thenReturn(0L);
        when(seasonRepository.hasAnyFixtures(10L)).thenReturn(false);
        when(seasonRepository.hasAnyFantasySquads(10L)).thenReturn(false);

        seasonService().delete(1L, 10L);

        org.mockito.Mockito.verify(seasonRepository).delete(existing);
    }

    @Test
    void deleteRejectsUnknownId() {
        when(seasonRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> seasonService().delete(1L, 99L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void deleteRejectsSeasonWithEntrants() {
        Season existing = Season.builder().id(10L).leagueId(1L).period("2025-26").teamLimit(20).startingBudget(1000).build();
        when(seasonRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(seasonRepository.countEntrants(10L)).thenReturn(3L);

        assertThatThrownBy(() -> seasonService().delete(1L, 10L)).isInstanceOf(ConflictException.class);
    }

    @Test
    void deleteRejectsSeasonWithFixtures() {
        Season existing = Season.builder().id(10L).leagueId(1L).period("2025-26").teamLimit(20).startingBudget(1000).build();
        when(seasonRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(seasonRepository.countEntrants(10L)).thenReturn(0L);
        when(seasonRepository.hasAnyFixtures(10L)).thenReturn(true);

        assertThatThrownBy(() -> seasonService().delete(1L, 10L)).isInstanceOf(ConflictException.class);
    }

    @Test
    void deleteRejectsSeasonWithFantasySquads() {
        Season existing = Season.builder().id(10L).leagueId(1L).period("2025-26").teamLimit(20).startingBudget(1000).build();
        when(seasonRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(seasonRepository.countEntrants(10L)).thenReturn(0L);
        when(seasonRepository.hasAnyFixtures(10L)).thenReturn(false);
        when(seasonRepository.hasAnyFantasySquads(10L)).thenReturn(true);

        assertThatThrownBy(() -> seasonService().delete(1L, 10L)).isInstanceOf(ConflictException.class);
    }

    @Test
    void addEntrantSavesNewEntrant() {
        Season existing = Season.builder().id(10L).leagueId(1L).period("2025-26").teamLimit(20).startingBudget(1000).build();
        when(seasonRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(teamRepository.existsById(5L)).thenReturn(true);
        when(seasonEntrantRepository.existsBySeasonIdAndTeamId(10L, 5L)).thenReturn(false);
        when(seasonEntrantRepository.countBySeasonId(10L)).thenReturn(3L);
        when(seasonEntrantRepository.save(any(SeasonEntrant.class))).thenAnswer(invocation -> {
            SeasonEntrant e = invocation.getArgument(0);
            e.setId(100L);
            return e;
        });

        SeasonEntrantResponse response = seasonService().addEntrant(1L, 10L, new AddSeasonEntrantRequest(5L));

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.seasonId()).isEqualTo(10L);
        assertThat(response.teamId()).isEqualTo(5L);
    }

    @Test
    void addEntrantRejectsUnknownSeason() {
        when(seasonRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> seasonService().addEntrant(1L, 99L, new AddSeasonEntrantRequest(5L)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void addEntrantRejectsUnknownTeam() {
        Season existing = Season.builder().id(10L).leagueId(1L).period("2025-26").teamLimit(20).startingBudget(1000).build();
        when(seasonRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(teamRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> seasonService().addEntrant(1L, 10L, new AddSeasonEntrantRequest(99L)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void addEntrantRejectsTeamAlreadyEntered() {
        Season existing = Season.builder().id(10L).leagueId(1L).period("2025-26").teamLimit(20).startingBudget(1000).build();
        when(seasonRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(teamRepository.existsById(5L)).thenReturn(true);
        when(seasonEntrantRepository.existsBySeasonIdAndTeamId(10L, 5L)).thenReturn(true);

        assertThatThrownBy(() -> seasonService().addEntrant(1L, 10L, new AddSeasonEntrantRequest(5L)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void addEntrantRejectsWhenSeasonIsFull() {
        Season existing = Season.builder().id(10L).leagueId(1L).period("2025-26").teamLimit(20).startingBudget(1000).build();
        when(seasonRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(teamRepository.existsById(5L)).thenReturn(true);
        when(seasonEntrantRepository.existsBySeasonIdAndTeamId(10L, 5L)).thenReturn(false);
        when(seasonEntrantRepository.countBySeasonId(10L)).thenReturn(20L);

        assertThatThrownBy(() -> seasonService().addEntrant(1L, 10L, new AddSeasonEntrantRequest(5L)))
                .isInstanceOf(ConflictException.class);
    }
}
