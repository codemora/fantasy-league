package com.codemora.fantasy_league.standings;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.codemora.fantasy_league.auth.JwtService;
import com.codemora.fantasy_league.common.error.NotFoundException;
import com.codemora.fantasy_league.standings.dto.LeagueTableRowResponse;

/**
 * addFilters = false: see TeamControllerTest -- same rationale.
 */
@WebMvcTest(StandingsController.class)
@AutoConfigureMockMvc(addFilters = false)
class StandingsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StandingsService standingsService;

    @MockitoBean
    private JwtService jwtService;

    private LeagueTableRowResponse row(int position, long teamId, String name, int points) {
        return new LeagueTableRowResponse(position, teamId, name, 3, 1, 0, 2, 4, 3, 1, points);
    }

    @Test
    void tableSuccessReturns200() throws Exception {
        when(standingsService.getTable(eq(1L), eq(10L))).thenReturn(List.of(row(1, 101L, "Arsenal", 4)));

        mockMvc.perform(get("/api/v1/leagues/1/seasons/10/standings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].teamName").value("Arsenal"))
                .andExpect(jsonPath("$[0].position").value(1));
    }

    @Test
    void tableUnknownSeasonReturns404() throws Exception {
        when(standingsService.getTable(eq(1L), eq(99L))).thenThrow(new NotFoundException("No season with id 99"));

        mockMvc.perform(get("/api/v1/leagues/1/seasons/99/standings"))
                .andExpect(status().isNotFound());
    }

    @Test
    void teamPositionSuccessReturns200() throws Exception {
        when(standingsService.getTeamPosition(eq(1L), eq(10L), eq(101L))).thenReturn(row(1, 101L, "Arsenal", 4));

        mockMvc.perform(get("/api/v1/leagues/1/seasons/10/standings/teams/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teamId").value(101));
    }

    @Test
    void teamPositionUnknownTeamReturns404() throws Exception {
        when(standingsService.getTeamPosition(eq(1L), eq(10L), eq(999L)))
                .thenThrow(new NotFoundException("Team 999 is not entered in season 10"));

        mockMvc.perform(get("/api/v1/leagues/1/seasons/10/standings/teams/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void winnerSuccessReturns200() throws Exception {
        when(standingsService.getWinner(eq(1L), eq(10L))).thenReturn(row(1, 101L, "Arsenal", 4));

        mockMvc.perform(get("/api/v1/leagues/1/seasons/10/standings/winner"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teamId").value(101));
    }

    @Test
    void topTeamsSuccessReturns200() throws Exception {
        when(standingsService.getTopTeams(eq(1L), eq(10L))).thenReturn(List.of(row(1, 101L, "Arsenal", 4)));

        mockMvc.perform(get("/api/v1/leagues/1/seasons/10/standings/top-teams"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].teamId").value(101));
    }

    @Test
    void relegatedSuccessReturns200() throws Exception {
        when(standingsService.getRelegatedTeams(eq(1L), eq(10L))).thenReturn(List.of(row(4, 104L, "Everton", 1)));

        mockMvc.perform(get("/api/v1/leagues/1/seasons/10/standings/relegated"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].teamId").value(104));
    }
}
