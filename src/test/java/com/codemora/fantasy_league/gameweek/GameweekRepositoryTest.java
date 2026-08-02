package com.codemora.fantasy_league.gameweek;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.codemora.fantasy_league.auth.Role;
import com.codemora.fantasy_league.auth.User;
import com.codemora.fantasy_league.auth.UserRepository;
import com.codemora.fantasy_league.league.League;
import com.codemora.fantasy_league.league.LeagueRepository;
import com.codemora.fantasy_league.season.Season;
import com.codemora.fantasy_league.season.SeasonRepository;

@DataJpaTest
class GameweekRepositoryTest {

    @Autowired
    private GameweekRepository gameweekRepository;
    @Autowired
    private SeasonRepository seasonRepository;
    @Autowired
    private LeagueRepository leagueRepository;
    @Autowired
    private UserRepository userRepository;

    private Long seasonId;

    @BeforeEach
    void setUp() {
        Long adminId = userRepository.save(User.builder().username("admin").passwordHash("hashed").role(Role.ADMIN).build()).getId();
        Long leagueId = leagueRepository.save(League.builder().createdByUserId(adminId).name("Premier League").build()).getId();
        seasonId = seasonRepository.save(Season.builder().leagueId(leagueId).period("2025-26").teamLimit(20).startingBudget(1000).build()).getId();
    }

    @Test
    void findBySeasonIdOrderByNumberReturnsThemInAscendingOrder() {
        gameweekRepository.save(Gameweek.builder().seasonId(seasonId).number(3)
                .deadlineDateTime(LocalDateTime.of(2025, 9, 1, 12, 0)).status(GameweekStatus.UPCOMING).build());
        gameweekRepository.save(Gameweek.builder().seasonId(seasonId).number(1)
                .deadlineDateTime(LocalDateTime.of(2025, 8, 1, 12, 0)).status(GameweekStatus.COMPLETE).build());
        gameweekRepository.save(Gameweek.builder().seasonId(seasonId).number(2)
                .deadlineDateTime(LocalDateTime.of(2025, 8, 8, 12, 0)).status(GameweekStatus.UPCOMING).build());

        assertThat(gameweekRepository.findBySeasonIdOrderByNumber(seasonId))
                .extracting(Gameweek::getNumber).containsExactly(1, 2, 3);
    }
}
