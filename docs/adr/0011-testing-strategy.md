# 0011: Testing strategy

## Status
Accepted

## Context
No testing conventions existed beyond ADR 0003 fixing H2 for the general test profile. Left undecided, this project would accumulate an ad hoc mix of test styles as each feature is implemented, with no consistent answer to "where does this kind of test live" or "does it run against real Postgres." ADR 0005 (deterministic seeded simulation) already implies golden and statistical tests are needed but never formalized where they live or how they're run without becoming a source of flakiness.

## Decision

**Test pyramid**, each level using the narrowest tool that can verify it:
- **Unit** — JUnit 5 + AssertJ, no Spring context: pure domain logic (`PlayerPerformance.getFantasyPoints()`, `Season.generateFixtures()`, `FantasySquad.isValid()`, the simulation engine). These are fast and dependency-free because ADR 0001 kept behavioral methods pure.
- **Service** — JUnit 5 + Mockito: business rules (transfer budget checks, deadline-lock enforcement, formation validation) tested against mocked repositories.
- **Repository** — `@DataJpaTest` against H2 (ADR 0003): query correctness and DB constraints (`(season_id, position)` on `ScoringRule`, `(season_id, team_id)` on `SeasonEntrant`).
- **Web** — `@WebMvcTest` + `spring-security-test`: controller DTO mapping, `ProblemDetail` error shape (ADR 0009), role-based authorization (ADR 0008) — mocked service layer.
- **Integration** — `@SpringBootTest` + **Testcontainers** running real Postgres: full flows through HTTP (create season → add teams → generate fixtures → simulate → check leaderboard).

**Testcontainers, not H2, for integration tests.** ADR 0010 already runs a Postgres service container in CI specifically because H2 isn't faithful enough to trust for migrations. This extends the same reasoning to the integration suite: Testcontainers gives every developer an ephemeral, real Postgres automatically on `mvn verify`, identical locally and in CI, rather than integration fidelity existing only inside GitHub Actions YAML. Requires Docker as a contributor prerequisite — accepted, since ADR 0010 already required Docker for the Buildpacks-produced container image.

**Maven phase split: Surefire vs Failsafe.** Unit/service/web tests are named `*Test.java` and run via Surefire in the `test` phase, so `mvn test` stays fast for local iteration. Testcontainers-backed integration tests are named `*IT.java` and run via Failsafe in the `verify` phase — the phase CI already runs per ADR 0010.

**Simulation tests are tagged and split by cost.** Golden tests (`simulate(teamA, teamB, seed=42)` asserted against an exact expected result) run in the default suite. Statistical tests (thousands of seeds, asserting aggregate properties like mean goals ≈ 2.7 fall in an expected range) are tagged `@Tag("statistical")` and excluded from the default run — they're inherently slower and carry a small flakiness risk from tolerance-band tightness, so they shouldn't gate every PR.

**Coverage: JaCoCo, report-only, no CI gate.** Coverage is visible in every CI run but doesn't block merges. A hard threshold this early — before most of the codebase exists — incentivizes tests written to hit a number rather than to verify behavior, and penalizes legitimately low-value-to-test code (DTOs, config classes).

**Test data builders** — a small `TestFixtures`/builder utility per aggregate (`Season`, `FantasySquad`, etc.), since building a valid 15-player squad within budget/formation by hand in every test would be tedious and brittle to constraint changes.

**Explicitly deferred:**
- **Consumer-driven contract testing** (e.g. Spring Cloud Contract) — no separate frontend team exists yet to hold a contract against; `@WebMvcTest` + integration tests cover the same ground more cheaply at this stage.
- **Mutation testing** (PIT) — valuable for checking the test suite catches real bugs, not just executes lines, but a later-stage investment once there's a substantial suite to mutate.

## Consequences
- Contributors need Docker locally to run the full `mvn verify` (integration tests), not just to build the deployable image.
- `mvn test` (fast, Surefire-only) vs `mvn verify` (full, includes Failsafe + Testcontainers) becomes the standard local-iteration-vs-full-check split — worth documenting in a contributor-facing README section once one exists.
- Statistical simulation tests need a separate invocation path (e.g. a Maven profile or explicit tag inclusion) if they're ever run on a schedule rather than never — this ADR doesn't fix that cadence, just that they're excluded from the default run.
- No coverage number is enforced, so coverage regressions are visible but not blocking — relies on code review, not CI, to catch under-tested changes.
