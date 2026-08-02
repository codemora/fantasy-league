package com.codemora.fantasy_league.lineup;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
import com.codemora.fantasy_league.lineup.dto.GameweekLineupResponse;
import com.codemora.fantasy_league.lineup.dto.SubmitLineupRequest;
import com.codemora.fantasy_league.player.Player;
import com.codemora.fantasy_league.player.PlayerRepository;
import com.codemora.fantasy_league.season.Season;
import com.codemora.fantasy_league.season.SeasonRepository;

@ExtendWith(MockitoExtension.class)
class LineupServiceTest {

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
    private GameweekLineupRepository gameweekLineupRepository;
    @Mock
    private LineupSlotRepository lineupSlotRepository;
    @Mock
    private CurrentUserProvider currentUserProvider;

    /** Real guard rather than a mock: it's a pure component, so exercise the actual rule. */
    private LineupService lineupService() {
        return new LineupService(seasonRepository, gameweekRepository, fantasySquadRepository, squadPlayerRepository,
                playerRepository, gameweekLineupRepository, lineupSlotRepository, new GameweekDeadlineGuard(),
                currentUserProvider);
    }

    private Season season() {
        return Season.builder().id(10L).leagueId(1L).period("2025-26").teamLimit(20).startingBudget(1000).build();
    }

    /** Deadline must stay in the future or GameweekDeadlineGuard closes it (#36). */
    private Gameweek gameweek() {
        return Gameweek.builder().id(20L).seasonId(10L).number(1)
                .deadlineDateTime(LocalDateTime.now().plusDays(1)).status(GameweekStatus.UPCOMING).build();
    }

    private FantasySquad squad() {
        return FantasySquad.builder().id(500L).userId(7L).seasonId(10L).bankBalance(100).freeTransfers(1).build();
    }

    /** 2 GK / 5 DEF / 5 MID / 3 FWD across 5 teams, mirroring FantasySquadServiceTest's validSquad(). */
    private List<Player> squadOf15() {
        List<Player> players = new ArrayList<>();
        long id = 1;
        players.add(player(id++, 1L, Position.GK));
        players.add(player(id++, 1L, Position.DEF));
        players.add(player(id++, 1L, Position.DEF));
        players.add(player(id++, 2L, Position.GK));
        players.add(player(id++, 2L, Position.DEF));
        players.add(player(id++, 2L, Position.DEF));
        players.add(player(id++, 3L, Position.DEF));
        players.add(player(id++, 3L, Position.MID));
        players.add(player(id++, 3L, Position.MID));
        players.add(player(id++, 4L, Position.MID));
        players.add(player(id++, 4L, Position.MID));
        players.add(player(id++, 4L, Position.MID));
        players.add(player(id++, 5L, Position.FWD));
        players.add(player(id++, 5L, Position.FWD));
        players.add(player(id++, 5L, Position.FWD));
        return players;
    }

    private Player player(long id, long teamId, Position position) {
        return Player.builder().id(id).teamId(teamId).createdByUserId(7L).name("Player" + id).position(position).build();
    }

    /** 1 GK, 4 DEF, 3 MID, 3 FWD -- valid formation, ids 1,2,3,5,6,8,9,10,13,14,15. Bench: 4,7,11,12. */
    private List<Long> starterIds() {
        return List.of(1L, 2L, 3L, 5L, 6L, 8L, 9L, 10L, 13L, 14L, 15L);
    }

    private List<Long> benchIds() {
        return List.of(4L, 7L, 11L, 12L);
    }

    private void stubSeasonGameweekUserSquad() {
        when(seasonRepository.findById(10L)).thenReturn(Optional.of(season()));
        when(gameweekRepository.findById(20L)).thenReturn(Optional.of(gameweek()));
        when(currentUserProvider.getUserId()).thenReturn(7L);
        when(fantasySquadRepository.findByUserIdAndSeasonId(7L, 10L)).thenReturn(Optional.of(squad()));
    }

    private void stubSquadMembers() {
        List<Player> players = squadOf15();
        when(squadPlayerRepository.findBySquadId(500L)).thenReturn(players.stream()
                .map(p -> SquadPlayer.builder().squadId(500L).playerId(p.getId()).purchasePrice(60).addedAt(LocalDateTime.now()).build())
                .toList());
    }

    private void stubSaves() {
        when(gameweekLineupRepository.findBySquadIdAndGameweekId(500L, 20L)).thenReturn(Optional.empty());
        when(gameweekLineupRepository.save(any(GameweekLineup.class))).thenAnswer(invocation -> {
            GameweekLineup lineup = invocation.getArgument(0);
            lineup.setId(900L);
            return lineup;
        });
        when(lineupSlotRepository.save(any(LineupSlot.class))).thenAnswer(invocation -> {
            LineupSlot slot = invocation.getArgument(0);
            slot.setId(slot.getPlayerId() + 1000);
            return slot;
        });
    }

