# Fantasy League
This is an API for a fantasy league. Admins run club leagues, seasons, and fixtures; users draft simulated players onto a fantasy squad and score points based on those players' simulated match performances.

# Class Diagram

```mermaid
classDiagram

class Team{
-int id
-String name
-String slogan
-create() bool
-update() bool
+save() bool
+delete() bool
}

class League{
-int id
-String name
-create() bool
-update() bool
+save() bool
+delete() bool
}

class Season{
-int id
-String period
-int league_id
-int team_limit
-List~Team~ teams
-List~Fixture~ fixtures
-List~Gameweek~ gameweeks
-bool isDoubleLeg
-Date startDate
-Date endDate
-create() bool
-update() bool
+save() bool
+delete() bool
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
-create() bool
-update() bool
+save() bool
+getScores() String
+getString() String
+getWinner() Team
}

class LeagueTableRow{
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
-bool isComplete
+save() bool
+isComplete() bool
}

class Player{
-int id
-int team_id
-String name
-String position
-int value
-create() bool
-update() bool
+save() bool
+delete() bool
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
-int yellowCards
-int redCards
+save() bool
+getFantasyPoints(ScoringRule) int
}

class ScoringRule{
-int id
-String position
-int pointsPerGoal
-int pointsPerAssist
-int pointsPerCleanSheet
-int pointsPerYellowCard
-int pointsPerRedCard
+save() bool
}

class FantasySquad{
-int id
-int user_id
-int season_id
-int budget
-int captain_player_id
-List~SquadPlayer~ players
+save() bool
+addPlayer(Player) bool
+removePlayer(Player) bool
+setCaptain(Player) bool
+getGameweekPoints(Gameweek) int
+getTotalPoints() int
}

class SquadPlayer{
-int id
-int squad_id
-int player_id
-bool isCaptain
-bool isStarting
+save() bool
}

class FantasyLeaderboard{
-int season_id
+getRankings() List~FantasySquad~
}

Team "1" --> "many" Player : squad
Fixture "many" --> "many" PlayerPerformance : records
Player "1" --> "many" PlayerPerformance : has
Season "1" --> "many" Gameweek : has
Gameweek "1" --> "many" Fixture : groups
FantasySquad "1" --> "many" SquadPlayer : contains
SquadPlayer "many" --> "1" Player : selects
FantasySquad "many" --> "1" Season : plays_in
```
```mermaid
erDiagram
ADMIN ||--|| LEAGUE : creates
LEAGUE ||--o{ SEASON : has
SEASON ||--o{ TEAM : has
SEASON ||--o{ GAMEWEEK : has
GAMEWEEK ||--o{ FIXTURE : groups
SEASON ||--o{ FIXTURE : has
USER ||--o| TEAM : creates
TEAM ||--|| FIXTURE : participates
TEAM ||--o{ PLAYER : squad
PLAYER ||--o{ PLAYER_PERFORMANCE : has
FIXTURE ||--o{ PLAYER_PERFORMANCE : records
USER ||--o{ FANTASY_SQUAD : owns
SEASON ||--o{ FANTASY_SQUAD : has
FANTASY_SQUAD ||--o{ SQUAD_PLAYER : contains
SQUAD_PLAYER }o--|| PLAYER : selects

ADMIN{
int admin_id
string name
}

LEAGUE {
int league_id
string name
}

SEASON {
int season_id
int year
}

GAMEWEEK {
int gameweek_id
int season_id
int number
}

TEAM {
int team_id
string team_name
int user_id
}

FIXTURE {
int fixture_id
int season_id
int gameweek_id
int home_team_id
int away_team_id
date match_date
}

USER {
int user_id
string username
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
}

FANTASY_SQUAD {
int squad_id
int user_id
int season_id
int budget
}

SQUAD_PLAYER {
int squad_player_id
int squad_id
int player_id
boolean is_captain
boolean is_starting
}
```


# Scoring Rules

Default fantasy points, weighted by position. Bench players never score; a starting player who doesn't feature scores 0 (no auto-substitutions in v1).

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
