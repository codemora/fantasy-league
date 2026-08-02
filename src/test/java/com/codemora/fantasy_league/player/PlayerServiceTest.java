package com.codemora.fantasy_league.player;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.codemora.fantasy_league.common.Position;
import com.codemora.fantasy_league.common.error.ConflictException;
import com.codemora.fantasy_league.common.error.NotFoundException;
import com.codemora.fantasy_league.config.CurrentUserProvider;
import com.codemora.fantasy_league.player.dto.PlayerResponse;
import com.codemora.fantasy_league.team.TeamRepository;

@ExtendWith(MockitoExtension.class)
class PlayerServiceTest {

    @Mock
    private TeamRepository teamRepository;
    @Mock
    private PlayerRepository playerRepository;
    @Mock
    private CurrentUserProvider currentUserProvider;

    private final PlayerNameGenerator nameGenerator = new PlayerNameGenerator();

    private PlayerService playerService() {
        return new PlayerService(teamRepository, playerRepository, nameGenerator, currentUserProvider);
    }

    @Test
    void generateSquadCreatesTheFullPositionComposition() {
        when(teamRepository.existsById(1L)).thenReturn(true);
        when(playerRepository.countByTeamId(1L)).thenReturn(0L);
        when(currentUserProvider.getUserId()).thenReturn(7L);
        when(playerRepository.save(any(Player.class))).thenAnswer(invocation -> {
            Player player = invocation.getArgument(0);
            player.setId((long) (100 + player.hashCode() % 1000));
            return player;
        });

        List<PlayerResponse> squad = playerService().generateSquad(1L);

        assertThat(squad).hasSize(15);
        Map<Position, Long> byPosition = squad.stream()
                .collect(Collectors.groupingBy(PlayerResponse::position, Collectors.counting()));
        assertThat(byPosition.get(Position.GK)).isEqualTo(2);
        assertThat(byPosition.get(Position.DEF)).isEqualTo(5);
        assertThat(byPosition.get(Position.MID)).isEqualTo(5);
        assertThat(byPosition.get(Position.FWD)).isEqualTo(3);
        assertThat(squad).allSatisfy(p -> {
            assertThat(p.teamId()).isEqualTo(1L);
            assertThat(p.name()).isNotBlank();
            assertThat(p.marketValue()).isPositive();
        });
    }

    @Test
    void generateSquadRejectsUnknownTeam() {
        when(teamRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> playerService().generateSquad(99L)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void generateSquadRejectsTeamThatAlreadyHasPlayers() {
        when(teamRepository.existsById(1L)).thenReturn(true);
        when(playerRepository.countByTeamId(1L)).thenReturn(15L);

        assertThatThrownBy(() -> playerService().generateSquad(1L)).isInstanceOf(ConflictException.class);
    }

    @Test
    void findByTeamReturnsTheRoster() {
        when(teamRepository.existsById(1L)).thenReturn(true);
        when(playerRepository.findByTeamId(1L)).thenReturn(List.of(
                Player.builder().id(500L).teamId(1L).createdByUserId(7L).name("Bruno Silva").position(Position.MID).marketValue(65).build()));

        List<PlayerResponse> roster = playerService().findByTeam(1L);

        assertThat(roster).hasSize(1);
        assertThat(roster.get(0).name()).isEqualTo("Bruno Silva");
    }

    @Test
    void findByTeamRejectsUnknownTeam() {
        when(teamRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> playerService().findByTeam(99L)).isInstanceOf(NotFoundException.class);
    }
}
