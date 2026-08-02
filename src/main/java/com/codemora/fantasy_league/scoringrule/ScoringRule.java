package com.codemora.fantasy_league.scoringrule;

import com.codemora.fantasy_league.common.Position;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "scoring_rule")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class ScoringRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "rule_id")
    private Long id;

    @Column(name = "season_id", nullable = false)
    private Long seasonId;

    @Column(name = "created_by_user_id", nullable = false)
    private Long createdByUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Position position;

    @Column(name = "points_per_goal", nullable = false)
    private Integer pointsPerGoal;

    @Column(name = "points_per_assist", nullable = false)
    private Integer pointsPerAssist;

    @Column(name = "points_per_clean_sheet", nullable = false)
    private Integer pointsPerCleanSheet;

    @Column(name = "points_per_appearance_60", nullable = false)
    private Integer pointsPerAppearance60;

    @Column(name = "points_per_appearance_1to59", nullable = false)
    private Integer pointsPerAppearance1to59;

    @Column(name = "points_per_goals_conceded_per_three", nullable = false)
    private Integer pointsPerGoalsConcededPerThree;

    @Column(name = "points_per_penalty_save", nullable = false)
    private Integer pointsPerPenaltySave;

    @Column(name = "points_per_penalty_miss", nullable = false)
    private Integer pointsPerPenaltyMiss;

    @Column(name = "points_per_yellow_card", nullable = false)
    private Integer pointsPerYellowCard;

    @Column(name = "points_per_red_card", nullable = false)
    private Integer pointsPerRedCard;

    @Column(name = "points_per_own_goal", nullable = false)
    private Integer pointsPerOwnGoal;
}
