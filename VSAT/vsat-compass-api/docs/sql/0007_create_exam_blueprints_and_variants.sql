-- 0007_create_exam_blueprints_and_variants.sql
-- Phase: Expand
-- Matrix-based blueprint definitions and permutation variants

-- 1. Exam blueprints (matrix root)
CREATE TABLE IF NOT EXISTS exam_blueprints (
    id                 BIGSERIAL PRIMARY KEY,
    code               VARCHAR(50) NOT NULL UNIQUE,
    name               VARCHAR(200) NOT NULL,
    subject_id         BIGINT NOT NULL REFERENCES subjects(id),
    scoring_profile_id BIGINT REFERENCES scoring_profiles(id),
    total_questions    INT NOT NULL,
    max_raw_score      NUMERIC(7,2) NOT NULL,
    version            INT NOT NULL DEFAULT 1,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_exam_blueprints_subject ON exam_blueprints(subject_id);

-- 2. Blueprint cells (matrix 3D distribution requirements)
CREATE TABLE IF NOT EXISTS exam_blueprint_cells (
    id              BIGSERIAL PRIMARY KEY,
    blueprint_id    BIGINT NOT NULL REFERENCES exam_blueprints(id) ON DELETE CASCADE,
    topic_id        BIGINT REFERENCES topics(id),
    subtopic_id     BIGINT REFERENCES subtopics(id),
    question_type   question_type NOT NULL,
    cognitive_level cognitive_level,
    difficulty      difficulty_level,
    question_count  INT NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_blueprint_cells_bp ON exam_blueprint_cells(blueprint_id);

-- 3. Exam variants (permutations 101, 102, ...)
CREATE TABLE IF NOT EXISTS exam_variants (
    id           BIGSERIAL PRIMARY KEY,
    exam_id      BIGINT NOT NULL REFERENCES exams(id) ON DELETE CASCADE,
    variant_code VARCHAR(30) NOT NULL,
    random_seed  VARCHAR(100) NOT NULL,
    content_hash VARCHAR(64),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(exam_id, variant_code)
);
CREATE INDEX IF NOT EXISTS idx_exam_variants_exam ON exam_variants(exam_id);

-- 4. Variant question order & option permutation mapping
CREATE TABLE IF NOT EXISTS exam_variant_questions (
    id               BIGSERIAL PRIMARY KEY,
    variant_id       BIGINT NOT NULL REFERENCES exam_variants(id) ON DELETE CASCADE,
    exam_question_id BIGINT NOT NULL REFERENCES exam_questions(id),
    question_order   INT NOT NULL,
    option_order     JSONB,
    subitem_order    JSONB,
    UNIQUE(variant_id, question_order)
);
CREATE INDEX IF NOT EXISTS idx_variant_questions_var ON exam_variant_questions(variant_id);
