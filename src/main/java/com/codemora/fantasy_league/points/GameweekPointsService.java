package com.codemora.fantasy_league.points;

import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.codemora.fantasy_league.common.Position;
import com.codemora.fantasy_league.common.error.NotFoundException;
import com.codemora.fantasy_league.config.CurrentUserProvider;
import com.codemora.fantasy_league.fantasysquad.FantasySquad;
import com.codemora.fantasy_league.fantasysquad.FantasySquadRepository;
import com.codemora.fantasy_league.gameweek.Gameweek;
import com.codemora.fantasy_league.gameweek.GameweekRepository;
import com.codemora.fantasy_league.gameweek.GameweekStatus;
import com.codemora.fantasy_league.lineup.GameweekLineupRepository;
import com.codemora.fantasy_league.points.dto.GameweekPointsResponse;
import com.codemora.fantasy_league.scoringrule.ScoringRule;
import com.codemora.fantasy_league.scoringrule.ScoringRuleRepository;
import com.codemora.fantasy_league.season.Season;
import com.codemora.fantasy_league.season.SeasonRepository;

/**
 * Points are computed live from PlayerPerformance + the season's ScoringRule
 * (see ScoringRuleService), never stored -- the same "recompute on read"
 * approach as StandingsService, so editing a scoring rule never requires
 * rewriting past gameweeks. The arithmetic itself lives in SquadScorer, shared
 * with the leaderboard (#33).
 */
@Service
public class GameweekPointsService {

    private final SeasonRepository seasonRepository;
    private final GameweekRepository gameweekRepository;
    private final FantasySquadRepository fantasySquadRepository;
    private final GameweekLineupRepository gameweekLineupRepository;
    private final ScoringRuleRepository scoringRuleRepository;
    private final SquadScorer squadScorer;
    private final CurrentUserProvider currentUserProvider;

    public GameweekPointsService(
            SeasonRepository seasonRepository,
            GameweekRepository gameweekRepository,
            FantasySquadRepository fantasySquadRepository,
            GameweekLineupRepository gameweekLineupRepository,
            ScoringRuleRepository scoringRuleRepository,
            SquadScorer squadScorer,
            CurrentUserProvider currentUserProvider) {
        this.seasonRepository = seasonRepository;
        this.gameweekRepository = gameweekRepository;
        this.fantasySquadRepository = fantasySquadRepository;
        this.gameweekLineupRepository = gameweekLineupRepository;
        this.scoringRuleRepository = scoringRuleRepository;
        this.squadScorer = squadScorer;
        this.currentUserProvider = currentUserProvider;
    }

    public GameweekPointsResponse findPoints(Long leagueId, Long seasonId, Long gameweekId) {
        Gameweek gameweek = findGameweekInSeason(leagueId, seasonId, gameweekId);
        Long userId = currentUserProvider.getUserId();
        FantasySquad squad = fantasySquadRepository.findByUserIdAndSeasonId(userId, seasonId)
                .orElseThrow(() -> new NotFoundException("You don't have a fantasy squad for this season"));
        // The leaderboard treats a missing lineup as zero, but asking for *your*
        // breakdown when you never submitted one is a mistake worth surfacing.
        if (gameweekLineupRepository.findBySquadIdAndGameweekId(squad.getId(), gameweekId).isEmpty()) {
            throw new NotFoundException("You haven't submitted a lineup for this gameweek");
        }

        Map<Position, ScoringRule> rulesByPosition = scoringRuleRepository.findBySeasonId(seasonId).stream()
                .collect(Collectors.toMap(ScoringRule::getPosition, r -> r));
        SquadGameweekScore score = squadScorer.score(squad.getId(), squadScorer.loadContext(gameweekId), rulesByPosition);

        return new GameweekPointsResponse(gameweekId, gameweek.getNumber(), score.players(),
                score.playerPoints(), score.transferPointsCost(), score.totalPoints(),
                gameweek.getStatus() == GameweekStatus.COMPLETE, score.activeChip());
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
