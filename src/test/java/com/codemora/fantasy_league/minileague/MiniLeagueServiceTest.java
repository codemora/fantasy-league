package com.codemora.fantasy_league.minileague;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.codemora.fantasy_league.common.error.ConflictException;
import com.codemora.fantasy_league.common.error.NotFoundException;
import com.codemora.fantasy_league.config.CurrentUserProvider;
import com.codemora.fantasy_league.fantasysquad.FantasySquad;
import com.codemora.fantasy_league.fantasysquad.FantasySquadRepository;
import com.codemora.fantasy_league.leaderboard.LeaderboardService;
import com.codemora.fantasy_league.leaderboard.dto.LeaderboardRowResponse;
import com.codemora.fantasy_league.minileague.dto.CreateMiniLeagueRequest;
import com.codemora.fantasy_league.minileague.dto.MiniLeagueResponse;
import com.codemora.fantasy_league.season.Season;
import com.codemora.fantasy_league.season.SeasonRepository;

@ExtendWith(MockitoExtension.class)
class MiniLeagueServiceTest {

    @Mock
    private SeasonRepository seasonRepository;
    @Mock
    private FantasySquadRepository fantasySquadRepository;
    @Mock
    private MiniLeagueRepository miniLeagueRepository;
    @Mock
    private MiniLeagueMemberRepository miniLeagueMemberRepository;
    @Mock
    private LeaderboardService leaderboardService;
    @Mock
    private CurrentUserProvider currentUserProvider;

    private static final Long SEASON_ID = 10L;
    private static final Long ML_ID = 700L;

    private MiniLeagueService service() {
        return new MiniLeagueService(seasonRepository, fantasySquadRepository, miniLeagueRepository,
                miniLeagueMemberRepository, leaderboardService, currentUserProvider);
    }

    private Season season() {
        return Season.builder().id(SEASON_ID).leagueId(1L).period("2025-26").teamLimit(20).startingBudget(1000).build();
    }

    private MiniLeague miniLeague() {
        return MiniLeague.builder().id(ML_ID).seasonId(SEASON_ID).createdByUserId(7L)
                .name("Office League").inviteCode("ABCD2345").createdAt(LocalDateTime.now()).build();
    }

    private MiniLeagueMember member(Long miniLeagueId, Long userId) {
        return MiniLeagueMember.builder().miniLeagueId(miniLeagueId).userId(userId).joinedAt(LocalDateTime.now()).build();
    }

    @Test
    void createSavesAMiniLeagueAndAutoJoinsTheCreator() {
        when(seasonRepository.findById(SEASON_ID)).thenReturn(Optional.of(season()));
        when(currentUserProvider.getUserId()).thenReturn(7L);
        when(fantasySquadRepository.existsByUserIdAndSeasonId(7L, SEASON_ID)).thenReturn(true);
        when(miniLeagueRepository.save(any(MiniLeague.class))).thenAnswer(i -> {
            MiniLeague ml = i.getArgument(0);
            ml.setId(ML_ID);
            return ml;
        });
        when(miniLeagueMemberRepository.save(any(MiniLeagueMember.class))).thenAnswer(i -> i.getArgument(0));

        MiniLeagueResponse response = service().create(1L, SEASON_ID, new CreateMiniLeagueRequest("Office League"));

        assertThat(response.name()).isEqualTo("Office League");
        assertThat(response.memberCount()).isEqualTo(1);
        assertThat(response.inviteCode()).hasSize(8);
    }

