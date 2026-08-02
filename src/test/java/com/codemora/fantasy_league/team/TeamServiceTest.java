package com.codemora.fantasy_league.team;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.codemora.fantasy_league.common.error.ConflictException;
import com.codemora.fantasy_league.common.error.NotFoundException;
import com.codemora.fantasy_league.config.CurrentUserProvider;
import com.codemora.fantasy_league.team.dto.CreateTeamRequest;
import com.codemora.fantasy_league.team.dto.TeamResponse;
import com.codemora.fantasy_league.team.dto.UpdateTeamRequest;

@ExtendWith(MockitoExtension.class)
class TeamServiceTest {

    @Mock
    private TeamRepository teamRepository;
    @Mock
    private CurrentUserProvider currentUserProvider;

    private TeamService teamService() {
        return new TeamService(teamRepository, currentUserProvider);
    }

    @Test
    void createSavesTeamStampedWithCreator() {
        when(teamRepository.existsByName("Arsenal")).thenReturn(false);
        when(currentUserProvider.getUserId()).thenReturn(7L);
        when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> {
            Team t = invocation.getArgument(0);
            t.setId(1L);
            return t;
        });

        TeamResponse response = teamService().create(new CreateTeamRequest("Arsenal", "Victoria Concordia Crescit"));

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Arsenal");
        assertThat(response.slogan()).isEqualTo("Victoria Concordia Crescit");
    }

    @Test
    void createRejectsDuplicateName() {
        when(teamRepository.existsByName("Arsenal")).thenReturn(true);

        assertThatThrownBy(() -> teamService().create(new CreateTeamRequest("Arsenal", null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void updateSavesNewDetails() {
        Team existing = Team.builder().id(1L).createdByUserId(7L).name("Arsenal").slogan("Old").build();
        when(teamRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(teamRepository.existsByNameAndIdNot("Arsenal FC", 1L)).thenReturn(false);
        when(teamRepository.save(any(Team.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TeamResponse response = teamService().update(1L, new UpdateTeamRequest("Arsenal FC", "New"));

        assertThat(response.name()).isEqualTo("Arsenal FC");
        assertThat(response.slogan()).isEqualTo("New");
    }

    @Test
    void updateRejectsUnknownId() {
        when(teamRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> teamService().update(99L, new UpdateTeamRequest("Arsenal", null)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateRejectsNameClashingWithAnotherTeam() {
        Team existing = Team.builder().id(1L).createdByUserId(7L).name("Arsenal").build();
        when(teamRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(teamRepository.existsByNameAndIdNot("Chelsea", 1L)).thenReturn(true);

        assertThatThrownBy(() -> teamService().update(1L, new UpdateTeamRequest("Chelsea", null)))
                .isInstanceOf(ConflictException.class);
    }
}
