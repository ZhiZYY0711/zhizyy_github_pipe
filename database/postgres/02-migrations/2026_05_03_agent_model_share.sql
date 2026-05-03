ALTER TABLE analysis_session
    ADD COLUMN IF NOT EXISTS pinned boolean NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS archived_at timestamptz;

ALTER TABLE agent_run
    ADD COLUMN IF NOT EXISTS model_tier varchar(32);

CREATE TABLE IF NOT EXISTS agent_share (
    id varchar(64) PRIMARY KEY,
    session_id varchar(64) NOT NULL REFERENCES analysis_session(id) ON DELETE CASCADE,
    created_by varchar(64) NOT NULL DEFAULT 'system',
    share_type varchar(16) NOT NULL,
    title varchar(200) NOT NULL,
    snapshot jsonb NOT NULL,
    created_at timestamptz DEFAULT now(),
    expires_at timestamptz,
    revoked_at timestamptz,
    CONSTRAINT ck_agent_share_type CHECK (share_type IN ('link', 'md'))
);

CREATE INDEX IF NOT EXISTS idx_analysis_session_archive_pin
    ON analysis_session (archived_at, pinned, updated_at);

CREATE INDEX IF NOT EXISTS idx_agent_share_session
    ON agent_share (session_id);
