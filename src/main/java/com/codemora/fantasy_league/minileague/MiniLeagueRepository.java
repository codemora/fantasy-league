package com.codemora.fantasy_league.minileague;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MiniLeagueRepository extends JpaRepository<MiniLeague, Long> {

    Optional<MiniLeague> findByInviteCode(String inviteCode);

    List<MiniLeague> findBySeasonId(Long seasonId);
}
