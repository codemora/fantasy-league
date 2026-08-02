package com.codemora.fantasy_league.season;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SeasonEntrantRepository extends JpaRepository<SeasonEntrant, Long> {

    boolean existsBySeasonIdAndTeamId(Long seasonId, Long teamId);

    long countBySeasonId(Long seasonId);
}
