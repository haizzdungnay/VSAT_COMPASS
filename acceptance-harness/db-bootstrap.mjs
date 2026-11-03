#!/usr/bin/env node
import { spawnSync } from 'node:child_process';
import { readFileSync, existsSync } from 'node:fs';
import { resolve, join } from 'node:path';

const HARNESS_DIR = import.meta.dirname;
const REPO_ROOT = resolve(HARNESS_DIR, '..');
const CONTAINER_NAME = process.env.CLEAN_PG_CONTAINER || 'vsat-clean-pg';
const DB_USER = process.env.CLEAN_PG_USER || 'vsat_clean';
const DB_NAME = process.env.CLEAN_PG_DB || 'vsat_compass_acceptance';

function runSqlInContainer(sqlContent, label = 'SQL script') {
  console.log(`[BOOTSTRAP] Executing ${label}...`);
  const proc = spawnSync(
    'docker',
    ['exec', '-i', CONTAINER_NAME, 'psql', '-U', DB_USER, '-d', DB_NAME, '-v', 'ON_ERROR_STOP=1'],
    {
      input: sqlContent,
      encoding: 'utf8',
      stdio: ['pipe', 'pipe', 'pipe']
    }
  );

  if (proc.status !== 0) {
    console.error(`[BOOTSTRAP ERROR] Failed executing ${label}`);
    console.error(`stderr: ${proc.stderr}`);
    console.error(`stdout: ${proc.stdout}`);
    throw new Error(`SQL execution failure: ${label} (exit ${proc.status})`);
  }
}

function runSqlFile(relativeFilePath, label) {
  const fullPath = resolve(REPO_ROOT, relativeFilePath);
  if (!existsSync(fullPath)) {
    throw new Error(`File not found: ${fullPath}`);
  }
  const content = readFileSync(fullPath, 'utf8');
  runSqlInContainer(content, label || relativeFilePath);
}

