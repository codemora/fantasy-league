package com.codemora.fantasy_league.season;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.codemora.fantasy_league.auth.Role;
import com.codemora.fantasy_league.auth.User;
import com.codemora.fantasy_league.auth.UserRepository;
import com.codemora.fantasy_league.league.League;
import com.codemora.fantasy_league.league.LeagueRepository;
import com.codemora.fantasy_league.team.Team;
import com.codemora.fantasy_league.team.TeamRepository;

@DataJpaTest
class SeasonEntrantRepositoryTest {

    @Autowired
    private SeasonEntrantRepository seasonEntrantRepository;
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
    void existsBySeasonIdAndTeamIdAndCountBySeasonIdReflectSavedEntrants() {
        assertThat(seasonEntrantRepository.existsBySeasonIdAndTeamId(seasonId, arsenalId)).isFalse();
        assertThat(seasonEntrantRepository.countBySeasonId(seasonId)).isZero();

        seasonEntrantRepository.save(SeasonEntrant.builder().seasonId(seasonId).teamId(arsenalId).build());
        seasonEntrantRepository.save(SeasonEntrant.builder().seasonId(seasonId).teamId(chelseaId).build());

        assertThat(seasonEntrantRepository.existsBySeasonIdAndTeamId(seasonId, arsenalId)).isTrue();
        assertThat(seasonEntrantRepository.existsBySeasonIdAndTeamId(seasonId, 999L)).isFalse();
        assertThat(seasonEntrantRepository.countBySeasonId(seasonId)).isEqualTo(2L);
    }
}
