package com.codemora.fantasy_league.season;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.codemora.fantasy_league.common.error.NotFoundException;
import com.codemora.fantasy_league.league.LeagueRepository;
import com.codemora.fantasy_league.season.dto.CreateSeasonRequest;
import com.codemora.fantasy_league.season.dto.SeasonResponse;

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
}
