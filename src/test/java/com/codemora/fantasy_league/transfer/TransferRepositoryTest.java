package com.codemora.fantasy_league.transfer;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import com.codemora.fantasy_league.auth.Role;
import com.codemora.fantasy_league.auth.User;
import com.codemora.fantasy_league.auth.UserRepository;
import com.codemora.fantasy_league.common.Position;
import com.codemora.fantasy_league.fantasysquad.FantasySquad;
import com.codemora.fantasy_league.fantasysquad.FantasySquadRepository;
import com.codemora.fantasy_league.gameweek.Gameweek;
import com.codemora.fantasy_league.gameweek.GameweekRepository;
import com.codemora.fantasy_league.gameweek.GameweekStatus;
import com.codemora.fantasy_league.league.League;
import com.codemora.fantasy_league.league.LeagueRepository;
import com.codemora.fantasy_league.player.Player;
import com.codemora.fantasy_league.player.PlayerRepository;
import com.codemora.fantasy_league.season.Season;
import com.codemora.fantasy_league.season.SeasonRepository;
import com.codemora.fantasy_league.team.Team;
import com.codemora.fantasy_league.team.TeamRepository;

@DataJpaTest
class TransferRepositoryTest {

    @Autowired
    private TransferRepository transferRepository;
    @Autowired
    private FantasySquadRepository fantasySquadRepository;
    @Autowired
    private GameweekRepository gameweekRepository;
    @Autowired
    private PlayerRepository playerRepository;
    @Autowired
    private TeamRepository teamRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private LeagueRepository leagueRepository;
    @Autowired
    private SeasonRepository seasonRepository;

    private Long squadId;
    private Long gameweek1Id;
    private Long gameweek2Id;
    private Long playerOutId;
    private Long playerInId;

    @BeforeEach
    void setUp() {
        Long userId = userRepository.save(User.builder().username("alice").passwordHash("hashed").role(Role.USER).build()).getId();
        Long adminId = userRepository.save(User.builder().username("admin").passwordHash("hashed").role(Role.ADMIN).build()).getId();
        Long teamId = teamRepository.save(Team.builder().createdByUserId(adminId).name("Arsenal").build()).getId();
        playerOutId = playerRepository.save(Player.builder().teamId(teamId).createdByUserId(adminId)
                .name("Out Player").position(Position.MID).marketValue(60).build()).getId();
        playerInId = playerRepository.save(Player.builder().teamId(teamId).createdByUserId(adminId)
                .name("In Player").position(Position.MID).marketValue(70).build()).getId();
        Long leagueId = leagueRepository.save(League.builder().createdByUserId(adminId).name("Premier League").build()).getId();
        Long seasonId = seasonRepository.save(Season.builder().leagueId(leagueId).period("2025-26")
                .teamLimit(20).startingBudget(1000).build()).getId();
        squadId = fantasySquadRepository.save(FantasySquad.builder().userId(userId).seasonId(seasonId)
                .bankBalance(935).freeTransfers(1).build()).getId();
        gameweek1Id = gameweekRepository.save(Gameweek.builder().seasonId(seasonId).number(1)
                .deadlineDateTime(LocalDateTime.of(2025, 8, 1, 12, 0)).status(GameweekStatus.UPCOMING).build()).getId();
        gameweek2Id = gameweekRepository.save(Gameweek.builder().seasonId(seasonId).number(2)
                .deadlineDateTime(LocalDateTime.of(2025, 8, 8, 12, 0)).status(GameweekStatus.UPCOMING).build()).getId();
    }

    private Transfer transfer(Long gameweekId, int pointsCost, LocalDateTime at) {
        return Transfer.builder().squadId(squadId).gameweekId(gameweekId)
                .playerOutId(playerOutId).playerInId(playerInId).pointsCost(pointsCost).timestamp(at).build();
    }

    @Test
    void findBySquadIdOrderByTimestampReturnsOldestFirst() {
        transferRepository.save(transfer(gameweek2Id, 4, LocalDateTime.of(2025, 8, 8, 10, 0)));
        transferRepository.save(transfer(gameweek1Id, 0, LocalDateTime.of(2025, 8, 1, 10, 0)));

        assertThat(transferRepository.findBySquadIdOrderByTimestamp(squadId))
                .extracting(Transfer::getGameweekId)
                .containsExactly(gameweek1Id, gameweek2Id);
    }

    @Test
    void findBySquadIdAndGameweekIdScopesToThatGameweek() {
        transferRepository.save(transfer(gameweek1Id, 0, LocalDateTime.of(2025, 8, 1, 10, 0)));
        transferRepository.save(transfer(gameweek2Id, 4, LocalDateTime.of(2025, 8, 8, 10, 0)));

        assertThat(transferRepository.findBySquadIdAndGameweekId(squadId, gameweek1Id)).hasSize(1);
        assertThat(transferRepository.findBySquadIdAndGameweekId(squadId, 999L)).isEmpty();
    }

    @Test
    void countBySquadIdAndGameweekIdCountsThatGameweeksTransfers() {
        transferRepository.save(transfer(gameweek1Id, 0, LocalDateTime.of(2025, 8, 1, 10, 0)));
        transferRepository.save(transfer(gameweek1Id, 4, LocalDateTime.of(2025, 8, 1, 11, 0)));
        transferRepository.save(transfer(gameweek2Id, 0, LocalDateTime.of(2025, 8, 8, 10, 0)));

        assertThat(transferRepository.countBySquadIdAndGameweekId(squadId, gameweek1Id)).isEqualTo(2);
        assertThat(transferRepository.countBySquadIdAndGameweekId(squadId, gameweek2Id)).isEqualTo(1);
    }
}
