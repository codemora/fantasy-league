package com.codemora.fantasy_league.leaderboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.codemora.fantasy_league.auth.Role;
import com.codemora.fantasy_league.auth.User;
import com.codemora.fantasy_league.auth.UserRepository;
import com.codemora.fantasy_league.common.error.NotFoundException;
import com.codemora.fantasy_league.fantasysquad.FantasySquad;
import com.codemora.fantasy_league.fantasysquad.FantasySquadRepository;
import com.codemora.fantasy_league.gameweek.Gameweek;
import com.codemora.fantasy_league.gameweek.GameweekRepository;
import com.codemora.fantasy_league.gameweek.GameweekStatus;
import com.codemora.fantasy_league.leaderboard.dto.LeaderboardRowResponse;
import com.codemora.fantasy_league.points.SquadGameweekScore;
import com.codemora.fantasy_league.points.SquadScorer;
import com.codemora.fantasy_league.scoringrule.ScoringRuleRepository;
import com.codemora.fantasy_league.season.Season;
import com.codemora.fantasy_league.season.SeasonRepository;

@ExtendWith(MockitoExtension.class)
class LeaderboardServiceTest {

    @Mock
    private SeasonRepository seasonRepository;
    @Mock
    private GameweekRepository gameweekRepository;
    @Mock
    private FantasySquadRepository fantasySquadRepository;
    @Mock
    private ScoringRuleRepository scoringRuleRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SquadScorer squadScorer;

    private LeaderboardService service() {
        return new LeaderboardService(seasonRepository, gameweekRepository, fantasySquadRepository,
                scoringRuleRepository, userRepository, squadScorer);
    }

    private Season season() {
        return Season.builder().id(10L).leagueId(1L).period("2025-26").teamLimit(20).startingBudget(1000).build();
    }

    private FantasySquad squad(long id, long userId) {
        return FantasySquad.builder().id(id).userId(userId).seasonId(10L).bankBalance(0).freeTransfers(1).build();
    }

    private User user(long id, String username) {
        return User.builder().id(id).username(username).passwordHash("hashed").role(Role.USER).build();
    }

    private Gameweek gameweek(long id, int number) {
        return Gameweek.builder().id(id).seasonId(10L).number(number)
                .deadlineDateTime(LocalDateTime.now().plusDays(number)).status(GameweekStatus.COMPLETE).build();
    }

    private void stubSeasonWithSquads(List<FantasySquad> squads, List<User> users, List<Gameweek> gameweeks) {
        when(seasonRepository.findById(10L)).thenReturn(Optional.of(season()));
        when(fantasySquadRepository.findBySeasonId(10L)).thenReturn(squads);
        when(scoringRuleRepository.findBySeasonId(10L)).thenReturn(List.of());
        when(userRepository.findAllById(anyCollection())).thenReturn(users);
        when(gameweekRepository.findBySeasonIdOrderByNumber(10L)).thenReturn(gameweeks);
        // loadContext is only reached when there's at least one gameweek to score
        if (!gameweeks.isEmpty()) {
            when(squadScorer.loadContext(any())).thenAnswer(i ->
                    new SquadScorer.GameweekContext(i.getArgument(0), Map.of()));
        }
    }

    private void stubScore(long squadId, int points, int hits) {
        when(squadScorer.score(eq(squadId), any(), anyMap()))
                .thenReturn(new SquadGameweekScore(List.of(), points + hits, hits, points));
    }

    @Test
    void ranksSquadsByTotalPointsDescending() {
        stubSeasonWithSquads(
                List.of(squad(100L, 1L), squad(200L, 2L), squad(300L, 3L)),
                List.of(user(1L, "alice"), user(2L, "bob"), user(3L, "carol")),
                List.of(gameweek(20L, 1)));
        stubScore(100L, 30, 0);
        stubScore(200L, 50, 0);
        stubScore(300L, 40, 0);

        List<LeaderboardRowResponse> rows = service().findBySeason(1L, 10L);

        assertThat(rows).extracting(LeaderboardRowResponse::username).containsExactly("bob", "carol", "alice");
        assertThat(rows).extracting(LeaderboardRowResponse::rank).containsExactly(1, 2, 3);
        assertThat(rows).extracting(LeaderboardRowResponse::totalPoints).containsExactly(50, 40, 30);
    }

