package com.codemora.fantasy_league.season;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.codemora.fantasy_league.common.error.ConflictException;
import com.codemora.fantasy_league.common.error.NotFoundException;
import com.codemora.fantasy_league.league.LeagueRepository;
import com.codemora.fantasy_league.season.dto.CreateSeasonRequest;
import com.codemora.fantasy_league.season.dto.SeasonResponse;
import com.codemora.fantasy_league.season.dto.UpdateSeasonRequest;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class SeasonService {

    private final SeasonRepository seasonRepository;
    private final LeagueRepository leagueRepository;

    public SeasonService(SeasonRepository seasonRepository, LeagueRepository leagueRepository) {
        this.seasonRepository = seasonRepository;
        this.leagueRepository = leagueRepository;
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
