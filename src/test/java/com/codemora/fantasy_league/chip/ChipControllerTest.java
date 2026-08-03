package com.codemora.fantasy_league.chip;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.codemora.fantasy_league.auth.JwtService;
import com.codemora.fantasy_league.chip.dto.ChipResponse;
import com.codemora.fantasy_league.common.error.ConflictException;
import com.codemora.fantasy_league.common.error.NotFoundException;

/**
 * addFilters = false: see TeamControllerTest -- same rationale.
 */
@WebMvcTest(ChipController.class)
@AutoConfigureMockMvc(addFilters = false)
class ChipControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChipService chipService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void activateSuccessReturns201() throws Exception {
        when(chipService.activate(eq(1L), eq(10L), eq(21L), eq(ChipType.WILDCARD))).thenReturn(
                new ChipResponse(900L, ChipType.WILDCARD, 21L, 1, LocalDateTime.of(2025, 8, 1, 10, 0)));

        mockMvc.perform(post("/api/v1/leagues/1/seasons/10/gameweeks/21/chips/WILDCARD"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.chipType").value("WILDCARD"))
                .andExpect(jsonPath("$.gameweekNumber").value(1));
    }

    @Test
    void activateReturns409WhenAChipIsAlreadyActiveThatGameweek() throws Exception {
        when(chipService.activate(eq(1L), eq(10L), eq(21L), eq(ChipType.TRIPLE_CAPTAIN)))
                .thenThrow(new ConflictException("You've already played a chip for gameweek 1"));

        mockMvc.perform(post("/api/v1/leagues/1/seasons/10/gameweeks/21/chips/TRIPLE_CAPTAIN"))
                .andExpect(status().isConflict());
    }

    @Test
    void activateReturns404ForAnUnknownGameweek() throws Exception {
        when(chipService.activate(eq(1L), eq(10L), eq(99L), eq(ChipType.BENCH_BOOST)))
                .thenThrow(new NotFoundException("No gameweek with id 99"));

        mockMvc.perform(post("/api/v1/leagues/1/seasons/10/gameweeks/99/chips/BENCH_BOOST"))
                .andExpect(status().isNotFound());
    }

    @Test
    void findHistoryReturnsChipsPlayedThisSeason() throws Exception {
        when(chipService.findHistory(1L, 10L)).thenReturn(List.of(
                new ChipResponse(900L, ChipType.WILDCARD, 21L, 1, LocalDateTime.of(2025, 8, 1, 10, 0))));

        mockMvc.perform(get("/api/v1/leagues/1/seasons/10/chips"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].chipType").value("WILDCARD"));
    }
}
