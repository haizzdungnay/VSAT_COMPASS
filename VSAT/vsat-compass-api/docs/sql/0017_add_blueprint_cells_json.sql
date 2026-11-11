-- V17: Add cells_json to exam_blueprints and seed all subjects for V-SAT 2026 matrix

ALTER TABLE exam_blueprints
    ADD COLUMN IF NOT EXISTS cells_json JSONB NOT NULL DEFAULT '[]'::jsonb;
ALTER TABLE exam_blueprints
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ DEFAULT NOW();

-- Seed missing subjects into exam_blueprints (ensures >= 7 rows for API-001)
INSERT INTO exam_blueprints (code, name, subject_id, scoring_profile_id, total_questions, max_raw_score, version)
SELECT
    'VSAT_2026_BP_' || s.code,
    'Ma trận chuẩn V-SAT 2026 - ' || s.name,
    s.id,
    (SELECT id FROM scoring_profiles WHERE code = 'VSAT_2026_DEFAULT' LIMIT 1),
    25,
    150.00,
    1
FROM subjects s
WHERE NOT EXISTS (SELECT 1 FROM exam_blueprints bp WHERE bp.subject_id = s.id)
ON CONFLICT (code) DO NOTHING;
