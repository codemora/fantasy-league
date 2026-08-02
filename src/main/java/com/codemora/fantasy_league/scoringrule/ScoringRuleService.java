package com.codemora.fantasy_league.scoringrule;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.codemora.fantasy_league.common.Position;
import com.codemora.fantasy_league.common.error.NotFoundException;
import com.codemora.fantasy_league.scoringrule.dto.ScoringRuleResponse;
import com.codemora.fantasy_league.scoringrule.dto.UpdateScoringRuleRequest;
import com.codemora.fantasy_league.season.Season;
import com.codemora.fantasy_league.season.SeasonRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ScoringRuleService {

    private final ScoringRuleRepository scoringRuleRepository;
    private final SeasonRepository seasonRepository;

    public ScoringRuleService(ScoringRuleRepository scoringRuleRepository, SeasonRepository seasonRepository) {
        this.scoringRuleRepository = scoringRuleRepository;
        this.seasonRepository = seasonRepository;
    }

    /**
     * Called from SeasonService#create so every new season starts with a
     * sensible, adjustable rule set (the README Scoring Rules table) rather
     * than requiring the admin to configure four rows before anything can be
     * scored.
     */
    @Transactional
    public void seedDefaults(Long seasonId, Long createdByUserId) {
        for (ScoringRuleDefault preset : ScoringRuleDefault.values()) {
            scoringRuleRepository.save(preset.toEntity(seasonId, createdByUserId));
        }
        log.info("scoring_rules_seeded season_id={}", seasonId);
    }

    public List<ScoringRuleResponse> findBySeason(Long leagueId, Long seasonId) {
        findInLeague(leagueId, seasonId);
        return scoringRuleRepository.findBySeasonId(seasonId).stream().map(this::toResponse).toList();
    }

    /**
     * Editing a rule only affects points calculated from this point forward
     * (points are computed live from stats + the current rule, per ADR/README
     * -- see StandingsService and PlayerPerformance#getFantasyPoints), never
     * rewriting a gameweek that's already COMPLETE.
     */
    @Transactional
    public ScoringRuleResponse update(Long leagueId, Long seasonId, Position position, UpdateScoringRuleRequest request) {
        findInLeague(leagueId, seasonId);
        ScoringRule rule = scoringRuleRepository.findBySeasonIdAndPosition(seasonId, position)
                .orElseThrow(() -> new NotFoundException("No scoring rule for position " + position + " in season " + seasonId));
        rule.setPointsPerGoal(request.pointsPerGoal());
        rule.setPointsPerAssist(request.pointsPerAssist());
        rule.setPointsPerCleanSheet(request.pointsPerCleanSheet());
        rule.setPointsPerAppearance60(request.pointsPerAppearance60());
        rule.setPointsPerAppearance1to59(request.pointsPerAppearance1to59());
        rule.setPointsPerGoalsConcededPerThree(request.pointsPerGoalsConcededPerThree());
        rule.setPointsPerPenaltySave(request.pointsPerPenaltySave());
        rule.setPointsPerPenaltyMiss(request.pointsPerPenaltyMiss());
        rule.setPointsPerYellowCard(request.pointsPerYellowCard());
        rule.setPointsPerRedCard(request.pointsPerRedCard());
        rule.setPointsPerOwnGoal(request.pointsPerOwnGoal());
        ScoringRule saved = scoringRuleRepository.save(rule);
        log.info("scoring_rule_updated season_id={} position={}", seasonId, position);
        return toResponse(saved);
    }

    private Season findInLeague(Long leagueId, Long seasonId) {
        Season season = seasonRepository.findById(seasonId)
                .orElseThrow(() -> new NotFoundException("No season with id " + seasonId));
        if (!season.getLeagueId().equals(leagueId)) {
            throw new NotFoundException("No season with id " + seasonId + " under league " + leagueId);
        }
        return season;
    }

    private ScoringRuleResponse toResponse(ScoringRule rule) {
        return new ScoringRuleResponse(
                rule.getId(),
                rule.getSeasonId(),
                rule.getPosition(),
                rule.getPointsPerGoal(),
                rule.getPointsPerAssist(),
                rule.getPointsPerCleanSheet(),
                rule.getPointsPerAppearance60(),
                rule.getPointsPerAppearance1to59(),
                rule.getPointsPerGoalsConcededPerThree(),
                rule.getPointsPerPenaltySave(),
                rule.getPointsPerPenaltyMiss(),
                rule.getPointsPerYellowCard(),
                rule.getPointsPerRedCard(),
                rule.getPointsPerOwnGoal());
    }
}
