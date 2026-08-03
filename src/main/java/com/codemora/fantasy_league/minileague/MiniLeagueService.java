package com.codemora.fantasy_league.minileague;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

import lombok.extern.slf4j.Slf4j;

/**
 * Private, invite-code-joined mini-leagues: a season-scoped leaderboard
 * restricted to a subset of squads rather than a whole-season one. Named
 * MiniLeague, not League -- League already means the admin-managed
 * underlying football competition (see README note).
 */
@Service
@Slf4j
public class MiniLeagueService {

    private static final String INVITE_CODE_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"; // no 0/O/1/I/L
    private static final int INVITE_CODE_LENGTH = 8;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SeasonRepository seasonRepository;
    private final FantasySquadRepository fantasySquadRepository;
    private final MiniLeagueRepository miniLeagueRepository;
    private final MiniLeagueMemberRepository miniLeagueMemberRepository;
    private final LeaderboardService leaderboardService;
    private final CurrentUserProvider currentUserProvider;

    public MiniLeagueService(
            SeasonRepository seasonRepository,
            FantasySquadRepository fantasySquadRepository,
            MiniLeagueRepository miniLeagueRepository,
            MiniLeagueMemberRepository miniLeagueMemberRepository,
            LeaderboardService leaderboardService,
            CurrentUserProvider currentUserProvider) {
        this.seasonRepository = seasonRepository;
        this.fantasySquadRepository = fantasySquadRepository;
        this.miniLeagueRepository = miniLeagueRepository;
        this.miniLeagueMemberRepository = miniLeagueMemberRepository;
        this.leaderboardService = leaderboardService;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional
    public MiniLeagueResponse create(Long leagueId, Long seasonId, CreateMiniLeagueRequest request) {
        findSeasonInLeague(leagueId, seasonId);
        Long userId = currentUserProvider.getUserId();
        assertHasSquad(userId, seasonId);

        MiniLeague miniLeague = miniLeagueRepository.save(MiniLeague.builder()
                .seasonId(seasonId)
                .createdByUserId(userId)
                .name(request.name())
                .inviteCode(generateInviteCode())
                .createdAt(LocalDateTime.now())
                .build());
        miniLeagueMemberRepository.save(MiniLeagueMember.builder()
                .miniLeagueId(miniLeague.getId())
                .userId(userId)
                .joinedAt(LocalDateTime.now())
                .build());

        log.info("mini_league_created id={} season_id={} created_by_user_id={}", miniLeague.getId(), seasonId, userId);
        return toResponse(miniLeague, 1);
    }

    @Transactional
    public MiniLeagueResponse join(String inviteCode) {
        MiniLeague miniLeague = miniLeagueRepository.findByInviteCode(inviteCode)
                .orElseThrow(() -> new NotFoundException("No mini-league with invite code " + inviteCode));
        Long userId = currentUserProvider.getUserId();
        assertHasSquad(userId, miniLeague.getSeasonId());

        if (miniLeagueMemberRepository.existsByMiniLeagueIdAndUserId(miniLeague.getId(), userId)) {
            throw new ConflictException("You're already a member of this mini-league");
        }
        miniLeagueMemberRepository.save(MiniLeagueMember.builder()
                .miniLeagueId(miniLeague.getId())
                .userId(userId)
                .joinedAt(LocalDateTime.now())
                .build());

        log.info("mini_league_joined id={} user_id={}", miniLeague.getId(), userId);
        return toResponse(miniLeague, miniLeagueMemberRepository.findByMiniLeagueId(miniLeague.getId()).size());
    }

    public List<MiniLeagueResponse> findMine(Long leagueId, Long seasonId) {
        findSeasonInLeague(leagueId, seasonId);
        Long userId = currentUserProvider.getUserId();
        Set<Long> myMiniLeagueIds = miniLeagueMemberRepository.findByUserId(userId).stream()
                .map(MiniLeagueMember::getMiniLeagueId)
                .collect(Collectors.toSet());

        return miniLeagueRepository.findBySeasonId(seasonId).stream()
                .filter(ml -> myMiniLeagueIds.contains(ml.getId()))
                .map(ml -> toResponse(ml, miniLeagueMemberRepository.findByMiniLeagueId(ml.getId()).size()))
                .toList();
    }

    public List<LeaderboardRowResponse> leaderboard(Long leagueId, Long seasonId, Long miniLeagueId) {
        findSeasonInLeague(leagueId, seasonId);
        MiniLeague miniLeague = findMiniLeagueInSeason(seasonId, miniLeagueId);
        Long userId = currentUserProvider.getUserId();
        // 404, not 403 -- a private mini-league's existence isn't visible to non-members.
        if (!miniLeagueMemberRepository.existsByMiniLeagueIdAndUserId(miniLeague.getId(), userId)) {
            throw new NotFoundException("No mini-league with id " + miniLeagueId);
        }

        Set<Long> memberUserIds = miniLeagueMemberRepository.findByMiniLeagueId(miniLeague.getId()).stream()
                .map(MiniLeagueMember::getUserId)
                .collect(Collectors.toSet());
        List<FantasySquad> memberSquads = fantasySquadRepository.findBySeasonId(seasonId).stream()
                .filter(squad -> memberUserIds.contains(squad.getUserId()))
                .toList();

        return leaderboardService.rank(seasonId, memberSquads);
    }

    private void assertHasSquad(Long userId, Long seasonId) {
        if (!fantasySquadRepository.existsByUserIdAndSeasonId(userId, seasonId)) {
            throw new ConflictException("You need a fantasy squad for this season before joining a mini-league");
        }
    }

    private String generateInviteCode() {
        StringBuilder code = new StringBuilder(INVITE_CODE_LENGTH);
        for (int i = 0; i < INVITE_CODE_LENGTH; i++) {
            code.append(INVITE_CODE_ALPHABET.charAt(RANDOM.nextInt(INVITE_CODE_ALPHABET.length())));
        }
        return code.toString();
    }

    private MiniLeagueResponse toResponse(MiniLeague miniLeague, int memberCount) {
        return new MiniLeagueResponse(miniLeague.getId(), miniLeague.getSeasonId(), miniLeague.getName(),
                miniLeague.getInviteCode(), memberCount, miniLeague.getCreatedAt());
    }

    private Season findSeasonInLeague(Long leagueId, Long seasonId) {
        Season season = seasonRepository.findById(seasonId)
                .orElseThrow(() -> new NotFoundException("No season with id " + seasonId));
        if (!season.getLeagueId().equals(leagueId)) {
            throw new NotFoundException("No season with id " + seasonId + " under league " + leagueId);
        }
        return season;
    }

    private MiniLeague findMiniLeagueInSeason(Long seasonId, Long miniLeagueId) {
        MiniLeague miniLeague = miniLeagueRepository.findById(miniLeagueId)
                .orElseThrow(() -> new NotFoundException("No mini-league with id " + miniLeagueId));
        if (!miniLeague.getSeasonId().equals(seasonId)) {
            throw new NotFoundException("No mini-league with id " + miniLeagueId + " in season " + seasonId);
        }
        return miniLeague;
    }
}
