# 0009: REST API conventions

## Status
Accepted

## Context
No controller code exists yet — this decision fixes the API's shape before implementation starts, so every endpoint added afterward follows the same conventions instead of each one reinventing pagination, errors, and versioning independently.

## Decision

**Resource design.** Nest a resource under its parent only where creation needs that parent for context or the JWT determines "mine" (`/leagues/{id}/seasons`, `/seasons/{id}/entrants`, `/fantasy-squads/me`); keep anything addressable by its own ID flat (`/fixtures/{id}`, `/players/{id}`). Captain selection (#30) is not a separate endpoint — it's part of the lineup-submission payload (#37), since `captain_player_id` and the `LineupSlot`s live on the same `GameweekLineup` and a lineup without a captain shouldn't be a valid intermediate state.

**DTOs, never entities, on the wire.** Request/response shapes are Java records, mapped to/from entities at the controller boundary. This avoids leaking columns like `created_by_user_id` to non-admin clients, avoids Hibernate-proxy/Jackson lazy-loading serialization failures, and keeps the API contract independent of schema changes (e.g. a future column rename doesn't have to be a breaking API change).

**Errors: RFC 7807 `ProblemDetail`.** Spring Boot 3 / Spring 6 support this natively (`org.springframework.http.ProblemDetail`, no extra dependency). A `@ControllerAdvice` maps domain exceptions (squad invalid, gameweek locked, budget exceeded) to `application/problem+json` responses with `type`, `title`, `status`, `detail`, and a `errors` extension member for field-level validation failures. This is what actually implements the "clear error (e.g. 409)" requirement that #13, #29, #31, #36 all specify without defining a shape.

**Validation split.** Bean Validation (`spring-boot-starter-validation`) on request DTOs for structural checks (`@NotBlank`, `@Size`, `@Min`). Business rules that need a DB lookup or cross-field logic — squad formation (2/5/5/3), max-3-per-club, budget, the gameweek deadline lock — live in the service layer and throw typed exceptions, not validation annotations.

**Versioning: URI prefix.** All endpoints under `/api/v1/...`. Cheapest form of future-proofing; no content-negotiation complexity, appropriate given there's no imminent v2 *API* (v2 *features* — chips, mini-leagues, auto-subs — are tracked separately in #39–41 and don't require a new API version on their own).

**Pagination: custom envelope**, not Spring Data's `Page<T>` serialized directly:

```json
{
  "content": [ ... ],
  "page": 0,
  "size": 20,
  "totalElements": 137,
  "totalPages": 7
}
```

Implemented as a generic `PageResponse<T>` record with a `PageResponse.from(Page<T>)` factory. List endpoints (teams, players, fixtures, leaderboard) still accept a Spring Data `Pageable` on the *input* side (`?page=0&size=20&sort=name,asc` via `@PageableDefault`) — only the output is remapped, so query-param handling doesn't need to be reimplemented.

## Consequences
- Every list endpoint returns the same four-field envelope; a client only needs to learn the pagination shape once.
- `PageResponse<T>` is a small amount of code to own, but decouples the API contract from Spring Data's internal `Page<T>` JSON shape (which includes Spring-specific fields like `pageable` and `sort` metadata that aren't meaningful to expose long-term).
- The DTO layer means every entity needs at least one corresponding response DTO before its endpoint can be built — more upfront boilerplate per resource, in exchange for a stable, intentional public contract.
- `ProblemDetail` responses need every domain exception (squad invalid, budget exceeded, gameweek locked, etc.) mapped once in a central `@ControllerAdvice`, rather than each controller handling its own errors ad hoc.
