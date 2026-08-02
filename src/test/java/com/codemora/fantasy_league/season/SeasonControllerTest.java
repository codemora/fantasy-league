package com.codemora.fantasy_league.season;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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

import java.util.List;

import com.codemora.fantasy_league.auth.JwtService;
import com.codemora.fantasy_league.common.error.ConflictException;
import com.codemora.fantasy_league.common.error.NotFoundException;
import com.codemora.fantasy_league.season.dto.GeneratedEntrantResponse;
import com.codemora.fantasy_league.season.dto.SeasonEntrantResponse;
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

    @Test
    void updateSuccessReturns200() throws Exception {
        when(seasonService.update(eq(1L), eq(10L), any()))
                .thenReturn(new SeasonResponse(10L, 1L, "2025-26 (revised)", 18, 1100, true, null, null));

        mockMvc.perform(put("/api/v1/leagues/1/seasons/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"period\":\"2025-26 (revised)\",\"teamLimit\":18,\"startingBudget\":1100,\"doubleLeg\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.period").value("2025-26 (revised)"))
                .andExpect(jsonPath("$.teamLimit").value(18));
    }

    @Test
    void updateUnknownIdReturns404() throws Exception {
        when(seasonService.update(eq(1L), eq(99L), any())).thenThrow(new NotFoundException("No season with id 99"));

        mockMvc.perform(put("/api/v1/leagues/1/seasons/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"period\":\"2025-26\",\"teamLimit\":20,\"startingBudget\":1000}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateTeamLimitBelowEntrantsReturns409() throws Exception {
        when(seasonService.update(eq(1L), eq(10L), any()))
                .thenThrow(new ConflictException("team_limit (10) can't be less than the current number of entered teams (15)"));

        mockMvc.perform(put("/api/v1/leagues/1/seasons/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"period\":\"2025-26\",\"teamLimit\":10,\"startingBudget\":1000}"))
                .andExpect(status().isConflict());
    }

    @Test
    void deleteSuccessReturns204() throws Exception {
        mockMvc.perform(delete("/api/v1/leagues/1/seasons/10"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteUnknownIdReturns404() throws Exception {
        doThrow(new NotFoundException("No season with id 99")).when(seasonService).delete(1L, 99L);

        mockMvc.perform(delete("/api/v1/leagues/1/seasons/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteSeasonInUseReturns409() throws Exception {
        doThrow(new ConflictException("Season '2025-26' has teams entered and can't be deleted"))
                .when(seasonService).delete(1L, 10L);

        mockMvc.perform(delete("/api/v1/leagues/1/seasons/10"))
                .andExpect(status().isConflict());
    }

    @Test
    void addEntrantWithMissingTeamIdReturnsProblemDetail() throws Exception {
        mockMvc.perform(post("/api/v1/leagues/1/seasons/10/entrants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("teamId"));
    }

    @Test
    void addEntrantSuccessReturns201() throws Exception {
        when(seasonService.addEntrant(eq(1L), eq(10L), any())).thenReturn(new SeasonEntrantResponse(100L, 10L, 5L));

        mockMvc.perform(post("/api/v1/leagues/1/seasons/10/entrants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"teamId\":5}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.teamId").value(5));
    }

    @Test
    void addEntrantConflictReturns409() throws Exception {
        when(seasonService.addEntrant(eq(1L), eq(10L), any()))
                .thenThrow(new ConflictException("Team 5 is already entered in this season"));

        mockMvc.perform(post("/api/v1/leagues/1/seasons/10/entrants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"teamId\":5}"))
                .andExpect(status().isConflict());
    }

    @Test
    void removeEntrantSuccessReturns204() throws Exception {
        mockMvc.perform(delete("/api/v1/leagues/1/seasons/10/entrants/5"))
                .andExpect(status().isNoContent());
    }

    @Test
    void removeEntrantNotEnteredReturns404() throws Exception {
        doThrow(new NotFoundException("Team 5 is not entered in season 10")).when(seasonService).removeEntrant(1L, 10L, 5L);

        mockMvc.perform(delete("/api/v1/leagues/1/seasons/10/entrants/5"))
                .andExpect(status().isNotFound());
    }

    @Test
    void removeEntrantOnceFixturesExistReturns409() throws Exception {
        doThrow(new ConflictException("Season 10 already has fixtures generated -- teams can't be removed now"))
                .when(seasonService).removeEntrant(1L, 10L, 5L);

        mockMvc.perform(delete("/api/v1/leagues/1/seasons/10/entrants/5"))
                .andExpect(status().isConflict());
    }

    @Test
    void generateEntrantsSuccessReturns201WithGeneratedTeams() throws Exception {
        when(seasonService.generateEntrants(1L, 10L))
                .thenReturn(List.of(new GeneratedEntrantResponse(200L, 100L, "North United")));

        mockMvc.perform(post("/api/v1/leagues/1/seasons/10/entrants/generate"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].teamName").value("North United"));
    }

    @Test
    void generateEntrantsUnknownSeasonReturns404() throws Exception {
        when(seasonService.generateEntrants(1L, 99L)).thenThrow(new NotFoundException("No season with id 99"));

        mockMvc.perform(post("/api/v1/leagues/1/seasons/99/entrants/generate"))
                .andExpect(status().isNotFound());
    }
}
