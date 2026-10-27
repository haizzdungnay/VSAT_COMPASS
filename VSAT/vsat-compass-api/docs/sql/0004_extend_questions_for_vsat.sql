-- 0004_extend_questions_for_vsat.sql
-- Phase: Expand (nullable addition)
-- Add cognitive_level to questions table without altering existing difficulty column
-- Nullable initially to preserve compatibility with existing legacy questions
ALTER TABLE questions
    ADD COLUMN IF NOT EXISTS cognitive_level cognitive_level;

CREATE INDEX IF NOT EXISTS idx_questions_cognitive_level
    ON questions(cognitive_level);
