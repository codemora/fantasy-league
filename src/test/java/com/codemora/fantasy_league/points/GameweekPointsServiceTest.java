package com.codemora.fantasy_league.points;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
import com.codemora.fantasy_league.gameweek.Gameweek;
import com.codemora.fantasy_league.gameweek.GameweekRepository;
import com.codemora.fantasy_league.gameweek.GameweekStatus;
import com.codemora.fantasy_league.lineup.GameweekLineup;
import com.codemora.fantasy_league.lineup.GameweekLineupRepository;
import com.codemora.fantasy_league.lineup.LineupRole;
import com.codemora.fantasy_league.points.dto.GameweekPointsResponse;
import com.codemora.fantasy_league.points.dto.PlayerPointsResponse;
import com.codemora.fantasy_league.scoringrule.ScoringRuleRepository;
import com.codemora.fantasy_league.season.Season;
import com.codemora.fantasy_league.season.SeasonRepository;

/**
 * The scoring arithmetic itself lives in SquadScorerTest; this covers the
 * lookups, ownership checks and the official flag layered on top of it.
 */
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
    private ScoringRuleRepository scoringRuleRepository;
    @Mock
    private SquadScorer squadScorer;
    @Mock
    private CurrentUserProvider currentUserProvider;

    private GameweekPointsService service() {
        return new GameweekPointsService(seasonRepository, gameweekRepository, fantasySquadRepository,
                gameweekLineupRepository, scoringRuleRepository, squadScorer, currentUserProvider);
    }

    private Season season() {
        return Season.builder().id(10L).leagueId(1L).period("2025-26").teamLimit(20).startingBudget(1000).build();
    }

    private Gameweek gameweek(GameweekStatus status) {
        return Gameweek.builder().id(20L).seasonId(10L).number(3)
                .deadlineDateTime(LocalDateTime.of(2025, 8, 1, 12, 0)).status(status).build();
    }

    private void stubUpTo(GameweekStatus status, boolean lineupExists) {
        when(seasonRepository.findById(10L)).thenReturn(Optional.of(season()));
        when(gameweekRepository.findById(20L)).thenReturn(Optional.of(gameweek(status)));
        when(currentUserProvider.getUserId()).thenReturn(7L);
        when(fantasySquadRepository.findByUserIdAndSeasonId(7L, 10L)).thenReturn(Optional.of(
                FantasySquad.builder().id(500L).userId(7L).seasonId(10L).bankBalance(100).freeTransfers(1).build()));
        when(gameweekLineupRepository.findBySquadIdAndGameweekId(500L, 20L)).thenReturn(lineupExists
                ? Optional.of(GameweekLineup.builder().id(900L).squadId(500L).gameweekId(20L).captainPlayerId(1L).build())
                : Optional.empty());
    }

    private void stubScore() {
        when(scoringRuleRepository.findBySeasonId(10L)).thenReturn(List.of());
        when(squadScorer.loadContext(20L)).thenReturn(new SquadScorer.GameweekContext(20L, Map.of()));
        when(squadScorer.score(eq(500L), any(), anyMap())).thenReturn(new SquadGameweekScore(
                List.of(new PlayerPointsResponse(1L, "Keeper", Position.GK, LineupRole.STARTER, true, 16, 32)),
                32, 4, 28));
    }

    @Test
    void returnsTheBreakdownWithPlayerPointsHitsAndNetTotal() {
        stubUpTo(GameweekStatus.IN_PROGRESS, true);
        stubScore();

        GameweekPointsResponse response = service().findPoints(1L, 10L, 20L);

        assertThat(response.gameweekNumber()).isEqualTo(3);
        assertThat(response.players()).hasSize(1);
        assertThat(response.playerPoints()).isEqualTo(32);
        assertThat(response.transferPointsCost()).isEqualTo(4);
        assertThat(response.totalPoints()).isEqualTo(28);
    }

    @Test
    void pointsAreNotOfficialUntilTheGameweekIsComplete() {
        stubUpTo(GameweekStatus.IN_PROGRESS, true);
        stubScore();

        assertThat(service().findPoints(1L, 10L, 20L).official()).isFalse();
    }

    @Test
    void pointsAreOfficialOnceTheGameweekIsComplete() {
        stubUpTo(GameweekStatus.COMPLETE, true);
        stubScore();

        assertThat(service().findPoints(1L, 10L, 20L).official()).isTrue();
    }

    @Test
    void rejectsWhenNoLineupWasSubmittedForTheGameweek() {
        stubUpTo(GameweekStatus.IN_PROGRESS, false);

        assertThatThrownBy(() -> service().findPoints(1L, 10L, 20L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("haven't submitted a lineup");
    }

    @Test
    void rejectsWhenUserHasNoSquad() {
        when(seasonRepository.findById(10L)).thenReturn(Optional.of(season()));
        when(gameweekRepository.findById(20L)).thenReturn(Optional.of(gameweek(GameweekStatus.IN_PROGRESS)));
        when(currentUserProvider.getUserId()).thenReturn(7L);
        when(fantasySquadRepository.findByUserIdAndSeasonId(7L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().findPoints(1L, 10L, 20L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void rejectsUnknownSeason() {
        when(seasonRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().findPoints(1L, 99L, 20L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void rejectsGameweekFromAnotherSeason() {
        when(seasonRepository.findById(10L)).thenReturn(Optional.of(season()));
        when(gameweekRepository.findById(20L)).thenReturn(Optional.of(
                Gameweek.builder().id(20L).seasonId(99L).number(3)
                        .deadlineDateTime(LocalDateTime.now()).status(GameweekStatus.COMPLETE).build()));

        assertThatThrownBy(() -> service().findPoints(1L, 10L, 20L)).isInstanceOf(NotFoundException.class);
    }
}
