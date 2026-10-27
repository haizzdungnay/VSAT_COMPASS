-- 0010_seed_vsat_blueprints.sql
-- Seed standard V-SAT 2026 blueprints and blueprint cells for subjects

INSERT INTO exam_blueprints (
    code,
    name,
    subject_id,
    scoring_profile_id,
    total_questions,
    max_raw_score,
    version
)
SELECT
    'VSAT_2026_BP_MATH',
    'Ma trận V-SAT Toán học chuẩn 2026 (25 câu - 150 điểm)',
    s.id,
    sp.id,
    25,
    150.00,
    1
FROM subjects s
LEFT JOIN scoring_profiles sp ON sp.code = 'VSAT_2026_DRAFT_SPEC_150'
WHERE s.code = 'MATH'
ON CONFLICT (code) DO NOTHING;

INSERT INTO exam_blueprints (
    code,
    name,
    subject_id,
    scoring_profile_id,
    total_questions,
    max_raw_score,
    version
)
SELECT
    'VSAT_2026_BP_ENG',
    'Ma trận V-SAT Tiếng Anh chuẩn 2026 (25 câu - 150 điểm)',
    s.id,
    sp.id,
    25,
    150.00,
    1
FROM subjects s
LEFT JOIN scoring_profiles sp ON sp.code = 'VSAT_2026_DRAFT_SPEC_150'
WHERE s.code = 'ENG'
ON CONFLICT (code) DO NOTHING;