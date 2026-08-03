package com.codemora.fantasy_league.chip;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.codemora.fantasy_league.chip.dto.ChipResponse;
import com.codemora.fantasy_league.common.error.ConflictException;
import com.codemora.fantasy_league.common.error.NotFoundException;
import com.codemora.fantasy_league.config.CurrentUserProvider;
import com.codemora.fantasy_league.fantasysquad.FantasySquad;
import com.codemora.fantasy_league.fantasysquad.FantasySquadRepository;
import com.codemora.fantasy_league.gameweek.Gameweek;
import com.codemora.fantasy_league.gameweek.GameweekDeadlineGuard;
import com.codemora.fantasy_league.gameweek.GameweekRepository;
import com.codemora.fantasy_league.gameweek.GameweekStatus;
import com.codemora.fantasy_league.season.Season;
import com.codemora.fantasy_league.season.SeasonRepository;

@ExtendWith(MockitoExtension.class)
class ChipServiceTest {

    @Mock
    private SeasonRepository seasonRepository;
    @Mock
    private GameweekRepository gameweekRepository;
    @Mock
    private FantasySquadRepository fantasySquadRepository;
    @Mock
    private SquadChipRepository squadChipRepository;
    @Mock
    private CurrentUserProvider currentUserProvider;

    private static final Long SQUAD_ID = 500L;
    private static final Long GW1 = 21L;

    /** Real guard rather than a mock: it's a pure component, so exercise the actual rule. */
    private ChipService service() {
        return new ChipService(seasonRepository, gameweekRepository, fantasySquadRepository,
                squadChipRepository, new GameweekDeadlineGuard(), currentUserProvider);
    }

    private Season season() {
        return Season.builder().id(10L).leagueId(1L).period("2025-26").teamLimit(20).startingBudget(1000).build();
    }

    private Gameweek gameweek(Long id, int number) {
        return Gameweek.builder().id(id).seasonId(10L).number(number)
                .deadlineDateTime(LocalDateTime.now().plusDays(number)).status(GameweekStatus.UPCOMING).build();
    }

    private FantasySquad squad() {
        return FantasySquad.builder().id(SQUAD_ID).userId(7L).seasonId(10L)
                .bankBalance(1000).freeTransfers(1).build();
    }

    private void stubLookups() {
        when(seasonRepository.findById(10L)).thenReturn(Optional.of(season()));
        when(gameweekRepository.findById(GW1)).thenReturn(Optional.of(gameweek(GW1, 1)));
        when(currentUserProvider.getUserId()).thenReturn(7L);
        when(fantasySquadRepository.findByUserIdAndSeasonId(7L, 10L)).thenReturn(Optional.of(squad()));
    }

    @Test
    void activatesAChipAndReturnsItsGameweekNumber() {
        stubLookups();
        when(squadChipRepository.save(any(SquadChip.class))).thenAnswer(i -> {
            SquadChip c = i.getArgument(0);
            c.setId(900L);
            return c;
        });

        ChipResponse response = service().activate(1L, 10L, GW1, ChipType.WILDCARD);

        assertThat(response.chipType()).isEqualTo(ChipType.WILDCARD);
        assertThat(response.gameweekId()).isEqualTo(GW1);
        assertThat(response.gameweekNumber()).isEqualTo(1);
    }

    @Test
    void savesTheChipAgainstTheCallersSquadAndGameweek() {
        stubLookups();
        ArgumentCaptor<SquadChip> captor = ArgumentCaptor.forClass(SquadChip.class);
        when(squadChipRepository.save(captor.capture())).thenAnswer(i -> i.getArgument(0));

        service().activate(1L, 10L, GW1, ChipType.TRIPLE_CAPTAIN);

        assertThat(captor.getValue().getSquadId()).isEqualTo(SQUAD_ID);
        assertThat(captor.getValue().getGameweekId()).isEqualTo(GW1);
        assertThat(captor.getValue().getChipType()).isEqualTo(ChipType.TRIPLE_CAPTAIN);
    }

    @Test
    void rejectsASecondChipInTheSameGameweek() {
        stubLookups();
        when(squadChipRepository.existsBySquadIdAndGameweekId(SQUAD_ID, GW1)).thenReturn(true);

        assertThatThrownBy(() -> service().activate(1L, 10L, GW1, ChipType.BENCH_BOOST))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("only one chip can be active per gameweek");
    }

