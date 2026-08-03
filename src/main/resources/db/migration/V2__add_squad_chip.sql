-- #40: WILDCARD, TRIPLE_CAPTAIN, BENCH_BOOST. Both uniques double as the
-- business rules: at most one chip active per squad per gameweek, and each
-- chip type usable at most once per squad, ever.
CREATE TABLE squad_chip (
    squad_chip_id  BIGSERIAL PRIMARY KEY,
    squad_id       BIGINT NOT NULL REFERENCES fantasy_squad (squad_id),
    gameweek_id    BIGINT NOT NULL REFERENCES gameweek (gameweek_id),
    chip_type      VARCHAR(16) NOT NULL,
    activated_at   TIMESTAMP NOT NULL,
    UNIQUE (squad_id, gameweek_id),
    UNIQUE (squad_id, chip_type)
);
