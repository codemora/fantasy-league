# 0007: Season/Team as many-to-many via SeasonEntrant

## Status
Accepted

## Context
The original ER diagram modeled `SEASON ||--o{ TEAM : has` — a one-to-many, meaning each `Team` belongs to exactly one `Season` — but `TEAM` had no `season_id` column in either diagram, so the relationship had no backing foreign key. That also contradicted issue #1's own wording ("create a team so it can participate in leagues **and seasons**", plural), which describes a `Team` as a persistent football club that competes across many seasons over time — the same club entering this year's season and next year's. A one-to-many can't express that; a `Team` can only ever belong to one `Season` under that shape.

## Decision
Model `Season` and `Team` as many-to-many via an explicit join entity, `SeasonEntrant` (`id`, `season_id`, `team_id`, unique on `(season_id, team_id)`), matching the same pattern already used for `SquadPlayer` (FantasySquad↔Player) and `LineupSlot` (GameweekLineup↔Player) rather than a bare M:N line, so it stays consistent with the rest of the diagram's style and leaves room for future per-entrant attributes (e.g. an entry date) without a schema change.

`Season.teams: List<Team>` becomes `Season.entrants: List<SeasonEntrant>`; issue #11 ("add teams to a season") creates a `SeasonEntrant` row, and #45 ("remove a team from a season") deletes one.

## Consequences
- A `Team` created once (#1) can be entered into many seasons over its lifetime, matching real-world football clubs and issue #1's framing.
- Deleting a `Team` (#3) or a `Season` (#43) now needs to check for `SeasonEntrant` rows referencing it, not just direct fixtures/squads — both issues' acceptance criteria already reflect this.
- Slightly more query overhead than a direct FK (`Team.season_id`) would have been — an extra join to find a team's seasons — which is the accepted cost of correctly modeling the many-to-many relationship.
