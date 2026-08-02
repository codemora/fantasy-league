package com.codemora.fantasy_league.player;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.codemora.fantasy_league.common.Position;
import com.codemora.fantasy_league.scoringrule.ScoringRule;

class PlayerPerformanceTest {

    /** GK preset from ScoringRuleDefault: 10/3/4/2/1/-1/5/-2/-1/-3/-2. */
    private ScoringRule gkRule() {
        return ScoringRule.builder().seasonId(10L).createdByUserId(1L).position(Position.GK)
                .pointsPerGoal(10).pointsPerAssist(3).pointsPerCleanSheet(4)
                .pointsPerAppearance60(2).pointsPerAppearance1to59(1)
                .pointsPerGoalsConcededPerThree(-1).pointsPerPenaltySave(5)
                .pointsPerPenaltyMiss(-2).pointsPerYellowCard(-1).pointsPerRedCard(-3).pointsPerOwnGoal(-2)
                .build();
    }

    @Test
    void awardsGoalAssistCleanSheetAndFullAppearancePoints() {
        PlayerPerformance performance = PlayerPerformance.builder()
                .playerId(1L).fixtureId(100L)
                .goals(1).assists(1).minutesPlayed(90).cleanSheet(true)
                .build();

        // 1*10 (goal) + 1*3 (assist) + 4 (clean sheet) + 2 (played 60+) = 19
        assertThat(performance.getFantasyPoints(gkRule())).isEqualTo(19);
    }

    @Test
    void awardsPartialAppearancePointsInsteadOfFullAppearance() {
        PlayerPerformance performance = PlayerPerformance.builder()
                .playerId(1L).fixtureId(100L).minutesPlayed(45)
                .build();

        assertThat(performance.getFantasyPoints(gkRule())).isEqualTo(1);
    }

    @Test
    void awardsNoAppearancePointsWhenUnused() {
        PlayerPerformance performance = PlayerPerformance.builder()
                .playerId(1L).fixtureId(100L).minutesPlayed(0)
                .build();

        assertThat(performance.getFantasyPoints(gkRule())).isEqualTo(0);
    }

    @Test
    void deductsOnePointPerThreeGoalsConcededRoundingDown() {
        PlayerPerformance performance = PlayerPerformance.builder()
                .playerId(1L).fixtureId(100L).minutesPlayed(90).goalsConceded(5)
                .build();

        // 2 (played 60+) + floor(5/3)=1 * -1 = -1 => 1
        assertThat(performance.getFantasyPoints(gkRule())).isEqualTo(1);
    }

    @Test
    void appliesPenaltySavesMissesCardsAndOwnGoals() {
        PlayerPerformance performance = PlayerPerformance.builder()
                .playerId(1L).fixtureId(100L).minutesPlayed(90)
                .penaltiesSaved(1).penaltiesMissed(1).yellowCards(1).redCards(1).ownGoals(1)
                .build();

        // 2 (played 60+) + 5 (pen save) - 2 (pen miss) - 1 (yellow) - 3 (red) - 2 (own goal) = -1
        assertThat(performance.getFantasyPoints(gkRule())).isEqualTo(-1);
    }
}
