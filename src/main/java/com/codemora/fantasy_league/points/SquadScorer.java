package com.codemora.fantasy_league.points;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.codemora.fantasy_league.common.Position;
import com.codemora.fantasy_league.fixture.Fixture;
import com.codemora.fantasy_league.fixture.FixtureRepository;
import com.codemora.fantasy_league.lineup.GameweekLineup;
import com.codemora.fantasy_league.lineup.GameweekLineupRepository;
import com.codemora.fantasy_league.lineup.LineupRole;
import com.codemora.fantasy_league.lineup.LineupSlot;
import com.codemora.fantasy_league.lineup.LineupSlotRepository;
import com.codemora.fantasy_league.player.Player;
import com.codemora.fantasy_league.player.PlayerPerformance;
import com.codemora.fantasy_league.player.PlayerPerformanceRepository;
import com.codemora.fantasy_league.player.PlayerRepository;
import com.codemora.fantasy_league.points.dto.PlayerPointsResponse;
import com.codemora.fantasy_league.scoringrule.ScoringRule;
import com.codemora.fantasy_league.transfer.Transfer;
import com.codemora.fantasy_league.transfer.TransferRepository;

/**
 * The single definition of what a squad scored in a gameweek, shared by the
 * per-gameweek breakdown (#32) and the season leaderboard (#33). Kept in one
 * place deliberately: two implementations of "captain doubles, bench scores
 * nothing, hits are deducted" would eventually disagree, and a leaderboard
 * that contradicts a user's own points page is a nasty bug to chase.
 */
@Component
public class SquadScorer {

    private final GameweekLineupRepository gameweekLineupRepository;
    private final LineupSlotRepository lineupSlotRepository;
    private final PlayerRepository playerRepository;
    private final PlayerPerformanceRepository playerPerformanceRepository;
    private final FixtureRepository fixtureRepository;
    private final TransferRepository transferRepository;

    public SquadScorer(
            GameweekLineupRepository gameweekLineupRepository,
            LineupSlotRepository lineupSlotRepository,
            PlayerRepository playerRepository,
            PlayerPerformanceRepository playerPerformanceRepository,
            FixtureRepository fixtureRepository,
            TransferRepository transferRepository) {
        this.gameweekLineupRepository = gameweekLineupRepository;
        this.lineupSlotRepository = lineupSlotRepository;
        this.playerRepository = playerRepository;
        this.playerPerformanceRepository = playerPerformanceRepository;
        this.fixtureRepository = fixtureRepository;
        this.transferRepository = transferRepository;
    }

    /**
     * Everything about a gameweek that's the same for every squad, loaded
     * once. A team plays at most one fixture per gameweek, so a player's
     * performance for that gameweek is unambiguous and can be keyed on player
     * id alone. Absent from the map means "didn't feature" -- no fixture, or
     * no performance recorded -- and scores nothing either way.
     */
    public record GameweekContext(Long gameweekId, Map<Long, PlayerPerformance> performanceByPlayerId) {
    }

    public GameweekContext loadContext(Long gameweekId) {
        Map<Long, PlayerPerformance> byPlayerId = new HashMap<>();
        for (Fixture fixture : fixtureRepository.findByGameweekId(gameweekId)) {
            for (PlayerPerformance performance : playerPerformanceRepository.findByFixtureId(fixture.getId())) {
                byPlayerId.put(performance.getPlayerId(), performance);
            }
        }
        return new GameweekContext(gameweekId, byPlayerId);
    }

    public SquadGameweekScore score(Long squadId, GameweekContext context, Map<Position, ScoringRule> rulesByPosition) {
        // Hits are charged against the gameweek the transfer was made in (#31),
        // independently of whether a lineup was ever submitted.
        int transferPointsCost = transferRepository.findBySquadIdAndGameweekId(squadId, context.gameweekId()).stream()
                .mapToInt(Transfer::getPointsCost)
                .sum();

        Optional<GameweekLineup> maybeLineup =
                gameweekLineupRepository.findBySquadIdAndGameweekId(squadId, context.gameweekId());
        if (maybeLineup.isEmpty()) {
            return SquadGameweekScore.withoutLineup(transferPointsCost);
        }
        GameweekLineup lineup = maybeLineup.get();

        List<LineupSlot> slots = lineupSlotRepository.findByLineupId(lineup.getId());
        Map<Long, Player> playersById = playerRepository.findAllById(slots.stream().map(LineupSlot::getPlayerId).toList())
                .stream().collect(Collectors.toMap(Player::getId, p -> p));

        List<PlayerPointsResponse> breakdown = new ArrayList<>();
        int playerPoints = 0;
        for (LineupSlot slot : slots) {
            Player player = playersById.get(slot.getPlayerId());
            int rawPoints = rawPoints(player, context, rulesByPosition);
            boolean isCaptain = player.getId().equals(lineup.getCaptainPlayerId());
            boolean starting = slot.getRole() == LineupRole.STARTER;
            int points = starting ? (isCaptain ? rawPoints * 2 : rawPoints) : 0;
            playerPoints += points;
            breakdown.add(new PlayerPointsResponse(player.getId(), player.getName(), player.getPosition(),
                    slot.getRole(), isCaptain, rawPoints, points));
        }
        breakdown.sort(Comparator.comparing(PlayerPointsResponse::role)
                .thenComparing(PlayerPointsResponse::position)
                .thenComparing(PlayerPointsResponse::playerName));

        return SquadGameweekScore.of(breakdown, playerPoints, transferPointsCost);
    }

    private int rawPoints(Player player, GameweekContext context, Map<Position, ScoringRule> rulesByPosition) {
        PlayerPerformance performance = context.performanceByPlayerId().get(player.getId());
        if (performance == null) {
            return 0;
        }
        return performance.getFantasyPoints(rulesByPosition.get(player.getPosition()));
    }
}
