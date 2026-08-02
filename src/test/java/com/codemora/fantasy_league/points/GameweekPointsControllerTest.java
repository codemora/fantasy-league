package com.codemora.fantasy_league.points;

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
import com.codemora.fantasy_league.common.Position;
import com.codemora.fantasy_league.common.error.NotFoundException;
import com.codemora.fantasy_league.lineup.LineupRole;
import com.codemora.fantasy_league.points.dto.GameweekPointsResponse;
import com.codemora.fantasy_league.points.dto.PlayerPointsResponse;

/**
 * addFilters = false: see TeamControllerTest -- same rationale.
 */
@WebMvcTest(GameweekPointsController.class)
@AutoConfigureMockMvc(addFilters = false)
class GameweekPointsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GameweekPointsService gameweekPointsService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void findPointsSuccessReturns200() throws Exception {
        when(gameweekPointsService.findPoints(eq(1L), eq(10L), eq(20L))).thenReturn(new GameweekPointsResponse(
                20L, 3, List.of(new PlayerPointsResponse(1L, "Keeper", Position.GK, LineupRole.STARTER, true, 16, 32)),
                32, 4, 28));

        mockMvc.perform(get("/api/v1/leagues/1/seasons/10/gameweeks/20/points"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.playerPoints").value(32))
                .andExpect(jsonPath("$.transferPointsCost").value(4))
                .andExpect(jsonPath("$.totalPoints").value(28))
                .andExpect(jsonPath("$.players[0].captain").value(true))
                .andExpect(jsonPath("$.players[0].points").value(32));
    }

    @Test
    void findPointsNoLineupReturns404() throws Exception {
        when(gameweekPointsService.findPoints(eq(1L), eq(10L), eq(20L)))
                .thenThrow(new NotFoundException("You haven't submitted a lineup for this gameweek"));

        mockMvc.perform(get("/api/v1/leagues/1/seasons/10/gameweeks/20/points"))
                .andExpect(status().isNotFound());
    }
}
