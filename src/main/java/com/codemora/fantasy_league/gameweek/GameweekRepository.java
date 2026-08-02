package com.codemora.fantasy_league.gameweek;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GameweekRepository extends JpaRepository<Gameweek, Long> {

    List<Gameweek> findBySeasonIdOrderByNumber(Long seasonId);
}
