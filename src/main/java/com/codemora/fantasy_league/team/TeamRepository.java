package com.codemora.fantasy_league.team;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TeamRepository extends JpaRepository<Team, Long> {

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    // season_entrant/player/fixture don't have JPA entities yet (#10/#11/#19/#25 are
    // still open) -- these check the tables directly, which already exist per the
    // Flyway schema, rather than building out those features just for this check.

    @Query(value = "SELECT EXISTS (SELECT 1 FROM season_entrant WHERE team_id = :teamId)", nativeQuery = true)
    boolean isEnteredInAnySeason(@Param("teamId") Long teamId);

    @Query(value = "SELECT EXISTS (SELECT 1 FROM player WHERE team_id = :teamId)", nativeQuery = true)
    boolean hasAnyPlayers(@Param("teamId") Long teamId);

    @Query(value = "SELECT EXISTS (SELECT 1 FROM fixture WHERE home_team_id = :teamId OR away_team_id = :teamId)", nativeQuery = true)
    boolean hasAnyFixtures(@Param("teamId") Long teamId);
}
