CREATE TABLE refresh_token_session (
                                       id uuid PRIMARY KEY,

                                       user_id uuid NOT NULL REFERENCES app_user(id) ON DELETE CASCADE,

                                       token_hash text NOT NULL UNIQUE,

                                       created_at timestamptz NOT NULL,
                                       expires_at timestamptz NOT NULL,

                                       revoked_at timestamptz NULL,
                                       replaced_by_session_id uuid NULL REFERENCES refresh_token_session(id)
);

CREATE INDEX ix_refresh_token_session_user_id
    ON refresh_token_session(user_id);

CREATE INDEX ix_refresh_token_session_active_user_id
    ON refresh_token_session(user_id)
    WHERE revoked_at IS NULL;