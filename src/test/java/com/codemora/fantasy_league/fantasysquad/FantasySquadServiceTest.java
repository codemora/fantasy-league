package com.codemora.fantasy_league.fantasysquad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

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
import com.codemora.fantasy_league.fantasysquad.dto.CreateFantasySquadRequest;
import com.codemora.fantasy_league.fantasysquad.dto.FantasySquadResponse;
import com.codemora.fantasy_league.player.Player;
import com.codemora.fantasy_league.player.PlayerRepository;
import com.codemora.fantasy_league.season.Season;
import com.codemora.fantasy_league.season.SeasonRepository;

@ExtendWith(MockitoExtension.class)
class FantasySquadServiceTest {

    @Mock
    private SeasonRepository seasonRepository;
    @Mock
    private PlayerRepository playerRepository;
    @Mock
    private FantasySquadRepository fantasySquadRepository;
    @Mock
    private SquadPlayerRepository squadPlayerRepository;
    @Mock
    private CurrentUserProvider currentUserProvider;

    private FantasySquadService fantasySquadService() {
        return new FantasySquadService(seasonRepository, playerRepository, fantasySquadRepository, squadPlayerRepository, currentUserProvider);
    }

    private Season season() {
        return Season.builder().id(10L).leagueId(1L).period("2025-26").teamLimit(20).startingBudget(1000).build();
    }

    /** 5 teams x 3 players each, composition 2 GK / 5 DEF / 5 MID / 3 FWD, market_value 60 each (total 900). */
    private List<Player> validSquad() {
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
        return Player.builder().id(id).teamId(teamId).createdByUserId(7L).name("Player" + id).position(position).marketValue(60).build();
    }

    private CreateFantasySquadRequest requestFor(List<Player> players) {
        return new CreateFantasySquadRequest(players.stream().map(Player::getId).toList());
    }

    private void stubHappyPathSaves() {
        when(fantasySquadRepository.save(any(FantasySquad.class))).thenAnswer(invocation -> {
            FantasySquad squad = invocation.getArgument(0);
            squad.setId(500L);
            return squad;
        });
        when(squadPlayerRepository.save(any(SquadPlayer.class))).thenAnswer(invocation -> {
            SquadPlayer sp = invocation.getArgument(0);
            sp.setId((long) (sp.getPlayerId() + 1000));
            return sp;
        });
    }

    @Test
    void createSavesSquadWithCorrectBankBalance() {
        when(seasonRepository.findById(10L)).thenReturn(Optional.of(season()));
        when(currentUserProvider.getUserId()).thenReturn(7L);
        when(fantasySquadRepository.existsByUserIdAndSeasonId(7L, 10L)).thenReturn(false);
        List<Player> players = validSquad();
        when(playerRepository.findAllById(anyCollection())).thenReturn(players);
        stubHappyPathSaves();

        FantasySquadResponse response = fantasySquadService().create(1L, 10L, requestFor(players));

        assertThat(response.id()).isEqualTo(500L);
        assertThat(response.bankBalance()).isEqualTo(1000 - 900); // 15 players x 60
        assertThat(response.freeTransfers()).isEqualTo(1);
        assertThat(response.players()).hasSize(15);
    }

