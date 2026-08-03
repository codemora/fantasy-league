package com.codemora.fantasy_league.chip;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.codemora.fantasy_league.chip.dto.ChipResponse;
import com.codemora.fantasy_league.common.error.ConflictException;
import com.codemora.fantasy_league.common.error.NotFoundException;
import com.codemora.fantasy_league.config.CurrentUserProvider;
import com.codemora.fantasy_league.fantasysquad.FantasySquad;
import com.codemora.fantasy_league.fantasysquad.FantasySquadRepository;
import com.codemora.fantasy_league.gameweek.Gameweek;
import com.codemora.fantasy_league.gameweek.GameweekDeadlineGuard;
import com.codemora.fantasy_league.gameweek.GameweekRepository;
import com.codemora.fantasy_league.season.Season;
import com.codemora.fantasy_league.season.SeasonRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * #40: each of WILDCARD, TRIPLE_CAPTAIN, and BENCH_BOOST may be played at
 * most once per squad per season, and only one chip may be active in any
 * given gameweek -- both enforced by unique constraints on squad_chip (see
 * V2__add_squad_chip.sql) rather than re-derived here. Activation is
 * immediate and final, same as a transfer: there's no provisional "armed but
 * not yet confirmed" state to manage. WILDCARD's effect on transfers lives in
 * TransferService; TRIPLE_CAPTAIN and BENCH_BOOST's effect on scoring lives in
 * SquadScorer -- this service only owns the activation record itself.
 */
@Service
@Slf4j
public class ChipService {

    private final SeasonRepository seasonRepository;
    private final GameweekRepository gameweekRepository;
    private final FantasySquadRepository fantasySquadRepository;
    private final SquadChipRepository squadChipRepository;
    private final GameweekDeadlineGuard gameweekDeadlineGuard;
    private final CurrentUserProvider currentUserProvider;

    public ChipService(
            SeasonRepository seasonRepository,
            GameweekRepository gameweekRepository,
            FantasySquadRepository fantasySquadRepository,
            SquadChipRepository squadChipRepository,
            GameweekDeadlineGuard gameweekDeadlineGuard,
            CurrentUserProvider currentUserProvider) {
        this.seasonRepository = seasonRepository;
        this.gameweekRepository = gameweekRepository;
        this.fantasySquadRepository = fantasySquadRepository;
        this.squadChipRepository = squadChipRepository;
        this.gameweekDeadlineGuard = gameweekDeadlineGuard;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional
    public ChipResponse activate(Long leagueId, Long seasonId, Long gameweekId, ChipType chipType) {
        findSeasonInLeague(leagueId, seasonId);
        Gameweek gameweek = findGameweekInSeason(seasonId, gameweekId);
        gameweekDeadlineGuard.assertOpenForChanges(gameweek, "play a chip");
        FantasySquad squad = findMySquad(seasonId);

        if (squadChipRepository.existsBySquadIdAndGameweekId(squad.getId(), gameweekId)) {
            throw new ConflictException("You've already played a chip for gameweek " + gameweek.getNumber()
                    + " -- only one chip can be active per gameweek");
        }
        if (squadChipRepository.existsBySquadIdAndChipType(squad.getId(), chipType)) {
            throw new ConflictException("You've already used your " + chipType
                    + " chip this season -- each chip can only be played once");
        }

        SquadChip chip = squadChipRepository.save(SquadChip.builder()
                .squadId(squad.getId())
                .gameweekId(gameweekId)
                .chipType(chipType)
                .activatedAt(LocalDateTime.now())
                .build());

        log.info("chip_activated squad_id={} gameweek_id={} chip={}", squad.getId(), gameweekId, chipType);
        return toResponse(chip, gameweek);
    }

    public List<ChipResponse> findHistory(Long leagueId, Long seasonId) {
        findSeasonInLeague(leagueId, seasonId);
        FantasySquad squad = findMySquad(seasonId);
        List<SquadChip> chips = squadChipRepository.findBySquadId(squad.getId());
        if (chips.isEmpty()) {
            return List.of();
        }

        Map<Long, Gameweek> gameweeksById = gameweekRepository.findBySeasonIdOrderByNumber(seasonId).stream()
                .collect(Collectors.toMap(Gameweek::getId, g -> g));
        return chips.stream()
                .map(c -> toResponse(c, gameweeksById.get(c.getGameweekId())))
                .sorted(Comparator.comparing(ChipResponse::activatedAt))
                .toList();
    }

    private ChipResponse toResponse(SquadChip chip, Gameweek gameweek) {
        return new ChipResponse(chip.getId(), chip.getChipType(), chip.getGameweekId(),
                gameweek != null ? gameweek.getNumber() : null, chip.getActivatedAt());
    }

    private FantasySquad findMySquad(Long seasonId) {
        Long userId = currentUserProvider.getUserId();
        return fantasySquadRepository.findByUserIdAndSeasonId(userId, seasonId)
                .orElseThrow(() -> new NotFoundException("You don't have a fantasy squad for this season"));
    }

    private Season findSeasonInLeague(Long leagueId, Long seasonId) {
        Season season = seasonRepository.findById(seasonId)
                .orElseThrow(() -> new NotFoundException("No season with id " + seasonId));
        if (!season.getLeagueId().equals(leagueId)) {
            throw new NotFoundException("No season with id " + seasonId + " under league " + leagueId);
        }
        return season;
    }

    private Gameweek findGameweekInSeason(Long seasonId, Long gameweekId) {
        Gameweek gameweek = gameweekRepository.findById(gameweekId)
                .orElseThrow(() -> new NotFoundException("No gameweek with id " + gameweekId));
        if (!gameweek.getSeasonId().equals(seasonId)) {
            throw new NotFoundException("No gameweek with id " + gameweekId + " in season " + seasonId);
        }
        return gameweek;
    }
}
