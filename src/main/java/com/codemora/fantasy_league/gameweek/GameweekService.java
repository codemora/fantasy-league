package com.codemora.fantasy_league.gameweek;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.codemora.fantasy_league.common.error.NotFoundException;
import com.codemora.fantasy_league.gameweek.dto.GameweekResponse;
import com.codemora.fantasy_league.season.Season;
import com.codemora.fantasy_league.season.SeasonRepository;

@Service
public class GameweekService {

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
    public List<GameweekResponse> findBySeason(Long leagueId, Long seasonId) {
        Season season = seasonRepository.findById(seasonId)
                .orElseThrow(() -> new NotFoundException("No season with id " + seasonId));
        if (!season.getLeagueId().equals(leagueId)) {
            throw new NotFoundException("No season with id " + seasonId + " under league " + leagueId);
        }

        List<Gameweek> gameweeks = gameweekRepository.findBySeasonIdOrderByNumber(seasonId);
        LocalDateTime now = LocalDateTime.now();
        Long nextId = gameweeks.stream()
                .filter(gw -> gw.getStatus() == GameweekStatus.UPCOMING)
                .min((a, b) -> a.getDeadlineDateTime().compareTo(b.getDeadlineDateTime()))
                .map(Gameweek::getId)
                .orElse(null);

        return gameweeks.stream()
                .map(gw -> new GameweekResponse(
                        gw.getId(),
                        gw.getSeasonId(),
                        gw.getNumber(),
                        gw.getDeadlineDateTime(),
                        gw.getStatus(),
                        gw.getId().equals(nextId),
                        Duration.between(now, gw.getDeadlineDateTime()).toMinutes()))
                .toList();
    }
}
