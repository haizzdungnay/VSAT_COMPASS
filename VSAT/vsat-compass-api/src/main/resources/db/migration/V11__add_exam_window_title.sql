-- 0011_add_exam_window_title.sql
-- Add human-readable title column to exam_windows
ALTER TABLE exam_windows
    ADD COLUMN IF NOT EXISTS title VARCHAR(200);
