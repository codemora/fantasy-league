package com.codemora.fantasy_league.transfer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.codemora.fantasy_league.common.Position;
import com.codemora.fantasy_league.common.error.ConflictException;
import com.codemora.fantasy_league.common.error.NotFoundException;
import com.codemora.fantasy_league.config.CurrentUserProvider;
import com.codemora.fantasy_league.fantasysquad.FantasySquad;
import com.codemora.fantasy_league.fantasysquad.FantasySquadRepository;
import com.codemora.fantasy_league.fantasysquad.SquadPlayer;
import com.codemora.fantasy_league.fantasysquad.SquadPlayerRepository;
import com.codemora.fantasy_league.gameweek.Gameweek;
import com.codemora.fantasy_league.gameweek.GameweekDeadlineGuard;
import com.codemora.fantasy_league.gameweek.GameweekRepository;
import com.codemora.fantasy_league.gameweek.GameweekStatus;
import com.codemora.fantasy_league.player.Player;
import com.codemora.fantasy_league.player.PlayerRepository;
import com.codemora.fantasy_league.season.Season;
import com.codemora.fantasy_league.season.SeasonRepository;
import com.codemora.fantasy_league.transfer.dto.MakeTransferRequest;
import com.codemora.fantasy_league.transfer.dto.MakeTransferResponse;
import com.codemora.fantasy_league.transfer.dto.TransferResponse;

@ExtendWith(MockitoExtension.class)
class TransferServiceTest {

    @Mock
    private SeasonRepository seasonRepository;
    @Mock
    private GameweekRepository gameweekRepository;
    @Mock
    private FantasySquadRepository fantasySquadRepository;
    @Mock
    private SquadPlayerRepository squadPlayerRepository;
    @Mock
    private PlayerRepository playerRepository;
    @Mock
    private TransferRepository transferRepository;
    @Mock
    private CurrentUserProvider currentUserProvider;

    private static final Long SQUAD_ID = 500L;
    private static final Long GW1 = 21L;
    private static final Long GW2 = 22L;
    private static final Long GW3 = 23L;

    /** Real guard rather than a mock: it's a pure component, so exercise the actual rule. */
    private TransferService service() {
        return new TransferService(seasonRepository, gameweekRepository, fantasySquadRepository,
                squadPlayerRepository, playerRepository, transferRepository, new GameweekDeadlineGuard(),
                currentUserProvider);
    }

    private Season season() {
        return Season.builder().id(10L).leagueId(1L).period("2025-26").teamLimit(20).startingBudget(1000).build();
    }

    /** Deadlines stay in the future or GameweekDeadlineGuard closes the gameweek (#36). */
    private Gameweek gameweek(Long id, int number) {
        return Gameweek.builder().id(id).seasonId(10L).number(number)
                .deadlineDateTime(LocalDateTime.now().plusDays(number)).status(GameweekStatus.UPCOMING).build();
    }

    private Player player(long id, long teamId, Position position, int marketValue) {
        return Player.builder().id(id).teamId(teamId).createdByUserId(7L)
                .name("Player" + id).position(position).marketValue(marketValue).build();
    }

    private SquadPlayer squadPlayer(long id, long playerId, int purchasePrice) {
        return SquadPlayer.builder().id(id).squadId(SQUAD_ID).playerId(playerId)
                .purchasePrice(purchasePrice).addedAt(LocalDateTime.now()).build();
    }

    private FantasySquad squad(int bankBalance) {
        return FantasySquad.builder().id(SQUAD_ID).userId(7L).seasonId(10L)
                .bankBalance(bankBalance).freeTransfers(1).build();
    }

    /** Season + gameweek + current user + squad: the lookups every makeTransfer call performs. */
    private void stubLookups(int bankBalance) {
        when(seasonRepository.findById(10L)).thenReturn(Optional.of(season()));
        when(gameweekRepository.findById(GW1)).thenReturn(Optional.of(gameweek(GW1, 1)));
        when(currentUserProvider.getUserId()).thenReturn(7L);
        when(fantasySquadRepository.findByUserIdAndSeasonId(7L, 10L)).thenReturn(Optional.of(squad(bankBalance)));
        // squad of 3 MIDs (ids 1-3) on teams 1, 1, 2
        when(squadPlayerRepository.findBySquadId(SQUAD_ID))
                .thenReturn(List.of(squadPlayer(101L, 1L, 60), squadPlayer(102L, 2L, 60), squadPlayer(103L, 3L, 60)));
    }

