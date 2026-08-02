package com.codemora.fantasy_league.scoringrule;

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
import com.codemora.fantasy_league.common.error.NotFoundException;
import com.codemora.fantasy_league.scoringrule.dto.ScoringRuleResponse;

/**
 * addFilters = false: see TeamControllerTest -- same rationale.
 */
@WebMvcTest(ScoringRuleController.class)
@AutoConfigureMockMvc(addFilters = false)
class ScoringRuleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ScoringRuleService scoringRuleService;

    @MockitoBean
    private JwtService jwtService;

    private ScoringRuleResponse gkResponse() {
        return new ScoringRuleResponse(500L, 10L, Position.GK, 10, 3, 4, 2, 1, -1, 5, -2, -1, -3, -2);
    }

    @Test
    void findBySeasonSuccessReturns200() throws Exception {
        when(scoringRuleService.findBySeason(eq(1L), eq(10L))).thenReturn(List.of(gkResponse()));

        mockMvc.perform(get("/api/v1/leagues/1/seasons/10/scoring-rules"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].position").value("GK"))
                .andExpect(jsonPath("$[0].pointsPerGoal").value(10));
    }

    @Test
    void findBySeasonUnknownSeasonReturns404() throws Exception {
        when(scoringRuleService.findBySeason(eq(1L), eq(99L))).thenThrow(new NotFoundException("No season with id 99"));

        mockMvc.perform(get("/api/v1/leagues/1/seasons/99/scoring-rules"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateSuccessReturns200() throws Exception {
        when(scoringRuleService.update(eq(1L), eq(10L), eq(Position.GK), any())).thenReturn(gkResponse());

        mockMvc.perform(put("/api/v1/leagues/1/seasons/10/scoring-rules/GK")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pointsPerGoal\":10,\"pointsPerAssist\":3,\"pointsPerCleanSheet\":4,"
                                + "\"pointsPerAppearance60\":2,\"pointsPerAppearance1to59\":1,"
                                + "\"pointsPerGoalsConcededPerThree\":-1,\"pointsPerPenaltySave\":5,"
                                + "\"pointsPerPenaltyMiss\":-2,\"pointsPerYellowCard\":-1,"
                                + "\"pointsPerRedCard\":-3,\"pointsPerOwnGoal\":-2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pointsPerGoal").value(10));
    }

    @Test
    void updateUnknownRuleReturns404() throws Exception {
        when(scoringRuleService.update(eq(1L), eq(10L), eq(Position.GK), any()))
                .thenThrow(new NotFoundException("No scoring rule for position GK in season 10"));

        mockMvc.perform(put("/api/v1/leagues/1/seasons/10/scoring-rules/GK")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pointsPerGoal\":10,\"pointsPerAssist\":3,\"pointsPerCleanSheet\":4,"
                                + "\"pointsPerAppearance60\":2,\"pointsPerAppearance1to59\":1,"
                                + "\"pointsPerGoalsConcededPerThree\":-1,\"pointsPerPenaltySave\":5,"
                                + "\"pointsPerPenaltyMiss\":-2,\"pointsPerYellowCard\":-1,"
                                + "\"pointsPerRedCard\":-3,\"pointsPerOwnGoal\":-2}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateWithMissingFieldReturnsProblemDetail() throws Exception {
        mockMvc.perform(put("/api/v1/leagues/1/seasons/10/scoring-rules/GK")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pointsPerAssist\":3,\"pointsPerCleanSheet\":4,"
                                + "\"pointsPerAppearance60\":2,\"pointsPerAppearance1to59\":1,"
                                + "\"pointsPerGoalsConcededPerThree\":-1,\"pointsPerPenaltySave\":5,"
                                + "\"pointsPerPenaltyMiss\":-2,\"pointsPerYellowCard\":-1,"
                                + "\"pointsPerRedCard\":-3,\"pointsPerOwnGoal\":-2}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("pointsPerGoal"));
    }
}
