# 0001: Repositories/services over active record

## Status
Accepted

## Context
The original class diagram gave every entity `save()`, `delete()`, `create()`, and `update()` — an active-record style where the domain object persists itself. Spring Boot's idiomatic persistence model is Spring Data JPA repositories plus service classes, which pulls in the opposite direction from active record. Building the domain model without picking one first would mean re-deciding it, implicitly and inconsistently, on every entity as it's implemented.

## Decision
Entities are plain JPA-mapped domain objects with no persistence methods on them. All reads and writes go through Spring Data repositories (`TeamRepository`, `FantasySquadRepository`, etc.), with business rules that don't fit a single repository call (squad validity, transfer point cost, fixture generation) living in service classes.

Entities keep their *behavioral* methods — `getFantasyPoints(ScoringRule)`, `generateFixtures()`, `getPoints()`, `getTable()` — since those are domain logic, not persistence, and are valuable precisely because they're pure and unit-testable without a database.

## Consequences
- The class diagram in the README omits `save()`/`create()`/`update()`/`delete()` for every entity — they're implied, not drawn.
- Entities stay easy to unit test in isolation; persistence is tested separately via repository/integration tests.
- Contributors used to active-record frameworks (e.g. Rails, Eloquent) need to look in a service class, not the entity, to find where a rule like "squad must have exactly 15 players" is enforced.
