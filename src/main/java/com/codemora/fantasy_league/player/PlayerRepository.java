package com.codemora.fantasy_league.player;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.codemora.fantasy_league.common.Position;

public interface PlayerRepository extends JpaRepository<Player, Long> {

    List<Player> findByTeamId(Long teamId);

    List<Player> findByTeamIdAndPosition(Long teamId, Position position);

    long countByTeamId(Long teamId);
}