const CLEAN_FIXTURES_SQL = `
-- ============================================================================
-- CLEAN-ROOM ACCEPTANCE FIXTURES (Deterministic)
-- Passwords for all accounts: Passw0rd123!
-- BCrypt hash ($2a$10$): $2a$10$CMuCUmiHOoGa20tx96R20e8JZgaCThRwYJxWNCU3anO1QecLhCXcW
-- ============================================================================

-- 1. Standard Accounts for 4 Roles
INSERT INTO users (
    email, password_hash, full_name, phone, role, status, email_verified, leaderboard_opt_in
) VALUES
(
    'student@vsat.com',
    '$2a$10$CMuCUmiHOoGa20tx96R20e8JZgaCThRwYJxWNCU3anO1QecLhCXcW',
    'Học Sinh Clean Room',
    '0901111111',
    'STUDENT',
    'ACTIVE',
    TRUE,
    TRUE
),
(
    'collab@vsat.com',
    '$2a$10$CMuCUmiHOoGa20tx96R20e8JZgaCThRwYJxWNCU3anO1QecLhCXcW',
    'Cộng Tác Viên Clean Room',
    '0902222222',
    'COLLABORATOR',
    'ACTIVE',
    TRUE,
    FALSE
),
(
    'contentadmin@vsat.com',
    '$2a$10$CMuCUmiHOoGa20tx96R20e8JZgaCThRwYJxWNCU3anO1QecLhCXcW',
    'Quản Trị Nội Dung Clean Room',
    '0903333333',
    'CONTENT_ADMIN',
    'ACTIVE',
    TRUE,
    FALSE
),
(
    'superadmin@vsat.com',
    '$2a$10$CMuCUmiHOoGa20tx96R20e8JZgaCThRwYJxWNCU3anO1QecLhCXcW',
    'Tổng Quản Trị Clean Room',
    '0904444444',
    'SUPER_ADMIN',
    'ACTIVE',
    TRUE,
    FALSE
)
ON CONFLICT (email) DO UPDATE SET
    password_hash = EXCLUDED.password_hash,
    status = 'ACTIVE',
    email_verified = TRUE,
    leaderboard_opt_in = EXCLUDED.leaderboard_opt_in;

-- 2. Subjects
INSERT INTO subjects (code, name, description, is_active, display_order)
VALUES
    ('MATH', 'Toán học', 'Môn Toán kỳ thi đánh giá năng lực V-SAT', TRUE, 1),
    ('ENG', 'Tiếng Anh', 'Môn Tiếng Anh chuẩn V-SAT', TRUE, 2)
ON CONFLICT (code) DO UPDATE SET is_active = TRUE;

-- 3. Topics
INSERT INTO topics (subject_id, code, name, description, is_active, display_order)
SELECT s.id, 'MATH_ALG', 'Đại số & Giải tích', 'Hàm số, phương trình, bất phương trình', TRUE, 1
FROM subjects s WHERE s.code = 'MATH'
ON CONFLICT (code) DO NOTHING;

INSERT INTO topics (subject_id, code, name, description, is_active, display_order)
SELECT s.id, 'MATH_CALC', 'Giải tích tích phân', 'Đạo hàm, tích phân và ứng dụng', TRUE, 2
FROM subjects s WHERE s.code = 'MATH'
ON CONFLICT (code) DO NOTHING;

-- 4. Subtopics
INSERT INTO subtopics (topic_id, code, name, description, is_active, display_order)
SELECT t.id, 'MATH_CALC_DERIVATIVE', 'Đạo hàm và cực trị', 'Cực trị hàm bậc 3 và bậc 4', TRUE, 1
FROM topics t WHERE t.code = 'MATH_CALC'
ON CONFLICT (code) DO NOTHING;

-- 5. Ensure Default Scoring Profile
INSERT INTO scoring_profiles (
    code, name, max_raw_score, rules, scaled_method, version, is_active
) VALUES (
    'VSAT_2026_DRAFT_SPEC_150',
    'Quy chuẩn khảo sát V-SAT 2026 (Tham chiếu đặc tả nháp)',
    150.00,
    '{
        "TRUE_FALSE": {
            "correctCountPoints": {
                "0": 0, "1": 1, "2": 2, "3": 3, "4": 6
            }
        },
        "SINGLE_CHOICE": { "correct": 6 },
        "MATCHING": { "perCorrectItem": 1.5 },
        "SHORT_ANSWER": { "correct": 6 }
    }'::jsonb,
    NULL,
    1,
    TRUE
)
ON CONFLICT (code) DO UPDATE SET is_active = TRUE;

-- 6. Sample Questions covering all 4 formats
-- Q1: SINGLE_CHOICE
INSERT INTO questions (
    question_code, subject_id, topic_id, difficulty, cognitive_level, question_type,
    question_text, question_html, explanation, status, created_by, version
)
SELECT
    'Q_CLEAN_MCQ_001',
    s.id,
    t.id,
    'MEDIUM',
    'APPLICATION',
    'SINGLE_CHOICE',
    'Giải phương trình $x^2 - 4 = 0$. Tìm các nghiệm thực của phương trình.',
    '<p>Giải phương trình $x^2 - 4 = 0$. Tìm các nghiệm thực của phương trình.</p>',
    'Phương trình $(x-2)(x+2) = 0 \\iff x = \\pm 2$.',
    'PUBLISHED',
    u.id,
    1
FROM subjects s, topics t, users u
WHERE s.code = 'MATH' AND t.code = 'MATH_ALG' AND u.email = 'superadmin@vsat.com'
ON CONFLICT (question_code) DO NOTHING;

INSERT INTO question_options (question_id, option_label, option_text, option_html, is_correct, display_order)
SELECT q.id, opt.label, opt.txt, opt.txt, opt.correct, opt.ord
FROM questions q
CROSS JOIN (
    VALUES
        ('A', 'x = 2', FALSE, 1),
        ('B', 'x = -2', FALSE, 2),
        ('C', 'x = \\pm 2', TRUE, 3),
        ('D', 'x = 4', FALSE, 4)
) AS opt(label, txt, correct, ord)
WHERE q.question_code = 'Q_CLEAN_MCQ_001'
ON CONFLICT DO NOTHING;

-- Q2: TRUE_FALSE (4 Sub-items)
INSERT INTO questions (
    question_code, subject_id, topic_id, difficulty, cognitive_level, question_type,
    question_text, question_html, explanation, status, created_by, version
)
SELECT
    'Q_CLEAN_TF_001',
    s.id,
    t.id,
    'MEDIUM',
    'UNDERSTANDING',
    'TRUE_FALSE',
    'Cho hàm số bậc hai $f(x) = x^2 - 2x + 3$. Xét tính đúng/sai của từng khẳng định sau:',
    '<p>Cho hàm số bậc hai $f(x) = x^2 - 2x + 3$. Xét tính đúng/sai của từng khẳng định sau:</p>',
    'Đỉnh parabol $I(1, 2)$. Trục đối xứng $x = 1$. Giá trị nhỏ nhất bằng 2.',
    'PUBLISHED',
    u.id,
    1
FROM subjects s, topics t, users u
WHERE s.code = 'MATH' AND t.code = 'MATH_ALG' AND u.email = 'superadmin@vsat.com'
ON CONFLICT (question_code) DO NOTHING;

INSERT INTO question_true_false_items (question_id, item_key, statement_text, statement_html, is_correct, display_order)
SELECT q.id, item.key, item.txt, item.txt, item.correct, item.ord
FROM questions q
CROSS JOIN (
    VALUES
        ('a', 'Tọa độ đỉnh của parabol là $I(1, 2)$', TRUE, 1),
        ('b', 'Hàm số đồng biến trên khoảng $(1, +\\infty)$', TRUE, 2),
        ('c', 'Giá trị nhỏ nhất của hàm số bằng 3', FALSE, 3),
        ('d', 'Đồ thị cắt trục hoành tại 2 điểm phân biệt', FALSE, 4)
) AS item(key, txt, correct, ord)
WHERE q.question_code = 'Q_CLEAN_TF_001'
ON CONFLICT (question_id, item_key) DO NOTHING;

-- Q3: MATCHING
INSERT INTO questions (
    question_code, subject_id, topic_id, difficulty, cognitive_level, question_type,
    question_text, question_html, explanation, status, created_by, version
)
SELECT
    'Q_CLEAN_MATCH_001',
    s.id,
    t.id,
    'HARD',
    'APPLICATION',
    'MATCHING',
    'Ghép mỗi hàm số sau với tập xác định tương ứng:',
    '<p>Ghép mỗi hàm số sau với tập xác định tương ứng:</p>',
    'y = x^2 + 1 xác định trên R. y = ln(x) xác định trên (0, +inf). y = 1/x xác định trên R\\\\{0}.',
    'PUBLISHED',
    u.id,
    1
FROM subjects s, topics t, users u
WHERE s.code = 'MATH' AND t.code = 'MATH_ALG' AND u.email = 'superadmin@vsat.com'
ON CONFLICT (question_code) DO NOTHING;

INSERT INTO question_matching_choices (question_id, choice_key, choice_text, choice_html, display_order)
SELECT q.id, c.k, c.txt, c.txt, c.ord
FROM questions q
CROSS JOIN (
    VALUES
        ('A', 'D = \\mathbb{R}', 1),
        ('B', 'D = (0, +\\infty)', 2),
        ('C', 'D = \\mathbb{R} \\setminus \\{0\\}', 3)
) AS c(k, txt, ord)
WHERE q.question_code = 'Q_CLEAN_MATCH_001'
ON CONFLICT (question_id, choice_key) DO NOTHING;

INSERT INTO question_matching_items (question_id, item_key, item_text, item_html, correct_choice_key, display_order)
SELECT q.id, it.k, it.txt, it.txt, it.target, it.ord
FROM questions q
CROSS JOIN (
    VALUES
        ('1', 'y = x^2 + 1', 'A', 1),
        ('2', 'y = \\ln(x)', 'B', 2),
        ('3', 'y = \\frac{1}{x}', 'C', 3)
) AS it(k, txt, target, ord)
WHERE q.question_code = 'Q_CLEAN_MATCH_001'
ON CONFLICT (question_id, item_key) DO NOTHING;

-- Q4: SHORT_ANSWER
INSERT INTO questions (
    question_code, subject_id, topic_id, difficulty, cognitive_level, question_type,
    question_text, question_html, explanation, status, created_by, version
)
SELECT
    'Q_CLEAN_SA_001',
    s.id,
    t.id,
    'HARD',
    'HIGH_APPLICATION',
    'SHORT_ANSWER',
    'Cho hàm số $y = x^3 - 3x$. Tính giá trị cực đại của hàm số.',
    '<p>Cho hàm số $y = x^3 - 3x$. Tính giá trị cực đại của hàm số.</p>',
    'y prime = 3x^2 - 3 = 0 suy ra x = plus minus 1. Cực đại tại x = -1 có y = (-1)^3 - 3(-1) = 2.',
    'PUBLISHED',
    u.id,
    1
FROM subjects s, topics t, users u
WHERE s.code = 'MATH' AND t.code = 'MATH_CALC' AND u.email = 'superadmin@vsat.com'
ON CONFLICT (question_code) DO NOTHING;

INSERT INTO question_short_answer_keys (
    question_id, value_type, accepted_answers, numeric_value, numeric_tolerance, trim_whitespace
)
SELECT
    q.id,
    'NUMERIC',
    '["2", "2.0"]'::jsonb,
    2.00000000,
    0.00010000,
    TRUE
FROM questions q
WHERE q.question_code = 'Q_CLEAN_SA_001'
ON CONFLICT (question_id) DO NOTHING;

-- 7. Sample Published Exam
INSERT INTO exams (
    exam_code, title, subject_id, scoring_profile_id, description,
    question_count, duration_minutes, difficulty, pricing_type, price,
    status, created_by, version
)
SELECT
    'CLEAN_ROOM_MOCK_001',
    'Đề Thi Thử V-SAT Toán Học Clean-Room 2026',
    s.id,
    sp.id,
    'Đề thi mẫu chuẩn 4 dạng thức phục vụ kiểm thử chấp nhận tự động',
    4,
    90,
    'MEDIUM',
    'FREE',
    0,
    'PUBLISHED',
    u.id,
    1
FROM subjects s
JOIN scoring_profiles sp ON sp.code = 'VSAT_2026_DRAFT_SPEC_150'
JOIN users u ON u.email = 'superadmin@vsat.com'
WHERE s.code = 'MATH'
ON CONFLICT (exam_code) DO NOTHING;

-- 8. Exam Sections (4 format sections)
INSERT INTO exam_sections (exam_id, question_type, section_order, section_title, required_question_count, max_raw_score)
SELECT e.id, sec.qt::question_type, sec.ord, sec.title, sec.req_cnt, sec.score
FROM exams e
CROSS JOIN (
    VALUES
        ('SINGLE_CHOICE', 1, 'Phần 1: Trắc nghiệm 4 lựa chọn', 1, 6.00),
        ('TRUE_FALSE', 2, 'Phần 2: Trắc nghiệm Đúng/Sai', 1, 6.00),
        ('MATCHING', 3, 'Phần 3: Ghép nối', 1, 4.50),
        ('SHORT_ANSWER', 4, 'Phần 4: Trả lời ngắn', 1, 6.00)
) AS sec(qt, ord, title, req_cnt, score)
WHERE e.exam_code = 'CLEAN_ROOM_MOCK_001'
ON CONFLICT (exam_id, section_order) DO NOTHING;

-- 9. Exam Questions Mapping
INSERT INTO exam_questions (exam_id, question_id, section_id, question_order, max_raw_score)
SELECT e.id, q.id, es.id, mapping.ord, mapping.score
FROM (
    VALUES
        ('Q_CLEAN_MCQ_001', 1, 6.00),
        ('Q_CLEAN_TF_001', 2, 6.00),
        ('Q_CLEAN_MATCH_001', 3, 4.50),
        ('Q_CLEAN_SA_001', 4, 6.00)
) AS mapping(q_code, ord, score)
JOIN questions q ON q.question_code = mapping.q_code
CROSS JOIN exams e
JOIN exam_sections es ON es.exam_id = e.id AND es.question_type = q.question_type
WHERE e.exam_code = 'CLEAN_ROOM_MOCK_001'
ON CONFLICT (exam_id, question_id) DO NOTHING;

-- 10. Exam Delivery Windows (Practice & Scheduled Mock currently OPEN)
INSERT INTO exam_windows (
    exam_id, window_code, title, mode, start_at, end_at,
    grace_period_minutes, max_attempts, result_policy, hard_stop_at_end, allow_offline
)
SELECT
    e.id,
    'WINDOW_CLEAN_PRACTICE_001',
    'Ca Luyện Tập Tự Do 24/7',
    'PRACTICE'::exam_delivery_mode,
    NULL,
    NULL,
    0,
    NULL,
    'IMMEDIATE_FULL'::result_release_policy,
    FALSE,
    TRUE
FROM exams e WHERE e.exam_code = 'CLEAN_ROOM_MOCK_001'
ON CONFLICT (exam_id, window_code) DO NOTHING;

INSERT INTO exam_windows (
    exam_id, window_code, title, mode, start_at, end_at,
    grace_period_minutes, max_attempts, result_policy, hard_stop_at_end, allow_offline
)
SELECT
    e.id,
    'WINDOW_CLEAN_MOCK_001',
    'Kỳ Thi Thử Trực Tuyến Đang Mở',
    'SCHEDULED_MOCK'::exam_delivery_mode,
    NOW() - INTERVAL '2 hours',
    NOW() + INTERVAL '48 hours',
    10,
    3,
    'IMMEDIATE_FULL'::result_release_policy,
    TRUE,
    TRUE
FROM exams e WHERE e.exam_code = 'CLEAN_ROOM_MOCK_001'
ON CONFLICT (exam_id, window_code) DO NOTHING;
`;

