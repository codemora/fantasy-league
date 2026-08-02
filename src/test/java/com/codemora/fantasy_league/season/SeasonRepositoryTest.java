package com.codemora.fantasy_league.season;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

import com.codemora.fantasy_league.auth.Role;
import com.codemora.fantasy_league.auth.User;
import com.codemora.fantasy_league.auth.UserRepository;
import com.codemora.fantasy_league.league.League;
import com.codemora.fantasy_league.league.LeagueRepository;
import com.codemora.fantasy_league.team.Team;
import com.codemora.fantasy_league.team.TeamRepository;

@DataJpaTest
class SeasonRepositoryTest {

    @Autowired
    private SeasonRepository seasonRepository;
    @Autowired
    private LeagueRepository leagueRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Long leagueId;
    private Long adminId;

    @BeforeEach
    void setUp() {
        adminId = userRepository.save(User.builder()
                        .username("admin")
                        .passwordHash("hashed")
                        .role(Role.ADMIN)
                        .build())
                .getId();
        leagueId = leagueRepository.save(League.builder().createdByUserId(adminId).name("Premier League").build())
                .getId();
    }

    @Test
    void savedSeasonRoundTripsAllFieldsIncludingBooleanAndDates() {
        Season saved = seasonRepository.save(Season.builder()
                .leagueId(leagueId)
                .period("2025-26")
                .teamLimit(20)
                .startingBudget(1000)
                .doubleLeg(true)
                .startDate(LocalDate.of(2025, 8, 1))
                .endDate(LocalDate.of(2026, 5, 24))
                .build());

        Season found = seasonRepository.findById(saved.getId()).orElseThrow();

        assertThat(found.getLeagueId()).isEqualTo(leagueId);
        assertThat(found.getPeriod()).isEqualTo("2025-26");
        assertThat(found.getTeamLimit()).isEqualTo(20);
        assertThat(found.getStartingBudget()).isEqualTo(1000);
        assertThat(found.isDoubleLeg()).isTrue();
        assertThat(found.getStartDate()).isEqualTo(LocalDate.of(2025, 8, 1));
        assertThat(found.getEndDate()).isEqualTo(LocalDate.of(2026, 5, 24));
    }

    @Test
    void countEntrantsReflectsSeasonEntrantRows() {
        Season season = seasonRepository.save(
                Season.builder().leagueId(leagueId).period("2025-26").teamLimit(20).startingBudget(1000).build());
        Team arsenal = teamRepository.save(Team.builder().createdByUserId(adminId).name("Arsenal").build());
        Team chelsea = teamRepository.save(Team.builder().createdByUserId(adminId).name("Chelsea").build());

        assertThat(seasonRepository.countEntrants(season.getId())).isZero();

        jdbcTemplate.update(
                "INSERT INTO season_entrant (season_id, team_id) VALUES (?, ?)", season.getId(), arsenal.getId());
        jdbcTemplate.update(
                "INSERT INTO season_entrant (season_id, team_id) VALUES (?, ?)", season.getId(), chelsea.getId());

        assertThat(seasonRepository.countEntrants(season.getId())).isEqualTo(2L);
    }

    @Test
    void usageChecksAreAllFalseForAnUnusedSeason() {
        Season season = seasonRepository.save(
                Season.builder().leagueId(leagueId).period("2025-26").teamLimit(20).startingBudget(1000).build());

        // Exercises the actual native SQL against H2 -- same rationale as
        // TeamRepositoryTest's equivalent test.
        assertThat(seasonRepository.hasAnyFixtures(season.getId())).isFalse();
        assertThat(seasonRepository.hasAnyFantasySquads(season.getId())).isFalse();
    }

    @Test
    void hasAnyFantasySquadsIsTrueOnceASquadExists() {
        Season season = seasonRepository.save(
                Season.builder().leagueId(leagueId).period("2025-26").teamLimit(20).startingBudget(1000).build());

        jdbcTemplate.update(
                "INSERT INTO fantasy_squad (user_id, season_id, bank_balance, free_transfers) VALUES (?, ?, ?, ?)",
                adminId, season.getId(), 1000, 1);

        assertThat(seasonRepository.hasAnyFantasySquads(season.getId())).isTrue();
    }
}
