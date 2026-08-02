package com.codemora.fantasy_league.fixture;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.codemora.fantasy_league.common.error.ConflictException;
import com.codemora.fantasy_league.common.error.NotFoundException;
import com.codemora.fantasy_league.fixture.dto.EditFixtureRequest;
import com.codemora.fantasy_league.fixture.dto.FixtureResponse;
import com.codemora.fantasy_league.fixture.dto.GenerateFixturesResponse;
import com.codemora.fantasy_league.gameweek.Gameweek;
import com.codemora.fantasy_league.gameweek.GameweekRepository;
import com.codemora.fantasy_league.gameweek.GameweekStatus;
import com.codemora.fantasy_league.season.Season;
import com.codemora.fantasy_league.season.SeasonEntrant;
import com.codemora.fantasy_league.season.SeasonEntrantRepository;
import com.codemora.fantasy_league.season.SeasonRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class FixtureService {

    private final SeasonRepository seasonRepository;
    private final SeasonEntrantRepository seasonEntrantRepository;
    private final GameweekRepository gameweekRepository;
    private final FixtureRepository fixtureRepository;
    private final RoundRobinScheduler roundRobinScheduler;

    public FixtureService(
            SeasonRepository seasonRepository,
            SeasonEntrantRepository seasonEntrantRepository,
            GameweekRepository gameweekRepository,
            FixtureRepository fixtureRepository,
            RoundRobinScheduler roundRobinScheduler) {
        this.seasonRepository = seasonRepository;
        this.seasonEntrantRepository = seasonEntrantRepository;
        this.gameweekRepository = gameweekRepository;
        this.fixtureRepository = fixtureRepository;
        this.roundRobinScheduler = roundRobinScheduler;
    }

    /**
     * Generates the full fixture list (a round-robin schedule, single or double
     * leg per the season's doubleLeg flag) and groups it into Gameweeks, each
     * with a deadline -- required for gameweek deadline locking (#36) and the
     * gameweek list (#38) to function. Refuses to run if fixtures already
     * exist for the season, which is what guarantees "no duplicate matches"
     * (#12's acceptance criteria) rather than trying to detect duplicates
     * after the fact.
     */
    @Transactional
    public GenerateFixturesResponse generate(Long leagueId, Long seasonId) {
        Season season = seasonRepository.findById(seasonId)
                .orElseThrow(() -> new NotFoundException("No season with id " + seasonId));
        if (!season.getLeagueId().equals(leagueId)) {
            throw new NotFoundException("No season with id " + seasonId + " under league " + leagueId);
        }
        if (seasonRepository.hasAnyFixtures(seasonId)) {
            log.warn("fixture_generation_conflict season_id={} reason=already_generated", seasonId);
            throw new ConflictException("Season " + seasonId + " already has fixtures generated");
        }

        List<Long> teamIds = seasonEntrantRepository.findBySeasonId(seasonId).stream()
                .map(SeasonEntrant::getTeamId)
                .toList();
        if (teamIds.size() < 2) {
            throw new ConflictException("Season " + seasonId + " needs at least 2 entered teams to generate fixtures");
        }

        List<List<TeamPairing>> rounds = roundRobinScheduler.generateRounds(teamIds, season.isDoubleLeg());
        LocalDateTime scheduleStart = season.getStartDate() != null ? season.getStartDate().atStartOfDay() : LocalDateTime.now();

        int fixturesCreated = 0;
        for (int i = 0; i < rounds.size(); i++) {
            LocalDateTime deadline = scheduleStart.plusWeeks(i);
            Gameweek gameweek = gameweekRepository.save(Gameweek.builder()
                    .seasonId(seasonId)
                    .number(i + 1)
                    .deadlineDateTime(deadline)
                    .status(GameweekStatus.UPCOMING)
                    .build());

            for (TeamPairing pairing : rounds.get(i)) {
                fixtureRepository.save(Fixture.builder()
                        .seasonId(seasonId)
                        .gameweekId(gameweek.getId())
                        .homeTeamId(pairing.homeTeamId())
                        .awayTeamId(pairing.awayTeamId())
                        .played(false)
                        .startDateTime(deadline)
                        .simulationSeed(ThreadLocalRandom.current().nextLong())
                        .build());
                fixturesCreated++;
            }
        }

        log.info("fixtures_generated season_id={} gameweeks={} fixtures={}", seasonId, rounds.size(), fixturesCreated);
        return new GenerateFixturesResponse(rounds.size(), fixturesCreated);
    }

    /**
     * Date/time only -- swapping opponents post-generation would need to
     * re-validate the round-robin's no-duplicate-pairing guarantee, which is
     * out of scope here. 409 once the fixture has a recorded result or its
     * gameweek's deadline has passed, so a played/locked fixture can't be
     * retroactively altered.
     */
    @Transactional
    public FixtureResponse update(Long leagueId, Long seasonId, Long fixtureId, EditFixtureRequest request) {
        Fixture fixture = findInSeason(leagueId, seasonId, fixtureId);
        if (fixture.isPlayed()) {
            log.warn("fixture_update_conflict id={} reason=already_played", fixtureId);
            throw new ConflictException("Fixture " + fixtureId + " already has a recorded result and can't be edited");
        }
        Gameweek gameweek = gameweekRepository.findById(fixture.getGameweekId())
                .orElseThrow(() -> new NotFoundException("No gameweek with id " + fixture.getGameweekId()));
        if (gameweek.getDeadlineDateTime().isBefore(LocalDateTime.now())) {
            log.warn("fixture_update_conflict id={} reason=deadline_passed", fixtureId);
            throw new ConflictException("Fixture " + fixtureId + "'s gameweek deadline has passed and can't be edited");
        }
        fixture.setStartDateTime(request.startDateTime());
        Fixture saved = fixtureRepository.save(fixture);
        log.info("fixture_updated id={} start_date_time={}", saved.getId(), saved.getStartDateTime());
        return toResponse(saved);
    }

    /**
     * Addressing a fixture through a mismatched (league, season) pair in the
     * path is treated as not-found, same as findInLeague on SeasonService --
     * the nested URL implies that relationship.
     */
    private Fixture findInSeason(Long leagueId, Long seasonId, Long fixtureId) {
        Season season = seasonRepository.findById(seasonId)
                .orElseThrow(() -> new NotFoundException("No season with id " + seasonId));
        if (!season.getLeagueId().equals(leagueId)) {
            throw new NotFoundException("No season with id " + seasonId + " under league " + leagueId);
        }
        Fixture fixture = fixtureRepository.findById(fixtureId)
                .orElseThrow(() -> new NotFoundException("No fixture with id " + fixtureId));
        if (!fixture.getSeasonId().equals(seasonId)) {
            throw new NotFoundException("No fixture with id " + fixtureId + " under season " + seasonId);
        }
        return fixture;
    }

    private FixtureResponse toResponse(Fixture fixture) {
        return new FixtureResponse(
                fixture.getId(),
                fixture.getSeasonId(),
                fixture.getGameweekId(),
                fixture.getHomeTeamId(),
                fixture.getAwayTeamId(),
                fixture.getHomeTeamScore(),
                fixture.getAwayTeamScore(),
                fixture.isPlayed(),
                fixture.getStartDateTime());
    }
}
