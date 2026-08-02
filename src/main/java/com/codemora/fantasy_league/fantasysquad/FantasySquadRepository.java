package com.codemora.fantasy_league.fantasysquad;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FantasySquadRepository extends JpaRepository<FantasySquad, Long> {

    Optional<FantasySquad> findByUserIdAndSeasonId(Long userId, Long seasonId);

    boolean existsByUserIdAndSeasonId(Long userId, Long seasonId);
}
