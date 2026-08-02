package com.codemora.fantasy_league.team;

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

import com.codemora.fantasy_league.auth.JwtService;
import com.codemora.fantasy_league.common.error.ConflictException;
import com.codemora.fantasy_league.common.error.NotFoundException;
import com.codemora.fantasy_league.team.dto.TeamResponse;

/**
 * addFilters = false: this slice verifies request validation and response shape,
 * not @PreAuthorize role enforcement -- that needs the full SecurityConfig +
 * @EnableMethodSecurity wiring, which belongs in a full integration test (ADR 0011),
 * not a @WebMvcTest slice.
 */
@WebMvcTest(TeamController.class)
@AutoConfigureMockMvc(addFilters = false)
class TeamControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TeamService teamService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    void createWithBlankNameReturnsProblemDetail() throws Exception {
        mockMvc.perform(post("/api/v1/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("name"));
    }

    @Test
    void createSuccessReturns201() throws Exception {
        when(teamService.create(any())).thenReturn(new TeamResponse(1L, "Arsenal", "Victoria Concordia Crescit"));

        mockMvc.perform(post("/api/v1/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Arsenal\",\"slogan\":\"Victoria Concordia Crescit\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Arsenal"));
    }

    @Test
    void createDuplicateNameReturns409() throws Exception {
        when(teamService.create(any())).thenThrow(new ConflictException("A team named 'Arsenal' already exists"));

        mockMvc.perform(post("/api/v1/teams")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Arsenal\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void updateSuccessReturns200() throws Exception {
        when(teamService.update(eq(1L), any())).thenReturn(new TeamResponse(1L, "Arsenal FC", "New"));

        mockMvc.perform(put("/api/v1/teams/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Arsenal FC\",\"slogan\":\"New\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Arsenal FC"));
    }

    @Test
    void updateUnknownIdReturns404() throws Exception {
        when(teamService.update(eq(99L), any())).thenThrow(new NotFoundException("No team with id 99"));

        mockMvc.perform(put("/api/v1/teams/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Arsenal\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateWithBlankNameReturnsProblemDetail() throws Exception {
        mockMvc.perform(put("/api/v1/teams/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("name"));
    }

    @Test
    void deleteSuccessReturns204() throws Exception {
        mockMvc.perform(delete("/api/v1/teams/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void deleteUnknownIdReturns404() throws Exception {
        doThrow(new NotFoundException("No team with id 99")).when(teamService).delete(99L);

        mockMvc.perform(delete("/api/v1/teams/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteTeamInUseReturns409() throws Exception {
        doThrow(new ConflictException("Team 'Arsenal' has players and can't be deleted")).when(teamService).delete(1L);

        mockMvc.perform(delete("/api/v1/teams/1"))
                .andExpect(status().isConflict());
    }
}
