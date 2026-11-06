-- V15: Add proctoring columns to exam_sessions (P4 - Issue #17)
ALTER TABLE exam_sessions ADD COLUMN IF NOT EXISTS violation_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE exam_sessions ADD COLUMN IF NOT EXISTS proctoring_logs JSONB NOT NULL DEFAULT '[]'::jsonb;
