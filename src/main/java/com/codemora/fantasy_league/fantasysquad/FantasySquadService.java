package com.codemora.fantasy_league.fantasysquad;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
import com.codemora.fantasy_league.fantasysquad.dto.CreateFantasySquadRequest;
import com.codemora.fantasy_league.fantasysquad.dto.FantasySquadResponse;
import com.codemora.fantasy_league.fantasysquad.dto.SquadPlayerResponse;
import com.codemora.fantasy_league.gameweek.Gameweek;
import com.codemora.fantasy_league.gameweek.GameweekDeadlineGuard;
import com.codemora.fantasy_league.gameweek.GameweekRepository;
import com.codemora.fantasy_league.player.Player;
import com.codemora.fantasy_league.player.PlayerRepository;
import com.codemora.fantasy_league.season.Season;
import com.codemora.fantasy_league.season.SeasonRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class FantasySquadService {

    /** Mirrors the Squad Rules composition in the README: 2 GK, 5 DEF, 5 MID, 3 FWD. */
    private static final Map<Position, Integer> REQUIRED_COMPOSITION = Map.of(
            Position.GK, 2,
            Position.DEF, 5,
            Position.MID, 5,
            Position.FWD, 3);

    private static final int SQUAD_SIZE = 15;
    private static final int MAX_PLAYERS_PER_TEAM = 3;
    private static final int DEFAULT_FREE_TRANSFERS = 1;

    private final SeasonRepository seasonRepository;
    private final PlayerRepository playerRepository;
    private final FantasySquadRepository fantasySquadRepository;
    private final SquadPlayerRepository squadPlayerRepository;
    private final GameweekRepository gameweekRepository;
    private final GameweekDeadlineGuard gameweekDeadlineGuard;
    private final CurrentUserProvider currentUserProvider;

    public FantasySquadService(
            SeasonRepository seasonRepository,
            PlayerRepository playerRepository,
            FantasySquadRepository fantasySquadRepository,
            SquadPlayerRepository squadPlayerRepository,
            GameweekRepository gameweekRepository,
            GameweekDeadlineGuard gameweekDeadlineGuard,
            CurrentUserProvider currentUserProvider) {
        this.seasonRepository = seasonRepository;
        this.playerRepository = playerRepository;
        this.fantasySquadRepository = fantasySquadRepository;
        this.squadPlayerRepository = squadPlayerRepository;
        this.gameweekRepository = gameweekRepository;
        this.gameweekDeadlineGuard = gameweekDeadlineGuard;
        this.currentUserProvider = currentUserProvider;
    }

    /**
     * purchasePrice is always the player's current market_value at the moment
     * of purchase, not client-supplied -- trusting a client-chosen price would
     * let the budget check be gamed.
     */
    @Transactional
    public FantasySquadResponse create(Long leagueId, Long seasonId, CreateFantasySquadRequest request) {
        Season season = findInLeague(leagueId, seasonId);
        assertSeasonAcceptingSquads(seasonId);
        Long userId = currentUserProvider.getUserId();

        if (fantasySquadRepository.existsByUserIdAndSeasonId(userId, seasonId)) {
            log.warn("fantasy_squad_conflict user_id={} season_id={} reason=already_exists", userId, seasonId);
            throw new ConflictException("You already have a fantasy squad for this season");
        }

        Set<Long> distinctIds = new HashSet<>(request.playerIds());
        if (distinctIds.size() != SQUAD_SIZE) {
            throw new ConflictException("Squad must contain " + SQUAD_SIZE + " distinct players");
        }

        List<Player> players = playerRepository.findAllById(distinctIds);
        if (players.size() != SQUAD_SIZE) {
            throw new NotFoundException("One or more players don't exist");
        }

        validateComposition(players);
        validateTeamLimit(players);
        int totalCost = players.stream().mapToInt(Player::getMarketValue).sum();
        if (totalCost > season.getStartingBudget()) {
            log.warn("fantasy_squad_conflict user_id={} season_id={} reason=over_budget total_cost={} budget={}",
                    userId, seasonId, totalCost, season.getStartingBudget());
            throw new ConflictException("Squad costs " + totalCost + " but the season's starting budget is " + season.getStartingBudget());
        }

        FantasySquad squad = fantasySquadRepository.save(FantasySquad.builder()
                .userId(userId)
                .seasonId(seasonId)
                .bankBalance(season.getStartingBudget() - totalCost)
                .freeTransfers(DEFAULT_FREE_TRANSFERS)
                .build());

        LocalDateTime now = LocalDateTime.now();
        List<SquadPlayer> squadPlayers = players.stream()
                .map(player -> squadPlayerRepository.save(SquadPlayer.builder()
                        .squadId(squad.getId())
                        .playerId(player.getId())
                        .purchasePrice(player.getMarketValue())
                        .addedAt(now)
                        .build()))
                .toList();

        log.info("fantasy_squad_created id={} user_id={} season_id={} total_cost={}", squad.getId(), userId, seasonId, totalCost);
        return toResponse(squad, squadPlayers, players);
    }

    public FantasySquadResponse findMine(Long leagueId, Long seasonId) {
        findInLeague(leagueId, seasonId);
        Long userId = currentUserProvider.getUserId();
        FantasySquad squad = fantasySquadRepository.findByUserIdAndSeasonId(userId, seasonId)
                .orElseThrow(() -> new NotFoundException("You don't have a fantasy squad for this season"));

        List<SquadPlayer> squadPlayers = squadPlayerRepository.findBySquadId(squad.getId());
        List<Player> players = playerRepository.findAllById(
                squadPlayers.stream().map(SquadPlayer::getPlayerId).toList());
        return toResponse(squad, squadPlayers, players);
    }

    /**
     * Squad creation isn't tied to a gameweek in the URL, so "only while the
     * gameweek is UPCOMING" (#36) resolves to: there must still be a gameweek
     * left to play. A season with no gameweeks generated yet hasn't started,
     * so drafting is fine; once gameweeks exist and every one of them has
     * locked, the season is underway and a new squad can no longer join it.
     */
    private void assertSeasonAcceptingSquads(Long seasonId) {
        List<Gameweek> gameweeks = gameweekRepository.findBySeasonIdOrderByNumber(seasonId);
        if (gameweeks.isEmpty()) {
            return;
        }
        boolean anyOpen = gameweeks.stream().anyMatch(gameweekDeadlineGuard::isOpenForChanges);
        if (!anyOpen) {
            throw new ConflictException("This season is no longer accepting new squads: "
                    + "every gameweek's deadline has passed");
        }
    }

    private void validateComposition(List<Player> players) {
        Map<Position, Long> byPosition = players.stream()
                .collect(Collectors.groupingBy(Player::getPosition, Collectors.counting()));
        for (Map.Entry<Position, Integer> required : REQUIRED_COMPOSITION.entrySet()) {
            long actual = byPosition.getOrDefault(required.getKey(), 0L);
            if (actual != required.getValue()) {
                throw new ConflictException("Squad must have exactly " + required.getValue() + " " + required.getKey()
                        + " players (found " + actual + ")");
            }
        }
    }

    private void validateTeamLimit(List<Player> players) {
        Map<Long, Long> byTeam = players.stream().collect(Collectors.groupingBy(Player::getTeamId, Collectors.counting()));
        for (Map.Entry<Long, Long> entry : byTeam.entrySet()) {
            if (entry.getValue() > MAX_PLAYERS_PER_TEAM) {
                throw new ConflictException("Squad can't have more than " + MAX_PLAYERS_PER_TEAM
                        + " players from team " + entry.getKey());
            }
        }
    }

    private Season findInLeague(Long leagueId, Long seasonId) {
        Season season = seasonRepository.findById(seasonId)
                .orElseThrow(() -> new NotFoundException("No season with id " + seasonId));
        if (!season.getLeagueId().equals(leagueId)) {
            throw new NotFoundException("No season with id " + seasonId + " under league " + leagueId);
        }
        return season;
    }

    private FantasySquadResponse toResponse(FantasySquad squad, List<SquadPlayer> squadPlayers, List<Player> players) {
        Map<Long, Player> playersById = players.stream().collect(Collectors.toMap(Player::getId, p -> p));
        List<SquadPlayerResponse> playerResponses = new ArrayList<>(squadPlayers.stream()
                .map(sp -> {
                    Player player = playersById.get(sp.getPlayerId());
                    return new SquadPlayerResponse(
                            player.getId(), player.getName(), player.getPosition(), player.getTeamId(),
                            sp.getPurchasePrice(), sp.getAddedAt());
                })
                .toList());
        playerResponses.sort(Comparator.comparing(SquadPlayerResponse::position).thenComparing(SquadPlayerResponse::playerName));

        return new FantasySquadResponse(squad.getId(), squad.getUserId(), squad.getSeasonId(),
                squad.getBankBalance(), squad.getFreeTransfers(), playerResponses);
    }
}
