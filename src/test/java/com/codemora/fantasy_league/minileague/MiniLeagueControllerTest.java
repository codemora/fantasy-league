package com.codemora.fantasy_league.minileague;

import static org.mockito.ArgumentMatchers.any;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.codemora.fantasy_league.auth.JwtService;
import com.codemora.fantasy_league.common.error.ConflictException;
import com.codemora.fantasy_league.common.error.NotFoundException;
import com.codemora.fantasy_league.leaderboard.dto.LeaderboardRowResponse;
import com.codemora.fantasy_league.minileague.dto.MiniLeagueResponse;

/**
 * addFilters = false: see TeamControllerTest -- same rationale.
 */
@WebMvcTest(MiniLeagueController.class)
@AutoConfigureMockMvc(addFilters = false)
class MiniLeagueControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MiniLeagueService miniLeagueService;

    @MockitoBean
    private JwtService jwtService;

    private MiniLeagueResponse response() {
        return new MiniLeagueResponse(700L, 10L, "Office League", "ABCD2345", 1, LocalDateTime.of(2025, 8, 1, 10, 0));
    }

    @Test
    void createSuccessReturns201() throws Exception {
        when(miniLeagueService.create(eq(1L), eq(10L), any())).thenReturn(response());

        mockMvc.perform(post("/api/v1/leagues/1/seasons/10/mini-leagues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Office League\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.inviteCode").value("ABCD2345"))
                .andExpect(jsonPath("$.memberCount").value(1));
    }

    @Test
    void createReturns409WhenCallerHasNoSquad() throws Exception {
        when(miniLeagueService.create(eq(1L), eq(10L), any()))
                .thenThrow(new ConflictException("You need a fantasy squad for this season before joining a mini-league"));

        mockMvc.perform(post("/api/v1/leagues/1/seasons/10/mini-leagues")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Office League\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void findMineReturnsTheCallersMiniLeagues() throws Exception {
        when(miniLeagueService.findMine(1L, 10L)).thenReturn(List.of(response()));

        mockMvc.perform(get("/api/v1/leagues/1/seasons/10/mini-leagues"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Office League"));
    }

    @Test
    void leaderboardReturnsTheRankedRows() throws Exception {
        when(miniLeagueService.leaderboard(1L, 10L, 700L)).thenReturn(List.of(
                new LeaderboardRowResponse(1, 500L, 7L, "alice", 80, 4)));

        mockMvc.perform(get("/api/v1/leagues/1/seasons/10/mini-leagues/700/leaderboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("alice"))
                .andExpect(jsonPath("$[0].totalPoints").value(80));
    }

    @Test
    void leaderboardReturns404WhenCallerIsNotAMember() throws Exception {
        when(miniLeagueService.leaderboard(1L, 10L, 700L))
                .thenThrow(new NotFoundException("No mini-league with id 700"));

        mockMvc.perform(get("/api/v1/leagues/1/seasons/10/mini-leagues/700/leaderboard"))
                .andExpect(status().isNotFound());
    }

    @Test
    void joinSuccessReturns201() throws Exception {
        when(miniLeagueService.join("ABCD2345")).thenReturn(response());

        mockMvc.perform(post("/api/v1/mini-leagues/ABCD2345/join"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Office League"));
    }

    @Test
    void joinReturns404ForAnUnknownInviteCode() throws Exception {
        when(miniLeagueService.join("NOPE0000")).thenThrow(new NotFoundException("No mini-league with invite code NOPE0000"));

        mockMvc.perform(post("/api/v1/mini-leagues/NOPE0000/join"))
                .andExpect(status().isNotFound());
    }
}
