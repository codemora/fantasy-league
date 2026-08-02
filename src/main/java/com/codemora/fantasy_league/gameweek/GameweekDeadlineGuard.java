package com.codemora.fantasy_league.gameweek;

import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.codemora.fantasy_league.common.error.ConflictException;

/**
 * The fairness invariant behind #36: once a gameweek's deadline has passed,
 * nothing that affects its scoring may change.
 *
 * <p>Deliberately checks the clock as well as the status. Status transitions
 * are admin-driven (see GameweekService#updateStatus) and nothing moves them
 * automatically, so a gameweek can sit at UPCOMING long after its deadline.
 * Trusting the status alone would leave the invariant unenforced in exactly
 * the window it exists to protect -- a user could set a captain after kickoff
 * simply because no admin had got round to locking the gameweek.
 */
@Component
public class GameweekDeadlineGuard {

    public boolean isOpenForChanges(Gameweek gameweek) {
        return gameweek.getStatus() == GameweekStatus.UPCOMING
                && gameweek.getDeadlineDateTime().isAfter(LocalDateTime.now());
    }

    public void assertOpenForChanges(Gameweek gameweek, String action) {
        if (gameweek.getStatus() != GameweekStatus.UPCOMING) {
            throw new ConflictException("Can't " + action + ": gameweek " + gameweek.getNumber()
                    + " is " + gameweek.getStatus() + ", changes are only allowed while it's UPCOMING");
        }
        if (!gameweek.getDeadlineDateTime().isAfter(LocalDateTime.now())) {
            throw new ConflictException("Can't " + action + ": the deadline for gameweek "
                    + gameweek.getNumber() + " passed at " + gameweek.getDeadlineDateTime());
        }
    }
}
