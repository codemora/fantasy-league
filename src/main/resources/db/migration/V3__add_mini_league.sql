-- #41: private, invite-code-joined mini-leagues with their own leaderboard,
-- scoped to a single season -- same scope as the season-wide leaderboard,
-- just restricted to member squads. Named MiniLeague, not League, since
-- League already means the admin-managed underlying football competition
-- (see README).
CREATE TABLE mini_league (
    mini_league_id      BIGSERIAL PRIMARY KEY,
    season_id            BIGINT NOT NULL REFERENCES season (season_id),
    created_by_user_id   BIGINT NOT NULL REFERENCES app_user (user_id),
    name                 VARCHAR(128) NOT NULL,
    invite_code          VARCHAR(16) NOT NULL UNIQUE,
    created_at           TIMESTAMP NOT NULL
);

CREATE TABLE mini_league_member (
    mini_league_member_id  BIGSERIAL PRIMARY KEY,
    mini_league_id          BIGINT NOT NULL REFERENCES mini_league (mini_league_id),
    user_id                  BIGINT NOT NULL REFERENCES app_user (user_id),
    joined_at                 TIMESTAMP NOT NULL,
    UNIQUE (mini_league_id, user_id)
);
