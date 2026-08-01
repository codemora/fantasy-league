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
-int goalsConceded
-int ownGoals
-int penaltiesSaved
-int penaltiesMissed
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
-int pointsPerAppearance60
-int pointsPerAppearance1to59
-int pointsPerGoalsConcededPerThree
-int pointsPerPenaltySave
-int pointsPerPenaltyMiss
-int pointsPerYellowCard
-int pointsPerRedCard
-int pointsPerOwnGoal
+save() bool
}

class FantasySquad{
-int id
-int user_id
-int season_id
-int budget
-List~SquadPlayer~ players
+save() bool
+addPlayer(Player) bool
+removePlayer(Player) bool
+getGameweekPoints(Gameweek) int
+getTotalPoints() int
}

class SquadPlayer{
-int id
-int squad_id
-int player_id
-DateTime addedAt
+save() bool
}

class GameweekLineup{
-int id
-int squad_id
-int gameweek_id
-int captain_player_id
-List~int~ startingPlayerIds
-List~int~ benchPlayerIds
+save() bool
+getPoints() int
}

class Transfer{
-int id
-int squad_id
-int gameweek_id
-int player_out_id
-int player_in_id
-DateTime timestamp
+save() bool
}

class FantasyLeaderboard{
-int season_id
+getRankings() List~FantasySquad~
}

Team "1" --> "many" Player : squad
Fixture "1" --> "many" PlayerPerformance : records
Player "1" --> "many" PlayerPerformance : has
Season "1" --> "many" Gameweek : has
Gameweek "1" --> "many" Fixture : groups
FantasySquad "1" --> "many" SquadPlayer : contains
SquadPlayer "many" --> "1" Player : selects
FantasySquad "many" --> "1" Season : plays_in
FantasySquad "1" --> "many" GameweekLineup : sets
FantasySquad "1" --> "many" Transfer : logs
Gameweek "1" --> "many" GameweekLineup : has
Gameweek "1" --> "many" Transfer : has
```
```mermaid
erDiagram
ADMIN ||--o{ LEAGUE : creates
ADMIN ||--o{ TEAM : creates
ADMIN ||--o{ PLAYER : generates
ADMIN ||--o{ SCORING_RULE : configures
LEAGUE ||--o{ SEASON : has
SEASON ||--o{ TEAM : has
SEASON ||--o{ GAMEWEEK : has
GAMEWEEK ||--o{ FIXTURE : groups
SEASON ||--o{ FIXTURE : has
TEAM ||--o{ FIXTURE : "plays home"
TEAM ||--o{ FIXTURE : "plays away"
TEAM ||--o{ PLAYER : squad
PLAYER ||--o{ PLAYER_PERFORMANCE : has
FIXTURE ||--o{ PLAYER_PERFORMANCE : records
USER ||--o{ FANTASY_SQUAD : owns
SEASON ||--o{ FANTASY_SQUAD : has
FANTASY_SQUAD ||--o{ SQUAD_PLAYER : contains
SQUAD_PLAYER }o--|| PLAYER : selects
FANTASY_SQUAD ||--o{ GAMEWEEK_LINEUP : sets
GAMEWEEK ||--o{ GAMEWEEK_LINEUP : has
FANTASY_SQUAD ||--o{ TRANSFER : logs
GAMEWEEK ||--o{ TRANSFER : has

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
int goals_conceded
int own_goals
int penalties_saved
int penalties_missed
}

SCORING_RULE {
int rule_id
string position
int points_per_goal
int points_per_assist
int points_per_clean_sheet
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
}

GAMEWEEK_LINEUP {
int lineup_id
int squad_id
int gameweek_id
int captain_player_id
}

TRANSFER {
int transfer_id
int squad_id
int gameweek_id
int player_out_id
int player_in_id
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


# Squad Rules

Each fantasy squad has 15 players: 2 goalkeepers, 5 defenders, 5 midfielders, 3 forwards, all within a shared budget. For each gameweek's GameweekLineup, the starting XI is chosen from that squad in a valid formation: exactly 1 GK, 3-5 DEF, 2-5 MID, 1-3 FWD, totaling 11 starters (the remaining 4 are bench and never score, per the Scoring Rules above). One starter is designated captain on the GameweekLineup and their points are doubled for that gameweek.