    /** Only reached once every validation passes. */
    private void stubPersistence(long transfersAlreadyMadeThisGameweek) {
        when(gameweekRepository.findBySeasonIdOrderByNumber(10L)).thenReturn(List.of(gameweek(GW1, 1)));
        when(transferRepository.countBySquadIdAndGameweekId(SQUAD_ID, GW1)).thenReturn(transfersAlreadyMadeThisGameweek);
        when(fantasySquadRepository.save(any(FantasySquad.class))).thenAnswer(i -> i.getArgument(0));
        when(transferRepository.save(any(Transfer.class))).thenAnswer(i -> {
            Transfer t = i.getArgument(0);
            t.setId(900L);
            return t;
        });
    }

    /** Player 1 (MID, team 1, bought for 60) out; player 9 (MID, team 3) in. */
    private void stubSwap(int incomingMarketValue) {
        when(playerRepository.findById(1L)).thenReturn(Optional.of(player(1L, 1L, Position.MID, 60)));
        when(playerRepository.findById(9L)).thenReturn(Optional.of(player(9L, 3L, Position.MID, incomingMarketValue)));
        when(playerRepository.findAllById(anyCollection())).thenReturn(List.of(
                player(2L, 1L, Position.MID, 60), player(3L, 2L, Position.MID, 60)));
    }

    @Test
    void firstTransferOfAGameweekIsFreeAndAdjustsTheBank() {
        stubLookups(100);
        stubSwap(70);
        stubPersistence(0);

        MakeTransferResponse response = service().makeTransfer(1L, 10L, GW1, new MakeTransferRequest(1L, 9L));

        assertThat(response.transfer().pointsCost()).isZero();
        // bank 100 + 60 (sold) - 70 (bought) = 90
        assertThat(response.bankBalance()).isEqualTo(90);
        assertThat(response.freeTransfersRemaining()).isZero();
    }

    @Test
    void secondTransferInTheSameGameweekCostsFourPoints() {
        stubLookups(100);
        stubSwap(70);
        stubPersistence(1); // the gameweek's free transfer is already spent

        MakeTransferResponse response = service().makeTransfer(1L, 10L, GW1, new MakeTransferRequest(1L, 9L));

        assertThat(response.transfer().pointsCost()).isEqualTo(4);
        assertThat(response.freeTransfersRemaining()).isZero();
    }

    @Test
    void freeTransfersAccrueOnePerGameweek() {
        when(gameweekRepository.findBySeasonIdOrderByNumber(10L))
                .thenReturn(List.of(gameweek(GW1, 1), gameweek(GW2, 2), gameweek(GW3, 3)));
        when(transferRepository.countBySquadIdAndGameweekId(anyLong(), anyLong())).thenReturn(0L);

        assertThat(service().freeTransfersAvailable(SQUAD_ID, 10L, gameweek(GW1, 1))).isEqualTo(1);
        assertThat(service().freeTransfersAvailable(SQUAD_ID, 10L, gameweek(GW2, 2))).isEqualTo(2);
    }

    @Test
    void bankedFreeTransfersAreCappedAtTwo() {
        when(gameweekRepository.findBySeasonIdOrderByNumber(10L))
                .thenReturn(List.of(gameweek(GW1, 1), gameweek(GW2, 2), gameweek(GW3, 3)));
        when(transferRepository.countBySquadIdAndGameweekId(anyLong(), anyLong())).thenReturn(0L);

        // three untouched gameweeks would be 3, but the cap holds it at 2
        assertThat(service().freeTransfersAvailable(SQUAD_ID, 10L, gameweek(GW3, 3))).isEqualTo(2);
    }

    @Test
    void transfersConsumeBankedFreeTransfers() {
        when(gameweekRepository.findBySeasonIdOrderByNumber(10L))
                .thenReturn(List.of(gameweek(GW1, 1), gameweek(GW2, 2), gameweek(GW3, 3)));
        when(transferRepository.countBySquadIdAndGameweekId(SQUAD_ID, GW1)).thenReturn(1L);
        when(transferRepository.countBySquadIdAndGameweekId(SQUAD_ID, GW2)).thenReturn(0L);
        when(transferRepository.countBySquadIdAndGameweekId(SQUAD_ID, GW3)).thenReturn(0L);

        // gw1: +1 then -1 = 0; gw2: +1 = 1; gw3: +1 = 2
        assertThat(service().freeTransfersAvailable(SQUAD_ID, 10L, gameweek(GW2, 2))).isEqualTo(1);
        assertThat(service().freeTransfersAvailable(SQUAD_ID, 10L, gameweek(GW3, 3))).isEqualTo(2);
    }

