package com.codemora.fantasy_league.chip;

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
import com.codemora.fantasy_league.fantasysquad.FantasySquad;
import com.codemora.fantasy_league.fantasysquad.FantasySquadRepository;
import com.codemora.fantasy_league.gameweek.Gameweek;
import com.codemora.fantasy_league.gameweek.GameweekRepository;
import com.codemora.fantasy_league.gameweek.GameweekStatus;
import com.codemora.fantasy_league.league.League;
import com.codemora.fantasy_league.league.LeagueRepository;
import com.codemora.fantasy_league.season.Season;
import com.codemora.fantasy_league.season.SeasonRepository;

@DataJpaTest
class SquadChipRepositoryTest {

    @Autowired
    private SquadChipRepository squadChipRepository;
    @Autowired
    private FantasySquadRepository fantasySquadRepository;
    @Autowired
    private GameweekRepository gameweekRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private LeagueRepository leagueRepository;
    @Autowired
    private SeasonRepository seasonRepository;

    private Long squadId;
    private Long gameweek1Id;
    private Long gameweek2Id;

    @BeforeEach
    void setUp() {
        Long userId = userRepository.save(User.builder().username("alice").passwordHash("hashed").role(Role.USER).build()).getId();
        Long leagueId = leagueRepository.save(League.builder().createdByUserId(userId).name("Premier League").build()).getId();
        Long seasonId = seasonRepository.save(Season.builder().leagueId(leagueId).period("2025-26")
                .teamLimit(20).startingBudget(1000).build()).getId();
        squadId = fantasySquadRepository.save(FantasySquad.builder().userId(userId).seasonId(seasonId)
                .bankBalance(1000).freeTransfers(1).build()).getId();
        gameweek1Id = gameweekRepository.save(Gameweek.builder().seasonId(seasonId).number(1)
                .deadlineDateTime(LocalDateTime.of(2025, 8, 1, 12, 0)).status(GameweekStatus.UPCOMING).build()).getId();
        gameweek2Id = gameweekRepository.save(Gameweek.builder().seasonId(seasonId).number(2)
                .deadlineDateTime(LocalDateTime.of(2025, 8, 8, 12, 0)).status(GameweekStatus.UPCOMING).build()).getId();
    }

    private SquadChip chip(Long gameweekId, ChipType chipType) {
        return SquadChip.builder().squadId(squadId).gameweekId(gameweekId)
                .chipType(chipType).activatedAt(LocalDateTime.now()).build();
    }

    @Test
    void existsBySquadIdAndGameweekIdFindsAnyChipThatGameweek() {
        squadChipRepository.save(chip(gameweek1Id, ChipType.WILDCARD));

        assertThat(squadChipRepository.existsBySquadIdAndGameweekId(squadId, gameweek1Id)).isTrue();
        assertThat(squadChipRepository.existsBySquadIdAndGameweekId(squadId, gameweek2Id)).isFalse();
    }

    @Test
    void existsBySquadIdAndChipTypeFindsUsageAcrossAnyGameweek() {
        squadChipRepository.save(chip(gameweek1Id, ChipType.TRIPLE_CAPTAIN));

        assertThat(squadChipRepository.existsBySquadIdAndChipType(squadId, ChipType.TRIPLE_CAPTAIN)).isTrue();
        assertThat(squadChipRepository.existsBySquadIdAndChipType(squadId, ChipType.BENCH_BOOST)).isFalse();
    }

    @Test
    void aSquadCanOnlyHaveOneChipActivePerGameweek() {
        squadChipRepository.save(chip(gameweek1Id, ChipType.WILDCARD));

        assertThatThrownBy(() -> squadChipRepository.saveAndFlush(chip(gameweek1Id, ChipType.TRIPLE_CAPTAIN)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void aSquadCanOnlyUseEachChipTypeOnce() {
        squadChipRepository.save(chip(gameweek1Id, ChipType.WILDCARD));

        assertThatThrownBy(() -> squadChipRepository.saveAndFlush(chip(gameweek2Id, ChipType.WILDCARD)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
