package com.codemora.fantasy_league.gameweek;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.codemora.fantasy_league.common.error.ConflictException;
import com.codemora.fantasy_league.common.error.NotFoundException;
import com.codemora.fantasy_league.gameweek.dto.GameweekResponse;
import com.codemora.fantasy_league.gameweek.dto.UpdateGameweekStatusRequest;
import com.codemora.fantasy_league.season.Season;
import com.codemora.fantasy_league.season.SeasonRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class GameweekService {

    /**
     * The lifecycle only moves forward, one step at a time. Rewinding a
     * gameweek to UPCOMING would reopen a deadline that has already passed and
     * hand back exactly the advantage #36 exists to prevent.
     */
    private static final Map<GameweekStatus, GameweekStatus> NEXT_STATUS = Map.of(
            GameweekStatus.UPCOMING, GameweekStatus.LOCKED,
            GameweekStatus.LOCKED, GameweekStatus.IN_PROGRESS,
            GameweekStatus.IN_PROGRESS, GameweekStatus.COMPLETE);

    private final SeasonRepository seasonRepository;
    private final GameweekRepository gameweekRepository;

    public GameweekService(SeasonRepository seasonRepository, GameweekRepository gameweekRepository) {
        this.seasonRepository = seasonRepository;
        this.gameweekRepository = gameweekRepository;
    }

    /**
     * The "next" gameweek is the earliest-deadline UPCOMING one -- the one a
     * user still has time to prepare for -- so users can tell at a glance
     * which deadline to plan around (#38's acceptance criteria).
     */
    /**
     * Admin-driven lifecycle step. Nothing advances a gameweek automatically,
     * so this is what eventually makes points official (COMPLETE) -- and why
     * GameweekDeadlineGuard checks the clock rather than trusting status alone.
     */
    @Transactional
    public GameweekResponse updateStatus(Long leagueId, Long seasonId, Long gameweekId, UpdateGameweekStatusRequest request) {
        findSeasonInLeague(leagueId, seasonId);
        Gameweek gameweek = gameweekRepository.findById(gameweekId)
                .orElseThrow(() -> new NotFoundException("No gameweek with id " + gameweekId));
        if (!gameweek.getSeasonId().equals(seasonId)) {
            throw new NotFoundException("No gameweek with id " + gameweekId + " in season " + seasonId);
        }

        GameweekStatus current = gameweek.getStatus();
        GameweekStatus requested = request.status();
        if (current == requested) {
            throw new ConflictException("Gameweek " + gameweek.getNumber() + " is already " + current);
        }
        if (NEXT_STATUS.get(current) != requested) {
            throw new ConflictException("Can't move gameweek " + gameweek.getNumber() + " from " + current
                    + " to " + requested + "; the only allowed next status is " + NEXT_STATUS.get(current));
        }

        gameweek.setStatus(requested);
        Gameweek saved = gameweekRepository.save(gameweek);
        log.info("gameweek_status_updated id={} season_id={} from={} to={}", gameweekId, seasonId, current, requested);
        return toResponse(saved, null, LocalDateTime.now());
    }

    public List<GameweekResponse> findBySeason(Long leagueId, Long seasonId) {
        findSeasonInLeague(leagueId, seasonId);

        List<Gameweek> gameweeks = gameweekRepository.findBySeasonIdOrderByNumber(seasonId);
        LocalDateTime now = LocalDateTime.now();
        Long nextId = gameweeks.stream()
                .filter(gw -> gw.getStatus() == GameweekStatus.UPCOMING)
                .min((a, b) -> a.getDeadlineDateTime().compareTo(b.getDeadlineDateTime()))
                .map(Gameweek::getId)
                .orElse(null);

        return gameweeks.stream().map(gw -> toResponse(gw, nextId, now)).toList();
    }

    private GameweekResponse toResponse(Gameweek gameweek, Long nextId, LocalDateTime now) {
        return new GameweekResponse(
                gameweek.getId(),
                gameweek.getSeasonId(),
                gameweek.getNumber(),
                gameweek.getDeadlineDateTime(),
                gameweek.getStatus(),
                gameweek.getId().equals(nextId),
                Duration.between(now, gameweek.getDeadlineDateTime()).toMinutes());
    }

    private Season findSeasonInLeague(Long leagueId, Long seasonId) {
        Season season = seasonRepository.findById(seasonId)
                .orElseThrow(() -> new NotFoundException("No season with id " + seasonId));
        if (!season.getLeagueId().equals(leagueId)) {
            throw new NotFoundException("No season with id " + seasonId + " under league " + leagueId);
        }
        return season;
    }
}