    @Test
    void createRejectsWhenUserAlreadyHasASquad() {
        when(seasonRepository.findById(10L)).thenReturn(Optional.of(season()));
        when(currentUserProvider.getUserId()).thenReturn(7L);
        when(fantasySquadRepository.existsByUserIdAndSeasonId(7L, 10L)).thenReturn(true);

        assertThatThrownBy(() -> fantasySquadService().create(1L, 10L, requestFor(validSquad())))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void createRejectsDuplicatePlayerIds() {
        when(seasonRepository.findById(10L)).thenReturn(Optional.of(season()));
        when(currentUserProvider.getUserId()).thenReturn(7L);
        when(fantasySquadRepository.existsByUserIdAndSeasonId(7L, 10L)).thenReturn(false);
        List<Long> ids = new ArrayList<>(validSquad().stream().map(Player::getId).toList());
        ids.set(1, ids.get(0)); // duplicate -> only 14 distinct

        assertThatThrownBy(() -> fantasySquadService().create(1L, 10L, new CreateFantasySquadRequest(ids)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void createRejectsUnknownPlayerId() {
        when(seasonRepository.findById(10L)).thenReturn(Optional.of(season()));
        when(currentUserProvider.getUserId()).thenReturn(7L);
        when(fantasySquadRepository.existsByUserIdAndSeasonId(7L, 10L)).thenReturn(false);
        List<Player> players = validSquad();
        when(playerRepository.findAllById(anyCollection())).thenReturn(players.subList(0, 14)); // one missing

        assertThatThrownBy(() -> fantasySquadService().create(1L, 10L, requestFor(players)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void createRejectsWrongPositionComposition() {
        when(seasonRepository.findById(10L)).thenReturn(Optional.of(season()));
        when(currentUserProvider.getUserId()).thenReturn(7L);
        when(fantasySquadRepository.existsByUserIdAndSeasonId(7L, 10L)).thenReturn(false);
        List<Player> players = new ArrayList<>(validSquad());
        players.set(14, player(99L, 6L, Position.MID)); // replaces a FWD with a MID -> FWD=2, MID=6
        when(playerRepository.findAllById(anyCollection())).thenReturn(players);

        assertThatThrownBy(() -> fantasySquadService().create(1L, 10L, requestFor(players)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void createRejectsMoreThanThreePlayersFromOneTeam() {
        when(seasonRepository.findById(10L)).thenReturn(Optional.of(season()));
        when(currentUserProvider.getUserId()).thenReturn(7L);
        when(fantasySquadRepository.existsByUserIdAndSeasonId(7L, 10L)).thenReturn(false);
        List<Player> players = new ArrayList<>(validSquad());
        players.set(14, player(99L, 1L, Position.FWD)); // 4th player from team 1 (still keeps FWD count at 3)
        when(playerRepository.findAllById(anyCollection())).thenReturn(players);

        assertThatThrownBy(() -> fantasySquadService().create(1L, 10L, requestFor(players)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void createRejectsWhenOverBudget() {
        Season tightBudget = Season.builder().id(10L).leagueId(1L).period("2025-26").teamLimit(20).startingBudget(100).build();
        when(seasonRepository.findById(10L)).thenReturn(Optional.of(tightBudget));
        when(currentUserProvider.getUserId()).thenReturn(7L);
        when(fantasySquadRepository.existsByUserIdAndSeasonId(7L, 10L)).thenReturn(false);
        List<Player> players = validSquad();
        when(playerRepository.findAllById(anyCollection())).thenReturn(players);

        assertThatThrownBy(() -> fantasySquadService().create(1L, 10L, requestFor(players)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void createRejectsUnknownSeason() {
        when(seasonRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fantasySquadService().create(1L, 99L, requestFor(validSquad())))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void createRejectsSeasonInDifferentLeague() {
        Season wrongLeagueSeason = Season.builder().id(10L).leagueId(2L).period("2025-26").teamLimit(20).startingBudget(1000).build();
        when(seasonRepository.findById(10L)).thenReturn(Optional.of(wrongLeagueSeason));

        assertThatThrownBy(() -> fantasySquadService().create(1L, 10L, requestFor(validSquad())))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void findMineReturnsTheUsersSquad() {
        when(seasonRepository.findById(10L)).thenReturn(Optional.of(season()));
        when(currentUserProvider.getUserId()).thenReturn(7L);
        FantasySquad squad = FantasySquad.builder().id(500L).userId(7L).seasonId(10L).bankBalance(100).freeTransfers(1).build();
        when(fantasySquadRepository.findByUserIdAndSeasonId(7L, 10L)).thenReturn(Optional.of(squad));
        List<Player> players = validSquad();
        List<SquadPlayer> squadPlayers = players.stream()
                .map(p -> SquadPlayer.builder().id(p.getId() + 1000).squadId(500L).playerId(p.getId()).purchasePrice(60).addedAt(java.time.LocalDateTime.now()).build())
                .toList();
        when(squadPlayerRepository.findBySquadId(500L)).thenReturn(squadPlayers);
        when(playerRepository.findAllById(anyCollection())).thenReturn(players);

        FantasySquadResponse response = fantasySquadService().findMine(1L, 10L);

        assertThat(response.id()).isEqualTo(500L);
        assertThat(response.players()).hasSize(15);
        // grouped by position: GK entries should come before DEF, MID, FWD
        assertThat(response.players().get(0).position()).isEqualTo(Position.GK);
        assertThat(response.players().get(14).position()).isEqualTo(Position.FWD);
    }

    @Test
    void findMineRejectsWhenNoSquadExists() {
        when(seasonRepository.findById(10L)).thenReturn(Optional.of(season()));
        when(currentUserProvider.getUserId()).thenReturn(7L);
        when(fantasySquadRepository.findByUserIdAndSeasonId(7L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fantasySquadService().findMine(1L, 10L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void findMineRejectsUnknownSeason() {
        when(seasonRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fantasySquadService().findMine(1L, 99L)).isInstanceOf(NotFoundException.class);
    }
}