    @Test
    void createRejectsWhenUserHasNoSquadForTheSeason() {
        when(seasonRepository.findById(SEASON_ID)).thenReturn(Optional.of(season()));
        when(currentUserProvider.getUserId()).thenReturn(7L);
        when(fantasySquadRepository.existsByUserIdAndSeasonId(7L, SEASON_ID)).thenReturn(false);

        assertThatThrownBy(() -> service().create(1L, SEASON_ID, new CreateMiniLeagueRequest("Office League")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("fantasy squad for this season");
    }

    @Test
    void createRejectsUnknownSeason() {
        when(seasonRepository.findById(SEASON_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().create(1L, SEASON_ID, new CreateMiniLeagueRequest("Office League")))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void joinAddsTheCallerAsAMember() {
        when(miniLeagueRepository.findByInviteCode("ABCD2345")).thenReturn(Optional.of(miniLeague()));
        when(currentUserProvider.getUserId()).thenReturn(8L);
        when(fantasySquadRepository.existsByUserIdAndSeasonId(8L, SEASON_ID)).thenReturn(true);
        when(miniLeagueMemberRepository.existsByMiniLeagueIdAndUserId(ML_ID, 8L)).thenReturn(false);
        when(miniLeagueMemberRepository.save(any(MiniLeagueMember.class))).thenAnswer(i -> i.getArgument(0));
        when(miniLeagueMemberRepository.findByMiniLeagueId(ML_ID)).thenReturn(List.of(member(ML_ID, 7L), member(ML_ID, 8L)));

        MiniLeagueResponse response = service().join("ABCD2345");

        assertThat(response.memberCount()).isEqualTo(2);
    }

    @Test
    void joinRejectsAnUnknownInviteCode() {
        when(miniLeagueRepository.findByInviteCode("NOPE0000")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().join("NOPE0000")).isInstanceOf(NotFoundException.class);
    }

    @Test
    void joinRejectsWhenUserHasNoSquadForTheMiniLeaguesSeason() {
        when(miniLeagueRepository.findByInviteCode("ABCD2345")).thenReturn(Optional.of(miniLeague()));
        when(currentUserProvider.getUserId()).thenReturn(8L);
        when(fantasySquadRepository.existsByUserIdAndSeasonId(8L, SEASON_ID)).thenReturn(false);

        assertThatThrownBy(() -> service().join("ABCD2345"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("fantasy squad for this season");
    }

    @Test
    void joinRejectsWhenAlreadyAMember() {
        when(miniLeagueRepository.findByInviteCode("ABCD2345")).thenReturn(Optional.of(miniLeague()));
        when(currentUserProvider.getUserId()).thenReturn(7L);
        when(fantasySquadRepository.existsByUserIdAndSeasonId(7L, SEASON_ID)).thenReturn(true);
        when(miniLeagueMemberRepository.existsByMiniLeagueIdAndUserId(ML_ID, 7L)).thenReturn(true);

        assertThatThrownBy(() -> service().join("ABCD2345"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already a member");
    }

    @Test
    void findMineReturnsOnlyMiniLeaguesTheCallerBelongsTo() {
        when(seasonRepository.findById(SEASON_ID)).thenReturn(Optional.of(season()));
        when(currentUserProvider.getUserId()).thenReturn(7L);
        when(miniLeagueMemberRepository.findByUserId(7L)).thenReturn(List.of(member(ML_ID, 7L)));
        when(miniLeagueRepository.findBySeasonId(SEASON_ID)).thenReturn(List.of(
                miniLeague(),
                MiniLeague.builder().id(701L).seasonId(SEASON_ID).createdByUserId(9L)
                        .name("Someone Else's League").inviteCode("ZZZZ9999").createdAt(LocalDateTime.now()).build()));
        when(miniLeagueMemberRepository.findByMiniLeagueId(anyLong())).thenReturn(List.of(member(ML_ID, 7L)));

        List<MiniLeagueResponse> mine = service().findMine(1L, SEASON_ID);

        assertThat(mine).hasSize(1);
        assertThat(mine.get(0).id()).isEqualTo(ML_ID);
    }

    @Test
    void leaderboardRanksOnlyMemberSquads() {
        when(seasonRepository.findById(SEASON_ID)).thenReturn(Optional.of(season()));
        when(miniLeagueRepository.findById(ML_ID)).thenReturn(Optional.of(miniLeague()));
        when(currentUserProvider.getUserId()).thenReturn(7L);
        when(miniLeagueMemberRepository.existsByMiniLeagueIdAndUserId(ML_ID, 7L)).thenReturn(true);
        when(miniLeagueMemberRepository.findByMiniLeagueId(ML_ID)).thenReturn(List.of(member(ML_ID, 7L), member(ML_ID, 8L)));
        when(fantasySquadRepository.findBySeasonId(SEASON_ID)).thenReturn(List.of(
                FantasySquad.builder().id(500L).userId(7L).seasonId(SEASON_ID).bankBalance(0).freeTransfers(1).build(),
                FantasySquad.builder().id(501L).userId(8L).seasonId(SEASON_ID).bankBalance(0).freeTransfers(1).build(),
                FantasySquad.builder().id(502L).userId(9L).seasonId(SEASON_ID).bankBalance(0).freeTransfers(1).build()));
        ArgumentCaptor<List<FantasySquad>> captor = ArgumentCaptor.forClass(List.class);
        when(leaderboardService.rank(eq(SEASON_ID), captor.capture())).thenReturn(List.of());

        service().leaderboard(1L, SEASON_ID, ML_ID);

        assertThat(captor.getValue()).extracting(FantasySquad::getUserId).containsExactlyInAnyOrder(7L, 8L);
    }

    @Test
    void leaderboardRejectsWhenCallerIsNotAMember() {
        when(seasonRepository.findById(SEASON_ID)).thenReturn(Optional.of(season()));
        when(miniLeagueRepository.findById(ML_ID)).thenReturn(Optional.of(miniLeague()));
        when(currentUserProvider.getUserId()).thenReturn(99L);
        when(miniLeagueMemberRepository.existsByMiniLeagueIdAndUserId(ML_ID, 99L)).thenReturn(false);

        assertThatThrownBy(() -> service().leaderboard(1L, SEASON_ID, ML_ID))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void leaderboardRejectsAMiniLeagueFromAnotherSeason() {
        when(seasonRepository.findById(SEASON_ID)).thenReturn(Optional.of(season()));
        when(miniLeagueRepository.findById(ML_ID)).thenReturn(Optional.of(
                MiniLeague.builder().id(ML_ID).seasonId(99L).createdByUserId(7L)
                        .name("Office League").inviteCode("ABCD2345").createdAt(LocalDateTime.now()).build()));

        assertThatThrownBy(() -> service().leaderboard(1L, SEASON_ID, ML_ID))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void leaderboardRejectsAnUnknownMiniLeague() {
        when(seasonRepository.findById(SEASON_ID)).thenReturn(Optional.of(season()));
        when(miniLeagueRepository.findById(ML_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().leaderboard(1L, SEASON_ID, ML_ID))
                .isInstanceOf(NotFoundException.class);
    }
}
