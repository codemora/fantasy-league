package com.codemora.fantasy_league.player;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PlayerPerformanceRepository extends JpaRepository<PlayerPerformance, Long> {

    List<PlayerPerformance> findByPlayerId(Long playerId);

    Optional<PlayerPerformance> findByPlayerIdAndFixtureId(Long playerId, Long fixtureId);

    List<PlayerPerformance> findByFixtureId(Long fixtureId);
}
