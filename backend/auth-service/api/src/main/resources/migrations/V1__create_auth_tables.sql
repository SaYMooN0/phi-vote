CREATE TABLE app_user
(
    id                UUID PRIMARY KEY,
    unique_name       VARCHAR(30)  NOT NULL UNIQUE,
    email             VARCHAR(320) NOT NULL UNIQUE,
    password_hash     TEXT         NOT NULL,
    registration_date TIMESTAMPTZ  NOT NULL
);

CREATE TABLE refresh_token_session
(
    id         UUID PRIMARY KEY,
    user_id    UUID        NOT NULL REFERENCES app_user (id),
    token_hash TEXT        NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ NULL
);

CREATE TABLE unconfirmed_user
(
    id                 UUID PRIMARY KEY,
    unique_name        VARCHAR(30)  NOT NULL UNIQUE,
    email              VARCHAR(320) NOT NULL UNIQUE,
    password_hash      TEXT         NOT NULL,
    confirmation_token TEXT         NOT NULL UNIQUE
);