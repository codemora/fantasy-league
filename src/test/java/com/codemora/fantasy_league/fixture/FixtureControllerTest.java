package com.codemora.fantasy_league.fixture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

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
import com.codemora.fantasy_league.fixture.dto.FixtureResponse;
import com.codemora.fantasy_league.fixture.dto.GenerateFixturesResponse;

/**
 * addFilters = false: see TeamControllerTest -- same rationale.
 */
@WebMvcTest(FixtureController.class)
@AutoConfigureMockMvc(addFilters = false)
class FixtureControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FixtureService fixtureService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void generateSuccessReturns201() throws Exception {
        when(fixtureService.generate(eq(1L), eq(10L))).thenReturn(new GenerateFixturesResponse(3, 6));

        mockMvc.perform(post("/api/v1/leagues/1/seasons/10/fixtures/generate"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.gameweeksCreated").value(3))
                .andExpect(jsonPath("$.fixturesCreated").value(6));
    }

    @Test
    void generateUnknownSeasonReturns404() throws Exception {
        when(fixtureService.generate(eq(1L), eq(99L))).thenThrow(new NotFoundException("No season with id 99"));

        mockMvc.perform(post("/api/v1/leagues/1/seasons/99/fixtures/generate"))
                .andExpect(status().isNotFound());
    }

    @Test
    void generateAlreadyExistsReturns409() throws Exception {
        when(fixtureService.generate(eq(1L), eq(10L)))
                .thenThrow(new ConflictException("Season 10 already has fixtures generated"));

        mockMvc.perform(post("/api/v1/leagues/1/seasons/10/fixtures/generate"))
                .andExpect(status().isConflict());
    }

    @Test
    void updateSuccessReturns200() throws Exception {
        LocalDateTime newTime = LocalDateTime.of(2025, 8, 2, 17, 30);
        when(fixtureService.update(eq(1L), eq(10L), eq(500L), any()))
                .thenReturn(new FixtureResponse(500L, 10L, 200L, 101L, 102L, null, null, false, newTime));

        mockMvc.perform(put("/api/v1/leagues/1/seasons/10/fixtures/500")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startDateTime\":\"2025-08-02T17:30:00\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.startDateTime").value("2025-08-02T17:30:00"));
    }

    @Test
    void updateUnknownFixtureReturns404() throws Exception {
        when(fixtureService.update(eq(1L), eq(10L), eq(999L), any()))
                .thenThrow(new NotFoundException("No fixture with id 999"));

        mockMvc.perform(put("/api/v1/leagues/1/seasons/10/fixtures/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startDateTime\":\"2025-08-02T17:30:00\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateAlreadyPlayedReturns409() throws Exception {
        when(fixtureService.update(eq(1L), eq(10L), eq(500L), any()))
                .thenThrow(new ConflictException("Fixture 500 already has a recorded result and can't be edited"));

        mockMvc.perform(put("/api/v1/leagues/1/seasons/10/fixtures/500")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"startDateTime\":\"2025-08-02T17:30:00\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void updateWithMissingStartDateTimeReturnsProblemDetail() throws Exception {
        mockMvc.perform(put("/api/v1/leagues/1/seasons/10/fixtures/500")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("startDateTime"));
    }
}
