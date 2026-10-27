-- F9 — Leaderboard opt-in flag.
-- Add a per-user boolean so users explicitly opt-in before appearing on the
-- leaderboard. Default is false (hidden). The presence of a submitted
-- exam_sessions row is what puts them on the ranking; the flag gates visibility.
--
-- The column is added with a safe default so existing rows are valid against
-- Hibernate's `ddl-auto=validate` immediately after the migration runs.

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS leaderboard_opt_in BOOLEAN NOT NULL DEFAULT FALSE;

-- Backfill is a no-op (default already FALSE) but the explicit UPDATE is here
-- for clarity and so a future flip-of-default can be undone with a one-liner.
UPDATE users SET leaderboard_opt_in = FALSE WHERE leaderboard_opt_in IS NULL;
