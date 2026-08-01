# 0002: Package by feature, not by layer

## Status
Accepted

## Context
Two conventional ways to organize a Spring Boot codebase: layer-first (`controllers/`, `services/`, `repositories/`, `entities/`, each holding every feature's classes) or feature-first (`league/`, `season/`, `fantasy/`, each holding its own controller, service, repository, and entity). The domain here spans several fairly independent slices — league/season/fixture administration, the simulation engine, and the fantasy squad/lineup/transfer layer — that mostly don't need to reach into each other's internals.

## Decision
Package by feature:

```
com.codemora.fantasy_league
├── league/ season/ team/ player/     (entity, repository, service, controller each)
├── fixture/ performance/ scoring/
├── fantasy/        squads, lineups, transfers
├── simulation/     no JPA dependency at all
└── common/         enums, errors, money
```

The `simulation` package in particular has no JPA dependency — it's a pure function of `(teams, ratings, seed) -> (scoreline, performances)`, called by a service in `fixture` or `performance` that handles persistence.

## Consequences
- A change to one feature (e.g. transfer rules) touches one package, not five.
- Cross-feature dependencies (e.g. `fantasy` depending on `player` and `fixture`) are visible as package imports rather than hidden inside a shared `services/` folder.
- Layer-first conventions some contributors expect (e.g. "all controllers live in `controllers/`") don't apply here — navigating by feature name, not by class suffix, is the expected mental model.
