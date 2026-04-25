-- ============================================================================
-- V-SAT Compass — Smoke Test Seed Data
-- ============================================================================
-- Purpose  : Minimum data required for smoke_sessions.sh to pass against
--            the Phase B Session API endpoints.
-- Idempotent: Safe to run multiple times (uses ON CONFLICT DO NOTHING /
--             WHERE NOT EXISTS guards).
-- Usage    : Paste into Neon Console → V-SAT COMPASS → SQL Editor → Run.
-- Notes    : This data is a temporary placeholder for smoke testing only.
--            It will be replaced by real fixtures when Phase C (exam
--            management) ships. Do NOT rely on these IDs in production code.
-- ============================================================================

-- Step 1: Seed subject MATH (required as FK for exam)
INSERT INTO subjects (code, name, description, display_order, is_active)
VALUES ('MATH', 'Toán học', 'Môn Toán', 1, TRUE)
ON CONFLICT (code) DO NOTHING;

-- Step 2: Seed placeholder exam for smoke testing
-- created_by = student@vsat.com (exists from manual user seed)
INSERT INTO exams (
    exam_code,
    title,
    subject_id,
    description,
    question_count,
    duration_minutes,
    difficulty,
    pricing_type,
    status,
    created_by,
    version
)
SELECT
    'SMOKE_001',
    'Đề thi mẫu (Smoke Test) — KHÔNG dùng cho người dùng thật',
    (SELECT id FROM subjects WHERE code = 'MATH'),
    'Placeholder exam for backend smoke testing. Will be replaced by Phase C fixtures.',
    30,
    60,
    'MEDIUM',
    'FREE',
    'PUBLISHED',
    (SELECT id FROM users WHERE email = 'student@vsat.com'),
    1
WHERE NOT EXISTS (
    SELECT 1 FROM exams WHERE exam_code = 'SMOKE_001'
);

-- Step 3: Verify — should show 1 row for each
SELECT 'subjects' AS table_name, COUNT(*) AS rows FROM subjects WHERE code = 'MATH'
UNION ALL
SELECT 'exams'    AS table_name, COUNT(*) AS rows FROM exams WHERE exam_code = 'SMOKE_001';

-- Step 4: Print the exam ID to use in smoke_sessions.sh
-- If this returns a different value than 1, pass it via EXAM_ID env var:
--   EXAM_ID=<value> BASE_URL=https://vsat-compass-api.onrender.com/api/v1 bash smoke_sessions.sh
SELECT id AS smoke_exam_id, exam_code, title, status
FROM exams
WHERE exam_code = 'SMOKE_001';
