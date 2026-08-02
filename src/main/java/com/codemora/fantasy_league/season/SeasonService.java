package com.codemora.fantasy_league.season;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.codemora.fantasy_league.common.error.NotFoundException;
import com.codemora.fantasy_league.league.LeagueRepository;
import com.codemora.fantasy_league.season.dto.CreateSeasonRequest;
import com.codemora.fantasy_league.season.dto.SeasonResponse;

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
