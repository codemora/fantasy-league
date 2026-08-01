# Fantasy League
This is an API for a fantasy league. Admins run club leagues, seasons, and fixtures; users draft simulated players onto a fantasy squad and score points based on those players' simulated match performances.

> Note: `League` here means the underlying football competition (admin-managed). Private user-vs-user mini-leagues are a planned feature and will need a distinct name (e.g. `MiniLeague`) to avoid colliding with this entity.

# Class Diagram

Persistence methods (`save()`/`create()`/`update()`/`delete()`) are omitted below — they're implied for every entity. Only behavioral methods are shown.

```mermaid
classDiagram

class Team{
-int id
-String name
-String slogan
}

class League{
-int id
-String name
}

class Season{
-int id
-String period
-int league_id
-int team_limit
-int startingBudget
-List~Team~ teams
-List~Fixture~ fixtures
-List~Gameweek~ gameweeks
-bool isDoubleLeg
-Date startDate
-Date endDate
+getTable() List~LeagueTableRow~
+generateFixtures() String
+getWinner() Team
+getTopFourTeams() List~Teams~
+getRelegationTeams() List~Teams~
}

class Fixture{
-int id
-int season_id
-int gameweek_id
-int home_team_id
-int away_team_id
-int? home_team_score
-int? away_team_score
-bool isPlayed
-DateTime startDateTime
-long simulationSeed
+getScores() String
+getString() String
+getWinner() Team
}

class LeagueTableRow{
<<projection>>
-String teamName
-int matchesPlayed
-int wins
-int draws
-int losses
-int goalsFor
-int goalsAgainst
-int goalDifference
-int points
}

class Gameweek{
-int id
-int season_id
-int number
-List~Fixture~ fixtures
-DateTime deadlineDateTime
-GameweekStatus status
+isLocked() bool
}

class GameweekStatus{
<<enumeration>>
UPCOMING
LOCKED
IN_PROGRESS
COMPLETE
}

class Player{
-int id
-int team_id
-String name
-String position
-int value
+getSeasonStats() List~PlayerPerformance~
}

class PlayerPerformance{
-int id
-int player_id
-int fixture_id
-int goals
-int assists
-int minutesPlayed
-bool cleanSheet
-int goalsConceded
-int ownGoals
-int penaltiesSaved
-int penaltiesMissed
-int yellowCards
-int redCards
+getFantasyPoints(ScoringRule) int
}

class ScoringRule{
-int id
-int season_id
-String position
-int pointsPerGoal
-int pointsPerAssist
-int pointsPerCleanSheet
-int pointsPerAppearance60
-int pointsPerAppearance1to59
-int pointsPerGoalsConcededPerThree
-int pointsPerPenaltySave
-int pointsPerPenaltyMiss
-int pointsPerYellowCard
-int pointsPerRedCard
-int pointsPerOwnGoal
}

class FantasySquad{
-int id
-int user_id
-int season_id
-int bankBalance
-int freeTransfers
-List~SquadPlayer~ players
+addPlayer(Player) bool
+removePlayer(Player) bool
+isValid() bool
+getGameweekPoints(Gameweek) int
+getTotalPoints() int
}

class SquadPlayer{
-int id
-int squad_id
-int player_id
-int purchasePrice
-DateTime addedAt
}

class GameweekLineup{
-int id
-int squad_id
-int gameweek_id
-int captain_player_id
-List~LineupSlot~ slots
+getPoints() int
}

class LineupSlot{
-int id
-int lineup_id
-int player_id
-LineupRole role
-int benchOrder
}

class LineupRole{
<<enumeration>>
STARTER
BENCH
}

class Transfer{
-int id
-int squad_id
-int gameweek_id
-int player_out_id
-int player_in_id
-int pointsCost
-DateTime timestamp
}

class FantasyLeaderboard{
<<projection>>
-int season_id
+getRankings() List~FantasySquad~
}

class User{
-int id
-String username
-Role role
}

class Role{
<<enumeration>>
ADMIN
USER
}

Team "1" --> "many" Player : squad
Fixture "1" --> "many" PlayerPerformance : records
Player "1" --> "many" PlayerPerformance : has
Season "1" --> "many" Gameweek : has
Season "1" --> "many" ScoringRule : configures
Gameweek "1" --> "many" Fixture : groups
User "1" --> "many" FantasySquad : owns
FantasySquad "1" --> "many" SquadPlayer : contains
SquadPlayer "many" --> "1" Player : selects
FantasySquad "many" --> "1" Season : plays_in
FantasySquad "1" --> "many" GameweekLineup : sets
FantasySquad "1" --> "many" Transfer : logs
Gameweek "1" --> "many" GameweekLineup : has
Gameweek "1" --> "many" Transfer : has
GameweekLineup "1" --> "many" LineupSlot : has
LineupSlot "many" --> "1" Player : selects
```
`ADMIN` and `USER` are merged into a single `USER` entity with a `role`, since both are just accounts distinguished by permissions.

