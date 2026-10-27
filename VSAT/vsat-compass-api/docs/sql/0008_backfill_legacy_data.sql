-- 0008_backfill_legacy_data.sql
-- Phase: Backfill
-- Operational constraints compliance:
-- 1. DO NOT assume difficulty -> cognitive_level mapping.
--    Leave questions.cognitive_level as NULL for existing legacy questions.
--    Tagging cognitive levels for legacy questions must be done explicitly via administrative review.
-- 2. Seed initial drafting scoring profile labeled as draft specification, NOT official verified standard.

INSERT INTO scoring_profiles (
    code,
    name,
    max_raw_score,
    rules,
    scaled_method,
    version,
    is_active
) VALUES (
    'VSAT_2026_DRAFT_SPEC_150',
    'Quy chuẩn khảo sát V-SAT 2026 (Tham chiếu đặc tả nháp)',
    150.00,
    '{
        "TRUE_FALSE": {
            "correctCountPoints": {
                "0": 0,
                "1": 1,
                "2": 2,
                "3": 3,
                "4": 6
            }
        },
        "SINGLE_CHOICE": {
            "correct": 6
        },
        "MATCHING": {
            "perCorrectItem": 1.5
        },
        "SHORT_ANSWER": {
            "correct": 6
        }
    }'::jsonb,
    NULL,
    1,
    TRUE
)
ON CONFLICT (code) DO NOTHING;
