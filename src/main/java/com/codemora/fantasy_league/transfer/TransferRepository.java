package com.codemora.fantasy_league.transfer;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TransferRepository extends JpaRepository<Transfer, Long> {

    List<Transfer> findBySquadIdOrderByTimestamp(Long squadId);

    List<Transfer> findBySquadIdAndGameweekId(Long squadId, Long gameweekId);

    long countBySquadIdAndGameweekId(Long squadId, Long gameweekId);
}
