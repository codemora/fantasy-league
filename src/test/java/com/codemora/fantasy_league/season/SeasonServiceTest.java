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
import com.codemora.fantasy_league.season.dto.CreateSeasonRequest;
import com.codemora.fantasy_league.season.dto.SeasonResponse;
import com.codemora.fantasy_league.season.dto.UpdateSeasonRequest;

@ExtendWith(MockitoExtension.class)
class SeasonServiceTest {

    @Mock
    private SeasonRepository seasonRepository;
    @Mock
    private LeagueRepository leagueRepository;

    private SeasonService seasonService() {
        return new SeasonService(seasonRepository, leagueRepository);
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
}
