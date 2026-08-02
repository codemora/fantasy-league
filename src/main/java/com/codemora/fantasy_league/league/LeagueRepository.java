package com.codemora.fantasy_league.league;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LeagueRepository extends JpaRepository<League, Long> {

    boolean existsByName(String name);

    boolean existsByNameAndIdNot(String name, Long id);

    // season doesn't have a JPA entity yet (#10 is still open) -- see TeamRepository
    // for the same pattern, and why: querying the table directly rather than
    // building out the whole Season feature just for this usage check.
    @Query(value = "SELECT EXISTS (SELECT 1 FROM season WHERE league_id = :leagueId)", nativeQuery = true)
    boolean hasAnySeasons(@Param("leagueId") Long leagueId);
}
