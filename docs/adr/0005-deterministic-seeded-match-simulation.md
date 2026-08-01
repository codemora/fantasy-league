# 0005: Deterministic, seeded match simulation

## Status
Accepted

## Context
Player performances in this system are simulated, not real (see README "Match Simulation"). The simulation engine (team/player ratings feeding a Poisson-distributed goal count per side, with goal/assist/card attribution weighted by position) needs to be built and tested before any fantasy-scoring feature can be meaningfully verified end to end. If the engine calls `Math.random()` or any other ambient random source directly, a simulated fixture becomes a one-off: it can't be re-run to check a bug, can't be golden-tested (assert on an exact expected output), and can't be reproduced if a user disputes a result.

## Decision
- The simulation engine is a pure function: `simulate(homeTeam, awayTeam, seed) -> (scoreline, List<PlayerPerformance>)`. No Spring, no JPA, no wall-clock, no ambient randomness — it lives in the `simulation` package (see ADR 0002) with no dependency on persistence.
- Every random draw goes through a single injected `RandomGenerator` seeded from that one `seed` argument; `Math.random()` is never called anywhere in the simulation code.
- `Fixture.simulationSeed` (a `long`) is generated once when the fixture is created and persisted, so `simulate(fixture)` can be re-run at any time and produce a byte-identical result.

## Decision drivers
- **Golden tests**: `simulate(teamA, teamB, seed=42)` can be asserted to produce an exact scoreline and performance list — the strongest kind of test available for a stochastic system.
- **Statistical tests**: running the simulation thousands of times with varying seeds and checking aggregate properties (mean goals ≈ 2.7, home win rate ≈ 45%) catches calibration drift that a single golden test can't.
- **Debuggability**: if a simulated fixture produces an implausible result, it can be re-run in isolation with the same seed rather than being an unreproducible one-off.

## Consequences
- #21 (generate/simulate results) and #26 (generate player performance stats) both depend on this — their acceptance criteria explicitly require the seeded, deterministic approach and reproducibility.
- Any future change to the simulation algorithm (e.g. rebalancing scoring rates) will change historical results if fixtures are ever re-simulated with a stored seed against new code — this is expected and matches how the rest of the model treats historical data as immutable once a gameweek is `COMPLETE` (see ADR 0006 and the Gameweek Lifecycle section of the README).
