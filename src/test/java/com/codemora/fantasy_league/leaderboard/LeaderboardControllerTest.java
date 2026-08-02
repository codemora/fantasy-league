package com.codemora.fantasy_league.leaderboard;

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
import com.codemora.fantasy_league.leaderboard.dto.LeaderboardRowResponse;

/**
 * addFilters = false: see TeamControllerTest -- same rationale.
 */
@WebMvcTest(LeaderboardController.class)
@AutoConfigureMockMvc(addFilters = false)
class LeaderboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LeaderboardService leaderboardService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void findBySeasonSuccessReturns200() throws Exception {
        when(leaderboardService.findBySeason(eq(1L), eq(10L))).thenReturn(List.of(
                new LeaderboardRowResponse(1, 100L, 1L, "alice", 50, 0),
                new LeaderboardRowResponse(2, 200L, 2L, "bob", 40, 4),
                new LeaderboardRowResponse(2, 300L, 3L, "carol", 40, 0)));

        mockMvc.perform(get("/api/v1/leagues/1/seasons/10/leaderboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("alice"))
                .andExpect(jsonPath("$[0].totalPoints").value(50))
                .andExpect(jsonPath("$[1].rank").value(2))
                .andExpect(jsonPath("$[1].transferPointsCost").value(4))
                .andExpect(jsonPath("$[2].rank").value(2));
    }

    @Test
    void findBySeasonWithNoSquadsReturnsEmptyList() throws Exception {
        when(leaderboardService.findBySeason(eq(1L), eq(10L))).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/leagues/1/seasons/10/leaderboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void findBySeasonUnknownSeasonReturns404() throws Exception {
        when(leaderboardService.findBySeason(eq(1L), eq(99L)))
                .thenThrow(new NotFoundException("No season with id 99"));

        mockMvc.perform(get("/api/v1/leagues/1/seasons/99/leaderboard"))
                .andExpect(status().isNotFound());
    }
}
