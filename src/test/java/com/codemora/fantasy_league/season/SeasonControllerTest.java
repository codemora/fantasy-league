package com.codemora.fantasy_league.season;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.codemora.fantasy_league.auth.JwtService;
import com.codemora.fantasy_league.common.error.NotFoundException;
import com.codemora.fantasy_league.season.dto.SeasonResponse;

/**
 * addFilters = false: see TeamControllerTest -- same rationale.
 */
@WebMvcTest(SeasonController.class)
@AutoConfigureMockMvc(addFilters = false)
class SeasonControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SeasonService seasonService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void createWithBlankPeriodReturnsProblemDetail() throws Exception {
        mockMvc.perform(post("/api/v1/leagues/1/seasons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"period\":\"\",\"teamLimit\":20,\"startingBudget\":1000}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("period"));
    }

    @Test
    void createSuccessReturns201() throws Exception {
        when(seasonService.create(eq(1L), any()))
                .thenReturn(new SeasonResponse(10L, 1L, "2025-26", 20, 1000, false, null, null));

        mockMvc.perform(post("/api/v1/leagues/1/seasons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"period\":\"2025-26\",\"teamLimit\":20,\"startingBudget\":1000}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.period").value("2025-26"))
                .andExpect(jsonPath("$.teamLimit").value(20));
    }

    @Test
    void createUnknownLeagueReturns404() throws Exception {
        when(seasonService.create(eq(99L), any())).thenThrow(new NotFoundException("No league with id 99"));

        mockMvc.perform(post("/api/v1/leagues/99/seasons")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"period\":\"2025-26\",\"teamLimit\":20,\"startingBudget\":1000}"))
                .andExpect(status().isNotFound());
    }
}
