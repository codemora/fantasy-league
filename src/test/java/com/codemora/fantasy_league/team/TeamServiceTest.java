package com.codemora.fantasy_league.team;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.codemora.fantasy_league.common.error.ConflictException;
import com.codemora.fantasy_league.config.CurrentUserProvider;
import com.codemora.fantasy_league.team.dto.CreateTeamRequest;
import com.codemora.fantasy_league.team.dto.TeamResponse;

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
}
