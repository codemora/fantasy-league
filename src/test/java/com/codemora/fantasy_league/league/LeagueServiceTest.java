package com.codemora.fantasy_league.league;

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
import com.codemora.fantasy_league.league.dto.CreateLeagueRequest;
import com.codemora.fantasy_league.league.dto.LeagueResponse;

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
}
