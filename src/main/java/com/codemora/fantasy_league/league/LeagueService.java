package com.codemora.fantasy_league.league;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.codemora.fantasy_league.common.error.ConflictException;
import com.codemora.fantasy_league.config.CurrentUserProvider;
import com.codemora.fantasy_league.league.dto.CreateLeagueRequest;
import com.codemora.fantasy_league.league.dto.LeagueResponse;

@Service
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
            throw new ConflictException("A league named '" + request.name() + "' already exists");
        }
        League league = League.builder()
                .createdByUserId(currentUserProvider.getUserId())
                .name(request.name())
                .build();
        return toResponse(leagueRepository.save(league));
    }

    private LeagueResponse toResponse(League league) {
        return new LeagueResponse(league.getId(), league.getName());
    }
}
