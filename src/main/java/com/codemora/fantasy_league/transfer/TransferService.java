package com.codemora.fantasy_league.transfer;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.codemora.fantasy_league.common.error.ConflictException;
import com.codemora.fantasy_league.common.error.NotFoundException;
import com.codemora.fantasy_league.config.CurrentUserProvider;
import com.codemora.fantasy_league.fantasysquad.FantasySquad;
import com.codemora.fantasy_league.fantasysquad.FantasySquadRepository;
import com.codemora.fantasy_league.fantasysquad.SquadPlayer;
import com.codemora.fantasy_league.fantasysquad.SquadPlayerRepository;
import com.codemora.fantasy_league.gameweek.Gameweek;
import com.codemora.fantasy_league.gameweek.GameweekRepository;
import com.codemora.fantasy_league.player.Player;
import com.codemora.fantasy_league.player.PlayerRepository;
import com.codemora.fantasy_league.season.Season;
import com.codemora.fantasy_league.season.SeasonRepository;
import com.codemora.fantasy_league.transfer.dto.MakeTransferRequest;
import com.codemora.fantasy_league.transfer.dto.MakeTransferResponse;
import com.codemora.fantasy_league.transfer.dto.TransferResponse;

import lombok.extern.slf4j.Slf4j;

/**
 * Deadline locking ("transfers only while the gameweek is UPCOMING") is
 * deferred to #36, consistent with squad creation (#29) and lineup selection
 * (#37). #36 can now cover all three in one pass.
 */
@Service
@Slf4j
public class TransferService {

    /** README Squad Rules: 1 free transfer per gameweek, accruing to a maximum of 2 banked. */
    static final int MAX_BANKED_FREE_TRANSFERS = 2;
    static final int TRANSFER_POINTS_COST = 4;
    private static final int MAX_PLAYERS_PER_TEAM = 3;

    private final SeasonRepository seasonRepository;
    private final GameweekRepository gameweekRepository;
    private final FantasySquadRepository fantasySquadRepository;
    private final SquadPlayerRepository squadPlayerRepository;
    private final PlayerRepository playerRepository;
    private final TransferRepository transferRepository;
    private final CurrentUserProvider currentUserProvider;

