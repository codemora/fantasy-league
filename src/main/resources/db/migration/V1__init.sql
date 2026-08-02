-- Schema derived from README.md's class/ER diagrams (docs/adr/0003-postgres-and-flyway.md:
-- this migration, not the diagrams, is the source of truth for the actual schema).
-- "user" is a reserved word in Postgres, hence app_user.

CREATE TABLE app_user (
    user_id       BIGSERIAL PRIMARY KEY,
    username      VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          VARCHAR(16) NOT NULL
);

CREATE TABLE league (
    league_id          BIGSERIAL PRIMARY KEY,
    created_by_user_id BIGINT NOT NULL REFERENCES app_user (user_id),
    name               VARCHAR(128) NOT NULL UNIQUE
);

CREATE TABLE team (
    team_id            BIGSERIAL PRIMARY KEY,
    created_by_user_id BIGINT NOT NULL REFERENCES app_user (user_id),
    team_name          VARCHAR(128) NOT NULL UNIQUE,
    slogan             VARCHAR(255)
);

CREATE TABLE season (
    season_id       BIGSERIAL PRIMARY KEY,
    league_id       BIGINT NOT NULL REFERENCES league (league_id),
    period          VARCHAR(32) NOT NULL,
    team_limit      INT NOT NULL,
    starting_budget INT NOT NULL,
    is_double_leg   BOOLEAN NOT NULL DEFAULT FALSE,
    start_date      DATE,
    end_date        DATE
);

CREATE TABLE season_entrant (
    entrant_id BIGSERIAL PRIMARY KEY,
    season_id  BIGINT NOT NULL REFERENCES season (season_id),
    team_id    BIGINT NOT NULL REFERENCES team (team_id),
    UNIQUE (season_id, team_id)
);

CREATE TABLE gameweek (
    gameweek_id       BIGSERIAL PRIMARY KEY,
    season_id         BIGINT NOT NULL REFERENCES season (season_id),
    number            INT NOT NULL,
    deadline_datetime TIMESTAMP NOT NULL,
    status            VARCHAR(16) NOT NULL DEFAULT 'UPCOMING',
    UNIQUE (season_id, number)
);

CREATE TABLE fixture (
    fixture_id       BIGSERIAL PRIMARY KEY,
    season_id        BIGINT NOT NULL REFERENCES season (season_id),
    gameweek_id      BIGINT NOT NULL REFERENCES gameweek (gameweek_id),
    home_team_id     BIGINT NOT NULL REFERENCES team (team_id),
    away_team_id     BIGINT NOT NULL REFERENCES team (team_id),
    home_team_score  INT,
    away_team_score  INT,
    is_played        BOOLEAN NOT NULL DEFAULT FALSE,
    start_date_time  TIMESTAMP NOT NULL,
    simulation_seed  BIGINT NOT NULL,
    CHECK (home_team_id <> away_team_id)
);

CREATE TABLE player (
    player_id          BIGSERIAL PRIMARY KEY,
    team_id            BIGINT NOT NULL REFERENCES team (team_id),
    created_by_user_id BIGINT NOT NULL REFERENCES app_user (user_id),
    name               VARCHAR(128) NOT NULL,
    position           VARCHAR(16) NOT NULL,
    market_value       INT NOT NULL
);

CREATE TABLE player_performance (
    performance_id    BIGSERIAL PRIMARY KEY,
    player_id         BIGINT NOT NULL REFERENCES player (player_id),
    fixture_id        BIGINT NOT NULL REFERENCES fixture (fixture_id),
    goals             INT NOT NULL DEFAULT 0,
    assists           INT NOT NULL DEFAULT 0,
    minutes_played    INT NOT NULL DEFAULT 0,
    clean_sheet       BOOLEAN NOT NULL DEFAULT FALSE,
    goals_conceded    INT NOT NULL DEFAULT 0,
    own_goals         INT NOT NULL DEFAULT 0,
    penalties_saved   INT NOT NULL DEFAULT 0,
    penalties_missed  INT NOT NULL DEFAULT 0,
    yellow_cards      INT NOT NULL DEFAULT 0,
    red_cards         INT NOT NULL DEFAULT 0,
    UNIQUE (player_id, fixture_id)
);

