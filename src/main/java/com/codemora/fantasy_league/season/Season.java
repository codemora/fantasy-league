package com.codemora.fantasy_league.season;

import java.time.LocalDate;

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
@Table(name = "season")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Season {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "season_id")
    private Long id;

    @Column(name = "league_id", nullable = false)
    private Long leagueId;

    @Column(nullable = false, length = 32)
    private String period;

    @Column(name = "team_limit", nullable = false)
    private Integer teamLimit;

    @Column(name = "starting_budget", nullable = false)
    private Integer startingBudget;

    @Column(name = "is_double_leg", nullable = false)
    private boolean doubleLeg;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;
}
