-- Each row is now a session that keeps its id for its whole life: refreshing replaces the refresh token
-- stored on the row instead of replacing the row.
ALTER TABLE refresh_tokens RENAME TO sessions;
ALTER TABLE sessions RENAME COLUMN token_hash TO refresh_token_hash;

ALTER TABLE sessions RENAME CONSTRAINT refresh_tokens_pkey TO sessions_pkey;
ALTER TABLE sessions RENAME CONSTRAINT refresh_tokens_token_hash_key TO sessions_refresh_token_hash_key;
ALTER TABLE sessions RENAME CONSTRAINT refresh_tokens_user_id_fkey TO sessions_user_id_fkey;
ALTER INDEX idx_refresh_tokens_user_id RENAME TO idx_sessions_user_id;
