-- =========================================================
-- init.sql
-- Provisions TWO separate databases inside the same Postgres
-- instance: identity_db and thoughts_db.
-- Runs once, on first container start, via
-- docker-entrypoint-initdb.d (executed with psql, so \connect
-- meta-commands work here).
-- =========================================================

-- ---------------------------------------------------------
-- 1. Create the two databases
-- ---------------------------------------------------------
CREATE DATABASE identity_db;
CREATE DATABASE thoughts_db;


-- =========================================================
-- 2. identity_db  (owned by the Identity Service)
-- =========================================================
\connect identity_db

CREATE TABLE users (
                       id                BIGINT PRIMARY KEY,
                       username          VARCHAR(60) UNIQUE NOT NULL,
                       email             VARCHAR(256) UNIQUE NOT NULL,
                       password_hash     VARCHAR(256) NOT NULL, -- e.g. BCrypt hash
                       name              VARCHAR(60) NOT NULL,
                       profile_pic_url   TEXT,                  -- points to MinIO
                       bio               VARCHAR(250),
                       created_at        TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE followers (
                           follower_id   BIGINT REFERENCES users(id) ON DELETE CASCADE,
                           following_id  BIGINT REFERENCES users(id) ON DELETE CASCADE,
                           created_at    TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                           PRIMARY KEY (follower_id, following_id)
);

-- Who follows User B? (needed for the fan-out write process)
CREATE INDEX idx_followers_following_id ON followers(following_id);

-- Username lookups
CREATE INDEX idx_users_username ON users(username);


-- =========================================================
-- 3. thoughts_db  (owned by the Thought Service)
-- =========================================================
\connect thoughts_db

-- Denormalized copy of user data so the Thought Service doesn't
-- need a cross-database join to identity_db on every read.
CREATE TABLE users_cache (
                             id                BIGINT PRIMARY KEY,
                             username          VARCHAR(60) NOT NULL,
                             name              VARCHAR(60) NOT NULL,
                             profile_pic_url   TEXT
);

CREATE TABLE thoughts (
                          id                  BIGINT PRIMARY KEY,
                          user_id             BIGINT NOT NULL REFERENCES users_cache(id) ON DELETE CASCADE,
                          content             VARCHAR(512),
                          parent_thought_id   BIGINT REFERENCES thoughts(id) ON DELETE CASCADE,
    -- Denormalized counters to avoid N+1 aggregation on timeline reads
                          likes_count         INT DEFAULT 0,
                          opinions_count      INT DEFAULT 0,
                          reposts_count       INT DEFAULT 0,
                          created_at          TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE likes (
                       user_id      BIGINT REFERENCES users_cache(id) ON DELETE CASCADE,
                       thought_id   BIGINT REFERENCES thoughts(id) ON DELETE CASCADE,
                       created_at   TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                       PRIMARY KEY (user_id, thought_id)
);

CREATE TABLE mentions (
                          user_id      BIGINT REFERENCES users_cache(id) ON DELETE CASCADE,
                          thought_id   BIGINT REFERENCES thoughts(id) ON DELETE CASCADE,
                          created_at   TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                          PRIMARY KEY (user_id, thought_id)
);

CREATE TABLE media (
                       thought_id   BIGINT REFERENCES thoughts(id) ON DELETE CASCADE,
                       media_url    TEXT NOT NULL,
                       created_at   TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                       PRIMARY KEY (thought_id, media_url)
);

-- Fetching nested replies rapidly
CREATE INDEX idx_thoughts_parent_id ON thoughts(parent_thought_id);
-- Fetching a specific user's past thoughts on their profile
CREATE INDEX idx_thoughts_user_id ON thoughts(user_id);