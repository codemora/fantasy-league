# Architecture Decision Records

This directory records the significant architecture decisions for the Fantasy League API, using the lightweight ADR format (Context / Decision / Consequences). Each decision gets its own file, numbered sequentially; once accepted, a record is not edited to reflect a later reversal — instead a new ADR supersedes it and both stay in the log.

| # | Title | Status |
|---|---|---|
| [0001](0001-repositories-over-active-record.md) | Repositories/services over active record | Accepted |
| [0002](0002-package-by-feature.md) | Package by feature, not by layer | Accepted |
| [0003](0003-postgres-and-flyway.md) | Postgres + Flyway, H2 for tests | Accepted |
| [0004](0004-money-as-integer-tenths-of-millions.md) | Money as integer tenths-of-millions | Accepted |
| [0005](0005-deterministic-seeded-match-simulation.md) | Deterministic, seeded match simulation | Accepted |
| [0006](0006-points-computed-live-snapshotted-on-completion.md) | Points computed live, snapshotted on gameweek completion | Accepted |
| [0007](0007-season-team-many-to-many.md) | Season/Team as many-to-many via SeasonEntrant | Accepted |
| [0008](0008-jwt-authentication.md) | JWT authentication with revocable refresh tokens | Accepted |
| [0009](0009-rest-api-conventions.md) | REST API conventions (DTOs, ProblemDetail errors, /api/v1, custom pagination envelope) | Accepted |
| [0010](0010-infra-and-deployment.md) | CI, containerization, secrets, and deployment target (GitHub Actions, Buildpacks, Render) | Accepted |
| [0011](0011-testing-strategy.md) | Testing strategy (test pyramid, Testcontainers, Surefire/Failsafe split, coverage report-only) | Accepted |