    public TransferService(
            SeasonRepository seasonRepository,
            GameweekRepository gameweekRepository,
            FantasySquadRepository fantasySquadRepository,
            SquadPlayerRepository squadPlayerRepository,
            PlayerRepository playerRepository,
            TransferRepository transferRepository,
            CurrentUserProvider currentUserProvider) {
        this.seasonRepository = seasonRepository;
        this.gameweekRepository = gameweekRepository;
        this.fantasySquadRepository = fantasySquadRepository;
        this.squadPlayerRepository = squadPlayerRepository;
        this.playerRepository = playerRepository;
        this.transferRepository = transferRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional
    public MakeTransferResponse makeTransfer(Long leagueId, Long seasonId, Long gameweekId, MakeTransferRequest request) {
        findSeasonInLeague(leagueId, seasonId);
        Gameweek gameweek = findGameweekInSeason(seasonId, gameweekId);
        FantasySquad squad = findMySquad(seasonId);

        if (request.playerOutId().equals(request.playerInId())) {
            throw new ConflictException("Can't transfer a player for themselves");
        }

        List<SquadPlayer> squadPlayers = squadPlayerRepository.findBySquadId(squad.getId());
        SquadPlayer outgoing = squadPlayers.stream()
                .filter(sp -> sp.getPlayerId().equals(request.playerOutId()))
                .findFirst()
                .orElseThrow(() -> new ConflictException("Player " + request.playerOutId() + " isn't in your squad"));
        if (squadPlayers.stream().anyMatch(sp -> sp.getPlayerId().equals(request.playerInId()))) {
            throw new ConflictException("Player " + request.playerInId() + " is already in your squad");
        }

        Player playerOut = playerRepository.findById(request.playerOutId())
                .orElseThrow(() -> new NotFoundException("No player with id " + request.playerOutId()));
        Player playerIn = playerRepository.findById(request.playerInId())
                .orElseThrow(() -> new NotFoundException("No player with id " + request.playerInId()));

        // Same position keeps the squad's 2 GK / 5 DEF / 5 MID / 3 FWD composition valid
        // without having to re-derive it -- a cross-position swap would always break it.
        if (playerOut.getPosition() != playerIn.getPosition()) {
            throw new ConflictException("Transfer must be like-for-like: " + playerOut.getPosition()
                    + " out, so the incoming player must also be " + playerOut.getPosition()
                    + " (got " + playerIn.getPosition() + ")");
        }

        int bankAfter = squad.getBankBalance() + outgoing.getPurchasePrice() - playerIn.getMarketValue();
        if (bankAfter < 0) {
            log.warn("transfer_conflict squad_id={} reason=over_budget shortfall={}", squad.getId(), -bankAfter);
            throw new ConflictException("Not enough funds: " + playerIn.getName() + " costs "
                    + playerIn.getMarketValue() + " but you'd only have "
                    + (squad.getBankBalance() + outgoing.getPurchasePrice()) + " available");
        }
        validateTeamLimit(squadPlayers, outgoing, playerIn);

        int freeBefore = freeTransfersAvailable(squad.getId(), seasonId, gameweek);
        int pointsCost = freeBefore > 0 ? 0 : TRANSFER_POINTS_COST;

        squadPlayerRepository.delete(outgoing);
        squadPlayerRepository.save(SquadPlayer.builder()
                .squadId(squad.getId())
                .playerId(playerIn.getId())
                .purchasePrice(playerIn.getMarketValue())
                .addedAt(LocalDateTime.now())
                .build());

        squad.setBankBalance(bankAfter);
        int freeAfter = Math.max(0, freeBefore - 1);
        squad.setFreeTransfers(freeAfter);
        fantasySquadRepository.save(squad);

        Transfer transfer = transferRepository.save(Transfer.builder()
                .squadId(squad.getId())
                .gameweekId(gameweekId)
                .playerOutId(playerOut.getId())
                .playerInId(playerIn.getId())
                .pointsCost(pointsCost)
                .timestamp(LocalDateTime.now())
                .build());

        log.info("transfer_made squad_id={} gameweek_id={} out={} in={} points_cost={} bank={}",
                squad.getId(), gameweekId, playerOut.getId(), playerIn.getId(), pointsCost, bankAfter);
        return new MakeTransferResponse(
                toResponse(transfer, gameweek, Map.of(playerOut.getId(), playerOut, playerIn.getId(), playerIn)),
                bankAfter, freeAfter);
    }

    public List<TransferResponse> findHistory(Long leagueId, Long seasonId) {
        findSeasonInLeague(leagueId, seasonId);
        FantasySquad squad = findMySquad(seasonId);
        List<Transfer> transfers = transferRepository.findBySquadIdOrderByTimestamp(squad.getId());
        if (transfers.isEmpty()) {
            return List.of();
        }

        Map<Long, Gameweek> gameweeksById = gameweekRepository.findBySeasonIdOrderByNumber(seasonId).stream()
                .collect(Collectors.toMap(Gameweek::getId, g -> g));
        Map<Long, Player> playersById = playerRepository.findAllById(transfers.stream()
                        .flatMap(t -> Stream.of(t.getPlayerOutId(), t.getPlayerInId()))
                        .distinct()
                        .toList())
                .stream().collect(Collectors.toMap(Player::getId, p -> p));

        return transfers.stream()
                .map(t -> toResponse(t, gameweeksById.get(t.getGameweekId()), playersById))
                .toList();
    }

    /**
     * Replays the season's gameweeks in order rather than storing a running
     * total, so the bank can't drift: each gameweek grants one free transfer
     * (capped at MAX_BANKED_FREE_TRANSFERS), then that gameweek's transfers
     * consume from it. Anything consumed beyond the bank cost points at the
     * time and doesn't carry a debt forward, hence the floor at 0.
     */
    int freeTransfersAvailable(Long squadId, Long seasonId, Gameweek upTo) {
        int bank = 0;
        for (Gameweek gameweek : gameweekRepository.findBySeasonIdOrderByNumber(seasonId)) {
            if (gameweek.getNumber() > upTo.getNumber()) {
                break;
            }
            bank = Math.min(bank + 1, MAX_BANKED_FREE_TRANSFERS);
            long used = transferRepository.countBySquadIdAndGameweekId(squadId, gameweek.getId());
            bank = (int) Math.max(0, bank - used);
        }
        return bank;
    }

    private void validateTeamLimit(List<SquadPlayer> squadPlayers, SquadPlayer outgoing, Player playerIn) {
        List<Long> remainingPlayerIds = squadPlayers.stream()
                .filter(sp -> !sp.getId().equals(outgoing.getId()))
                .map(SquadPlayer::getPlayerId)
                .toList();
        long sameTeam = playerRepository.findAllById(remainingPlayerIds).stream()
                .filter(p -> p.getTeamId().equals(playerIn.getTeamId()))
                .count();
        if (sameTeam + 1 > MAX_PLAYERS_PER_TEAM) {
            throw new ConflictException("Squad can't have more than " + MAX_PLAYERS_PER_TEAM
                    + " players from team " + playerIn.getTeamId());
        }
    }

    private TransferResponse toResponse(Transfer transfer, Gameweek gameweek, Map<Long, Player> playersById) {
        Player out = playersById.get(transfer.getPlayerOutId());
        Player in = playersById.get(transfer.getPlayerInId());
        return new TransferResponse(
                transfer.getId(),
                transfer.getGameweekId(),
                gameweek != null ? gameweek.getNumber() : null,
                out.getId(), out.getName(),
                in.getId(), in.getName(),
                in.getPosition(),
                transfer.getPointsCost(),
                transfer.getTimestamp());
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
