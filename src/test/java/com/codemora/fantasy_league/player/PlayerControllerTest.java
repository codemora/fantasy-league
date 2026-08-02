package com.codemora.fantasy_league.player;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import com.codemora.fantasy_league.common.error.ConflictException;
import com.codemora.fantasy_league.common.error.NotFoundException;
import com.codemora.fantasy_league.player.dto.PlayerProfileResponse;
import com.codemora.fantasy_league.player.dto.PlayerResponse;

/**
 * addFilters = false: see TeamControllerTest -- same rationale.
 */
@WebMvcTest(PlayerController.class)
@AutoConfigureMockMvc(addFilters = false)
class PlayerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PlayerService playerService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void generateSuccessReturns201() throws Exception {
        when(playerService.generateSquad(eq(1L))).thenReturn(List.of(
                new PlayerResponse(500L, 1L, "Bruno Silva", Position.MID, 65)));

        mockMvc.perform(post("/api/v1/teams/1/players/generate"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].name").value("Bruno Silva"));
    }

    @Test
    void generateUnknownTeamReturns404() throws Exception {
        when(playerService.generateSquad(eq(99L))).thenThrow(new NotFoundException("No team with id 99"));

        mockMvc.perform(post("/api/v1/teams/99/players/generate"))
                .andExpect(status().isNotFound());
    }

    @Test
    void generateAlreadyHasPlayersReturns409() throws Exception {
        when(playerService.generateSquad(eq(1L))).thenThrow(new ConflictException("Team 1 already has players generated"));

        mockMvc.perform(post("/api/v1/teams/1/players/generate"))
                .andExpect(status().isConflict());
    }

    @Test
    void findByTeamSuccessReturns200() throws Exception {
        when(playerService.findByTeam(eq(1L))).thenReturn(List.of(
                new PlayerResponse(500L, 1L, "Bruno Silva", Position.MID, 65)));

        mockMvc.perform(get("/api/v1/teams/1/players"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].position").value("MID"));
    }

    @Test
    void findByTeamUnknownTeamReturns404() throws Exception {
        when(playerService.findByTeam(eq(99L))).thenThrow(new NotFoundException("No team with id 99"));

        mockMvc.perform(get("/api/v1/teams/99/players"))
                .andExpect(status().isNotFound());
    }

    @Test
    void findProfileSuccessReturns200() throws Exception {
        when(playerService.findProfile(eq(500L))).thenReturn(
                new PlayerProfileResponse(500L, 1L, "Arsenal", "Bruno Silva", Position.MID, 65, 10, 3, 4, 5));

        mockMvc.perform(get("/api/v1/players/500"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teamName").value("Arsenal"))
                .andExpect(jsonPath("$.goals").value(3))
                .andExpect(jsonPath("$.appearances").value(10));
    }

    @Test
    void findProfileUnknownPlayerReturns404() throws Exception {
        when(playerService.findProfile(eq(999L))).thenThrow(new NotFoundException("No player with id 999"));

        mockMvc.perform(get("/api/v1/players/999"))
                .andExpect(status().isNotFound());
    }
}
