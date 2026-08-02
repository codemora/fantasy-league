package com.codemora.fantasy_league.scoringrule;

import com.codemora.fantasy_league.common.Position;

/**
 * The default scoring rule set a new season is seeded with -- the README
 * Scoring Rules table, verbatim. "-" cells in that table (a stat that doesn't
 * apply to a position, e.g. penalty saves for outfield players) become 0.
 */
enum ScoringRuleDefault {

    GK(Position.GK, 10, 3, 4, 2, 1, -1, 5, -2, -1, -3, -2),
    DEF(Position.DEF, 6, 3, 4, 2, 1, -1, 0, -2, -1, -3, -2),
    MID(Position.MID, 5, 3, 1, 2, 1, 0, 0, -2, -1, -3, -2),
    FWD(Position.FWD, 4, 3, 0, 2, 1, 0, 0, -2, -1, -3, -2);

    private final Position position;
    private final int pointsPerGoal;
    private final int pointsPerAssist;
    private final int pointsPerCleanSheet;
    private final int pointsPerAppearance60;
    private final int pointsPerAppearance1to59;
    private final int pointsPerGoalsConcededPerThree;
    private final int pointsPerPenaltySave;
    private final int pointsPerPenaltyMiss;
    private final int pointsPerYellowCard;
    private final int pointsPerRedCard;
    private final int pointsPerOwnGoal;

    ScoringRuleDefault(
            Position position,
            int pointsPerGoal,
            int pointsPerAssist,
            int pointsPerCleanSheet,
            int pointsPerAppearance60,
            int pointsPerAppearance1to59,
            int pointsPerGoalsConcededPerThree,
            int pointsPerPenaltySave,
            int pointsPerPenaltyMiss,
            int pointsPerYellowCard,
            int pointsPerRedCard,
            int pointsPerOwnGoal) {
        this.position = position;
        this.pointsPerGoal = pointsPerGoal;
        this.pointsPerAssist = pointsPerAssist;
        this.pointsPerCleanSheet = pointsPerCleanSheet;
        this.pointsPerAppearance60 = pointsPerAppearance60;
        this.pointsPerAppearance1to59 = pointsPerAppearance1to59;
        this.pointsPerGoalsConcededPerThree = pointsPerGoalsConcededPerThree;
        this.pointsPerPenaltySave = pointsPerPenaltySave;
        this.pointsPerPenaltyMiss = pointsPerPenaltyMiss;
        this.pointsPerYellowCard = pointsPerYellowCard;
        this.pointsPerRedCard = pointsPerRedCard;
        this.pointsPerOwnGoal = pointsPerOwnGoal;
    }

    ScoringRule toEntity(Long seasonId, Long createdByUserId) {
        return ScoringRule.builder()
                .seasonId(seasonId)
                .createdByUserId(createdByUserId)
                .position(position)
                .pointsPerGoal(pointsPerGoal)
                .pointsPerAssist(pointsPerAssist)
                .pointsPerCleanSheet(pointsPerCleanSheet)
                .pointsPerAppearance60(pointsPerAppearance60)
                .pointsPerAppearance1to59(pointsPerAppearance1to59)
                .pointsPerGoalsConcededPerThree(pointsPerGoalsConcededPerThree)
                .pointsPerPenaltySave(pointsPerPenaltySave)
                .pointsPerPenaltyMiss(pointsPerPenaltyMiss)
                .pointsPerYellowCard(pointsPerYellowCard)
                .pointsPerRedCard(pointsPerRedCard)
                .pointsPerOwnGoal(pointsPerOwnGoal)
                .build();
    }
}
