package com.codemora.fantasy_league.points;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.codemora.fantasy_league.common.Position;
import com.codemora.fantasy_league.common.error.NotFoundException;
import com.codemora.fantasy_league.config.CurrentUserProvider;
import com.codemora.fantasy_league.fantasysquad.FantasySquad;
import com.codemora.fantasy_league.fantasysquad.FantasySquadRepository;
import com.codemora.fantasy_league.fixture.Fixture;
import com.codemora.fantasy_league.fixture.FixtureRepository;
import com.codemora.fantasy_league.gameweek.Gameweek;
import com.codemora.fantasy_league.gameweek.GameweekRepository;
import com.codemora.fantasy_league.gameweek.GameweekStatus;
import com.codemora.fantasy_league.lineup.GameweekLineup;
import com.codemora.fantasy_league.lineup.GameweekLineupRepository;
import com.codemora.fantasy_league.lineup.LineupRole;
import com.codemora.fantasy_league.lineup.LineupSlot;
import com.codemora.fantasy_league.lineup.LineupSlotRepository;
import com.codemora.fantasy_league.player.Player;
import com.codemora.fantasy_league.player.PlayerPerformance;
import com.codemora.fantasy_league.player.PlayerPerformanceRepository;
import com.codemora.fantasy_league.player.PlayerRepository;
import com.codemora.fantasy_league.points.dto.GameweekPointsResponse;
import com.codemora.fantasy_league.points.dto.PlayerPointsResponse;
import com.codemora.fantasy_league.scoringrule.ScoringRule;
import com.codemora.fantasy_league.scoringrule.ScoringRuleRepository;
import com.codemora.fantasy_league.season.Season;
import com.codemora.fantasy_league.season.SeasonRepository;
import com.codemora.fantasy_league.transfer.Transfer;
import com.codemora.fantasy_league.transfer.TransferRepository;

/**
 * Points are computed live from PlayerPerformance + the season's ScoringRule
 * (see ScoringRuleService), never stored -- the same "recompute on read"
 * approach as StandingsService, so editing a scoring rule never requires
 * rewriting past gameweeks.
 */
@Service
public class GameweekPointsService {

    private final SeasonRepository seasonRepository;
    private final GameweekRepository gameweekRepository;
    private final FantasySquadRepository fantasySquadRepository;
    private final GameweekLineupRepository gameweekLineupRepository;
    private final LineupSlotRepository lineupSlotRepository;
    private final PlayerRepository playerRepository;
    private final PlayerPerformanceRepository playerPerformanceRepository;
    private final FixtureRepository fixtureRepository;
    private final ScoringRuleRepository scoringRuleRepository;
    private final TransferRepository transferRepository;
    private final CurrentUserProvider currentUserProvider;

    public GameweekPointsService(
            SeasonRepository seasonRepository,
            GameweekRepository gameweekRepository,
            FantasySquadRepository fantasySquadRepository,
            GameweekLineupRepository gameweekLineupRepository,
            LineupSlotRepository lineupSlotRepository,
            PlayerRepository playerRepository,
            PlayerPerformanceRepository playerPerformanceRepository,
            FixtureRepository fixtureRepository,
            ScoringRuleRepository scoringRuleRepository,
            TransferRepository transferRepository,
            CurrentUserProvider currentUserProvider) {
        this.seasonRepository = seasonRepository;
        this.gameweekRepository = gameweekRepository;
        this.fantasySquadRepository = fantasySquadRepository;
        this.gameweekLineupRepository = gameweekLineupRepository;
        this.lineupSlotRepository = lineupSlotRepository;
        this.playerRepository = playerRepository;
        this.playerPerformanceRepository = playerPerformanceRepository;
        this.fixtureRepository = fixtureRepository;
        this.scoringRuleRepository = scoringRuleRepository;
        this.transferRepository = transferRepository;
        this.currentUserProvider = currentUserProvider;
    }

