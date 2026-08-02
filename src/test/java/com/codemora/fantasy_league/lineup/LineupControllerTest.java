package com.codemora.fantasy_league.lineup;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.codemora.fantasy_league.auth.JwtService;
import com.codemora.fantasy_league.common.Position;
import com.codemora.fantasy_league.common.error.ConflictException;
import com.codemora.fantasy_league.common.error.NotFoundException;
import com.codemora.fantasy_league.lineup.dto.GameweekLineupResponse;
import com.codemora.fantasy_league.lineup.dto.LineupSlotResponse;

/**
 * addFilters = false: see TeamControllerTest -- same rationale.
 */
@WebMvcTest(LineupController.class)
@AutoConfigureMockMvc(addFilters = false)
class LineupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LineupService lineupService;

    @MockitoBean
    private JwtService jwtService;

    private GameweekLineupResponse lineupResponse() {
        return new GameweekLineupResponse(900L, 500L, 20L, 1L,
                List.of(new LineupSlotResponse(1L, "Player1", Position.GK, null)),
                List.of(new LineupSlotResponse(4L, "Player4", Position.GK, 1)));
    }

    private String requestJson() {
        return "{\"starterPlayerIds\":[1,2,3,5,6,8,9,10,13,14,15],"
                + "\"benchPlayerIds\":[4,7,11,12],\"captainPlayerId\":1}";
    }

    @Test
    void submitSuccessReturns200() throws Exception {
        when(lineupService.submit(eq(1L), eq(10L), eq(20L), any())).thenReturn(lineupResponse());

        mockMvc.perform(put("/api/v1/leagues/1/seasons/10/gameweeks/20/lineup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.captainPlayerId").value(1))
                .andExpect(jsonPath("$.starters.length()").value(1));
    }

    @Test
    void submitWithWrongStarterCountReturnsProblemDetail() throws Exception {
        mockMvc.perform(put("/api/v1/leagues/1/seasons/10/gameweeks/20/lineup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"starterPlayerIds\":[1,2,3],\"benchPlayerIds\":[4,7,11,12],\"captainPlayerId\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("starterPlayerIds"));
    }

    @Test
    void submitInvalidFormationReturns409() throws Exception {
        when(lineupService.submit(eq(1L), eq(10L), eq(20L), any()))
                .thenThrow(new ConflictException("Starting XI must have 1-1 GK players (found 2)"));

        mockMvc.perform(put("/api/v1/leagues/1/seasons/10/gameweeks/20/lineup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson()))
                .andExpect(status().isConflict());
    }

    @Test
    void submitUnknownGameweekReturns404() throws Exception {
        when(lineupService.submit(eq(1L), eq(10L), eq(99L), any()))
                .thenThrow(new NotFoundException("No gameweek with id 99"));

        mockMvc.perform(put("/api/v1/leagues/1/seasons/10/gameweeks/99/lineup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson()))
                .andExpect(status().isNotFound());
    }

    @Test
    void findCurrentSuccessReturns200() throws Exception {
        when(lineupService.findCurrent(eq(1L), eq(10L), eq(20L))).thenReturn(lineupResponse());

        mockMvc.perform(get("/api/v1/leagues/1/seasons/10/gameweeks/20/lineup"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bench[0].benchOrder").value(1));
    }

    @Test
    void findCurrentNoLineupReturns404() throws Exception {
        when(lineupService.findCurrent(eq(1L), eq(10L), eq(20L)))
                .thenThrow(new NotFoundException("You haven't submitted a lineup for this gameweek"));

        mockMvc.perform(get("/api/v1/leagues/1/seasons/10/gameweeks/20/lineup"))
                .andExpect(status().isNotFound());
    }
}
