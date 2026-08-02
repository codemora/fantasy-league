package com.codemora.fantasy_league.leaderboard;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.codemora.fantasy_league.auth.User;
import com.codemora.fantasy_league.auth.UserRepository;
import com.codemora.fantasy_league.common.Position;
import com.codemora.fantasy_league.common.error.NotFoundException;
import com.codemora.fantasy_league.fantasysquad.FantasySquad;
import com.codemora.fantasy_league.fantasysquad.FantasySquadRepository;
import com.codemora.fantasy_league.gameweek.Gameweek;
import com.codemora.fantasy_league.gameweek.GameweekRepository;
import com.codemora.fantasy_league.leaderboard.dto.LeaderboardRowResponse;
import com.codemora.fantasy_league.points.SquadGameweekScore;
import com.codemora.fantasy_league.points.SquadScorer;
import com.codemora.fantasy_league.scoringrule.ScoringRule;
import com.codemora.fantasy_league.scoringrule.ScoringRuleRepository;
import com.codemora.fantasy_league.season.Season;
import com.codemora.fantasy_league.season.SeasonRepository;

/**
 * Ranks every squad in a season by points accumulated so far, recomputed on
 * read via SquadScorer -- the same arithmetic behind each user's own gameweek
 * breakdown, so the two can't disagree.
 */
@Service
public class LeaderboardService {

    private final SeasonRepository seasonRepository;
    private final GameweekRepository gameweekRepository;
    private final FantasySquadRepository fantasySquadRepository;
    private final ScoringRuleRepository scoringRuleRepository;
    private final UserRepository userRepository;
    private final SquadScorer squadScorer;

    public LeaderboardService(
            SeasonRepository seasonRepository,
            GameweekRepository gameweekRepository,
            FantasySquadRepository fantasySquadRepository,
            ScoringRuleRepository scoringRuleRepository,
            UserRepository userRepository,
            SquadScorer squadScorer) {
        this.seasonRepository = seasonRepository;
        this.gameweekRepository = gameweekRepository;
        this.fantasySquadRepository = fantasySquadRepository;
        this.scoringRuleRepository = scoringRuleRepository;
        this.userRepository = userRepository;
        this.squadScorer = squadScorer;
    }

    /**
     * Every gameweek counts, including ones still in progress -- the table is
     * live rather than settled. The per-gameweek `official` flag (#36) is where
     * "these points can still move" is expressed; freezing the leaderboard at
     * the last COMPLETE gameweek would instead make it silently stale.
     */
    public List<LeaderboardRowResponse> findBySeason(Long leagueId, Long seasonId) {
        findSeasonInLeague(leagueId, seasonId);

        List<FantasySquad> squads = fantasySquadRepository.findBySeasonId(seasonId);
        if (squads.isEmpty()) {
            return List.of();
        }

        Map<Position, ScoringRule> rulesByPosition = scoringRuleRepository.findBySeasonId(seasonId).stream()
                .collect(Collectors.toMap(ScoringRule::getPosition, r -> r));
        Map<Long, String> usernamesById = userRepository.findAllById(
                        squads.stream().map(FantasySquad::getUserId).distinct().toList())
                .stream().collect(Collectors.toMap(User::getId, User::getUsername));

        Map<Long, Integer> totalBySquadId = new HashMap<>();
        Map<Long, Integer> hitsBySquadId = new HashMap<>();
        // Outer loop over gameweeks so each gameweek's performances load once and
        // are reused across every squad, rather than once per squad per gameweek.
        for (Gameweek gameweek : gameweekRepository.findBySeasonIdOrderByNumber(seasonId)) {
            SquadScorer.GameweekContext context = squadScorer.loadContext(gameweek.getId());
            for (FantasySquad squad : squads) {
                SquadGameweekScore score = squadScorer.score(squad.getId(), context, rulesByPosition);
                totalBySquadId.merge(squad.getId(), score.totalPoints(), Integer::sum);
                hitsBySquadId.merge(squad.getId(), score.transferPointsCost(), Integer::sum);
            }
        }

        List<FantasySquad> ranked = new ArrayList<>(squads);
        ranked.sort(Comparator
                .comparingInt((FantasySquad s) -> totalBySquadId.getOrDefault(s.getId(), 0)).reversed()
                .thenComparing(s -> usernamesById.getOrDefault(s.getUserId(), "")));

        List<LeaderboardRowResponse> rows = new ArrayList<>();
        for (int i = 0; i < ranked.size(); i++) {
            FantasySquad squad = ranked.get(i);
            int total = totalBySquadId.getOrDefault(squad.getId(), 0);
            // Standard competition ranking: equal totals share a rank and the next
            // distinct total skips ahead (1, 2, 2, 4) rather than being broken
            // arbitrarily, per #33's tie requirement.
            int rank = (i > 0 && totalBySquadId.getOrDefault(ranked.get(i - 1).getId(), 0) == total)
                    ? rows.get(i - 1).rank()
                    : i + 1;
            rows.add(new LeaderboardRowResponse(rank, squad.getId(), squad.getUserId(),
                    usernamesById.get(squad.getUserId()), total, hitsBySquadId.getOrDefault(squad.getId(), 0)));
        }
        return rows;
    }

    private Season findSeasonInLeague(Long leagueId, Long seasonId) {
        Season season = seasonRepository.findById(seasonId)
                .orElseThrow(() -> new NotFoundException("No season with id " + seasonId));
        if (!season.getLeagueId().equals(leagueId)) {
            throw new NotFoundException("No season with id " + seasonId + " under league " + leagueId);
        }
        return season;
    }
}
