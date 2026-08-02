package com.codemora.fantasy_league.player;

import com.codemora.fantasy_league.scoringrule.ScoringRule;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "player_performance")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class PlayerPerformance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "performance_id")
    private Long id;

    @Column(name = "player_id", nullable = false)
    private Long playerId;

    @Column(name = "fixture_id", nullable = false)
    private Long fixtureId;

    @Column(nullable = false)
    private int goals;

    @Column(nullable = false)
    private int assists;

    @Column(name = "minutes_played", nullable = false)
    private int minutesPlayed;

    @Column(name = "clean_sheet", nullable = false)
    private boolean cleanSheet;

    @Column(name = "goals_conceded", nullable = false)
    private int goalsConceded;

    @Column(name = "own_goals", nullable = false)
    private int ownGoals;

    @Column(name = "penalties_saved", nullable = false)
    private int penaltiesSaved;

    @Column(name = "penalties_missed", nullable = false)
    private int penaltiesMissed;

    @Column(name = "yellow_cards", nullable = false)
    private int yellowCards;

    @Column(name = "red_cards", nullable = false)
    private int redCards;

    /** Matches README's Scoring Rules table; the "-" cells are already 0 on the position's ScoringRule row. */
    public int getFantasyPoints(ScoringRule rule) {
        int points = goals * rule.getPointsPerGoal()
                + assists * rule.getPointsPerAssist()
                + (goalsConceded / 3) * rule.getPointsPerGoalsConcededPerThree()
                + penaltiesSaved * rule.getPointsPerPenaltySave()
                + penaltiesMissed * rule.getPointsPerPenaltyMiss()
                + yellowCards * rule.getPointsPerYellowCard()
                + redCards * rule.getPointsPerRedCard()
                + ownGoals * rule.getPointsPerOwnGoal();
        if (cleanSheet) {
            points += rule.getPointsPerCleanSheet();
        }
        if (minutesPlayed >= 60) {
            points += rule.getPointsPerAppearance60();
        } else if (minutesPlayed >= 1) {
            points += rule.getPointsPerAppearance1to59();
        }
        return points;
    }
}
