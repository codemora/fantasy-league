package com.codemora.fantasy_league.team;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.codemora.fantasy_league.common.error.ConflictException;
import com.codemora.fantasy_league.config.CurrentUserProvider;
import com.codemora.fantasy_league.team.dto.CreateTeamRequest;
import com.codemora.fantasy_league.team.dto.TeamResponse;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class TeamService {

    private final TeamRepository teamRepository;
    private final CurrentUserProvider currentUserProvider;

    public TeamService(TeamRepository teamRepository, CurrentUserProvider currentUserProvider) {
        this.teamRepository = teamRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional
    public TeamResponse create(CreateTeamRequest request) {
        if (teamRepository.existsByName(request.name())) {
            log.warn("team_creation_conflict name={}", request.name());
            throw new ConflictException("A team named '" + request.name() + "' already exists");
        }
        Team team = Team.builder()
                .createdByUserId(currentUserProvider.getUserId())
                .name(request.name())
                .slogan(request.slogan())
                .build();
        team = teamRepository.save(team);
        log.info("team_created id={} name={} created_by_user_id={}", team.getId(), team.getName(), team.getCreatedByUserId());
        return toResponse(team);
    }

    private TeamResponse toResponse(Team team) {
        return new TeamResponse(team.getId(), team.getName(), team.getSlogan());
    }
}
