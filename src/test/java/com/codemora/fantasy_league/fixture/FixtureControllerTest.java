package com.codemora.fantasy_league.fixture;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.codemora.fantasy_league.auth.JwtService;
import com.codemora.fantasy_league.common.error.ConflictException;
import com.codemora.fantasy_league.common.error.NotFoundException;
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
}
