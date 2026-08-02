package com.codemora.fantasy_league.scoringrule.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateScoringRuleRequest(
        @NotNull Integer pointsPerGoal,
        @NotNull Integer pointsPerAssist,
        @NotNull Integer pointsPerCleanSheet,
        @NotNull Integer pointsPerAppearance60,
        @NotNull Integer pointsPerAppearance1to59,
        @NotNull Integer pointsPerGoalsConcededPerThree,
        @NotNull Integer pointsPerPenaltySave,
        @NotNull Integer pointsPerPenaltyMiss,
        @NotNull Integer pointsPerYellowCard,
        @NotNull Integer pointsPerRedCard,
        @NotNull Integer pointsPerOwnGoal) {
}
