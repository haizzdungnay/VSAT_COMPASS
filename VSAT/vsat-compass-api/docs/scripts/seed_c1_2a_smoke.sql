-- ============================================================================
-- V-SAT Compass - C1.2a Smoke Seed
-- ============================================================================
-- Purpose  : Ensure GET /exams and GET /exams/{id} have at least one
--            PUBLISHED + FREE exam to assert against during C1.2a.2 smoke.
--            Without this, smoke tests would only verify "200 with empty
--            array", which gives false-pass coverage if DTO shape is wrong.
-- Idempotent: Safe to run multiple times (uses ON CONFLICT DO NOTHING).
-- Usage    : Paste into Neon Console SQL Editor, OR run via psql.
-- Notes    : This data is for smoke ONLY. Will be replaced by real exam
--            fixtures when C1.2b ships admin CRUD. Do NOT rely on these
--            IDs in production code or app logic.
-- Secrets  : This file MUST NOT contain passwords, tokens, or any secrets.
-- ============================================================================

-- Pre-req: 'MATH' subject must already exist (seeded in Phase C1.0).
-- We use exam_code as the natural conflict key.

INSERT INTO exams (
    exam_code,
    title,
    subject_id,
    description,
    question_count,
    duration_minutes,
    difficulty,
    pricing_type,
    price,
    status,
    tags,
    created_by,
    version
)
SELECT
    'SMOKE_C1_2A_001',
    'De thi mau C1.2a (Smoke) - KHONG dung cho nguoi dung that',
    (SELECT id FROM subjects WHERE code = 'MATH'),
    'Placeholder PUBLISHED + FREE exam for C1.2a smoke. Replaced by Phase C1.2b fixtures.',
    0,
    90,
    'MEDIUM',
    'FREE',
    0,
    'PUBLISHED',
    'smoke,c1-2a',
    (SELECT id FROM users WHERE email = 'qbank-admin@vsat.com' LIMIT 1),
    1
ON CONFLICT (exam_code) DO NOTHING;
