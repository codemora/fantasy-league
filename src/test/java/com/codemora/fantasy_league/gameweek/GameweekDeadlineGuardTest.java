package com.codemora.fantasy_league.gameweek;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.codemora.fantasy_league.common.error.ConflictException;

class GameweekDeadlineGuardTest {

    private final GameweekDeadlineGuard guard = new GameweekDeadlineGuard();

    private Gameweek gameweek(GameweekStatus status, LocalDateTime deadline) {
        return Gameweek.builder().id(20L).seasonId(10L).number(3).deadlineDateTime(deadline).status(status).build();
    }

    private Gameweek open() {
        return gameweek(GameweekStatus.UPCOMING, LocalDateTime.now().plusDays(1));
    }

    @Test
    void anUpcomingGameweekBeforeItsDeadlineIsOpen() {
        assertThat(guard.isOpenForChanges(open())).isTrue();
        assertThatCode(() -> guard.assertOpenForChanges(open(), "do the thing")).doesNotThrowAnyException();
    }

    @Test
    void anUpcomingGameweekPastItsDeadlineIsClosed() {
        // the case that matters: nothing moves status automatically, so a stale
        // UPCOMING must still be treated as locked once the clock passes it
        Gameweek stale = gameweek(GameweekStatus.UPCOMING, LocalDateTime.now().minusMinutes(1));

        assertThat(guard.isOpenForChanges(stale)).isFalse();
        assertThatThrownBy(() -> guard.assertOpenForChanges(stale, "set a captain"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("deadline for gameweek 3 passed");
    }

    @ParameterizedTest
    @EnumSource(value = GameweekStatus.class, names = {"LOCKED", "IN_PROGRESS", "COMPLETE"})
    void anyNonUpcomingStatusIsClosedEvenWithADeadlineInTheFuture(GameweekStatus status) {
        Gameweek locked = gameweek(status, LocalDateTime.now().plusDays(1));

        assertThat(guard.isOpenForChanges(locked)).isFalse();
        assertThatThrownBy(() -> guard.assertOpenForChanges(locked, "make a transfer"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("is " + status);
    }

    @Test
    void aDeadlineExactlyNowIsClosed() {
        Gameweek onTheLine = gameweek(GameweekStatus.UPCOMING, LocalDateTime.now());

        assertThat(guard.isOpenForChanges(onTheLine)).isFalse();
    }
}
