package com.codemora.fantasy_league.league;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import com.codemora.fantasy_league.common.error.ConflictException;
import com.codemora.fantasy_league.common.error.NotFoundException;
import com.codemora.fantasy_league.league.dto.LeagueResponse;

/**
 * addFilters = false: see TeamControllerTest -- same rationale, this slice verifies
 * request validation and response shape, not @PreAuthorize role enforcement.
 */
@WebMvcTest(LeagueController.class)
@AutoConfigureMockMvc(addFilters = false)
class LeagueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LeagueService leagueService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void createWithBlankNameReturnsProblemDetail() throws Exception {
        mockMvc.perform(post("/api/v1/leagues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("name"));
    }

    @Test
    void createSuccessReturns201() throws Exception {
        when(leagueService.create(any())).thenReturn(new LeagueResponse(1L, "Premier League"));

        mockMvc.perform(post("/api/v1/leagues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Premier League\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Premier League"));
    }

    @Test
    void createDuplicateNameReturns409() throws Exception {
        when(leagueService.create(any())).thenThrow(new ConflictException("A league named 'Premier League' already exists"));

        mockMvc.perform(post("/api/v1/leagues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Premier League\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void updateSuccessReturns200() throws Exception {
        when(leagueService.update(eq(1L), any())).thenReturn(new LeagueResponse(1L, "EPL"));

        mockMvc.perform(put("/api/v1/leagues/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"EPL\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("EPL"));
    }

    @Test
    void updateUnknownIdReturns404() throws Exception {
        when(leagueService.update(eq(99L), any())).thenThrow(new NotFoundException("No league with id 99"));

        mockMvc.perform(put("/api/v1/leagues/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"EPL\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateWithBlankNameReturnsProblemDetail() throws Exception {
        mockMvc.perform(put("/api/v1/leagues/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("name"));
    }
}
