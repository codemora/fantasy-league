package com.codemora.fantasy_league.fantasysquad;

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
import com.codemora.fantasy_league.league.League;
import com.codemora.fantasy_league.league.LeagueRepository;
import com.codemora.fantasy_league.player.Player;
import com.codemora.fantasy_league.player.PlayerRepository;
import com.codemora.fantasy_league.season.Season;
import com.codemora.fantasy_league.season.SeasonRepository;
import com.codemora.fantasy_league.team.Team;
import com.codemora.fantasy_league.team.TeamRepository;

@DataJpaTest
class SquadPlayerRepositoryTest {

    @Autowired
    private SquadPlayerRepository squadPlayerRepository;
    @Autowired
    private FantasySquadRepository fantasySquadRepository;
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
    private Long playerId;

    @BeforeEach
    void setUp() {
        Long userId = userRepository.save(User.builder().username("alice").passwordHash("hashed").role(Role.USER).build()).getId();
        Long adminId = userRepository.save(User.builder().username("admin").passwordHash("hashed").role(Role.ADMIN).build()).getId();
        Long teamId = teamRepository.save(Team.builder().createdByUserId(adminId).name("Arsenal").build()).getId();
        playerId = playerRepository.save(Player.builder().teamId(teamId).createdByUserId(adminId).name("Bruno Silva").position(Position.MID).marketValue(65).build()).getId();
        Long leagueId = leagueRepository.save(League.builder().createdByUserId(adminId).name("Premier League").build()).getId();
        Long seasonId = seasonRepository.save(Season.builder().leagueId(leagueId).period("2025-26").teamLimit(20).startingBudget(1000).build()).getId();
        squadId = fantasySquadRepository.save(FantasySquad.builder().userId(userId).seasonId(seasonId).bankBalance(935).freeTransfers(1).build()).getId();
    }

    private SquadPlayer.SquadPlayerBuilder squadPlayer() {
        return SquadPlayer.builder().squadId(squadId).playerId(playerId).purchasePrice(65).addedAt(LocalDateTime.now());
    }

    @Test
    void squadAndPlayerMustBeUnique() {
        squadPlayerRepository.saveAndFlush(squadPlayer().build());

        assertThatThrownBy(() -> squadPlayerRepository.saveAndFlush(squadPlayer().build()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findBySquadIdReturnsThatSquadsPlayers() {
        squadPlayerRepository.save(squadPlayer().build());

        assertThat(squadPlayerRepository.findBySquadId(squadId)).hasSize(1);
        assertThat(squadPlayerRepository.findBySquadId(999L)).isEmpty();
    }
}
