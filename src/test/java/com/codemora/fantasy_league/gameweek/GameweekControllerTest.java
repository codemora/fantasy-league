package com.codemora.fantasy_league.gameweek;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.codemora.fantasy_league.auth.JwtService;
import com.codemora.fantasy_league.common.error.ConflictException;
import com.codemora.fantasy_league.common.error.NotFoundException;
import com.codemora.fantasy_league.gameweek.dto.GameweekResponse;

/**
 * addFilters = false: see TeamControllerTest -- same rationale.
 */
@WebMvcTest(GameweekController.class)
@AutoConfigureMockMvc(addFilters = false)
class GameweekControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GameweekService gameweekService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void findBySeasonSuccessReturns200() throws Exception {
        when(gameweekService.findBySeason(eq(1L), eq(10L))).thenReturn(List.of(
                new GameweekResponse(200L, 10L, 1, LocalDateTime.of(2025, 8, 1, 12, 0), GameweekStatus.UPCOMING, true, 120)));

        mockMvc.perform(get("/api/v1/leagues/1/seasons/10/gameweeks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].number").value(1))
                .andExpect(jsonPath("$[0].isNext").value(true))
                .andExpect(jsonPath("$[0].minutesUntilDeadline").value(120));
    }

    @Test
    void updateStatusSuccessReturns200() throws Exception {
        when(gameweekService.updateStatus(eq(1L), eq(10L), eq(200L), any())).thenReturn(
                new GameweekResponse(200L, 10L, 1, LocalDateTime.of(2025, 8, 1, 12, 0), GameweekStatus.LOCKED, false, 0));

        mockMvc.perform(patch("/api/v1/leagues/1/seasons/10/gameweeks/200/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"LOCKED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("LOCKED"));
    }

    @Test
    void updateStatusWithAnInvalidTransitionReturns409() throws Exception {
        when(gameweekService.updateStatus(eq(1L), eq(10L), eq(200L), any()))
                .thenThrow(new ConflictException("the only allowed next status is LOCKED"));

        mockMvc.perform(patch("/api/v1/leagues/1/seasons/10/gameweeks/200/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"COMPLETE\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void updateStatusWithNoStatusReturnsProblemDetail() throws Exception {
        mockMvc.perform(patch("/api/v1/leagues/1/seasons/10/gameweeks/200/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("status"));
    }

    @Test
    void findBySeasonUnknownSeasonReturns404() throws Exception {
        when(gameweekService.findBySeason(eq(1L), eq(99L))).thenThrow(new NotFoundException("No season with id 99"));

        mockMvc.perform(get("/api/v1/leagues/1/seasons/99/gameweeks"))
                .andExpect(status().isNotFound());
    }
}
