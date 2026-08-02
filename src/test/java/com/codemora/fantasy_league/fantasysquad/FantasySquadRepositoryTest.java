package com.codemora.fantasy_league.fantasysquad;

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
import com.codemora.fantasy_league.league.League;
import com.codemora.fantasy_league.league.LeagueRepository;
import com.codemora.fantasy_league.season.Season;
import com.codemora.fantasy_league.season.SeasonRepository;

@DataJpaTest
class FantasySquadRepositoryTest {

    @Autowired
    private FantasySquadRepository fantasySquadRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private LeagueRepository leagueRepository;
    @Autowired
    private SeasonRepository seasonRepository;

    private Long userId;
    private Long seasonId;

    @BeforeEach
    void setUp() {
        userId = userRepository.save(User.builder().username("alice").passwordHash("hashed").role(Role.USER).build()).getId();
        Long adminId = userRepository.save(User.builder().username("admin").passwordHash("hashed").role(Role.ADMIN).build()).getId();
        Long leagueId = leagueRepository.save(League.builder().createdByUserId(adminId).name("Premier League").build()).getId();
        seasonId = seasonRepository.save(Season.builder().leagueId(leagueId).period("2025-26").teamLimit(20).startingBudget(1000).build()).getId();
    }

    @Test
    void userAndSeasonMustBeUnique() {
        fantasySquadRepository.saveAndFlush(FantasySquad.builder().userId(userId).seasonId(seasonId).bankBalance(100).freeTransfers(1).build());

        assertThatThrownBy(() -> fantasySquadRepository.saveAndFlush(
                FantasySquad.builder().userId(userId).seasonId(seasonId).bankBalance(50).freeTransfers(1).build()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findByUserIdAndSeasonIdReturnsTheSquad() {
        fantasySquadRepository.save(FantasySquad.builder().userId(userId).seasonId(seasonId).bankBalance(100).freeTransfers(1).build());

        assertThat(fantasySquadRepository.findByUserIdAndSeasonId(userId, seasonId)).isPresent();
        assertThat(fantasySquadRepository.findByUserIdAndSeasonId(999L, seasonId)).isEmpty();
    }

    @Test
    void existsByUserIdAndSeasonIdReflectsSavedSquads() {
        assertThat(fantasySquadRepository.existsByUserIdAndSeasonId(userId, seasonId)).isFalse();

        fantasySquadRepository.save(FantasySquad.builder().userId(userId).seasonId(seasonId).bankBalance(100).freeTransfers(1).build());

        assertThat(fantasySquadRepository.existsByUserIdAndSeasonId(userId, seasonId)).isTrue();
    }
}
