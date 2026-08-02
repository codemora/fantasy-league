package com.codemora.fantasy_league.season;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.codemora.fantasy_league.common.error.ConflictException;
import com.codemora.fantasy_league.common.error.NotFoundException;
import com.codemora.fantasy_league.config.CurrentUserProvider;
import com.codemora.fantasy_league.league.LeagueRepository;
import com.codemora.fantasy_league.season.dto.AddSeasonEntrantRequest;
import com.codemora.fantasy_league.season.dto.CreateSeasonRequest;
import com.codemora.fantasy_league.season.dto.GeneratedEntrantResponse;
import com.codemora.fantasy_league.season.dto.SeasonEntrantResponse;
import com.codemora.fantasy_league.season.dto.SeasonResponse;
import com.codemora.fantasy_league.season.dto.UpdateSeasonRequest;
import com.codemora.fantasy_league.team.Team;
import com.codemora.fantasy_league.team.TeamRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SeasonService {

    private final SeasonRepository seasonRepository;
    private final LeagueRepository leagueRepository;
    private final SeasonEntrantRepository seasonEntrantRepository;
    private final TeamRepository teamRepository;
    private final SimulatedTeamNameGenerator teamNameGenerator;
    private final CurrentUserProvider currentUserProvider;

    public SeasonService(
            SeasonRepository seasonRepository,
            LeagueRepository leagueRepository,
            SeasonEntrantRepository seasonEntrantRepository,
            TeamRepository teamRepository,
            SimulatedTeamNameGenerator teamNameGenerator,
            CurrentUserProvider currentUserProvider) {
        this.seasonRepository = seasonRepository;
        this.leagueRepository = leagueRepository;
        this.seasonEntrantRepository = seasonEntrantRepository;
        this.teamRepository = teamRepository;
        this.teamNameGenerator = teamNameGenerator;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional
    public SeasonResponse create(Long leagueId, CreateSeasonRequest request) {
        if (!leagueRepository.existsById(leagueId)) {
            throw new NotFoundException("No league with id " + leagueId);
        }
        Season season = Season.builder()
                .leagueId(leagueId)
                .period(request.period())
                .teamLimit(request.teamLimit())
                .startingBudget(request.startingBudget())
                .doubleLeg(request.doubleLeg())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .build();
        season = seasonRepository.save(season);
        log.info("season_created id={} league_id={} period={}", season.getId(), season.getLeagueId(), season.getPeriod());
        return toResponse(season);
    }

    @Transactional
    public SeasonResponse update(Long leagueId, Long seasonId, UpdateSeasonRequest request) {
        Season season = findInLeague(leagueId, seasonId);
        long currentEntrants = seasonRepository.countEntrants(seasonId);
        if (request.teamLimit() < currentEntrants) {
            log.warn("season_update_conflict id={} reason=team_limit_below_entrants requested={} current_entrants={}",
                    seasonId, request.teamLimit(), currentEntrants);
            throw new ConflictException(
                    "team_limit (" + request.teamLimit() + ") can't be less than the current number of entered teams ("
                            + currentEntrants + ")");
        }
        season.setPeriod(request.period());
        season.setTeamLimit(request.teamLimit());
        season.setStartingBudget(request.startingBudget());
        season.setDoubleLeg(request.doubleLeg());
        season.setStartDate(request.startDate());
        season.setEndDate(request.endDate());
        season = seasonRepository.save(season);
        log.info("season_updated id={} league_id={} period={}", season.getId(), season.getLeagueId(), season.getPeriod());
        return toResponse(season);
    }

    @Transactional
    public void delete(Long leagueId, Long seasonId) {
        Season season = findInLeague(leagueId, seasonId);
        if (seasonRepository.countEntrants(seasonId) > 0) {
            log.warn("season_deletion_conflict id={} reason=has_entrants", seasonId);
            throw new ConflictException("Season '" + season.getPeriod() + "' has teams entered and can't be deleted");
        }
        if (seasonRepository.hasAnyFixtures(seasonId)) {
            log.warn("season_deletion_conflict id={} reason=has_fixtures", seasonId);
            throw new ConflictException("Season '" + season.getPeriod() + "' has fixtures and can't be deleted");
        }
        if (seasonRepository.hasAnyFantasySquads(seasonId)) {
            log.warn("season_deletion_conflict id={} reason=has_fantasy_squads", seasonId);
            throw new ConflictException("Season '" + season.getPeriod() + "' has fantasy squads and can't be deleted");
        }
        seasonRepository.delete(season);
        log.info("season_deleted id={} league_id={} period={}", season.getId(), season.getLeagueId(), season.getPeriod());
    }

    @Transactional
    public SeasonEntrantResponse addEntrant(Long leagueId, Long seasonId, AddSeasonEntrantRequest request) {
        Season season = findInLeague(leagueId, seasonId);
        Long teamId = request.teamId();
        if (!teamRepository.existsById(teamId)) {
            throw new NotFoundException("No team with id " + teamId);
        }
        if (seasonEntrantRepository.existsBySeasonIdAndTeamId(seasonId, teamId)) {
            log.warn("season_entrant_conflict season_id={} team_id={} reason=already_entered", seasonId, teamId);
            throw new ConflictException("Team " + teamId + " is already entered in this season");
        }
        long currentEntrants = seasonEntrantRepository.countBySeasonId(seasonId);
        if (currentEntrants >= season.getTeamLimit()) {
            log.warn("season_entrant_conflict season_id={} team_id={} reason=season_full current_entrants={} team_limit={}",
                    seasonId, teamId, currentEntrants, season.getTeamLimit());
            throw new ConflictException("Season '" + season.getPeriod() + "' already has its full " + season.getTeamLimit() + " teams");
        }
        SeasonEntrant entrant = seasonEntrantRepository.save(
                SeasonEntrant.builder().seasonId(seasonId).teamId(teamId).build());
        log.info("season_entrant_added season_id={} team_id={}", seasonId, teamId);
        return new SeasonEntrantResponse(entrant.getId(), entrant.getSeasonId(), entrant.getTeamId());
    }

    @Transactional
    public void removeEntrant(Long leagueId, Long seasonId, Long teamId) {
        findInLeague(leagueId, seasonId);
        SeasonEntrant entrant = seasonEntrantRepository.findBySeasonIdAndTeamId(seasonId, teamId)
                .orElseThrow(() -> new NotFoundException("Team " + teamId + " is not entered in season " + seasonId));
        if (seasonRepository.hasAnyFixtures(seasonId)) {
            log.warn("season_entrant_removal_conflict season_id={} team_id={} reason=has_fixtures", seasonId, teamId);
            throw new ConflictException("Season " + seasonId + " already has fixtures generated -- teams can't be removed now");
        }
        seasonEntrantRepository.delete(entrant);
        log.info("season_entrant_removed season_id={} team_id={}", seasonId, teamId);
    }

    /**
     * Creates brand-new simulated Team records and enters them into the season
     * in one action, up to team_limit -- distinct from addEntrant (#11), which
     * adds already-existing teams. Generates at most (team_limit - current
     * entrants) teams; if the season is already full, returns an empty list
     * rather than an error, since "top up to the limit" naturally yields zero.
     */
    @Transactional
    public List<GeneratedEntrantResponse> generateEntrants(Long leagueId, Long seasonId) {
        Season season = findInLeague(leagueId, seasonId);
        long currentEntrants = seasonEntrantRepository.countBySeasonId(seasonId);
        int toGenerate = (int) Math.max(0, season.getTeamLimit() - currentEntrants);

        List<GeneratedEntrantResponse> generated = new ArrayList<>();
        Long adminId = currentUserProvider.getUserId();
        for (String candidateName : teamNameGenerator.candidateNames()) {
            if (generated.size() >= toGenerate) {
                break;
            }
            if (teamRepository.existsByName(candidateName)) {
                continue;
            }
            Team team = teamRepository.save(Team.builder().createdByUserId(adminId).name(candidateName).build());
            SeasonEntrant entrant = seasonEntrantRepository.save(
                    SeasonEntrant.builder().seasonId(seasonId).teamId(team.getId()).build());
            generated.add(new GeneratedEntrantResponse(entrant.getId(), team.getId(), team.getName()));
        }
        log.info("season_entrants_generated season_id={} count={}", seasonId, generated.size());
        return generated;
    }

    /**
     * Addressing a season through a mismatched leagueId in the path (a season
     * that exists, but under a different league) is treated as not-found, same
     * as an unknown id -- the nested URL implies that relationship.
     */
    private Season findInLeague(Long leagueId, Long seasonId) {
        Season season = seasonRepository.findById(seasonId)
                .orElseThrow(() -> new NotFoundException("No season with id " + seasonId));
        if (!season.getLeagueId().equals(leagueId)) {
            throw new NotFoundException("No season with id " + seasonId + " under league " + leagueId);
        }
        return season;
    }

    private SeasonResponse toResponse(Season season) {
        return new SeasonResponse(
                season.getId(),
                season.getLeagueId(),
                season.getPeriod(),
                season.getTeamLimit(),
                season.getStartingBudget(),
                season.isDoubleLeg(),
                season.getStartDate(),
                season.getEndDate());
    }
}
