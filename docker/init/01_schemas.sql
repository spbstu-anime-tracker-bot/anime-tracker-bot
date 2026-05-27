CREATE SCHEMA IF NOT EXISTS anime;
CREATE SCHEMA IF NOT EXISTS "user";


CREATE TABLE IF NOT EXISTS anime.genre (
    id   BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS anime.anime (
    id              BIGINT PRIMARY KEY,
    title           VARCHAR(500),
    title_english   VARCHAR(500),
    type            VARCHAR(50),
    episodes        INTEGER,
    status          VARCHAR(100),
    start_date      DATE,
    end_date        DATE,
    year            INTEGER,
    score           DOUBLE PRECISION,
    scored_by       BIGINT,
    popularity_rank INTEGER,
    synopsis        TEXT
);

CREATE TABLE IF NOT EXISTS anime."animeGenres" (
    id       BIGSERIAL PRIMARY KEY,
    anime_id BIGINT NOT NULL REFERENCES anime.anime (id) ON DELETE CASCADE,
    genre_id BIGINT NOT NULL REFERENCES anime.genre (id) ON DELETE CASCADE
);


CREATE TABLE IF NOT EXISTS "user"."user" (
    id   BIGINT PRIMARY KEY,
    name VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS "user"."listViewed" (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT    NOT NULL REFERENCES "user"."user" (id) ON DELETE CASCADE,
    anime_id   BIGINT    NOT NULL REFERENCES anime.anime (id) ON DELETE CASCADE,
    user_score INTEGER,
    added_at   TIMESTAMP DEFAULT NOW(),
    CONSTRAINT uq_list_viewed UNIQUE (user_id, anime_id)
);

CREATE TABLE IF NOT EXISTS "user"."listToView" (
    id       BIGSERIAL PRIMARY KEY,
    user_id  BIGINT    NOT NULL REFERENCES "user"."user" (id) ON DELETE CASCADE,
    anime_id BIGINT    NOT NULL REFERENCES anime.anime (id) ON DELETE CASCADE,
    added_at TIMESTAMP DEFAULT NOW(),
    CONSTRAINT uq_list_to_view UNIQUE (user_id, anime_id)
);

CREATE TABLE IF NOT EXISTS "user"."recommendationRequests" (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES "user"."user" (id) ON DELETE CASCADE,
    status     VARCHAR(50)  NOT NULL DEFAULT 'CREATED',
    created_at TIMESTAMP             DEFAULT NOW(),
    updated_at TIMESTAMP             DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS "user"."recommendationCache" (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT    NOT NULL REFERENCES "user"."user" (id) ON DELETE CASCADE,
    request_id    BIGINT REFERENCES "user"."recommendationRequests" (id) ON DELETE SET NULL,
    anime_id      BIGINT    NOT NULL REFERENCES anime.anime (id) ON DELETE CASCADE,
    rank_position INTEGER,
    generated_at  TIMESTAMP DEFAULT NOW()
);