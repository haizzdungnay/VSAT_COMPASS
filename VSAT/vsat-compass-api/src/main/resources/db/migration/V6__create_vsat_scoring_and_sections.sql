-- 0006_create_vsat_scoring_and_sections.sql
-- Phase: Expand
-- Scoring profiles, exam sections, exam windows, and session extensions

-- 1. Scoring profiles
CREATE TABLE IF NOT EXISTS scoring_profiles (
    id             BIGSERIAL PRIMARY KEY,
    code           VARCHAR(50) NOT NULL UNIQUE,
    name           VARCHAR(200) NOT NULL,
    max_raw_score  NUMERIC(7,2) NOT NULL,
    rules          JSONB NOT NULL,
    scaled_method  VARCHAR(30),
    scaled_mean    NUMERIC(8,2),
    scaled_sd      NUMERIC(8,2),
    version        INT NOT NULL DEFAULT 1,
    is_active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_scoring_profiles_code ON scoring_profiles(code);

-- 2. Link scoring profile to exams (nullable for legacy compatibility)
ALTER TABLE exams
    ADD COLUMN IF NOT EXISTS scoring_profile_id BIGINT REFERENCES scoring_profiles(id);
CREATE INDEX IF NOT EXISTS idx_exams_scoring_profile ON exams(scoring_profile_id);

-- 3. Exam sections (dividing exams into 4 format sections)
CREATE TABLE IF NOT EXISTS exam_sections (
    id                      BIGSERIAL PRIMARY KEY,
    exam_id                 BIGINT NOT NULL REFERENCES exams(id) ON DELETE CASCADE,
    question_type           question_type NOT NULL,
    section_order           INT NOT NULL,
    section_title           VARCHAR(200),
    required_question_count INT NOT NULL,
    max_raw_score           NUMERIC(7,2) NOT NULL,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(exam_id, section_order),
    UNIQUE(exam_id, question_type)
);
CREATE INDEX IF NOT EXISTS idx_exam_sections_exam ON exam_sections(exam_id);

-- 4. Extend exam_questions for section mapping & individual point weights
ALTER TABLE exam_questions
    ADD COLUMN IF NOT EXISTS section_id BIGINT REFERENCES exam_sections(id);
ALTER TABLE exam_questions
    ADD COLUMN IF NOT EXISTS max_raw_score NUMERIC(7,2);
CREATE INDEX IF NOT EXISTS idx_exam_questions_section ON exam_questions(section_id);

-- 5. Exam delivery windows (schedules, practice vs scheduled mock)
CREATE TABLE IF NOT EXISTS exam_windows (
    id                   BIGSERIAL PRIMARY KEY,
    exam_id              BIGINT NOT NULL REFERENCES exams(id) ON DELETE CASCADE,
    window_code          VARCHAR(50) NOT NULL,
    mode                 exam_delivery_mode NOT NULL,
    start_at             TIMESTAMPTZ,
    end_at               TIMESTAMPTZ,
    grace_period_minutes INT NOT NULL DEFAULT 0,
    max_attempts         INT,
    result_policy        result_release_policy NOT NULL DEFAULT 'IMMEDIATE_FULL',
    hard_stop_at_end     BOOLEAN NOT NULL DEFAULT TRUE,
    allow_offline        BOOLEAN NOT NULL DEFAULT FALSE,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(exam_id, window_code)
);
CREATE INDEX IF NOT EXISTS idx_exam_windows_exam_mode ON exam_windows(exam_id, mode);

-- 6. Extend exam_sessions for raw scoring, deadline and window linkage
ALTER TABLE exam_sessions
    ADD COLUMN IF NOT EXISTS raw_score NUMERIC(7,2);
ALTER TABLE exam_sessions
    ADD COLUMN IF NOT EXISTS max_raw_score NUMERIC(7,2);
ALTER TABLE exam_sessions
    ADD COLUMN IF NOT EXISTS scaled_score NUMERIC(8,2);
ALTER TABLE exam_sessions
    ADD COLUMN IF NOT EXISTS scoring_profile_code VARCHAR(50);
ALTER TABLE exam_sessions
    ADD COLUMN IF NOT EXISTS scoring_model_version VARCHAR(50);
ALTER TABLE exam_sessions
    ADD COLUMN IF NOT EXISTS deadline_at TIMESTAMPTZ;
ALTER TABLE exam_sessions
    ADD COLUMN IF NOT EXISTS exam_window_id BIGINT REFERENCES exam_windows(id);
CREATE INDEX IF NOT EXISTS idx_exam_sessions_window ON exam_sessions(exam_window_id);

-- 7. Extend session_answers for subitem answers, JSON payload and points awarded
ALTER TABLE session_answers
    ADD COLUMN IF NOT EXISTS answer_payload JSONB;
ALTER TABLE session_answers
    ADD COLUMN IF NOT EXISTS correct_subitems INT;
ALTER TABLE session_answers
    ADD COLUMN IF NOT EXISTS total_subitems INT;
ALTER TABLE session_answers
    ADD COLUMN IF NOT EXISTS raw_points_awarded NUMERIC(7,2);