    public GameweekPointsResponse findPoints(Long leagueId, Long seasonId, Long gameweekId) {
        Gameweek gameweek = findGameweekInSeason(leagueId, seasonId, gameweekId);
        Long userId = currentUserProvider.getUserId();
        FantasySquad squad = fantasySquadRepository.findByUserIdAndSeasonId(userId, seasonId)
                .orElseThrow(() -> new NotFoundException("You don't have a fantasy squad for this season"));
        GameweekLineup lineup = gameweekLineupRepository.findBySquadIdAndGameweekId(squad.getId(), gameweekId)
                .orElseThrow(() -> new NotFoundException("You haven't submitted a lineup for this gameweek"));

        List<LineupSlot> slots = lineupSlotRepository.findByLineupId(lineup.getId());
        Map<Long, Player> playersById = playerRepository.findAllById(slots.stream().map(LineupSlot::getPlayerId).toList())
                .stream().collect(Collectors.toMap(Player::getId, p -> p));
        Map<Long, Long> fixtureIdByTeamId = fixtureIdByTeamId(gameweekId);
        Map<Position, ScoringRule> rulesByPosition = scoringRuleRepository.findBySeasonId(seasonId).stream()
                .collect(Collectors.toMap(ScoringRule::getPosition, r -> r));

        List<PlayerPointsResponse> breakdown = new ArrayList<>();
        int playerPoints = 0;
        for (LineupSlot slot : slots) {
            Player player = playersById.get(slot.getPlayerId());
            int rawPoints = rawPoints(player, fixtureIdByTeamId, rulesByPosition);
            boolean isCaptain = player.getId().equals(lineup.getCaptainPlayerId());
            boolean counts = slot.getRole() == LineupRole.STARTER;
            int points = counts ? (isCaptain ? rawPoints * 2 : rawPoints) : 0;
            playerPoints += points;
            breakdown.add(new PlayerPointsResponse(player.getId(), player.getName(), player.getPosition(), slot.getRole(), isCaptain, rawPoints, points));
        }
        breakdown.sort(Comparator.comparing(PlayerPointsResponse::role)
                .thenComparing(PlayerPointsResponse::position)
                .thenComparing(PlayerPointsResponse::playerName));

        // Transfers beyond the free allowance cost 4 points against the gameweek they were made in (#31).
        int transferPointsCost = transferRepository.findBySquadIdAndGameweekId(squad.getId(), gameweekId).stream()
                .mapToInt(Transfer::getPointsCost)
                .sum();

        return new GameweekPointsResponse(gameweekId, gameweek.getNumber(), breakdown,
                playerPoints, transferPointsCost, playerPoints - transferPointsCost,
                gameweek.getStatus() == GameweekStatus.COMPLETE);
    }

    private int rawPoints(Player player, Map<Long, Long> fixtureIdByTeamId, Map<Position, ScoringRule> rulesByPosition) {
        Long fixtureId = fixtureIdByTeamId.get(player.getTeamId());
        if (fixtureId == null) {
            return 0;
        }
        Optional<PlayerPerformance> performance = playerPerformanceRepository.findByPlayerIdAndFixtureId(player.getId(), fixtureId);
        if (performance.isEmpty()) {
            return 0;
        }
        ScoringRule rule = rulesByPosition.get(player.getPosition());
        return performance.get().getFantasyPoints(rule);
    }

    private Map<Long, Long> fixtureIdByTeamId(Long gameweekId) {
        Map<Long, Long> fixtureIdByTeamId = new HashMap<>();
        for (Fixture fixture : fixtureRepository.findByGameweekId(gameweekId)) {
            fixtureIdByTeamId.put(fixture.getHomeTeamId(), fixture.getId());
            fixtureIdByTeamId.put(fixture.getAwayTeamId(), fixture.getId());
        }
        return fixtureIdByTeamId;
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
}
