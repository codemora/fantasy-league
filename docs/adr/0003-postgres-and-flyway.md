# 0003: Postgres + Flyway, H2 for tests

## Status
Accepted

## Context
`pom.xml` currently has no persistence dependencies at all — no JPA starter, no database driver, no migration tool. The domain model (see README) has enough relational structure (many-to-many joins like `SeasonEntrant`, `SquadPlayer`, `LineupSlot`; unique constraints like `(season_id, position)` on `ScoringRule`) that a relational database is the natural fit, and letting Hibernate auto-generate the schema (`ddl-auto: update`) would make the README diagrams the only real record of what the schema is supposed to be, with silent drift over time.

## Decision
- **Postgres** as the production/dev database.
- **Flyway**-managed SQL migrations (`V1__init.sql`, etc.) as the source of truth for schema, with `spring.jpa.hibernate.ddl-auto: validate` so Hibernate checks the entity mappings against the migrated schema instead of generating it.
- **H2** (in-memory) for the test profile, with the same Flyway migrations applied, so tests run fast without needing a running Postgres instance.

Add to `pom.xml`: `spring-boot-starter-data-jpa`, `postgresql`, `flyway-core`, `spring-boot-starter-validation`, and `com.h2database:h2` (test scope).

## Consequences
- Every schema change is a reviewable, ordered migration file — the README diagrams and the Flyway migrations should describe the same schema; if they diverge, the migrations win.
- `ddl-auto: validate` means a mismatched entity mapping fails fast at startup instead of silently altering the schema.
- Tests run against H2, not Postgres — a behavior that's genuinely Postgres-specific (e.g. a Postgres-only SQL feature) won't be caught by the test suite; none of the current model requires anything Postgres-specific, so this is an acceptable tradeoff for now.