```mermaid
erDiagram
USER ||--o{ LEAGUE : creates
USER ||--o{ TEAM : creates
USER ||--o{ PLAYER : generates
USER ||--o{ SCORING_RULE : configures
USER ||--o{ FANTASY_SQUAD : owns
LEAGUE ||--o{ SEASON : has
SEASON ||--o{ TEAM : has
SEASON ||--o{ GAMEWEEK : has
SEASON ||--o{ SCORING_RULE : configures
GAMEWEEK ||--o{ FIXTURE : groups
SEASON ||--o{ FIXTURE : has
TEAM ||--o{ FIXTURE : "plays home"
TEAM ||--o{ FIXTURE : "plays away"
TEAM ||--o{ PLAYER : squad
PLAYER ||--o{ PLAYER_PERFORMANCE : has
FIXTURE ||--o{ PLAYER_PERFORMANCE : records
SEASON ||--o{ FANTASY_SQUAD : has
FANTASY_SQUAD ||--o{ SQUAD_PLAYER : contains
SQUAD_PLAYER }o--|| PLAYER : selects
FANTASY_SQUAD ||--o{ GAMEWEEK_LINEUP : sets
GAMEWEEK ||--o{ GAMEWEEK_LINEUP : has
GAMEWEEK_LINEUP ||--o{ LINEUP_SLOT : has
LINEUP_SLOT }o--|| PLAYER : selects
FANTASY_SQUAD ||--o{ TRANSFER : logs
GAMEWEEK ||--o{ TRANSFER : has

USER {
int user_id
string username
string role
}

LEAGUE {
int league_id
string name
}

SEASON {
int season_id
int year
int starting_budget
}

GAMEWEEK {
int gameweek_id
int season_id
int number
datetime deadline_datetime
string status
}

TEAM {
int team_id
string team_name
}

FIXTURE {
int fixture_id
int season_id
int gameweek_id
int home_team_id
int away_team_id
date match_date
long simulation_seed
}

PLAYER {
int player_id
int team_id
string name
string position
int value
}

PLAYER_PERFORMANCE {
int performance_id
int player_id
int fixture_id
int goals
int assists
boolean clean_sheet
int goals_conceded
int own_goals
int penalties_saved
int penalties_missed
}

SCORING_RULE {
int rule_id
int season_id
string position
int points_per_goal
int points_per_assist
int points_per_clean_sheet
}

FANTASY_SQUAD {
int squad_id
int user_id
int season_id
int bank_balance
int free_transfers
}

SQUAD_PLAYER {
int squad_player_id
int squad_id
int player_id
int purchase_price
}

GAMEWEEK_LINEUP {
int lineup_id
int squad_id
int gameweek_id
int captain_player_id
}

LINEUP_SLOT {
int slot_id
int lineup_id
int player_id
string role
int bench_order
}

TRANSFER {
int transfer_id
int squad_id
int gameweek_id
int player_out_id
int player_in_id
int points_cost
}
```

**Constraints not expressible in the diagram:** `PLAYER_PERFORMANCE` is unique on `(player_id, fixture_id)`; `SCORING_RULE` is unique on `(season_id, position)`.

# Scoring Rules

Scoring rules are configured per season (one `ScoringRule` row per position, per season), so changing them never rewrites points for gameweeks that already happened. The table below is the default set a new season is seeded with. Bench players never score; a starting player who doesn't feature scores 0 (no auto-substitutions in v1).

| Stat | GK | DEF | MID | FWD |
|---|---|---|---|---|
| Goal | 10 | 6 | 5 | 4 |
| Assist | 3 | 3 | 3 | 3 |
| Clean sheet | 4 | 4 | 1 | 0 |
| Played 60+ min | 2 | 2 | 2 | 2 |
| Played 1-59 min | 1 | 1 | 1 | 1 |
| Every 3 goals conceded | -1 | -1 | - | - |
| Penalty save | 5 | - | - | - |
| Penalty miss | -2 | -2 | -2 | -2 |
| Yellow card | -1 | -1 | -1 | -1 |
| Red card | -3 | -3 | -3 | -3 |
| Own goal | -2 | -2 | -2 | -2 |

The captain's total points for the gameweek are doubled.


# Squad Rules

Each fantasy squad has 15 players: 2 goalkeepers, 5 defenders, 5 midfielders, 3 forwards, all within the season's starting budget, with a maximum of 3 players from any one real team. Each `SquadPlayer` records the price paid (`purchasePrice`) so a squad's value stays accurate even if player prices change in a future version; a squad's remaining funds are tracked as `bankBalance`.

For each gameweek's `GameweekLineup`, the starting XI is chosen from that squad in a valid formation via `LineupSlot` rows: exactly 1 GK, 3-5 DEF, 2-5 MID, 1-3 FWD, totaling 11 starters (the remaining 4 are bench and never score, per the Scoring Rules above). One starter is designated captain on the `GameweekLineup` and their points are doubled for that gameweek.

Transfers between gameweeks swap a `SquadPlayer` in exchange for another within the remaining budget. Each squad gets 1 free transfer per gameweek (accruing up to a maximum of 2 banked), and each additional transfer beyond the free allowance costs 4 points, recorded as `pointsCost` on the `Transfer`.

# Gameweek Lifecycle

Each `Gameweek` has a `deadlineDateTime` and moves through `UPCOMING` → `LOCKED` → `IN_PROGRESS` → `COMPLETE`. Squad selection, lineup changes, and transfers are only permitted while a gameweek is `UPCOMING`; once its deadline passes it locks, preventing changes based on information from matches already in progress. Points become official when the gameweek reaches `COMPLETE`.

# Match Simulation

Player performances are generated, not real: each `Fixture` is simulated from team/player ratings using a Poisson-distributed goal count per side, with a seeded, deterministic random generator. The `simulationSeed` stored on `Fixture` means a simulation can be re-run to produce an identical result, which keeps the engine testable (fixed-seed golden tests) and debuggable (no simulation is ever a one-off you can't reproduce).
