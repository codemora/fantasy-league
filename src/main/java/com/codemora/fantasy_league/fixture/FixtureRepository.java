package com.codemora.fantasy_league.fixture;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FixtureRepository extends JpaRepository<Fixture, Long> {

    List<Fixture> findBySeasonIdAndPlayedFalse(Long seasonId);

    List<Fixture> findBySeasonIdAndGameweekIdAndPlayedFalse(Long seasonId, Long gameweekId);
}
