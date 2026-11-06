-- V12: Add optimistic locking column lock_version to exams and questions (TD-4)
ALTER TABLE exams ADD COLUMN IF NOT EXISTS lock_version INTEGER NOT NULL DEFAULT 0;
ALTER TABLE questions ADD COLUMN IF NOT EXISTS lock_version INTEGER NOT NULL DEFAULT 0;
