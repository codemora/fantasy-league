package com.codemora.fantasy_league.lineup;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GameweekLineupRepository extends JpaRepository<GameweekLineup, Long> {

    Optional<GameweekLineup> findBySquadIdAndGameweekId(Long squadId, Long gameweekId);
}
