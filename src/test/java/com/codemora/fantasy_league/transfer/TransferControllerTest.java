package com.codemora.fantasy_league.transfer;

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
import com.codemora.fantasy_league.transfer.dto.MakeTransferResponse;
import com.codemora.fantasy_league.transfer.dto.TransferResponse;

/**
 * addFilters = false: see TeamControllerTest -- same rationale.
 */
@WebMvcTest(TransferController.class)
@AutoConfigureMockMvc(addFilters = false)
class TransferControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TransferService transferService;

    @MockitoBean
    private JwtService jwtService;

    private TransferResponse transferResponse(int pointsCost) {
        return new TransferResponse(900L, 21L, 1, 1L, "Player1", 9L, "Player9",
                Position.MID, pointsCost, LocalDateTime.of(2025, 8, 1, 10, 0));
    }

    @Test
    void makeTransferSuccessReturns201() throws Exception {
        when(transferService.makeTransfer(eq(1L), eq(10L), eq(21L), any()))
                .thenReturn(new MakeTransferResponse(transferResponse(0), 90, 0));

        mockMvc.perform(post("/api/v1/leagues/1/seasons/10/gameweeks/21/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"playerOutId\":1,\"playerInId\":9}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.bankBalance").value(90))
                .andExpect(jsonPath("$.transfer.pointsCost").value(0))
                .andExpect(jsonPath("$.transfer.playerInName").value("Player9"));
    }

    @Test
    void makeTransferMissingPlayerIdReturnsProblemDetail() throws Exception {
        mockMvc.perform(post("/api/v1/leagues/1/seasons/10/gameweeks/21/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"playerOutId\":1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("playerInId"));
    }

    @Test
    void makeTransferOverBudgetReturns409() throws Exception {
        when(transferService.makeTransfer(eq(1L), eq(10L), eq(21L), any()))
                .thenThrow(new ConflictException("Not enough funds"));

        mockMvc.perform(post("/api/v1/leagues/1/seasons/10/gameweeks/21/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"playerOutId\":1,\"playerInId\":9}"))
                .andExpect(status().isConflict());
    }

    @Test
    void makeTransferWithoutASquadReturns404() throws Exception {
        when(transferService.makeTransfer(eq(1L), eq(10L), eq(21L), any()))
                .thenThrow(new NotFoundException("You don't have a fantasy squad for this season"));

        mockMvc.perform(post("/api/v1/leagues/1/seasons/10/gameweeks/21/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"playerOutId\":1,\"playerInId\":9}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void findHistorySuccessReturns200() throws Exception {
        when(transferService.findHistory(eq(1L), eq(10L))).thenReturn(List.of(transferResponse(4)));

        mockMvc.perform(get("/api/v1/leagues/1/seasons/10/transfers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].gameweekNumber").value(1))
                .andExpect(jsonPath("$[0].pointsCost").value(4));
    }

    @Test
    void findHistoryWithNoTransfersReturnsEmptyList() throws Exception {
        when(transferService.findHistory(eq(1L), eq(10L))).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/leagues/1/seasons/10/transfers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }
}
