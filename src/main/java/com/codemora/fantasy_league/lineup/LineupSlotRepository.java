package com.codemora.fantasy_league.lineup;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LineupSlotRepository extends JpaRepository<LineupSlot, Long> {

    List<LineupSlot> findByLineupId(Long lineupId);

    void deleteByLineupId(Long lineupId);
}
