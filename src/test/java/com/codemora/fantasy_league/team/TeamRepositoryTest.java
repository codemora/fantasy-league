package com.codemora.fantasy_league.team;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.codemora.fantasy_league.auth.Role;
import com.codemora.fantasy_league.auth.User;
import com.codemora.fantasy_league.auth.UserRepository;

@DataJpaTest
class TeamRepositoryTest {

    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long adminId;

    @BeforeEach
    void setUp() {
        adminId = userRepository.save(User.builder()
                        .username("admin")
                        .passwordHash("hashed")
                        .role(Role.ADMIN)
                        .build())
                .getId();
    }

    @Test
    void existsByNameReflectsSavedTeams() {
        teamRepository.save(Team.builder().createdByUserId(adminId).name("Arsenal").build());

        assertThat(teamRepository.existsByName("Arsenal")).isTrue();
        assertThat(teamRepository.existsByName("Chelsea")).isFalse();
    }

    @Test
    void teamNameMustBeUnique() {
        teamRepository.saveAndFlush(Team.builder().createdByUserId(adminId).name("Arsenal").build());

        assertThatThrownBy(() -> teamRepository.saveAndFlush(Team.builder().createdByUserId(adminId).name("Arsenal").build()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void existsByNameAndIdNotExcludesTheGivenTeamButNotOthers() {
        Team arsenal = teamRepository.save(Team.builder().createdByUserId(adminId).name("Arsenal").build());
        teamRepository.save(Team.builder().createdByUserId(adminId).name("Chelsea").build());

        assertThat(teamRepository.existsByNameAndIdNot("Arsenal", arsenal.getId())).isFalse();
        assertThat(teamRepository.existsByNameAndIdNot("Chelsea", arsenal.getId())).isTrue();
    }

    @Test
    void usageChecksAreAllFalseForAnUnusedTeam() {
        Team arsenal = teamRepository.save(Team.builder().createdByUserId(adminId).name("Arsenal").build());

        // Exercises the actual native SQL against H2, not just the decision logic
        // (already covered via mocks in TeamServiceTest) -- this is exactly the
        // class of bug the "value" reserved-word issue was (valid on one database,
        // rejected by the other).
        assertThat(teamRepository.isEnteredInAnySeason(arsenal.getId())).isFalse();
        assertThat(teamRepository.hasAnyPlayers(arsenal.getId())).isFalse();
        assertThat(teamRepository.hasAnyFixtures(arsenal.getId())).isFalse();
    }

    @Test
    void hasAnyPlayersIsTrueOnceATeamHasAPlayer() {
        Team arsenal = teamRepository.save(Team.builder().createdByUserId(adminId).name("Arsenal").build());

        jdbcTemplate.update(
                "INSERT INTO player (team_id, created_by_user_id, name, position, market_value) VALUES (?, ?, ?, ?, ?)",
                arsenal.getId(), adminId, "Bukayo Saka", "MID", 100);

        assertThat(teamRepository.hasAnyPlayers(arsenal.getId())).isTrue();
    }
}
