package com.codemora.fantasy_league.player;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.codemora.fantasy_league.auth.Role;
import com.codemora.fantasy_league.auth.User;
import com.codemora.fantasy_league.auth.UserRepository;
import com.codemora.fantasy_league.common.Position;
import com.codemora.fantasy_league.team.Team;
import com.codemora.fantasy_league.team.TeamRepository;

@DataJpaTest
class PlayerRepositoryTest {

    @Autowired
    private PlayerRepository playerRepository;
    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private UserRepository userRepository;

    private Long adminId;
    private Long teamId;

    @BeforeEach
    void setUp() {
        adminId = userRepository.save(User.builder().username("admin").passwordHash("hashed").role(Role.ADMIN).build()).getId();
        teamId = teamRepository.save(Team.builder().createdByUserId(adminId).name("Arsenal").build()).getId();
    }

    @Test
    void findByTeamIdReturnsOnlyThatTeamsPlayers() {
        playerRepository.save(Player.builder().teamId(teamId).createdByUserId(adminId).name("Bruno Silva").position(Position.MID).marketValue(65).build());
        Long otherTeamId = teamRepository.save(Team.builder().createdByUserId(adminId).name("Chelsea").build()).getId();
        playerRepository.save(Player.builder().teamId(otherTeamId).createdByUserId(adminId).name("Marcus Costa").position(Position.FWD).marketValue(80).build());

        assertThat(playerRepository.findByTeamId(teamId)).extracting(Player::getName).containsExactly("Bruno Silva");
    }

    @Test
    void findByTeamIdAndPositionFiltersByPosition() {
        playerRepository.save(Player.builder().teamId(teamId).createdByUserId(adminId).name("Bruno Silva").position(Position.MID).marketValue(65).build());
        playerRepository.save(Player.builder().teamId(teamId).createdByUserId(adminId).name("Marcus Costa").position(Position.FWD).marketValue(80).build());

        assertThat(playerRepository.findByTeamIdAndPosition(teamId, Position.FWD))
                .extracting(Player::getName).containsExactly("Marcus Costa");
    }

    @Test
    void countByTeamIdReflectsSavedPlayers() {
        assertThat(playerRepository.countByTeamId(teamId)).isZero();

        playerRepository.save(Player.builder().teamId(teamId).createdByUserId(adminId).name("Bruno Silva").position(Position.MID).marketValue(65).build());

        assertThat(playerRepository.countByTeamId(teamId)).isEqualTo(1);
    }
}
