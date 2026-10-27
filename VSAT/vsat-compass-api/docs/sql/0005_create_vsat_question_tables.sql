-- 0005_create_vsat_question_tables.sql
-- Phase: Expand
-- Child tables for True/False, Matching, and Short Answer question items
-- Preserves existing question_options table intact for SINGLE_CHOICE questions

-- 1. True/False question items (4 propositions a, b, c, d)
CREATE TABLE IF NOT EXISTS question_true_false_items (
    id             BIGSERIAL PRIMARY KEY,
    question_id    BIGINT NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    item_key       VARCHAR(5) NOT NULL,
    statement_text TEXT NOT NULL,
    statement_html TEXT,
    is_correct     BOOLEAN NOT NULL,
    display_order  INT NOT NULL,
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(question_id, item_key),
    UNIQUE(question_id, display_order)
);
CREATE INDEX IF NOT EXISTS idx_q_tf_items_qid ON question_true_false_items(question_id);

-- 2. Matching choices (target options, e.g. A, B, C, D, E, F)
CREATE TABLE IF NOT EXISTS question_matching_choices (
    id            BIGSERIAL PRIMARY KEY,
    question_id   BIGINT NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    choice_key    VARCHAR(10) NOT NULL,
    choice_text   TEXT NOT NULL,
    choice_html   TEXT,
    display_order INT NOT NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(question_id, choice_key),
    UNIQUE(question_id, display_order)
);
CREATE INDEX IF NOT EXISTS idx_q_matching_choices_qid ON question_matching_choices(question_id);

-- 3. Matching items (stems to match, e.g. 1, 2, 3, 4 -> correct_choice_key)
CREATE TABLE IF NOT EXISTS question_matching_items (
    id                 BIGSERIAL PRIMARY KEY,
    question_id        BIGINT NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    item_key           VARCHAR(10) NOT NULL,
    item_text          TEXT NOT NULL,
    item_html          TEXT,
    correct_choice_key VARCHAR(10) NOT NULL,
    display_order      INT NOT NULL,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE(question_id, item_key),
    UNIQUE(question_id, display_order)
);
CREATE INDEX IF NOT EXISTS idx_q_matching_items_qid ON question_matching_items(question_id);

-- 4. Short answer evaluation key
CREATE TABLE IF NOT EXISTS question_short_answer_keys (
    question_id       BIGINT PRIMARY KEY REFERENCES questions(id) ON DELETE CASCADE,
    value_type        short_answer_value_type NOT NULL,
    accepted_answers  JSONB NOT NULL DEFAULT '[]'::jsonb,
    numeric_value     NUMERIC(20,8),
    numeric_tolerance NUMERIC(20,8),
    unit              VARCHAR(50),
    case_sensitive    BOOLEAN NOT NULL DEFAULT FALSE,
    trim_whitespace   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT now()
);
