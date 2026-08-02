package com.codemora.fantasy_league.gameweek;

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
import com.codemora.fantasy_league.gameweek.dto.GameweekResponse;
import com.codemora.fantasy_league.season.Season;
import com.codemora.fantasy_league.season.SeasonRepository;

@ExtendWith(MockitoExtension.class)
class GameweekServiceTest {

    @Mock
    private SeasonRepository seasonRepository;
    @Mock
    private GameweekRepository gameweekRepository;

    private GameweekService gameweekService() {
        return new GameweekService(seasonRepository, gameweekRepository);
    }

    private Season season() {
        return Season.builder().id(10L).leagueId(1L).period("2025-26").teamLimit(20).startingBudget(1000).build();
    }

    @Test
    void findBySeasonFlagsTheEarliestUpcomingGameweekAsNext() {
        when(seasonRepository.findById(10L)).thenReturn(Optional.of(season()));
        Gameweek complete = Gameweek.builder().id(1L).seasonId(10L).number(1)
                .deadlineDateTime(LocalDateTime.now().minusDays(7)).status(GameweekStatus.COMPLETE).build();
        Gameweek nextUp = Gameweek.builder().id(2L).seasonId(10L).number(2)
                .deadlineDateTime(LocalDateTime.now().plusDays(2)).status(GameweekStatus.UPCOMING).build();
        Gameweek later = Gameweek.builder().id(3L).seasonId(10L).number(3)
                .deadlineDateTime(LocalDateTime.now().plusDays(9)).status(GameweekStatus.UPCOMING).build();
        when(gameweekRepository.findBySeasonIdOrderByNumber(10L)).thenReturn(List.of(complete, nextUp, later));

        List<GameweekResponse> response = gameweekService().findBySeason(1L, 10L);

        assertThat(response).hasSize(3);
        assertThat(response.get(0).isNext()).isFalse(); // COMPLETE, not eligible
        assertThat(response.get(1).isNext()).isTrue(); // earliest UPCOMING deadline
        assertThat(response.get(2).isNext()).isFalse();
    }

    @Test
    void findBySeasonComputesMinutesUntilDeadline() {
        when(seasonRepository.findById(10L)).thenReturn(Optional.of(season()));
        Gameweek future = Gameweek.builder().id(1L).seasonId(10L).number(1)
                .deadlineDateTime(LocalDateTime.now().plusHours(2)).status(GameweekStatus.UPCOMING).build();
        Gameweek past = Gameweek.builder().id(2L).seasonId(10L).number(2)
                .deadlineDateTime(LocalDateTime.now().minusHours(2)).status(GameweekStatus.COMPLETE).build();
        when(gameweekRepository.findBySeasonIdOrderByNumber(10L)).thenReturn(List.of(future, past));

        List<GameweekResponse> response = gameweekService().findBySeason(1L, 10L);

        assertThat(response.get(0).minutesUntilDeadline()).isGreaterThan(0);
        assertThat(response.get(1).minutesUntilDeadline()).isLessThan(0);
    }

    @Test
    void findBySeasonWithNoUpcomingGameweeksHasNoNext() {
        when(seasonRepository.findById(10L)).thenReturn(Optional.of(season()));
        Gameweek complete = Gameweek.builder().id(1L).seasonId(10L).number(1)
                .deadlineDateTime(LocalDateTime.now().minusDays(7)).status(GameweekStatus.COMPLETE).build();
        when(gameweekRepository.findBySeasonIdOrderByNumber(10L)).thenReturn(List.of(complete));

        List<GameweekResponse> response = gameweekService().findBySeason(1L, 10L);

        assertThat(response).allSatisfy(gw -> assertThat(gw.isNext()).isFalse());
    }

    @Test
    void findBySeasonRejectsUnknownSeason() {
        when(seasonRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gameweekService().findBySeason(1L, 99L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void findBySeasonRejectsSeasonInDifferentLeague() {
        Season wrongLeagueSeason = Season.builder().id(10L).leagueId(2L).period("2025-26").teamLimit(20).startingBudget(1000).build();
        when(seasonRepository.findById(10L)).thenReturn(Optional.of(wrongLeagueSeason));

        assertThatThrownBy(() -> gameweekService().findBySeason(1L, 10L)).isInstanceOf(NotFoundException.class);
    }
}
