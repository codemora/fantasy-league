package com.codemora.fantasy_league.player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.codemora.fantasy_league.common.Position;
import com.codemora.fantasy_league.common.error.ConflictException;
import com.codemora.fantasy_league.common.error.NotFoundException;
import com.codemora.fantasy_league.config.CurrentUserProvider;
import com.codemora.fantasy_league.player.dto.PlayerResponse;
import com.codemora.fantasy_league.team.TeamRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PlayerService {

    /** Mirrors the Squad Rules composition in the README: 2 GK, 5 DEF, 5 MID, 3 FWD. */
    private static final Map<Position, Integer> SQUAD_COMPOSITION = Map.of(
            Position.GK, 2,
            Position.DEF, 5,
            Position.MID, 5,
            Position.FWD, 3);

    /** market_value ranges (tenths-of-millions, see ADR 0004) per position, loosely mirroring real-world pricing. */
    private static final Map<Position, int[]> VALUE_RANGES = Map.of(
            Position.GK, new int[] {40, 55},
            Position.DEF, new int[] {40, 70},
            Position.MID, new int[] {45, 100},
            Position.FWD, new int[] {45, 120});

    private final TeamRepository teamRepository;
    private final PlayerRepository playerRepository;
    private final PlayerNameGenerator nameGenerator;
    private final CurrentUserProvider currentUserProvider;

    public PlayerService(
            TeamRepository teamRepository,
            PlayerRepository playerRepository,
            PlayerNameGenerator nameGenerator,
            CurrentUserProvider currentUserProvider) {
        this.teamRepository = teamRepository;
        this.playerRepository = playerRepository;
        this.nameGenerator = nameGenerator;
        this.currentUserProvider = currentUserProvider;
    }

    /**
     * An explicit action (rather than automatic on team creation) so that
     * simulated teams generated in bulk (#19) and manually created teams both
     * go through the same opt-in step, and existing team-creation tests/flows
     * aren't changed by this. 409 if the team already has any players, since
     * running this twice would double up the roster.
     */
    @Transactional
    public List<PlayerResponse> generateSquad(Long teamId) {
        if (!teamRepository.existsById(teamId)) {
            throw new NotFoundException("No team with id " + teamId);
        }
        if (playerRepository.countByTeamId(teamId) > 0) {
            log.warn("player_generation_conflict team_id={} reason=already_has_players", teamId);
            throw new ConflictException("Team " + teamId + " already has players generated");
        }

        int totalPlayers = SQUAD_COMPOSITION.values().stream().mapToInt(Integer::intValue).sum();
        List<String> candidateNames = new ArrayList<>(nameGenerator.candidateNames());
        Collections.shuffle(candidateNames, ThreadLocalRandom.current());

        Long adminId = currentUserProvider.getUserId();
        List<PlayerResponse> generated = new ArrayList<>(totalPlayers);
        int nameIndex = 0;
        for (Map.Entry<Position, Integer> entry : SQUAD_COMPOSITION.entrySet()) {
            Position position = entry.getKey();
            int[] range = VALUE_RANGES.get(position);
            for (int i = 0; i < entry.getValue(); i++) {
                String name = candidateNames.get(nameIndex++ % candidateNames.size());
                int marketValue = ThreadLocalRandom.current().nextInt(range[0], range[1] + 1);
                Player player = playerRepository.save(Player.builder()
                        .teamId(teamId)
                        .createdByUserId(adminId)
                        .name(name)
                        .position(position)
                        .marketValue(marketValue)
                        .build());
                generated.add(toResponse(player));
            }
        }
        log.info("players_generated team_id={} count={}", teamId, generated.size());
        return generated;
    }

    public List<PlayerResponse> findByTeam(Long teamId) {
        if (!teamRepository.existsById(teamId)) {
            throw new NotFoundException("No team with id " + teamId);
        }
        return playerRepository.findByTeamId(teamId).stream().map(this::toResponse).toList();
    }

    private PlayerResponse toResponse(Player player) {
        return new PlayerResponse(player.getId(), player.getTeamId(), player.getName(), player.getPosition(), player.getMarketValue());
    }
}
