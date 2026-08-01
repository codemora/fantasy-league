# 0006: Points computed live, snapshotted on gameweek completion

## Status
Accepted

## Context
Fantasy points (`PlayerPerformance.getFantasyPoints(ScoringRule)`, rolled up through `GameweekLineup.getPoints()` to `FantasySquad.getTotalPoints()` and the leaderboard, #33) can either be computed on every read by joining performances against the season's scoring rules, or computed once and stored. Pure on-demand computation is simple and always consistent with the current scoring rules, but the leaderboard (#33) re-derives every user's total across every gameweek on every request, which gets expensive as a season progresses — and once a gameweek is `COMPLETE`, its points shouldn't change anyway (`ScoringRule` is season-scoped and immutable-in-effect for past gameweeks per the README's Scoring Rules section).

## Decision
- While a `Gameweek` is `UPCOMING` or `IN_PROGRESS`, points are computed live from `PlayerPerformance` + the season's `ScoringRule`s — there's nothing to cache yet, since performances are still arriving from the simulation.
- When a `Gameweek` transitions to `COMPLETE` (see the Gameweek Lifecycle in the README and ADR 0005's determinism guarantee), each `GameweekLineup`'s points are computed once and persisted as a snapshot column, rather than re-derived on every read.
- The leaderboard (#33) and season-totals (#32) sum these snapshots for completed gameweeks, and add the live computation only for the current in-progress gameweek if one exists.

## Consequences
- Leaderboard reads become a sum over stored integers instead of a full performance/scoring-rule join across the whole season — this is what keeps #33 cheap as a season progresses.
- A snapshot is a point-in-time cache: if a bug in scoring logic is fixed after a gameweek is `COMPLETE`, the stored snapshot does **not** automatically update — a deliberate backfill/recompute action would be needed, matching the same "history doesn't silently rewrite itself" principle behind season-scoped `ScoringRule` and the deadline lock in #36.
- Adds one write (the snapshot) to the gameweek-completion transition; this is a reasonable amount of complexity given how much it saves on read-heavy leaderboard/points endpoints.