export async function bootstrapDatabase() {
  console.log('[BOOTSTRAP] Starting clean-room database bootstrap...');

  // 0. Reset clean schema to guarantee idempotency
  runSqlInContainer(
    'DROP SCHEMA IF EXISTS public CASCADE; CREATE SCHEMA public; GRANT ALL ON SCHEMA public TO vsat_clean; GRANT ALL ON SCHEMA public TO public;',
    'Reset Public Schema'
  );

  // 1. Initial base schema
  runSqlFile('VSAT/vsat_database_schema.sql', 'Base Schema (vsat_database_schema.sql)');

  // 2. Ordered migrations 0001 to 0011
  const migrations = [
    'VSAT/vsat-compass-api/docs/sql/0001_create_tickets.sql',
    'VSAT/vsat-compass-api/docs/sql/0002_add_leaderboard_opt_in.sql',
    'VSAT/vsat-compass-api/docs/sql/0003_add_vsat_domain_types.sql',
    'VSAT/vsat-compass-api/docs/sql/0004_extend_questions_for_vsat.sql',
    'VSAT/vsat-compass-api/docs/sql/0005_create_vsat_question_tables.sql',
    'VSAT/vsat-compass-api/docs/sql/0006_create_vsat_scoring_and_sections.sql',
    'VSAT/vsat-compass-api/docs/sql/0007_create_exam_blueprints_and_variants.sql',
    'VSAT/vsat-compass-api/docs/sql/0008_backfill_legacy_data.sql',
    'VSAT/vsat-compass-api/docs/sql/0009_add_vsat_constraints.sql',
    'VSAT/vsat-compass-api/docs/sql/0010_seed_vsat_blueprints.sql',
    'VSAT/vsat-compass-api/docs/sql/0011_add_exam_window_title.sql'
  ];

  for (const mig of migrations) {
    runSqlFile(mig, `Migration ${mig.split('/').pop()}`);
  }

  // 3. Clean fixtures
  runSqlInContainer(CLEAN_FIXTURES_SQL, 'Clean-Room Deterministic Fixtures');
  console.log('[BOOTSTRAP] Clean-room database bootstrap completed successfully.');
}

if (process.argv[1] && process.argv[1].endsWith('db-bootstrap.mjs')) {
  try {
    await bootstrapDatabase();
    process.exit(0);
  } catch (err) {
    console.error(`[BOOTSTRAP FAILED] ${err.message}`);
    process.exit(1);
  }
}
