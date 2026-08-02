package com.codemora.fantasy_league.season;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.codemora.fantasy_league.auth.Role;
import com.codemora.fantasy_league.auth.User;
import com.codemora.fantasy_league.auth.UserRepository;
import com.codemora.fantasy_league.league.League;
import com.codemora.fantasy_league.league.LeagueRepository;

@DataJpaTest
class SeasonRepositoryTest {

    @Autowired
    private SeasonRepository seasonRepository;
    @Autowired
    private LeagueRepository leagueRepository;
    @Autowired
    private UserRepository userRepository;

    private Long leagueId;

    @BeforeEach
    void setUp() {
        Long adminId = userRepository.save(User.builder()
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
}
