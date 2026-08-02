package com.codemora.fantasy_league.scoringrule;

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
import com.codemora.fantasy_league.common.Position;
import com.codemora.fantasy_league.league.League;
import com.codemora.fantasy_league.league.LeagueRepository;
import com.codemora.fantasy_league.season.Season;
import com.codemora.fantasy_league.season.SeasonRepository;

@DataJpaTest
class ScoringRuleRepositoryTest {

    @Autowired
    private ScoringRuleRepository scoringRuleRepository;
    @Autowired
    private SeasonRepository seasonRepository;
    @Autowired
    private LeagueRepository leagueRepository;
    @Autowired
    private UserRepository userRepository;

    private Long adminId;
    private Long seasonId;

    @BeforeEach
    void setUp() {
        adminId = userRepository.save(User.builder().username("admin").passwordHash("hashed").role(Role.ADMIN).build()).getId();
        Long leagueId = leagueRepository.save(League.builder().createdByUserId(adminId).name("Premier League").build()).getId();
        seasonId = seasonRepository.save(Season.builder().leagueId(leagueId).period("2025-26").teamLimit(20).startingBudget(1000).build()).getId();
    }

    private ScoringRule.ScoringRuleBuilder rule(Position position) {
        return ScoringRule.builder()
                .seasonId(seasonId).createdByUserId(adminId).position(position)
                .pointsPerGoal(10).pointsPerAssist(3).pointsPerCleanSheet(4).pointsPerAppearance60(2)
                .pointsPerAppearance1to59(1).pointsPerGoalsConcededPerThree(-1).pointsPerPenaltySave(5)
                .pointsPerPenaltyMiss(-2).pointsPerYellowCard(-1).pointsPerRedCard(-3).pointsPerOwnGoal(-2);
    }

    @Test
    void seasonAndPositionMustBeUnique() {
        scoringRuleRepository.saveAndFlush(rule(Position.GK).build());

        assertThatThrownBy(() -> scoringRuleRepository.saveAndFlush(rule(Position.GK).build()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findBySeasonIdReturnsAllPositionsForThatSeason() {
        scoringRuleRepository.save(rule(Position.GK).build());
        scoringRuleRepository.save(rule(Position.DEF).build());

        assertThat(scoringRuleRepository.findBySeasonId(seasonId)).hasSize(2);
    }

    @Test
    void findBySeasonIdAndPositionReturnsTheMatchingRule() {
        scoringRuleRepository.save(rule(Position.FWD).build());

        assertThat(scoringRuleRepository.findBySeasonIdAndPosition(seasonId, Position.FWD)).isPresent();
        assertThat(scoringRuleRepository.findBySeasonIdAndPosition(seasonId, Position.MID)).isEmpty();
    }
}
