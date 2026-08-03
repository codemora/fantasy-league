package com.codemora.fantasy_league.points;

import static com.codemora.fantasy_league.common.Position.DEF;
import static com.codemora.fantasy_league.common.Position.FWD;
import static com.codemora.fantasy_league.common.Position.GK;
import static com.codemora.fantasy_league.common.Position.MID;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.codemora.fantasy_league.points.BenchSubstitutionResolver.Participant;
import com.codemora.fantasy_league.points.BenchSubstitutionResolver.Result;

class BenchSubstitutionResolverTest {

    private final BenchSubstitutionResolver resolver = new BenchSubstitutionResolver();

    @Test
    void noSubstitutionsWhenEveryStarterPlayed() {
        List<Participant> starters = List.of(new Participant(1L, GK, true), new Participant(2L, DEF, true));
        List<Participant> bench = List.of(new Participant(3L, GK, true), new Participant(4L, DEF, true));

        Result result = resolver.resolve(starters, bench);

        assertThat(result.subbedOutPlayerIds()).isEmpty();
        assertThat(result.subbedInPlayerIds()).isEmpty();
    }

    @Test
    void substitutesTheReserveGoalkeeperForANonPlayingStartingGoalkeeper() {
        List<Participant> starters = List.of(new Participant(1L, GK, false), new Participant(2L, DEF, true));
        List<Participant> bench = List.of(new Participant(3L, GK, true), new Participant(4L, FWD, true));

        Result result = resolver.resolve(starters, bench);

        assertThat(result.subbedOutPlayerIds()).containsExactly(1L);
        assertThat(result.subbedInPlayerIds()).containsExactly(3L);
    }

    @Test
    void outfieldBenchNeverSubstitutesTheGoalkeeper() {
        List<Participant> starters = List.of(
                new Participant(1L, GK, false), new Participant(2L, DEF, true),
                new Participant(3L, DEF, true), new Participant(4L, DEF, true));
        List<Participant> bench = List.of(new Participant(5L, FWD, true)); // no reserve GK on the bench

        Result result = resolver.resolve(starters, bench);

        assertThat(result.subbedOutPlayerIds()).isEmpty();
        assertThat(result.subbedInPlayerIds()).isEmpty();
    }

    @Test
    void substitutesInBenchOrderSkippingBenchPlayersWhoAlsoDidNotPlay() {
        List<Participant> starters = List.of(new Participant(1L, GK, true), new Participant(2L, FWD, false));
        // bench order: MID first (didn't play, skipped), FWD second (played, comes on)
        List<Participant> bench = List.of(new Participant(3L, MID, false), new Participant(4L, FWD, true));

        Result result = resolver.resolve(starters, bench);

        assertThat(result.subbedOutPlayerIds()).containsExactly(2L);
        assertThat(result.subbedInPlayerIds()).containsExactly(4L);
    }

    @Test
    void doesNotSubstituteWhenItWouldDropAPositionBelowItsMinimum() {
        // DEF minimum is 3; this XI already sits at exactly 3, so the one who
        // didn't play can't be swapped out for a non-DEF bench player.
        List<Participant> starters = List.of(
                new Participant(1L, GK, true), new Participant(2L, DEF, false),
                new Participant(3L, DEF, true), new Participant(4L, DEF, true));
        List<Participant> bench = List.of(new Participant(5L, FWD, true));

        Result result = resolver.resolve(starters, bench);

        assertThat(result.subbedOutPlayerIds()).isEmpty();
        assertThat(result.subbedInPlayerIds()).isEmpty();
    }

    @Test
    void substitutesWithinTheSamePositionEvenAtTheMinimum() {
        List<Participant> starters = List.of(
                new Participant(1L, GK, true), new Participant(2L, DEF, false),
                new Participant(3L, DEF, true), new Participant(4L, DEF, true));
        List<Participant> bench = List.of(new Participant(5L, DEF, true));

        Result result = resolver.resolve(starters, bench);

        assertThat(result.subbedOutPlayerIds()).containsExactly(2L);
        assertThat(result.subbedInPlayerIds()).containsExactly(5L);
    }

    @Test
    void doesNotSubstituteWhenItWouldExceedAPositionsMaximum() {
        // FWD maximum is 3; the starting XI already has 3 FWD, so a bench FWD
        // can't come on for a non-playing MID (that would make 4 FWD).
        List<Participant> starters = List.of(
                new Participant(1L, GK, true), new Participant(2L, MID, false),
                new Participant(3L, MID, true), new Participant(4L, MID, true),
                new Participant(5L, FWD, true), new Participant(6L, FWD, true), new Participant(7L, FWD, true));
        List<Participant> bench = List.of(new Participant(8L, FWD, true));

        Result result = resolver.resolve(starters, bench);

        assertThat(result.subbedOutPlayerIds()).isEmpty();
        assertThat(result.subbedInPlayerIds()).isEmpty();
    }

    @Test
    void triesTheNextNonPlayingStarterWhenTheFirstCandidateSwapIsInvalid() {
        // A bench MID can't replace the non-playing DEF (already at the DEF
        // minimum), but can replace the non-playing MID starter instead.
        List<Participant> starters = List.of(
                new Participant(1L, GK, true), new Participant(2L, DEF, false),
                new Participant(3L, DEF, true), new Participant(4L, DEF, true), new Participant(5L, MID, false));
        List<Participant> bench = List.of(new Participant(6L, MID, true));

        Result result = resolver.resolve(starters, bench);

        assertThat(result.subbedOutPlayerIds()).containsExactly(5L);
        assertThat(result.subbedInPlayerIds()).containsExactly(6L);
    }
}