CREATE TABLE scoring_rule (
    rule_id                            BIGSERIAL PRIMARY KEY,
    season_id                          BIGINT NOT NULL REFERENCES season (season_id),
    created_by_user_id                 BIGINT NOT NULL REFERENCES app_user (user_id),
    position                           VARCHAR(16) NOT NULL,
    points_per_goal                    INT NOT NULL,
    points_per_assist                  INT NOT NULL,
    points_per_clean_sheet             INT NOT NULL,
    points_per_appearance_60           INT NOT NULL,
    points_per_appearance_1to59        INT NOT NULL,
    points_per_goals_conceded_per_three INT NOT NULL,
    points_per_penalty_save            INT NOT NULL,
    points_per_penalty_miss            INT NOT NULL,
    points_per_yellow_card             INT NOT NULL,
    points_per_red_card                INT NOT NULL,
    points_per_own_goal                INT NOT NULL,
    UNIQUE (season_id, position)
);

CREATE TABLE fantasy_squad (
    squad_id       BIGSERIAL PRIMARY KEY,
    user_id        BIGINT NOT NULL REFERENCES app_user (user_id),
    season_id      BIGINT NOT NULL REFERENCES season (season_id),
    bank_balance   INT NOT NULL,
    free_transfers INT NOT NULL DEFAULT 1,
    UNIQUE (user_id, season_id)
);

CREATE TABLE squad_player (
    squad_player_id BIGSERIAL PRIMARY KEY,
    squad_id        BIGINT NOT NULL REFERENCES fantasy_squad (squad_id),
    player_id       BIGINT NOT NULL REFERENCES player (player_id),
    purchase_price  INT NOT NULL,
    added_at        TIMESTAMP NOT NULL,
    UNIQUE (squad_id, player_id)
);

CREATE TABLE gameweek_lineup (
    lineup_id          BIGSERIAL PRIMARY KEY,
    squad_id           BIGINT NOT NULL REFERENCES fantasy_squad (squad_id),
    gameweek_id        BIGINT NOT NULL REFERENCES gameweek (gameweek_id),
    captain_player_id  BIGINT NOT NULL REFERENCES player (player_id),
    UNIQUE (squad_id, gameweek_id)
);

CREATE TABLE lineup_slot (
    slot_id      BIGSERIAL PRIMARY KEY,
    lineup_id    BIGINT NOT NULL REFERENCES gameweek_lineup (lineup_id),
    player_id    BIGINT NOT NULL REFERENCES player (player_id),
    role         VARCHAR(16) NOT NULL,
    bench_order  INT,
    UNIQUE (lineup_id, player_id)
);

CREATE TABLE transfer (
    transfer_id    BIGSERIAL PRIMARY KEY,
    squad_id       BIGINT NOT NULL REFERENCES fantasy_squad (squad_id),
    gameweek_id    BIGINT NOT NULL REFERENCES gameweek (gameweek_id),
    player_out_id  BIGINT NOT NULL REFERENCES player (player_id),
    player_in_id   BIGINT NOT NULL REFERENCES player (player_id),
    points_cost    INT NOT NULL DEFAULT 0,
    timestamp      TIMESTAMP NOT NULL
);

-- Not part of the domain model in README.md -- purely an auth implementation
-- detail supporting ADR 0008 (JWT access tokens + revocable refresh tokens).
CREATE TABLE refresh_token (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES app_user (user_id),
    token_hash  VARCHAR(255) NOT NULL UNIQUE,
    expires_at  TIMESTAMP NOT NULL,
    revoked_at  TIMESTAMP
);
