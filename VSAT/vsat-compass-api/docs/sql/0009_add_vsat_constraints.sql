-- 0009_add_vsat_constraints.sql
-- Phase: Constrain
-- Operational rule compliance: Only apply constraints when proven safe by pre-checks.

DO $$
BEGIN
    -- 1. Check if all questions have cognitive_level populated before adding NOT NULL
    IF NOT EXISTS (SELECT 1 FROM questions WHERE cognitive_level IS NULL) THEN
        ALTER TABLE questions ALTER COLUMN cognitive_level SET NOT NULL;
        RAISE NOTICE 'Applied NOT NULL constraint to questions.cognitive_level.';
    ELSE
        RAISE NOTICE 'Preserved NULLABLE on questions.cognitive_level because unclassified legacy rows exist.';
    END IF;

    -- 2. Constraint on exam_windows: schedule times
    IF NOT EXISTS (
        SELECT 1 FROM exam_windows
        WHERE mode = 'SCHEDULED_MOCK'
          AND (start_at IS NULL OR end_at IS NULL OR end_at <= start_at)
    ) THEN
        IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_exam_windows_time') THEN
            ALTER TABLE exam_windows
                ADD CONSTRAINT chk_exam_windows_time
                CHECK (
                    mode = 'PRACTICE'
                    OR (start_at IS NOT NULL AND end_at IS NOT NULL AND end_at > start_at)
                );
            RAISE NOTICE 'Applied constraint chk_exam_windows_time.';
        END IF;
    END IF;

    -- 3. Constraint on exam_windows: attempt limits
    IF NOT EXISTS (
        SELECT 1 FROM exam_windows WHERE max_attempts IS NOT NULL AND max_attempts <= 0
    ) THEN
        IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_exam_windows_attempts') THEN
            ALTER TABLE exam_windows
                ADD CONSTRAINT chk_exam_windows_attempts
                CHECK (max_attempts IS NULL OR max_attempts > 0);
            RAISE NOTICE 'Applied constraint chk_exam_windows_attempts.';
        END IF;
    END IF;

    -- 4. Constraint on exam_blueprint_cells: question count > 0
    IF NOT EXISTS (
        SELECT 1 FROM exam_blueprint_cells WHERE question_count <= 0
    ) THEN
        IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_blueprint_cells_count') THEN
            ALTER TABLE exam_blueprint_cells
                ADD CONSTRAINT chk_blueprint_cells_count
                CHECK (question_count > 0);
            RAISE NOTICE 'Applied constraint chk_blueprint_cells_count.';
        END IF;
    END IF;

    -- 5. Constraint on exam_sections: count and score non-negative
    IF NOT EXISTS (
        SELECT 1 FROM exam_sections WHERE required_question_count <= 0 OR max_raw_score < 0
    ) THEN
        IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_exam_sections_integrity') THEN
            ALTER TABLE exam_sections
                ADD CONSTRAINT chk_exam_sections_integrity
                CHECK (required_question_count > 0 AND max_raw_score >= 0);
            RAISE NOTICE 'Applied constraint chk_exam_sections_integrity.';
        END IF;
    END IF;
END $$;
