-- Fast lookup by refresh token hash during token refresh.
-- Also protects against accidental duplicate token hashes.
CREATE UNIQUE INDEX refresh_token_session_token_hash_uq
    ON refresh_token_session (token_hash);

-- Fast lookup of all sessions belonging to a user.
CREATE INDEX refresh_token_session_user_id_idx
    ON refresh_token_session (user_id);

-- Fast lookup/revocation of active sessions for a user.
-- Example query:
-- WHERE user_id = ? AND revoked_at IS NULL
CREATE INDEX refresh_token_session_active_user_idx
    ON refresh_token_session (user_id, expires_at)
    WHERE revoked_at IS NULL;

-- Fast cleanup of expired sessions.
-- Example query:
-- DELETE FROM refresh_token_session WHERE expires_at < ?
CREATE INDEX refresh_token_session_expires_at_idx
    ON refresh_token_session (expires_at);