package com.codemora.fantasy_league.chip;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SquadChipRepository extends JpaRepository<SquadChip, Long> {

    List<SquadChip> findBySquadId(Long squadId);

    Optional<SquadChip> findBySquadIdAndGameweekId(Long squadId, Long gameweekId);

    boolean existsBySquadIdAndGameweekId(Long squadId, Long gameweekId);

    boolean existsBySquadIdAndChipType(Long squadId, ChipType chipType);

    boolean existsBySquadIdAndGameweekIdAndChipType(Long squadId, Long gameweekId, ChipType chipType);
}
