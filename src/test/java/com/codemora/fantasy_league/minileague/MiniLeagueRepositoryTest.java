package com.codemora.fantasy_league.minileague;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import com.codemora.fantasy_league.auth.Role;
import com.codemora.fantasy_league.auth.User;
import com.codemora.fantasy_league.auth.UserRepository;
import com.codemora.fantasy_league.league.League;
import com.codemora.fantasy_league.league.LeagueRepository;
import com.codemora.fantasy_league.season.Season;
import com.codemora.fantasy_league.season.SeasonRepository;

@DataJpaTest
class MiniLeagueRepositoryTest {

    @Autowired
    private MiniLeagueRepository miniLeagueRepository;
    @Autowired
    private MiniLeagueMemberRepository miniLeagueMemberRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private LeagueRepository leagueRepository;
    @Autowired
    private SeasonRepository seasonRepository;

    private Long userId;
    private Long seasonId;

    @BeforeEach
    void setUp() {
        userId = userRepository.save(User.builder().username("alice").passwordHash("hashed").role(Role.USER).build()).getId();
        Long leagueId = leagueRepository.save(League.builder().createdByUserId(userId).name("Premier League").build()).getId();
        seasonId = seasonRepository.save(Season.builder().leagueId(leagueId).period("2025-26")
                .teamLimit(20).startingBudget(1000).build()).getId();
    }

    private MiniLeague miniLeague(String inviteCode) {
        return MiniLeague.builder().seasonId(seasonId).createdByUserId(userId)
                .name("Office League").inviteCode(inviteCode).createdAt(LocalDateTime.now()).build();
    }

    @Test
    void findByInviteCodeFindsAnExactMatch() {
        miniLeagueRepository.save(miniLeague("ABCD2345"));

        assertThat(miniLeagueRepository.findByInviteCode("ABCD2345")).isPresent();
        assertThat(miniLeagueRepository.findByInviteCode("NOPE0000")).isEmpty();
    }

    @Test
    void inviteCodesMustBeUnique() {
        miniLeagueRepository.save(miniLeague("ABCD2345"));

        assertThatThrownBy(() -> miniLeagueRepository.saveAndFlush(miniLeague("ABCD2345")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findBySeasonIdScopesToThatSeason() {
        miniLeagueRepository.save(miniLeague("ABCD2345"));

        assertThat(miniLeagueRepository.findBySeasonId(seasonId)).hasSize(1);
        assertThat(miniLeagueRepository.findBySeasonId(999L)).isEmpty();
    }

    @Test
    void aUserCanOnlyJoinAMiniLeagueOnce() {
        Long miniLeagueId = miniLeagueRepository.save(miniLeague("ABCD2345")).getId();
        miniLeagueMemberRepository.save(MiniLeagueMember.builder()
                .miniLeagueId(miniLeagueId).userId(userId).joinedAt(LocalDateTime.now()).build());

        assertThatThrownBy(() -> miniLeagueMemberRepository.saveAndFlush(MiniLeagueMember.builder()
                        .miniLeagueId(miniLeagueId).userId(userId).joinedAt(LocalDateTime.now()).build()))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void existsByMiniLeagueIdAndUserIdChecksMembership() {
        Long miniLeagueId = miniLeagueRepository.save(miniLeague("ABCD2345")).getId();
        miniLeagueMemberRepository.save(MiniLeagueMember.builder()
                .miniLeagueId(miniLeagueId).userId(userId).joinedAt(LocalDateTime.now()).build());

        assertThat(miniLeagueMemberRepository.existsByMiniLeagueIdAndUserId(miniLeagueId, userId)).isTrue();
        assertThat(miniLeagueMemberRepository.existsByMiniLeagueIdAndUserId(miniLeagueId, 999L)).isFalse();
    }
}
