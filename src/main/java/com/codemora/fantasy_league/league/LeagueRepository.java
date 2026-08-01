package com.codemora.fantasy_league.league;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LeagueRepository extends JpaRepository<League, Long> {

    boolean existsByName(String name);
}
