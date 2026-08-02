package com.codemora.fantasy_league.scoringrule;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.codemora.fantasy_league.common.Position;
import com.codemora.fantasy_league.common.error.NotFoundException;
import com.codemora.fantasy_league.scoringrule.dto.ScoringRuleResponse;
import com.codemora.fantasy_league.scoringrule.dto.UpdateScoringRuleRequest;
import com.codemora.fantasy_league.season.Season;
import com.codemora.fantasy_league.season.SeasonRepository;

@ExtendWith(MockitoExtension.class)
class ScoringRuleServiceTest {

    @Mock
    private ScoringRuleRepository scoringRuleRepository;
    @Mock
    private SeasonRepository seasonRepository;

    private ScoringRuleService scoringRuleService() {
        return new ScoringRuleService(scoringRuleRepository, seasonRepository);
    }

    private Season season() {
        return Season.builder().id(10L).leagueId(1L).period("2025-26").teamLimit(20).startingBudget(1000).build();
    }

    private ScoringRule gkRule() {
        return ScoringRule.builder()
                .id(500L).seasonId(10L).createdByUserId(7L).position(Position.GK)
                .pointsPerGoal(10).pointsPerAssist(3).pointsPerCleanSheet(4).pointsPerAppearance60(2)
                .pointsPerAppearance1to59(1).pointsPerGoalsConcededPerThree(-1).pointsPerPenaltySave(5)
                .pointsPerPenaltyMiss(-2).pointsPerYellowCard(-1).pointsPerRedCard(-3).pointsPerOwnGoal(-2)
                .build();
    }

    @Test
    void seedDefaultsSavesOneRulePerPosition() {
        scoringRuleService().seedDefaults(10L, 7L);

        verify(scoringRuleRepository, times(4)).save(any(ScoringRule.class));
    }

    @Test
    void findBySeasonReturnsAllRules() {
        when(seasonRepository.findById(10L)).thenReturn(Optional.of(season()));
        when(scoringRuleRepository.findBySeasonId(10L)).thenReturn(List.of(gkRule()));

        List<ScoringRuleResponse> rules = scoringRuleService().findBySeason(1L, 10L);

        assertThat(rules).hasSize(1);
        assertThat(rules.get(0).position()).isEqualTo(Position.GK);
    }

    @Test
    void findBySeasonRejectsUnknownSeason() {
        when(seasonRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> scoringRuleService().findBySeason(1L, 99L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void findBySeasonRejectsSeasonInDifferentLeague() {
        Season wrongLeagueSeason = Season.builder().id(10L).leagueId(2L).period("2025-26").teamLimit(20).startingBudget(1000).build();
        when(seasonRepository.findById(10L)).thenReturn(Optional.of(wrongLeagueSeason));

        assertThatThrownBy(() -> scoringRuleService().findBySeason(1L, 10L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateSavesTheNewPointValues() {
        when(seasonRepository.findById(10L)).thenReturn(Optional.of(season()));
        when(scoringRuleRepository.findBySeasonIdAndPosition(10L, Position.GK)).thenReturn(Optional.of(gkRule()));
        when(scoringRuleRepository.save(any(ScoringRule.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ScoringRuleResponse response = scoringRuleService().update(1L, 10L, Position.GK,
                new UpdateScoringRuleRequest(12, 4, 5, 2, 1, -1, 6, -2, -1, -3, -2));

        assertThat(response.pointsPerGoal()).isEqualTo(12);
        assertThat(response.pointsPerPenaltySave()).isEqualTo(6);
    }

    @Test
    void updateRejectsUnknownSeason() {
        when(seasonRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> scoringRuleService().update(1L, 99L, Position.GK,
                new UpdateScoringRuleRequest(12, 4, 5, 2, 1, -1, 6, -2, -1, -3, -2)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateRejectsWhenNoRuleExistsForThePosition() {
        when(seasonRepository.findById(10L)).thenReturn(Optional.of(season()));
        when(scoringRuleRepository.findBySeasonIdAndPosition(10L, Position.GK)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> scoringRuleService().update(1L, 10L, Position.GK,
                new UpdateScoringRuleRequest(12, 4, 5, 2, 1, -1, 6, -2, -1, -3, -2)))
                .isInstanceOf(NotFoundException.class);
    }
}
