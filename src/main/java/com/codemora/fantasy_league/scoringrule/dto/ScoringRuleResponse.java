package com.codemora.fantasy_league.scoringrule.dto;

import com.codemora.fantasy_league.common.Position;

public record ScoringRuleResponse(
        Long id,
        Long seasonId,
        Position position,
        Integer pointsPerGoal,
        Integer pointsPerAssist,
        Integer pointsPerCleanSheet,
        Integer pointsPerAppearance60,
        Integer pointsPerAppearance1to59,
        Integer pointsPerGoalsConcededPerThree,
        Integer pointsPerPenaltySave,
        Integer pointsPerPenaltyMiss,
        Integer pointsPerYellowCard,
        Integer pointsPerRedCard,
        Integer pointsPerOwnGoal) {
}
