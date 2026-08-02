package com.codemora.fantasy_league.league;

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
class LeagueRepositoryTest {

    @Autowired
    private LeagueRepository leagueRepository;
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
    void existsByNameReflectsSavedLeagues() {
        leagueRepository.save(League.builder().createdByUserId(adminId).name("Premier League").build());

        assertThat(leagueRepository.existsByName("Premier League")).isTrue();
        assertThat(leagueRepository.existsByName("La Liga")).isFalse();
    }

    @Test
    void leagueNameMustBeUnique() {
        leagueRepository.saveAndFlush(League.builder().createdByUserId(adminId).name("Premier League").build());

        assertThatThrownBy(() -> leagueRepository.saveAndFlush(
                        League.builder().createdByUserId(adminId).name("Premier League").build()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void existsByNameAndIdNotExcludesTheGivenLeagueButNotOthers() {
        League premierLeague = leagueRepository.save(League.builder().createdByUserId(adminId).name("Premier League").build());
        leagueRepository.save(League.builder().createdByUserId(adminId).name("La Liga").build());

        assertThat(leagueRepository.existsByNameAndIdNot("Premier League", premierLeague.getId())).isFalse();
        assertThat(leagueRepository.existsByNameAndIdNot("La Liga", premierLeague.getId())).isTrue();
    }
}
