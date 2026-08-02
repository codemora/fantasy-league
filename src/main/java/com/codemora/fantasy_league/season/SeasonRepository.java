package com.codemora.fantasy_league.season;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SeasonRepository extends JpaRepository<Season, Long> {

    // season_entrant/fixture/fantasy_squad don't have JPA entities yet -- see
    // TeamRepository for the same native-query pattern and why.
    @Query(value = "SELECT COUNT(*) FROM season_entrant WHERE season_id = :seasonId", nativeQuery = true)
    long countEntrants(@Param("seasonId") Long seasonId);

    @Query(value = "SELECT EXISTS (SELECT 1 FROM fixture WHERE season_id = :seasonId)", nativeQuery = true)
    boolean hasAnyFixtures(@Param("seasonId") Long seasonId);

    @Query(value = "SELECT EXISTS (SELECT 1 FROM fantasy_squad WHERE season_id = :seasonId)", nativeQuery = true)
    boolean hasAnyFantasySquads(@Param("seasonId") Long seasonId);
}