    @Test
    void submitSavesElevenStartersAndFourBenchWithCaptain() {
        stubSeasonGameweekUserSquad();
        stubSquadMembers();
        when(playerRepository.findAllById(anyCollection())).thenReturn(squadOf15());
        stubSaves();
        SubmitLineupRequest request = new SubmitLineupRequest(starterIds(), benchIds(), 1L);

        GameweekLineupResponse response = lineupService().submit(1L, 10L, 20L, request);

        assertThat(response.captainPlayerId()).isEqualTo(1L);
        assertThat(response.starters()).hasSize(11);
        assertThat(response.bench()).hasSize(4);
        verify(lineupSlotRepository).deleteByLineupId(900L);
    }

    @Test
    void submitReplacesAnExistingLineupForTheSameGameweek() {
        stubSeasonGameweekUserSquad();
        stubSquadMembers();
        when(playerRepository.findAllById(anyCollection())).thenReturn(squadOf15());
        GameweekLineup existing = GameweekLineup.builder().id(900L).squadId(500L).gameweekId(20L).captainPlayerId(2L).build();
        when(gameweekLineupRepository.findBySquadIdAndGameweekId(500L, 20L)).thenReturn(Optional.of(existing));
        when(gameweekLineupRepository.save(any(GameweekLineup.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(lineupSlotRepository.save(any(LineupSlot.class))).thenAnswer(invocation -> {
            LineupSlot slot = invocation.getArgument(0);
            slot.setId(slot.getPlayerId() + 1000);
            return slot;
        });
        SubmitLineupRequest request = new SubmitLineupRequest(starterIds(), benchIds(), 1L);

        GameweekLineupResponse response = lineupService().submit(1L, 10L, 20L, request);

        assertThat(response.id()).isEqualTo(900L);
        assertThat(response.captainPlayerId()).isEqualTo(1L);
        verify(lineupSlotRepository).deleteByLineupId(900L);
    }

    @Test
    void submitRejectsWrongStarterOrBenchCount() {
        stubSeasonGameweekUserSquad();
        SubmitLineupRequest request = new SubmitLineupRequest(starterIds().subList(0, 10), benchIds(), 1L);

        assertThatThrownBy(() -> lineupService().submit(1L, 10L, 20L, request)).isInstanceOf(ConflictException.class);
        verify(gameweekLineupRepository, never()).save(any());
    }

    @Test
    void submitRejectsOverlapBetweenStartersAndBench() {
        stubSeasonGameweekUserSquad();
        List<Long> overlappingBench = List.of(4L, 7L, 11L, 1L); // 1L is also a starter
        SubmitLineupRequest request = new SubmitLineupRequest(starterIds(), overlappingBench, 1L);

        assertThatThrownBy(() -> lineupService().submit(1L, 10L, 20L, request)).isInstanceOf(ConflictException.class);
    }

    @Test
    void submitRejectsCaptainNotAmongStarters() {
        stubSeasonGameweekUserSquad();
        SubmitLineupRequest request = new SubmitLineupRequest(starterIds(), benchIds(), 4L); // 4L is on the bench

        assertThatThrownBy(() -> lineupService().submit(1L, 10L, 20L, request)).isInstanceOf(ConflictException.class);
    }

    @Test
    void submitRejectsPlayersNotInTheUsersSquad() {
        stubSeasonGameweekUserSquad();
        stubSquadMembers();
        List<Long> starters = new ArrayList<>(starterIds());
        starters.set(0, 999L); // not in the squad; also make it the captain so the captain check passes first
        SubmitLineupRequest request = new SubmitLineupRequest(starters, benchIds(), 999L);

        assertThatThrownBy(() -> lineupService().submit(1L, 10L, 20L, request)).isInstanceOf(ConflictException.class);
    }

    @Test
    void submitRejectsInvalidFormation() {
        stubSeasonGameweekUserSquad();
        stubSquadMembers();
        when(playerRepository.findAllById(anyCollection())).thenReturn(squadOf15());
        // 2 GK among starters (1 and 4) instead of exactly 1
        List<Long> starters = List.of(1L, 4L, 2L, 3L, 5L, 8L, 9L, 10L, 13L, 14L, 15L);
        List<Long> bench = List.of(6L, 7L, 11L, 12L);
        SubmitLineupRequest request = new SubmitLineupRequest(starters, bench, 1L);

        assertThatThrownBy(() -> lineupService().submit(1L, 10L, 20L, request)).isInstanceOf(ConflictException.class);
    }

    @Test
    void submitRejectsOnceTheDeadlineHasPassed() {
        when(seasonRepository.findById(10L)).thenReturn(Optional.of(season()));
        when(gameweekRepository.findById(20L)).thenReturn(Optional.of(
                Gameweek.builder().id(20L).seasonId(10L).number(1)
                        .deadlineDateTime(LocalDateTime.now().minusMinutes(1))
                        .status(GameweekStatus.UPCOMING).build()));
        SubmitLineupRequest request = new SubmitLineupRequest(starterIds(), benchIds(), 1L);

        assertThatThrownBy(() -> lineupService().submit(1L, 10L, 20L, request))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("change your lineup");
        verify(gameweekLineupRepository, never()).save(any());
    }

    @Test
    void submitRejectsWhenTheGameweekIsLocked() {
        when(seasonRepository.findById(10L)).thenReturn(Optional.of(season()));
        when(gameweekRepository.findById(20L)).thenReturn(Optional.of(
                Gameweek.builder().id(20L).seasonId(10L).number(1)
                        .deadlineDateTime(LocalDateTime.now().plusDays(1))
                        .status(GameweekStatus.LOCKED).build()));
        SubmitLineupRequest request = new SubmitLineupRequest(starterIds(), benchIds(), 1L);

        assertThatThrownBy(() -> lineupService().submit(1L, 10L, 20L, request))
                .isInstanceOf(ConflictException.class);
        verify(gameweekLineupRepository, never()).save(any());
    }

    @Test
    void submitRejectsUnknownSeason() {
        when(seasonRepository.findById(10L)).thenReturn(Optional.empty());
        SubmitLineupRequest request = new SubmitLineupRequest(starterIds(), benchIds(), 1L);

        assertThatThrownBy(() -> lineupService().submit(1L, 10L, 20L, request)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void submitRejectsGameweekNotInSeason() {
        when(seasonRepository.findById(10L)).thenReturn(Optional.of(season()));
        Gameweek otherSeasonGameweek = Gameweek.builder().id(20L).seasonId(99L).number(1)
                .deadlineDateTime(LocalDateTime.now()).status(GameweekStatus.UPCOMING).build();
        when(gameweekRepository.findById(20L)).thenReturn(Optional.of(otherSeasonGameweek));
        SubmitLineupRequest request = new SubmitLineupRequest(starterIds(), benchIds(), 1L);

        assertThatThrownBy(() -> lineupService().submit(1L, 10L, 20L, request)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void submitRejectsWhenUserHasNoSquad() {
        when(seasonRepository.findById(10L)).thenReturn(Optional.of(season()));
        when(gameweekRepository.findById(20L)).thenReturn(Optional.of(gameweek()));
        when(currentUserProvider.getUserId()).thenReturn(7L);
        when(fantasySquadRepository.findByUserIdAndSeasonId(7L, 10L)).thenReturn(Optional.empty());
        SubmitLineupRequest request = new SubmitLineupRequest(starterIds(), benchIds(), 1L);

        assertThatThrownBy(() -> lineupService().submit(1L, 10L, 20L, request)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void findCurrentReturnsTheSubmittedLineup() {
        stubSeasonGameweekUserSquad();
        GameweekLineup lineup = GameweekLineup.builder().id(900L).squadId(500L).gameweekId(20L).captainPlayerId(1L).build();
        when(gameweekLineupRepository.findBySquadIdAndGameweekId(500L, 20L)).thenReturn(Optional.of(lineup));
        List<LineupSlot> slots = new ArrayList<>();
        starterIds().forEach(id -> slots.add(LineupSlot.builder().id(id + 1000).lineupId(900L).playerId(id).role(LineupRole.STARTER).build()));
        int order = 1;
        for (Long id : benchIds()) {
            slots.add(LineupSlot.builder().id(id + 1000).lineupId(900L).playerId(id).role(LineupRole.BENCH).benchOrder(order++).build());
        }
        when(lineupSlotRepository.findByLineupId(900L)).thenReturn(slots);
        when(playerRepository.findAllById(anyCollection())).thenReturn(squadOf15());

        GameweekLineupResponse response = lineupService().findCurrent(1L, 10L, 20L);

        assertThat(response.starters()).hasSize(11);
        assertThat(response.bench()).hasSize(4);
        assertThat(response.captainPlayerId()).isEqualTo(1L);
    }

    @Test
    void findCurrentRejectsWhenNoLineupSubmitted() {
        stubSeasonGameweekUserSquad();
        when(gameweekLineupRepository.findBySquadIdAndGameweekId(500L, 20L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> lineupService().findCurrent(1L, 10L, 20L)).isInstanceOf(NotFoundException.class);
    }
}
