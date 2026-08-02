package com.codemora.fantasy_league.team;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.codemora.fantasy_league.common.PageResponse;
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

    @Test
    void deleteRemovesAnUnusedTeam() {
        Team existing = Team.builder().id(1L).createdByUserId(7L).name("Arsenal").build();
        when(teamRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(teamRepository.isEnteredInAnySeason(1L)).thenReturn(false);
        when(teamRepository.hasAnyPlayers(1L)).thenReturn(false);
        when(teamRepository.hasAnyFixtures(1L)).thenReturn(false);

        teamService().delete(1L);

        org.mockito.Mockito.verify(teamRepository).delete(existing);
    }

    @Test
    void deleteRejectsUnknownId() {
        when(teamRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> teamService().delete(99L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void deleteRejectsTeamEnteredInASeason() {
        Team existing = Team.builder().id(1L).createdByUserId(7L).name("Arsenal").build();
        when(teamRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(teamRepository.isEnteredInAnySeason(1L)).thenReturn(true);

        assertThatThrownBy(() -> teamService().delete(1L)).isInstanceOf(ConflictException.class);
    }

    @Test
    void deleteRejectsTeamWithPlayers() {
        Team existing = Team.builder().id(1L).createdByUserId(7L).name("Arsenal").build();
        when(teamRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(teamRepository.isEnteredInAnySeason(1L)).thenReturn(false);
        when(teamRepository.hasAnyPlayers(1L)).thenReturn(true);

        assertThatThrownBy(() -> teamService().delete(1L)).isInstanceOf(ConflictException.class);
    }

    @Test
    void deleteRejectsTeamWithFixtures() {
        Team existing = Team.builder().id(1L).createdByUserId(7L).name("Arsenal").build();
        when(teamRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(teamRepository.isEnteredInAnySeason(1L)).thenReturn(false);
        when(teamRepository.hasAnyPlayers(1L)).thenReturn(false);
        when(teamRepository.hasAnyFixtures(1L)).thenReturn(true);

        assertThatThrownBy(() -> teamService().delete(1L)).isInstanceOf(ConflictException.class);
    }

    @Test
    void findByIdReturnsTheTeam() {
        Team existing = Team.builder().id(1L).createdByUserId(7L).name("Arsenal").build();
        when(teamRepository.findById(1L)).thenReturn(Optional.of(existing));

        assertThat(teamService().findById(1L).name()).isEqualTo("Arsenal");
    }

    @Test
    void findByIdRejectsUnknownId() {
        when(teamRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> teamService().findById(99L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void searchWithoutNameListsAllTeams() {
        Pageable pageable = PageRequest.of(0, 20);
        Team arsenal = Team.builder().id(1L).createdByUserId(7L).name("Arsenal").build();
        when(teamRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(arsenal), pageable, 1));

        PageResponse<TeamResponse> response = teamService().search(null, pageable);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).name()).isEqualTo("Arsenal");
        assertThat(response.totalElements()).isEqualTo(1);
    }

    @Test
    void searchWithNameFiltersByPartialCaseInsensitiveMatch() {
        Pageable pageable = PageRequest.of(0, 20);
        Team arsenal = Team.builder().id(1L).createdByUserId(7L).name("Arsenal").build();
        when(teamRepository.findByNameContainingIgnoreCase("arse", pageable))
                .thenReturn(new PageImpl<>(List.of(arsenal), pageable, 1));

        PageResponse<TeamResponse> response = teamService().search("arse", pageable);

        assertThat(response.content()).extracting(TeamResponse::name).containsExactly("Arsenal");
    }
}
