package com.codemora.fantasy_league.season;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SeasonEntrantRepository extends JpaRepository<SeasonEntrant, Long> {

    boolean existsBySeasonIdAndTeamId(Long seasonId, Long teamId);

    long countBySeasonId(Long seasonId);

    Optional<SeasonEntrant> findBySeasonIdAndTeamId(Long seasonId, Long teamId);

    List<SeasonEntrant> findBySeasonId(Long seasonId);
}
