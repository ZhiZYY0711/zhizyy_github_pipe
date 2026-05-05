import tarfile
from pathlib import Path


WORKTREE_ROOT = Path(__file__).resolve().parents[2]
MIGRATION_PATH = (
    "database/postgres/02-migrations/2026_05_05_agent_runtime_schema_patch.sql"
)


def test_agent_schema_patch_migration_covers_runtime_model_changes() -> None:
    with tarfile.open(WORKTREE_ROOT / "database.tar.xz") as archive:
        member = archive.extractfile(MIGRATION_PATH)
        assert member is not None
        sql = member.read().decode("utf-8")

    assert "ADD COLUMN IF NOT EXISTS user_id" in sql
    assert "ADD COLUMN IF NOT EXISTS candidate_type" in sql
    assert "ADD COLUMN IF NOT EXISTS preference_key" in sql
    assert "ADD COLUMN IF NOT EXISTS risk_level" in sql
    assert "ADD COLUMN IF NOT EXISTS proposed_action" in sql
    assert "ADD COLUMN IF NOT EXISTS source_text" in sql
    assert "ADD COLUMN IF NOT EXISTS reason" in sql
    assert "DROP CONSTRAINT IF EXISTS ck_agent_run_status" in sql
    assert "context_building" in sql
    assert "understanding" in sql
    assert "normalizing" in sql
    assert "CREATE TABLE IF NOT EXISTS user_memory" in sql
    assert "ck_user_memory_status" in sql
    assert "idx_memory_candidate_user_status" in sql
    assert "idx_user_memory_preference_key" in sql
    assert "CREATE TRIGGER trg_user_memory_updated_at" in sql
    assert "DROP INDEX IF EXISTS idx_memory_candidate_user_status_created" in sql
    assert "DROP INDEX IF EXISTS idx_user_memory_user_status_updated" in sql
    assert "ALTER COLUMN created_at TYPE timestamptz(6)" in sql
