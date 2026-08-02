package com.codemora.fantasy_league.lineup;

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
class LineupSlotRepositoryTest {

    @Autowired
    private LineupSlotRepository lineupSlotRepository;
    @Autowired
    private GameweekLineupRepository gameweekLineupRepository;
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

    private Long lineupId;
    private Long playerId;

    @BeforeEach
    void setUp() {
        Long userId = userRepository.save(User.builder().username("alice").passwordHash("hashed").role(Role.USER).build()).getId();
        Long adminId = userRepository.save(User.builder().username("admin").passwordHash("hashed").role(Role.ADMIN).build()).getId();
        Long teamId = teamRepository.save(Team.builder().createdByUserId(adminId).name("Arsenal").build()).getId();
        playerId = playerRepository.save(Player.builder().teamId(teamId).createdByUserId(adminId).name("Bruno Silva").position(Position.MID).marketValue(65).build()).getId();
        Long leagueId = leagueRepository.save(League.builder().createdByUserId(adminId).name("Premier League").build()).getId();
        Long seasonId = seasonRepository.save(Season.builder().leagueId(leagueId).period("2025-26").teamLimit(20).startingBudget(1000).build()).getId();
        Long squadId = fantasySquadRepository.save(FantasySquad.builder().userId(userId).seasonId(seasonId).bankBalance(935).freeTransfers(1).build()).getId();
        Long gameweekId = gameweekRepository.save(Gameweek.builder().seasonId(seasonId).number(1)
                .deadlineDateTime(LocalDateTime.of(2025, 8, 1, 12, 0)).status(GameweekStatus.UPCOMING).build()).getId();
        lineupId = gameweekLineupRepository.save(GameweekLineup.builder().squadId(squadId).gameweekId(gameweekId).captainPlayerId(playerId).build()).getId();
    }

    @Test
    void findByLineupIdReturnsThatLineupsSlots() {
        lineupSlotRepository.save(LineupSlot.builder().lineupId(lineupId).playerId(playerId).role(LineupRole.STARTER).build());

        assertThat(lineupSlotRepository.findByLineupId(lineupId)).hasSize(1);
        assertThat(lineupSlotRepository.findByLineupId(999L)).isEmpty();
    }

    @Test
    void deleteByLineupIdRemovesAllItsSlots() {
        lineupSlotRepository.save(LineupSlot.builder().lineupId(lineupId).playerId(playerId).role(LineupRole.STARTER).build());

        lineupSlotRepository.deleteByLineupId(lineupId);

        assertThat(lineupSlotRepository.findByLineupId(lineupId)).isEmpty();
    }
}
