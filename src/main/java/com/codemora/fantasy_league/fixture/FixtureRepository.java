package com.codemora.fantasy_league.fixture;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FixtureRepository extends JpaRepository<Fixture, Long> {

    List<Fixture> findBySeasonIdAndPlayedTrue(Long seasonId);

    List<Fixture> findBySeasonIdAndPlayedFalse(Long seasonId);

    List<Fixture> findBySeasonIdAndGameweekIdAndPlayedFalse(Long seasonId, Long gameweekId);

    List<Fixture> findByGameweekId(Long gameweekId);

    @Query("SELECT f FROM Fixture f WHERE f.homeTeamId = :teamId OR f.awayTeamId = :teamId")
    List<Fixture> findByTeamId(@Param("teamId") Long teamId);

    @Query("SELECT f FROM Fixture f WHERE (f.homeTeamId = :teamId OR f.awayTeamId = :teamId) AND f.played = :played")
    List<Fixture> findByTeamIdAndPlayed(@Param("teamId") Long teamId, @Param("played") boolean played);
}
