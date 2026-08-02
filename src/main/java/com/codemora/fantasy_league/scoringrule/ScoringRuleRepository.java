package com.codemora.fantasy_league.scoringrule;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.codemora.fantasy_league.common.Position;

public interface ScoringRuleRepository extends JpaRepository<ScoringRule, Long> {

    List<ScoringRule> findBySeasonId(Long seasonId);

    Optional<ScoringRule> findBySeasonIdAndPosition(Long seasonId, Position position);

    boolean existsBySeasonId(Long seasonId);
}
