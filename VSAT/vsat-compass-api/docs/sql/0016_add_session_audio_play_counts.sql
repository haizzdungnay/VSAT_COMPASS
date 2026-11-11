-- V16: Add audio_play_counts to exam_sessions for server-side audio play tracking
ALTER TABLE exam_sessions
    ADD COLUMN IF NOT EXISTS audio_play_counts JSONB NOT NULL DEFAULT '{}'::jsonb;
