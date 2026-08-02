package com.codemora.fantasy_league.lineup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.codemora.fantasy_league.common.Position;
import com.codemora.fantasy_league.common.error.ConflictException;
import com.codemora.fantasy_league.common.error.NotFoundException;
import com.codemora.fantasy_league.config.CurrentUserProvider;
import com.codemora.fantasy_league.fantasysquad.FantasySquad;
import com.codemora.fantasy_league.fantasysquad.FantasySquadRepository;
import com.codemora.fantasy_league.fantasysquad.SquadPlayer;
import com.codemora.fantasy_league.fantasysquad.SquadPlayerRepository;
import com.codemora.fantasy_league.gameweek.Gameweek;
import com.codemora.fantasy_league.gameweek.GameweekDeadlineGuard;
import com.codemora.fantasy_league.gameweek.GameweekRepository;
import com.codemora.fantasy_league.lineup.dto.GameweekLineupResponse;
import com.codemora.fantasy_league.lineup.dto.LineupSlotResponse;
import com.codemora.fantasy_league.lineup.dto.SubmitLineupRequest;
import com.codemora.fantasy_league.player.Player;
import com.codemora.fantasy_league.player.PlayerRepository;
import com.codemora.fantasy_league.season.Season;
import com.codemora.fantasy_league.season.SeasonRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class LineupService {

    private static final int STARTER_COUNT = 11;
    private static final int BENCH_COUNT = 4;

    /** [min, max] starters allowed per position, per the README Squad Rules. */
    private static final Map<Position, int[]> FORMATION_RANGE = Map.of(
            Position.GK, new int[] {1, 1},
            Position.DEF, new int[] {3, 5},
            Position.MID, new int[] {2, 5},
            Position.FWD, new int[] {1, 3});

    private final SeasonRepository seasonRepository;
    private final GameweekRepository gameweekRepository;
    private final FantasySquadRepository fantasySquadRepository;
    private final SquadPlayerRepository squadPlayerRepository;
    private final PlayerRepository playerRepository;
    private final GameweekLineupRepository gameweekLineupRepository;
    private final LineupSlotRepository lineupSlotRepository;
    private final GameweekDeadlineGuard gameweekDeadlineGuard;
    private final CurrentUserProvider currentUserProvider;

    public LineupService(
            SeasonRepository seasonRepository,
            GameweekRepository gameweekRepository,
            FantasySquadRepository fantasySquadRepository,
            SquadPlayerRepository squadPlayerRepository,
            PlayerRepository playerRepository,
            GameweekLineupRepository gameweekLineupRepository,
            LineupSlotRepository lineupSlotRepository,
            GameweekDeadlineGuard gameweekDeadlineGuard,
            CurrentUserProvider currentUserProvider) {
        this.seasonRepository = seasonRepository;
        this.gameweekRepository = gameweekRepository;
        this.fantasySquadRepository = fantasySquadRepository;
        this.squadPlayerRepository = squadPlayerRepository;
        this.playerRepository = playerRepository;
        this.gameweekLineupRepository = gameweekLineupRepository;
        this.lineupSlotRepository = lineupSlotRepository;
        this.gameweekDeadlineGuard = gameweekDeadlineGuard;
        this.currentUserProvider = currentUserProvider;
    }

    /** Create-or-replace: resubmitting for the same gameweek overwrites the previous selection. */
    @Transactional
    public GameweekLineupResponse submit(Long leagueId, Long seasonId, Long gameweekId, SubmitLineupRequest request) {
        Gameweek gameweek = findGameweekInSeason(leagueId, seasonId, gameweekId);
        gameweekDeadlineGuard.assertOpenForChanges(gameweek, "change your lineup");
        Long userId = currentUserProvider.getUserId();
        FantasySquad squad = fantasySquadRepository.findByUserIdAndSeasonId(userId, seasonId)
                .orElseThrow(() -> new NotFoundException("You don't have a fantasy squad for this season"));

        Set<Long> starterIds = new HashSet<>(request.starterPlayerIds());
        Set<Long> benchIds = new HashSet<>(request.benchPlayerIds());
        if (starterIds.size() != STARTER_COUNT || benchIds.size() != BENCH_COUNT) {
            throw new ConflictException("Lineup must have " + STARTER_COUNT + " distinct starters and " + BENCH_COUNT + " distinct bench players");
        }
        if (!Collections.disjoint(starterIds, benchIds)) {
            throw new ConflictException("A player can't be both a starter and on the bench");
        }
        if (!starterIds.contains(request.captainPlayerId())) {
            throw new ConflictException("Captain must be one of the starters");
        }

        Set<Long> selectedIds = new HashSet<>(starterIds);
        selectedIds.addAll(benchIds);
        Set<Long> squadPlayerIds = squadPlayerRepository.findBySquadId(squad.getId()).stream()
                .map(SquadPlayer::getPlayerId)
                .collect(Collectors.toSet());
        if (!squadPlayerIds.containsAll(selectedIds)) {
            throw new ConflictException("Lineup can only include players from your fantasy squad");
        }

        List<Player> players = playerRepository.findAllById(selectedIds);
        Map<Long, Player> playersById = players.stream().collect(Collectors.toMap(Player::getId, p -> p));
        validateFormation(starterIds, playersById);

        GameweekLineup lineup = gameweekLineupRepository.findBySquadIdAndGameweekId(squad.getId(), gameweekId)
                .orElseGet(() -> GameweekLineup.builder().squadId(squad.getId()).gameweekId(gameweekId).build());
        lineup.setCaptainPlayerId(request.captainPlayerId());
        lineup = gameweekLineupRepository.save(lineup);
        lineupSlotRepository.deleteByLineupId(lineup.getId());

        List<LineupSlot> slots = new ArrayList<>();
        for (Long playerId : request.starterPlayerIds()) {
            slots.add(lineupSlotRepository.save(LineupSlot.builder()
                    .lineupId(lineup.getId()).playerId(playerId).role(LineupRole.STARTER).build()));
        }
        int benchOrder = 1;
        for (Long playerId : request.benchPlayerIds()) {
            slots.add(lineupSlotRepository.save(LineupSlot.builder()
                    .lineupId(lineup.getId()).playerId(playerId).role(LineupRole.BENCH).benchOrder(benchOrder++).build()));
        }

        log.info("lineup_submitted squad_id={} gameweek_id={}", squad.getId(), gameweekId);
        return toResponse(lineup, slots, playersById);
    }

    public GameweekLineupResponse findCurrent(Long leagueId, Long seasonId, Long gameweekId) {
        findGameweekInSeason(leagueId, seasonId, gameweekId);
        Long userId = currentUserProvider.getUserId();
        FantasySquad squad = fantasySquadRepository.findByUserIdAndSeasonId(userId, seasonId)
                .orElseThrow(() -> new NotFoundException("You don't have a fantasy squad for this season"));
        GameweekLineup lineup = gameweekLineupRepository.findBySquadIdAndGameweekId(squad.getId(), gameweekId)
                .orElseThrow(() -> new NotFoundException("You haven't submitted a lineup for this gameweek"));

        List<LineupSlot> slots = lineupSlotRepository.findByLineupId(lineup.getId());
        List<Player> players = playerRepository.findAllById(slots.stream().map(LineupSlot::getPlayerId).toList());
        Map<Long, Player> playersById = players.stream().collect(Collectors.toMap(Player::getId, p -> p));
        return toResponse(lineup, slots, playersById);
    }

    private void validateFormation(Set<Long> starterIds, Map<Long, Player> playersById) {
        Map<Position, Long> counts = starterIds.stream()
                .map(playersById::get)
                .collect(Collectors.groupingBy(Player::getPosition, Collectors.counting()));
        FORMATION_RANGE.forEach((position, range) -> {
            long count = counts.getOrDefault(position, 0L);
            if (count < range[0] || count > range[1]) {
                throw new ConflictException("Starting XI must have " + range[0] + "-" + range[1]
                        + " " + position + " players (found " + count + ")");
            }
        });
    }

    private Gameweek findGameweekInSeason(Long leagueId, Long seasonId, Long gameweekId) {
        Season season = seasonRepository.findById(seasonId)
                .orElseThrow(() -> new NotFoundException("No season with id " + seasonId));
        if (!season.getLeagueId().equals(leagueId)) {
            throw new NotFoundException("No season with id " + seasonId + " under league " + leagueId);
        }
        Gameweek gameweek = gameweekRepository.findById(gameweekId)
                .orElseThrow(() -> new NotFoundException("No gameweek with id " + gameweekId));
        if (!gameweek.getSeasonId().equals(seasonId)) {
            throw new NotFoundException("No gameweek with id " + gameweekId + " in season " + seasonId);
        }
        return gameweek;
    }

    private GameweekLineupResponse toResponse(GameweekLineup lineup, List<LineupSlot> slots, Map<Long, Player> playersById) {
        List<LineupSlotResponse> starters = new ArrayList<>();
        List<LineupSlotResponse> bench = new ArrayList<>();
        for (LineupSlot slot : slots) {
            Player player = playersById.get(slot.getPlayerId());
            LineupSlotResponse response = new LineupSlotResponse(player.getId(), player.getName(), player.getPosition(), slot.getBenchOrder());
            if (slot.getRole() == LineupRole.STARTER) {
                starters.add(response);
            } else {
                bench.add(response);
            }
        }
        starters.sort(Comparator.comparing(LineupSlotResponse::position));
        bench.sort(Comparator.comparing(LineupSlotResponse::benchOrder));
        return new GameweekLineupResponse(lineup.getId(), lineup.getSquadId(), lineup.getGameweekId(), lineup.getCaptainPlayerId(), starters, bench);
    }
}
