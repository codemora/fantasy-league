package com.codemora.fantasy_league.fixture;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.codemora.fantasy_league.auth.Role;
import com.codemora.fantasy_league.auth.User;
import com.codemora.fantasy_league.auth.UserRepository;
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
class FixtureRepositoryTest {

    @Autowired
    private FixtureRepository fixtureRepository;
    @Autowired
    private GameweekRepository gameweekRepository;
    @Autowired
    private SeasonRepository seasonRepository;
    @Autowired
    private LeagueRepository leagueRepository;
    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private UserRepository userRepository;

    private Long seasonId;
    private Long arsenalId;
    private Long chelseaId;

    @BeforeEach
    void setUp() {
        Long adminId = userRepository.save(User.builder()
                        .username("admin")
                        .passwordHash("hashed")
                        .role(Role.ADMIN)
                        .build())
                .getId();
        Long leagueId = leagueRepository.save(League.builder().createdByUserId(adminId).name("Premier League").build())
                .getId();
        seasonId = seasonRepository.save(
                        Season.builder().leagueId(leagueId).period("2025-26").teamLimit(20).startingBudget(1000).build())
                .getId();
        arsenalId = teamRepository.save(Team.builder().createdByUserId(adminId).name("Arsenal").build()).getId();
        chelseaId = teamRepository.save(Team.builder().createdByUserId(adminId).name("Chelsea").build()).getId();
    }

    @Test
    void savedGameweekAndFixtureRoundTripAllFields() {
        LocalDateTime deadline = LocalDateTime.of(2025, 8, 1, 15, 0);
        Gameweek gameweek = gameweekRepository.save(Gameweek.builder()
                .seasonId(seasonId)
                .number(1)
                .deadlineDateTime(deadline)
                .status(GameweekStatus.UPCOMING)
                .build());

        Fixture fixture = fixtureRepository.save(Fixture.builder()
                .seasonId(seasonId)
                .gameweekId(gameweek.getId())
                .homeTeamId(arsenalId)
                .awayTeamId(chelseaId)
                .played(false)
                .startDateTime(deadline)
                .simulationSeed(42L)
                .build());

        Gameweek foundGameweek = gameweekRepository.findById(gameweek.getId()).orElseThrow();
        Fixture foundFixture = fixtureRepository.findById(fixture.getId()).orElseThrow();

        assertThat(foundGameweek.getSeasonId()).isEqualTo(seasonId);
        assertThat(foundGameweek.getNumber()).isEqualTo(1);
        assertThat(foundGameweek.getDeadlineDateTime()).isEqualTo(deadline);
        assertThat(foundGameweek.getStatus()).isEqualTo(GameweekStatus.UPCOMING);

        assertThat(foundFixture.getSeasonId()).isEqualTo(seasonId);
        assertThat(foundFixture.getGameweekId()).isEqualTo(gameweek.getId());
        assertThat(foundFixture.getHomeTeamId()).isEqualTo(arsenalId);
        assertThat(foundFixture.getAwayTeamId()).isEqualTo(chelseaId);
        assertThat(foundFixture.isPlayed()).isFalse();
        assertThat(foundFixture.getHomeTeamScore()).isNull();
        assertThat(foundFixture.getSimulationSeed()).isEqualTo(42L);
    }
}