    @Test
    void tiedSquadsShareARankAndTheNextDistinctTotalSkipsAhead() {
        stubSeasonWithSquads(
                List.of(squad(100L, 1L), squad(200L, 2L), squad(300L, 3L), squad(400L, 4L)),
                List.of(user(1L, "alice"), user(2L, "bob"), user(3L, "carol"), user(4L, "dave")),
                List.of(gameweek(20L, 1)));
        stubScore(100L, 50, 0);
        stubScore(200L, 40, 0);
        stubScore(300L, 40, 0);
        stubScore(400L, 10, 0);

        List<LeaderboardRowResponse> rows = service().findBySeason(1L, 10L);

        // standard competition ranking: 1, 2, 2, 4 -- not 1, 2, 3, 4 and not 1, 2, 2, 3
        assertThat(rows).extracting(LeaderboardRowResponse::rank).containsExactly(1, 2, 2, 4);
        // tied pair ordered by username so the response is at least deterministic
        assertThat(rows).extracting(LeaderboardRowResponse::username).containsExactly("alice", "bob", "carol", "dave");
    }

    @Test
    void sumsPointsAcrossEveryGameweek() {
        stubSeasonWithSquads(
                List.of(squad(100L, 1L)),
                List.of(user(1L, "alice")),
                List.of(gameweek(20L, 1), gameweek(21L, 2), gameweek(22L, 3)));
        stubScore(100L, 12, 4);

        List<LeaderboardRowResponse> rows = service().findBySeason(1L, 10L);

        assertThat(rows.get(0).totalPoints()).isEqualTo(36); // 12 x 3 gameweeks
        assertThat(rows.get(0).transferPointsCost()).isEqualTo(12); // 4 x 3
    }

    @Test
    void loadsEachGameweeksPerformancesOncePerGameweekNotOncePerSquad() {
        stubSeasonWithSquads(
                List.of(squad(100L, 1L), squad(200L, 2L), squad(300L, 3L)),
                List.of(user(1L, "alice"), user(2L, "bob"), user(3L, "carol")),
                List.of(gameweek(20L, 1), gameweek(21L, 2)));
        stubScore(100L, 10, 0);
        stubScore(200L, 10, 0);
        stubScore(300L, 10, 0);

        service().findBySeason(1L, 10L);

        // 2 gameweeks, 3 squads -> 2 context loads, not 6
        verify(squadScorer, times(2)).loadContext(any());
    }

    @Test
    void returnsEmptyWhenNoSquadsHaveBeenDrafted() {
        when(seasonRepository.findById(10L)).thenReturn(Optional.of(season()));
        when(fantasySquadRepository.findBySeasonId(10L)).thenReturn(List.of());

        assertThat(service().findBySeason(1L, 10L)).isEmpty();
    }

    @Test
    void countsSquadsWithNoGameweeksPlayedAsZero() {
        stubSeasonWithSquads(
                List.of(squad(100L, 1L)),
                List.of(user(1L, "alice")),
                List.of());

        List<LeaderboardRowResponse> rows = service().findBySeason(1L, 10L);

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).totalPoints()).isZero();
        assertThat(rows.get(0).rank()).isEqualTo(1);
    }

    @Test
    void rejectsUnknownSeason() {
        when(seasonRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().findBySeason(1L, 99L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void rejectsSeasonInDifferentLeague() {
        Season wrongLeague = Season.builder().id(10L).leagueId(2L).period("2025-26")
                .teamLimit(20).startingBudget(1000).build();
        when(seasonRepository.findById(10L)).thenReturn(Optional.of(wrongLeague));

        assertThatThrownBy(() -> service().findBySeason(1L, 10L)).isInstanceOf(NotFoundException.class);
    }
}
