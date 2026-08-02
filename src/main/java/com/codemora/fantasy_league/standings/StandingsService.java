package com.codemora.fantasy_league.standings;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.codemora.fantasy_league.common.error.NotFoundException;
import com.codemora.fantasy_league.fixture.Fixture;
import com.codemora.fantasy_league.fixture.FixtureRepository;
import com.codemora.fantasy_league.season.Season;
import com.codemora.fantasy_league.season.SeasonEntrant;
import com.codemora.fantasy_league.season.SeasonEntrantRepository;
import com.codemora.fantasy_league.season.SeasonRepository;
import com.codemora.fantasy_league.standings.dto.LeagueTableRowResponse;
import com.codemora.fantasy_league.team.Team;
import com.codemora.fantasy_league.team.TeamRepository;

@Service
public class StandingsService {

    private static final int TOP_TEAMS_COUNT = 4;
    private static final int RELEGATED_TEAMS_COUNT = 3;

    private final SeasonRepository seasonRepository;
    private final SeasonEntrantRepository seasonEntrantRepository;
    private final FixtureRepository fixtureRepository;
    private final TeamRepository teamRepository;

    public StandingsService(
            SeasonRepository seasonRepository,
            SeasonEntrantRepository seasonEntrantRepository,
            FixtureRepository fixtureRepository,
            TeamRepository teamRepository) {
        this.seasonRepository = seasonRepository;
        this.seasonEntrantRepository = seasonEntrantRepository;
        this.fixtureRepository = fixtureRepository;
        this.teamRepository = teamRepository;
    }

    /**
     * Computed live from entrants + played fixtures rather than stored, so a
     * newly recorded result is reflected immediately with nothing further to
     * update on the fixture-write side (see FixtureService#addResult).
     */
    public List<LeagueTableRowResponse> getTable(Long leagueId, Long seasonId) {
        findInLeague(leagueId, seasonId);

        Map<Long, Accumulator> accumulators = new LinkedHashMap<>();
        for (SeasonEntrant entrant : seasonEntrantRepository.findBySeasonId(seasonId)) {
            accumulators.put(entrant.getTeamId(), new Accumulator());
        }
        for (Fixture fixture : fixtureRepository.findBySeasonIdAndPlayedTrue(seasonId)) {
            Accumulator home = accumulators.get(fixture.getHomeTeamId());
            Accumulator away = accumulators.get(fixture.getAwayTeamId());
            if (home == null || away == null) {
                continue;
            }
            home.apply(fixture.getHomeTeamScore(), fixture.getAwayTeamScore());
            away.apply(fixture.getAwayTeamScore(), fixture.getHomeTeamScore());
        }

        Map<Long, String> teamNames = new HashMap<>();
        for (Team team : teamRepository.findAllById(accumulators.keySet())) {
            teamNames.put(team.getId(), team.getName());
        }

        List<Map.Entry<Long, Accumulator>> ranked = new ArrayList<>(accumulators.entrySet());
        ranked.sort(Comparator
                .comparingInt((Map.Entry<Long, Accumulator> e) -> e.getValue().points).reversed()
                .thenComparing(Comparator.comparingInt((Map.Entry<Long, Accumulator> e) -> e.getValue().goalDifference()).reversed())
                .thenComparing(Comparator.comparingInt((Map.Entry<Long, Accumulator> e) -> e.getValue().goalsFor).reversed()));

        List<LeagueTableRowResponse> table = new ArrayList<>();
        int position = 1;
        for (Map.Entry<Long, Accumulator> entry : ranked) {
            Long teamId = entry.getKey();
            Accumulator acc = entry.getValue();
            table.add(new LeagueTableRowResponse(
                    position++,
                    teamId,
                    teamNames.get(teamId),
                    acc.matchesPlayed,
                    acc.wins,
                    acc.draws,
                    acc.losses,
                    acc.goalsFor,
                    acc.goalsAgainst,
                    acc.goalDifference(),
                    acc.points));
        }
        return table;
    }

    public LeagueTableRowResponse getTeamPosition(Long leagueId, Long seasonId, Long teamId) {
        return getTable(leagueId, seasonId).stream()
                .filter(row -> row.teamId().equals(teamId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Team " + teamId + " is not entered in season " + seasonId));
    }

    public LeagueTableRowResponse getWinner(Long leagueId, Long seasonId) {
        List<LeagueTableRowResponse> table = getTable(leagueId, seasonId);
        if (table.isEmpty()) {
            throw new NotFoundException("Season " + seasonId + " has no entered teams");
        }
        return table.get(0);
    }

    /** Fixed v1 assumption (Premier-League-style 20-team season) rather than derived from team_limit. */
    public List<LeagueTableRowResponse> getTopTeams(Long leagueId, Long seasonId) {
        List<LeagueTableRowResponse> table = getTable(leagueId, seasonId);
        return table.subList(0, Math.min(TOP_TEAMS_COUNT, table.size()));
    }

    /** Fixed v1 assumption (Premier-League-style 20-team season) rather than derived from team_limit. */
    public List<LeagueTableRowResponse> getRelegatedTeams(Long leagueId, Long seasonId) {
        List<LeagueTableRowResponse> table = getTable(leagueId, seasonId);
        int from = Math.max(0, table.size() - RELEGATED_TEAMS_COUNT);
        return table.subList(from, table.size());
    }

    private Season findInLeague(Long leagueId, Long seasonId) {
        Season season = seasonRepository.findById(seasonId)
                .orElseThrow(() -> new NotFoundException("No season with id " + seasonId));
        if (!season.getLeagueId().equals(leagueId)) {
            throw new NotFoundException("No season with id " + seasonId + " under league " + leagueId);
        }
        return season;
    }

    private static class Accumulator {
        int matchesPlayed;
        int wins;
        int draws;
        int losses;
        int goalsFor;
        int goalsAgainst;
        int points;

        void apply(int scored, int conceded) {
            matchesPlayed++;
            goalsFor += scored;
            goalsAgainst += conceded;
            if (scored > conceded) {
                wins++;
                points += 3;
            } else if (scored == conceded) {
                draws++;
                points += 1;
            } else {
                losses++;
            }
        }

        int goalDifference() {
            return goalsFor - goalsAgainst;
        }
    }
}
