package com.codemora.fantasy_league.team;

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

@DataJpaTest
class TeamRepositoryTest {

    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private UserRepository userRepository;

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
}