    @Test
    void overspendingInOneGameweekDoesNotCarryADebtForward() {
        when(gameweekRepository.findBySeasonIdOrderByNumber(10L))
                .thenReturn(List.of(gameweek(GW1, 1), gameweek(GW2, 2)));
        when(transferRepository.countBySquadIdAndGameweekId(SQUAD_ID, GW1)).thenReturn(5L); // 1 free + 4 paid
        when(transferRepository.countBySquadIdAndGameweekId(SQUAD_ID, GW2)).thenReturn(0L);

        // gw1 floors at 0 rather than -4, so gw2 still grants its normal 1
        assertThat(service().freeTransfersAvailable(SQUAD_ID, 10L, gameweek(GW2, 2))).isEqualTo(1);
    }

    @Test
    void rejectsCrossPositionSwap() {
        stubLookups(100);
        when(playerRepository.findById(1L)).thenReturn(Optional.of(player(1L, 1L, Position.MID, 60)));
        when(playerRepository.findById(9L)).thenReturn(Optional.of(player(9L, 3L, Position.FWD, 60)));

        assertThatThrownBy(() -> service().makeTransfer(1L, 10L, GW1, new MakeTransferRequest(1L, 9L)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("like-for-like");
        verify(transferRepository, never()).save(any());
    }

    @Test
    void rejectsTransferThatWouldGoOverBudget() {
        stubLookups(10);
        when(playerRepository.findById(1L)).thenReturn(Optional.of(player(1L, 1L, Position.MID, 60)));
        when(playerRepository.findById(9L)).thenReturn(Optional.of(player(9L, 3L, Position.MID, 200)));

        // bank 10 + 60 sold - 200 bought < 0
        assertThatThrownBy(() -> service().makeTransfer(1L, 10L, GW1, new MakeTransferRequest(1L, 9L)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Not enough funds");
        verify(transferRepository, never()).save(any());
    }

    @Test
    void rejectsTransferBreachingTheThreePerTeamCap() {
        stubLookups(100);
        when(playerRepository.findById(1L)).thenReturn(Optional.of(player(1L, 1L, Position.MID, 60)));
        when(playerRepository.findById(9L)).thenReturn(Optional.of(player(9L, 2L, Position.MID, 60)));
        // after removing player 1, the rest of the squad already has 3 from team 2
        when(playerRepository.findAllById(anyCollection())).thenReturn(List.of(
                player(2L, 2L, Position.MID, 60), player(3L, 2L, Position.MID, 60), player(4L, 2L, Position.MID, 60)));

        assertThatThrownBy(() -> service().makeTransfer(1L, 10L, GW1, new MakeTransferRequest(1L, 9L)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("more than 3 players from team");
    }

    @Test
    void rejectsTransferringOutAPlayerNotInTheSquad() {
        stubLookups(100);

        assertThatThrownBy(() -> service().makeTransfer(1L, 10L, GW1, new MakeTransferRequest(99L, 9L)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("isn't in your squad");
    }

    @Test
    void rejectsTransferringInAPlayerAlreadyInTheSquad() {
        stubLookups(100);

        assertThatThrownBy(() -> service().makeTransfer(1L, 10L, GW1, new MakeTransferRequest(1L, 2L)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already in your squad");
    }

    @Test
    void rejectsSwappingAPlayerForThemselves() {
        when(seasonRepository.findById(10L)).thenReturn(Optional.of(season()));
        when(gameweekRepository.findById(GW1)).thenReturn(Optional.of(gameweek(GW1, 1)));
        when(currentUserProvider.getUserId()).thenReturn(7L);
        when(fantasySquadRepository.findByUserIdAndSeasonId(7L, 10L)).thenReturn(Optional.of(squad(100)));

        assertThatThrownBy(() -> service().makeTransfer(1L, 10L, GW1, new MakeTransferRequest(1L, 1L)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void rejectsWhenUserHasNoSquad() {
        when(seasonRepository.findById(10L)).thenReturn(Optional.of(season()));
        when(gameweekRepository.findById(GW1)).thenReturn(Optional.of(gameweek(GW1, 1)));
        when(currentUserProvider.getUserId()).thenReturn(7L);
        when(fantasySquadRepository.findByUserIdAndSeasonId(7L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().makeTransfer(1L, 10L, GW1, new MakeTransferRequest(1L, 9L)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void rejectsTransferOnceTheDeadlineHasPassed() {
        when(seasonRepository.findById(10L)).thenReturn(Optional.of(season()));
        when(gameweekRepository.findById(GW1)).thenReturn(Optional.of(
                Gameweek.builder().id(GW1).seasonId(10L).number(1)
                        .deadlineDateTime(LocalDateTime.now().minusMinutes(1))
                        .status(GameweekStatus.UPCOMING).build()));

        assertThatThrownBy(() -> service().makeTransfer(1L, 10L, GW1, new MakeTransferRequest(1L, 9L)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("make a transfer");
        verify(transferRepository, never()).save(any());
    }

    @Test
    void rejectsTransferWhenTheGameweekIsInProgress() {
        when(seasonRepository.findById(10L)).thenReturn(Optional.of(season()));
        when(gameweekRepository.findById(GW1)).thenReturn(Optional.of(
                Gameweek.builder().id(GW1).seasonId(10L).number(1)
                        .deadlineDateTime(LocalDateTime.now().plusDays(1))
                        .status(GameweekStatus.IN_PROGRESS).build()));

        assertThatThrownBy(() -> service().makeTransfer(1L, 10L, GW1, new MakeTransferRequest(1L, 9L)))
                .isInstanceOf(ConflictException.class);
        verify(transferRepository, never()).save(any());
    }

    @Test
    void rejectsGameweekFromAnotherSeason() {
        when(seasonRepository.findById(10L)).thenReturn(Optional.of(season()));
        when(gameweekRepository.findById(GW1)).thenReturn(Optional.of(
                Gameweek.builder().id(GW1).seasonId(99L).number(1)
                        .deadlineDateTime(LocalDateTime.now()).status(GameweekStatus.UPCOMING).build()));

        assertThatThrownBy(() -> service().makeTransfer(1L, 10L, GW1, new MakeTransferRequest(1L, 9L)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void findHistoryReturnsTransfersOldestFirstWithGameweekAndPlayerNames() {
        when(seasonRepository.findById(10L)).thenReturn(Optional.of(season()));
        when(currentUserProvider.getUserId()).thenReturn(7L);
        when(fantasySquadRepository.findByUserIdAndSeasonId(7L, 10L)).thenReturn(Optional.of(squad(90)));
        when(transferRepository.findBySquadIdOrderByTimestamp(SQUAD_ID)).thenReturn(List.of(
                Transfer.builder().id(900L).squadId(SQUAD_ID).gameweekId(GW1).playerOutId(1L).playerInId(9L)
                        .pointsCost(0).timestamp(LocalDateTime.of(2025, 8, 1, 10, 0)).build(),
                Transfer.builder().id(901L).squadId(SQUAD_ID).gameweekId(GW2).playerOutId(9L).playerInId(1L)
                        .pointsCost(4).timestamp(LocalDateTime.of(2025, 8, 8, 10, 0)).build()));
        when(gameweekRepository.findBySeasonIdOrderByNumber(10L)).thenReturn(List.of(gameweek(GW1, 1), gameweek(GW2, 2)));
        when(playerRepository.findAllById(anyCollection())).thenReturn(List.of(
                player(1L, 1L, Position.MID, 60), player(9L, 3L, Position.MID, 70)));

        List<TransferResponse> history = service().findHistory(1L, 10L);

        assertThat(history).hasSize(2);
        assertThat(history.get(0).gameweekNumber()).isEqualTo(1);
        assertThat(history.get(0).playerOutName()).isEqualTo("Player1");
        assertThat(history.get(0).playerInName()).isEqualTo("Player9");
        assertThat(history.get(0).pointsCost()).isZero();
        assertThat(history.get(1).pointsCost()).isEqualTo(4);
    }

    @Test
    void findHistoryReturnsEmptyWhenNoTransfersMade() {
        when(seasonRepository.findById(10L)).thenReturn(Optional.of(season()));
        when(currentUserProvider.getUserId()).thenReturn(7L);
        when(fantasySquadRepository.findByUserIdAndSeasonId(7L, 10L)).thenReturn(Optional.of(squad(100)));
        when(transferRepository.findBySquadIdOrderByTimestamp(SQUAD_ID)).thenReturn(List.of());

        assertThat(service().findHistory(1L, 10L)).isEmpty();
    }
}
