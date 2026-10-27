-- 0003_add_vsat_domain_types.sql
-- Step 1: Expand existing question_type enum with VSAT question formats
ALTER TYPE question_type ADD VALUE IF NOT EXISTS 'MATCHING';
ALTER TYPE question_type ADD VALUE IF NOT EXISTS 'SHORT_ANSWER';

-- Step 2: Create new domain ENUMs for cognitive levels, short-answer values, delivery modes, and result policies
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'cognitive_level') THEN
        CREATE TYPE cognitive_level AS ENUM (
            'RECOGNITION',
            'UNDERSTANDING',
            'APPLICATION',
            'HIGH_APPLICATION'
        );
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'short_answer_value_type') THEN
        CREATE TYPE short_answer_value_type AS ENUM (
            'TEXT',
            'NUMERIC'
        );
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'exam_delivery_mode') THEN
        CREATE TYPE exam_delivery_mode AS ENUM (
            'PRACTICE',
            'SCHEDULED_MOCK'
        );
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'result_release_policy') THEN
        CREATE TYPE result_release_policy AS ENUM (
            'IMMEDIATE_SCORE',
            'IMMEDIATE_FULL',
            'AFTER_WINDOW_SCORE',
            'AFTER_WINDOW_FULL',
            'MANUAL'
        );
    END IF;
END $$;
