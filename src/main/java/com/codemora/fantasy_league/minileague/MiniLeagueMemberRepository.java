package com.codemora.fantasy_league.minileague;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MiniLeagueMemberRepository extends JpaRepository<MiniLeagueMember, Long> {

    List<MiniLeagueMember> findByMiniLeagueId(Long miniLeagueId);

    List<MiniLeagueMember> findByUserId(Long userId);

    boolean existsByMiniLeagueIdAndUserId(Long miniLeagueId, Long userId);
}
