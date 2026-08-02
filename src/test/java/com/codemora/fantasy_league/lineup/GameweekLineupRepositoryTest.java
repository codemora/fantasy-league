package com.codemora.fantasy_league.lineup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import com.codemora.fantasy_league.auth.Role;
import com.codemora.fantasy_league.auth.User;
import com.codemora.fantasy_league.auth.UserRepository;
import com.codemora.fantasy_league.common.Position;
import com.codemora.fantasy_league.fantasysquad.FantasySquad;
import com.codemora.fantasy_league.fantasysquad.FantasySquadRepository;
import com.codemora.fantasy_league.gameweek.Gameweek;
import com.codemora.fantasy_league.gameweek.GameweekRepository;
import com.codemora.fantasy_league.gameweek.GameweekStatus;
import com.codemora.fantasy_league.league.League;
import com.codemora.fantasy_league.league.LeagueRepository;
import com.codemora.fantasy_league.player.Player;
import com.codemora.fantasy_league.player.PlayerRepository;
import com.codemora.fantasy_league.season.Season;
import com.codemora.fantasy_league.season.SeasonRepository;
import com.codemora.fantasy_league.team.Team;
import com.codemora.fantasy_league.team.TeamRepository;

import java.time.LocalDateTime;

@DataJpaTest
class GameweekLineupRepositoryTest {

    @Autowired
    private GameweekLineupRepository gameweekLineupRepository;
    @Autowired
    private FantasySquadRepository fantasySquadRepository;
    @Autowired
    private GameweekRepository gameweekRepository;
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

    private Long squadId;
    private Long gameweekId;
    private Long captainPlayerId;

    @BeforeEach
    void setUp() {
        Long userId = userRepository.save(User.builder().username("alice").passwordHash("hashed").role(Role.USER).build()).getId();
        Long adminId = userRepository.save(User.builder().username("admin").passwordHash("hashed").role(Role.ADMIN).build()).getId();
        Long teamId = teamRepository.save(Team.builder().createdByUserId(adminId).name("Arsenal").build()).getId();
        captainPlayerId = playerRepository.save(Player.builder().teamId(teamId).createdByUserId(adminId).name("Bruno Silva").position(Position.MID).marketValue(65).build()).getId();
        Long leagueId = leagueRepository.save(League.builder().createdByUserId(adminId).name("Premier League").build()).getId();
        Long seasonId = seasonRepository.save(Season.builder().leagueId(leagueId).period("2025-26").teamLimit(20).startingBudget(1000).build()).getId();
        squadId = fantasySquadRepository.save(FantasySquad.builder().userId(userId).seasonId(seasonId).bankBalance(935).freeTransfers(1).build()).getId();
        gameweekId = gameweekRepository.save(Gameweek.builder().seasonId(seasonId).number(1)
                .deadlineDateTime(LocalDateTime.of(2025, 8, 1, 12, 0)).status(GameweekStatus.UPCOMING).build()).getId();
    }

    @Test
    void squadAndGameweekMustBeUnique() {
        gameweekLineupRepository.saveAndFlush(GameweekLineup.builder().squadId(squadId).gameweekId(gameweekId).captainPlayerId(captainPlayerId).build());

        assertThatThrownBy(() -> gameweekLineupRepository.saveAndFlush(
                        GameweekLineup.builder().squadId(squadId).gameweekId(gameweekId).captainPlayerId(captainPlayerId).build()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findBySquadIdAndGameweekIdReturnsTheLineup() {
        gameweekLineupRepository.save(GameweekLineup.builder().squadId(squadId).gameweekId(gameweekId).captainPlayerId(captainPlayerId).build());

        assertThat(gameweekLineupRepository.findBySquadIdAndGameweekId(squadId, gameweekId)).isPresent();
        assertThat(gameweekLineupRepository.findBySquadIdAndGameweekId(squadId, 999L)).isEmpty();
    }
}
