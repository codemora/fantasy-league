package com.codemora.fantasy_league.fantasysquad;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SquadPlayerRepository extends JpaRepository<SquadPlayer, Long> {

    List<SquadPlayer> findBySquadId(Long squadId);
}
