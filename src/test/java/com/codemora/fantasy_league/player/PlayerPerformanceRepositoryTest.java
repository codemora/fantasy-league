package com.codemora.fantasy_league.player;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import com.codemora.fantasy_league.auth.Role;
import com.codemora.fantasy_league.auth.User;
import com.codemora.fantasy_league.auth.UserRepository;
import com.codemora.fantasy_league.common.Position;
import com.codemora.fantasy_league.fixture.Fixture;
import com.codemora.fantasy_league.fixture.FixtureRepository;
import com.codemora.fantasy_league.gameweek.Gameweek;
import com.codemora.fantasy_league.gameweek.GameweekRepository;
import com.codemora.fantasy_league.gameweek.GameweekStatus;
import com.codemora.fantasy_league.league.League;
import com.codemora.fantasy_league.league.LeagueRepository;
import com.codemora.fantasy_league.season.Season;
import com.codemora.fantasy_league.season.SeasonRepository;
import com.codemora.fantasy_league.team.Team;
import com.codemora.fantasy_league.team.TeamRepository;

@DataJpaTest
class PlayerPerformanceRepositoryTest {

    @Autowired
    private PlayerPerformanceRepository playerPerformanceRepository;
    @Autowired
    private PlayerRepository playerRepository;
    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private LeagueRepository leagueRepository;
    @Autowired
    private SeasonRepository seasonRepository;
    @Autowired
    private GameweekRepository gameweekRepository;
    @Autowired
    private FixtureRepository fixtureRepository;

    private Long adminId;
    private Long playerId;
    private Long fixtureId;

    @BeforeEach
    void setUp() {
        adminId = userRepository.save(User.builder().username("admin").passwordHash("hashed").role(Role.ADMIN).build()).getId();
        Long teamId = teamRepository.save(Team.builder().createdByUserId(adminId).name("Arsenal").build()).getId();
        Long awayTeamId = teamRepository.save(Team.builder().createdByUserId(adminId).name("Chelsea").build()).getId();
        playerId = playerRepository.save(Player.builder().teamId(teamId).createdByUserId(adminId).name("Bruno Silva").position(Position.MID).marketValue(65).build()).getId();

        Long leagueId = leagueRepository.save(League.builder().createdByUserId(adminId).name("Premier League").build()).getId();
        Long seasonId = seasonRepository.save(Season.builder().leagueId(leagueId).period("2025-26").teamLimit(20).startingBudget(1000).build()).getId();
        Long gameweekId = gameweekRepository.save(Gameweek.builder()
                        .seasonId(seasonId).number(1).deadlineDateTime(LocalDateTime.of(2025, 8, 1, 12, 0)).status(GameweekStatus.UPCOMING).build())
                .getId();
        fixtureId = fixtureRepository.save(Fixture.builder()
                        .seasonId(seasonId).gameweekId(gameweekId).homeTeamId(teamId).awayTeamId(awayTeamId)
                        .homeTeamScore(1).awayTeamScore(0).played(true)
                        .startDateTime(LocalDateTime.of(2025, 8, 1, 15, 0)).simulationSeed(42L).build())
                .getId();
    }

    private PlayerPerformance.PlayerPerformanceBuilder performance() {
        return PlayerPerformance.builder().playerId(playerId).fixtureId(fixtureId).goals(1).assists(0).minutesPlayed(90).cleanSheet(true).goalsConceded(0);
    }

    @Test
    void playerAndFixtureMustBeUnique() {
        playerPerformanceRepository.saveAndFlush(performance().build());

        assertThatThrownBy(() -> playerPerformanceRepository.saveAndFlush(performance().build()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findByPlayerIdReturnsThatPlayersPerformances() {
        playerPerformanceRepository.save(performance().build());

        assertThat(playerPerformanceRepository.findByPlayerId(playerId)).hasSize(1);
    }

    @Test
    void findByPlayerIdAndFixtureIdReturnsTheMatchingRow() {
        playerPerformanceRepository.save(performance().build());

        assertThat(playerPerformanceRepository.findByPlayerIdAndFixtureId(playerId, fixtureId)).isPresent();
        assertThat(playerPerformanceRepository.findByPlayerIdAndFixtureId(playerId, 999L)).isEmpty();
    }

    @Test
    void findByFixtureIdReturnsAllPerformancesInThatFixture() {
        playerPerformanceRepository.save(performance().build());

        assertThat(playerPerformanceRepository.findByFixtureId(fixtureId)).hasSize(1);
    }
}
