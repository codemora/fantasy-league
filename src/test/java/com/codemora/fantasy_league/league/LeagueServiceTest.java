package com.codemora.fantasy_league.league;

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
import com.codemora.fantasy_league.league.dto.CreateLeagueRequest;
import com.codemora.fantasy_league.league.dto.LeagueResponse;
import com.codemora.fantasy_league.league.dto.UpdateLeagueRequest;

@ExtendWith(MockitoExtension.class)
class LeagueServiceTest {

    @Mock
    private LeagueRepository leagueRepository;
    @Mock
    private CurrentUserProvider currentUserProvider;

    private LeagueService leagueService() {
        return new LeagueService(leagueRepository, currentUserProvider);
    }

    @Test
    void createSavesLeagueStampedWithCreator() {
        when(leagueRepository.existsByName("Premier League")).thenReturn(false);
        when(currentUserProvider.getUserId()).thenReturn(7L);
        when(leagueRepository.save(any(League.class))).thenAnswer(invocation -> {
            League l = invocation.getArgument(0);
            l.setId(1L);
            return l;
        });

        LeagueResponse response = leagueService().create(new CreateLeagueRequest("Premier League"));

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.name()).isEqualTo("Premier League");
    }

    @Test
    void createRejectsDuplicateName() {
        when(leagueRepository.existsByName("Premier League")).thenReturn(true);

        assertThatThrownBy(() -> leagueService().create(new CreateLeagueRequest("Premier League")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void updateSavesNewName() {
        League existing = League.builder().id(1L).createdByUserId(7L).name("Premier League").build();
        when(leagueRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(leagueRepository.existsByNameAndIdNot("EPL", 1L)).thenReturn(false);
        when(leagueRepository.save(any(League.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LeagueResponse response = leagueService().update(1L, new UpdateLeagueRequest("EPL"));

        assertThat(response.name()).isEqualTo("EPL");
    }

    @Test
    void updateRejectsUnknownId() {
        when(leagueRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> leagueService().update(99L, new UpdateLeagueRequest("EPL")))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateRejectsNameClashingWithAnotherLeague() {
        League existing = League.builder().id(1L).createdByUserId(7L).name("Premier League").build();
        when(leagueRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(leagueRepository.existsByNameAndIdNot("La Liga", 1L)).thenReturn(true);

        assertThatThrownBy(() -> leagueService().update(1L, new UpdateLeagueRequest("La Liga")))
                .isInstanceOf(ConflictException.class);
    }
}
