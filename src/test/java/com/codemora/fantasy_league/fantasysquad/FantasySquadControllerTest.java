package com.codemora.fantasy_league.fantasysquad;

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
import com.codemora.fantasy_league.common.Position;
import com.codemora.fantasy_league.common.error.ConflictException;
import com.codemora.fantasy_league.common.error.NotFoundException;
import com.codemora.fantasy_league.fantasysquad.dto.FantasySquadResponse;
import com.codemora.fantasy_league.fantasysquad.dto.SquadPlayerResponse;

/**
 * addFilters = false: see TeamControllerTest -- same rationale.
 */
@WebMvcTest(FantasySquadController.class)
@AutoConfigureMockMvc(addFilters = false)
class FantasySquadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FantasySquadService fantasySquadService;

    @MockitoBean
    private JwtService jwtService;

    private FantasySquadResponse squadResponse() {
        return new FantasySquadResponse(500L, 7L, 10L, 100, 1, List.of(
                new SquadPlayerResponse(1L, "Bruno Silva", Position.MID, 3L, 60, LocalDateTime.now())));
    }

    private String playerIdsJson() {
        StringBuilder sb = new StringBuilder("{\"playerIds\":[");
        for (int i = 1; i <= 15; i++) {
            sb.append(i);
            if (i < 15) sb.append(",");
        }
        return sb.append("]}").toString();
    }

    @Test
    void createSuccessReturns201() throws Exception {
        when(fantasySquadService.create(eq(1L), eq(10L), any())).thenReturn(squadResponse());

        mockMvc.perform(post("/api/v1/leagues/1/seasons/10/squad")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(playerIdsJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(500))
                .andExpect(jsonPath("$.bankBalance").value(100));
    }

    @Test
    void createWithWrongPlayerCountReturnsProblemDetail() throws Exception {
        mockMvc.perform(post("/api/v1/leagues/1/seasons/10/squad")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"playerIds\":[1,2,3]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("playerIds"));
    }

    @Test
    void createAlreadyHasSquadReturns409() throws Exception {
        when(fantasySquadService.create(eq(1L), eq(10L), any()))
                .thenThrow(new ConflictException("You already have a fantasy squad for this season"));

        mockMvc.perform(post("/api/v1/leagues/1/seasons/10/squad")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(playerIdsJson()))
                .andExpect(status().isConflict());
    }

    @Test
    void createUnknownSeasonReturns404() throws Exception {
        when(fantasySquadService.create(eq(1L), eq(99L), any()))
                .thenThrow(new NotFoundException("No season with id 99"));

        mockMvc.perform(post("/api/v1/leagues/1/seasons/99/squad")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(playerIdsJson()))
                .andExpect(status().isNotFound());
    }

    @Test
    void findMineSuccessReturns200() throws Exception {
        when(fantasySquadService.findMine(eq(1L), eq(10L))).thenReturn(squadResponse());

        mockMvc.perform(get("/api/v1/leagues/1/seasons/10/squad"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.freeTransfers").value(1))
                .andExpect(jsonPath("$.players[0].playerName").value("Bruno Silva"));
    }

    @Test
    void findMineNoSquadReturns404() throws Exception {
        when(fantasySquadService.findMine(eq(1L), eq(10L)))
                .thenThrow(new NotFoundException("You don't have a fantasy squad for this season"));

        mockMvc.perform(get("/api/v1/leagues/1/seasons/10/squad"))
                .andExpect(status().isNotFound());
    }
}
