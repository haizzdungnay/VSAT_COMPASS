-- V14: Add audio support columns to question_groups (P3 - Issue #16)
ALTER TABLE question_groups ADD COLUMN IF NOT EXISTS audio_url VARCHAR(500);
ALTER TABLE question_groups ADD COLUMN IF NOT EXISTS audio_duration_sec INTEGER;
ALTER TABLE question_groups ADD COLUMN IF NOT EXISTS max_plays INTEGER NOT NULL DEFAULT 2;
