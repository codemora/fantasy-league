package com.codemora.fantasy_league.league;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.codemora.fantasy_league.common.error.ConflictException;
import com.codemora.fantasy_league.common.error.NotFoundException;
import com.codemora.fantasy_league.config.CurrentUserProvider;
import com.codemora.fantasy_league.league.dto.CreateLeagueRequest;
import com.codemora.fantasy_league.league.dto.LeagueResponse;
import com.codemora.fantasy_league.league.dto.UpdateLeagueRequest;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class LeagueService {

    private final LeagueRepository leagueRepository;
    private final CurrentUserProvider currentUserProvider;

    public LeagueService(LeagueRepository leagueRepository, CurrentUserProvider currentUserProvider) {
        this.leagueRepository = leagueRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional
    public LeagueResponse create(CreateLeagueRequest request) {
        if (leagueRepository.existsByName(request.name())) {
            log.warn("league_creation_conflict name={}", request.name());
            throw new ConflictException("A league named '" + request.name() + "' already exists");
        }
        League league = League.builder()
                .createdByUserId(currentUserProvider.getUserId())
                .name(request.name())
                .build();
        league = leagueRepository.save(league);
        log.info("league_created id={} name={} created_by_user_id={}", league.getId(), league.getName(), league.getCreatedByUserId());
        return toResponse(league);
    }

    @Transactional
    public LeagueResponse update(Long id, UpdateLeagueRequest request) {
        League league = leagueRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("No league with id " + id));
        if (leagueRepository.existsByNameAndIdNot(request.name(), id)) {
            log.warn("league_update_conflict id={} name={}", id, request.name());
            throw new ConflictException("A league named '" + request.name() + "' already exists");
        }
        league.setName(request.name());
        league = leagueRepository.save(league);
        log.info("league_updated id={} name={}", league.getId(), league.getName());
        return toResponse(league);
    }

    private LeagueResponse toResponse(League league) {
        return new LeagueResponse(league.getId(), league.getName());
    }
}