    @Test
    void rejectsReusingAChipTypeAlreadyPlayedThisSeason() {
        stubLookups();
        when(squadChipRepository.existsBySquadIdAndChipType(SQUAD_ID, ChipType.WILDCARD)).thenReturn(true);

        assertThatThrownBy(() -> service().activate(1L, 10L, GW1, ChipType.WILDCARD))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already used your WILDCARD chip");
    }

    @Test
    void rejectsActivatingAChipOnceTheDeadlineHasPassed() {
        when(seasonRepository.findById(10L)).thenReturn(Optional.of(season()));
        when(gameweekRepository.findById(GW1)).thenReturn(Optional.of(
                Gameweek.builder().id(GW1).seasonId(10L).number(1)
                        .deadlineDateTime(LocalDateTime.now().minusMinutes(1))
                        .status(GameweekStatus.UPCOMING).build()));

        assertThatThrownBy(() -> service().activate(1L, 10L, GW1, ChipType.WILDCARD))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("play a chip");
    }

    @Test
    void rejectsWhenUserHasNoSquad() {
        when(seasonRepository.findById(10L)).thenReturn(Optional.of(season()));
        when(gameweekRepository.findById(GW1)).thenReturn(Optional.of(gameweek(GW1, 1)));
        when(currentUserProvider.getUserId()).thenReturn(7L);
        when(fantasySquadRepository.findByUserIdAndSeasonId(7L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().activate(1L, 10L, GW1, ChipType.WILDCARD))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void rejectsGameweekFromAnotherSeason() {
        when(seasonRepository.findById(10L)).thenReturn(Optional.of(season()));
        when(gameweekRepository.findById(GW1)).thenReturn(Optional.of(
                Gameweek.builder().id(GW1).seasonId(99L).number(1)
                        .deadlineDateTime(LocalDateTime.now()).status(GameweekStatus.UPCOMING).build()));

        assertThatThrownBy(() -> service().activate(1L, 10L, GW1, ChipType.WILDCARD))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void findHistoryReturnsChipsOldestFirstWithGameweekNumbers() {
        when(seasonRepository.findById(10L)).thenReturn(Optional.of(season()));
        when(currentUserProvider.getUserId()).thenReturn(7L);
        when(fantasySquadRepository.findByUserIdAndSeasonId(7L, 10L)).thenReturn(Optional.of(squad()));
        when(squadChipRepository.findBySquadId(SQUAD_ID)).thenReturn(List.of(
                SquadChip.builder().id(901L).squadId(SQUAD_ID).gameweekId(22L).chipType(ChipType.BENCH_BOOST)
                        .activatedAt(LocalDateTime.of(2025, 8, 8, 10, 0)).build(),
                SquadChip.builder().id(900L).squadId(SQUAD_ID).gameweekId(GW1).chipType(ChipType.WILDCARD)
                        .activatedAt(LocalDateTime.of(2025, 8, 1, 10, 0)).build()));
        when(gameweekRepository.findBySeasonIdOrderByNumber(10L))
                .thenReturn(List.of(gameweek(GW1, 1), gameweek(22L, 2)));

        List<ChipResponse> history = service().findHistory(1L, 10L);

        assertThat(history).hasSize(2);
        assertThat(history.get(0).chipType()).isEqualTo(ChipType.WILDCARD);
        assertThat(history.get(0).gameweekNumber()).isEqualTo(1);
        assertThat(history.get(1).chipType()).isEqualTo(ChipType.BENCH_BOOST);
        assertThat(history.get(1).gameweekNumber()).isEqualTo(2);
    }

    @Test
    void findHistoryReturnsEmptyWhenNoChipsPlayed() {
        when(seasonRepository.findById(10L)).thenReturn(Optional.of(season()));
        when(currentUserProvider.getUserId()).thenReturn(7L);
        when(fantasySquadRepository.findByUserIdAndSeasonId(7L, 10L)).thenReturn(Optional.of(squad()));
        when(squadChipRepository.findBySquadId(SQUAD_ID)).thenReturn(List.of());

        assertThat(service().findHistory(1L, 10L)).isEmpty();
    }
}
