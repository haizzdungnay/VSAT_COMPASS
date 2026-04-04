-- ============================================================================
-- V-SAT COMPASS — DATABASE SCHEMA
-- PostgreSQL (Neon Serverless)
-- Version: 1.0.0
-- Date: 2026-03-30
-- ============================================================================

-- ============================================================================
-- 1. ENUM TYPES
-- ============================================================================

CREATE TYPE user_role AS ENUM (
    'STUDENT',           -- Học viên
    'COLLABORATOR',      -- Cộng tác viên
    'CONTENT_ADMIN',     -- Admin nội dung
    'SUPER_ADMIN'        -- Super Admin
);

CREATE TYPE user_status AS ENUM (
    'ACTIVE',            -- Đang hoạt động
    'LOCKED',            -- Bị khóa
    'SUSPENDED',         -- Tạm ngưng
    'DEACTIVATED'        -- Vô hiệu hóa
);

CREATE TYPE gender_type AS ENUM (
    'MALE',
    'FEMALE',
    'OTHER'
);

CREATE TYPE difficulty_level AS ENUM (
    'EASY',              -- Dễ
    'MEDIUM',            -- Trung bình
    'HARD',              -- Khó
    'VERY_HARD'          -- Rất khó
);

CREATE TYPE question_type AS ENUM (
    'SINGLE_CHOICE',     -- Trắc nghiệm 1 đáp án
    'MULTIPLE_CHOICE',   -- Trắc nghiệm nhiều đáp án (mở rộng sau)
    'TRUE_FALSE',        -- Đúng/Sai (mở rộng sau)
    'FILL_IN_BLANK'      -- Điền khuyết (mở rộng sau)
);

CREATE TYPE question_status AS ENUM (
    'DRAFT',             -- Bản nháp
    'PENDING_REVIEW',    -- Chờ duyệt
    'NEEDS_REVISION',    -- Cần sửa
    'APPROVED',          -- Đã duyệt
    'PUBLISHED',         -- Đã xuất bản
    'HIDDEN',            -- Tạm ẩn
    'ARCHIVED'           -- Lưu trữ
);

CREATE TYPE exam_status AS ENUM (
    'DRAFT',             -- Bản nháp
    'COMPOSING',         -- Đang biên soạn
    'PENDING_REVIEW',    -- Chờ duyệt
    'PUBLISHED',         -- Đã xuất bản
    'HIDDEN',            -- Tạm ẩn
    'LOCKED',            -- Đã khóa
    'ARCHIVED'           -- Lưu trữ
);

CREATE TYPE exam_pricing_type AS ENUM (
    'FREE',              -- Miễn phí
    'PAID',              -- Trả phí theo lượt
    'PACKAGE'            -- Nằm trong gói
);

CREATE TYPE session_status AS ENUM (
    'IN_PROGRESS',       -- Đang làm bài
    'SUBMITTED',         -- Đã nộp
    'TIMED_OUT',         -- Hết giờ tự nộp
    'ABANDONED'          -- Bỏ dở
);

CREATE TYPE session_mode AS ENUM (
    'MOCK_EXAM',         -- Thi thử mô phỏng
    'PRACTICE'           -- Luyện tập theo chủ đề
);

CREATE TYPE ticket_type AS ENUM (
    'WRONG_ANSWER',      -- Sai đáp án
    'WRONG_EXPLANATION', -- Sai lời giải
    'TYPO',              -- Lỗi chính tả
    'DISPLAY_ERROR',     -- Lỗi hiển thị
    'UNCLEAR_CONTENT',   -- Nội dung không rõ
    'SUSPECTED_DUPLICATE'-- Nghi ngờ trùng lặp
);

CREATE TYPE ticket_status AS ENUM (
    'NEW',               -- Mới
    'IN_PROGRESS',       -- Đang xử lý
    'AWAITING_VERIFY',   -- Chờ xác minh
    'FIXED',             -- Đã sửa
    'CLOSED'             -- Đã đóng
);

CREATE TYPE review_action AS ENUM (
    'APPROVE',           -- Duyệt
    'REQUEST_REVISION',  -- Yêu cầu sửa
    'REJECT'             -- Từ chối
);

CREATE TYPE import_batch_status AS ENUM (
    'UPLOADED',          -- Đã upload
    'PARSING',           -- Đang parse
    'PARSED',            -- Parse xong
    'VALIDATING',        -- Đang validate
    'VALIDATED',         -- Validate xong
    'PREVIEWING',        -- Đang preview
    'PENDING_APPROVAL',  -- Chờ admin duyệt batch
    'APPROVED',          -- Batch đã duyệt
    'IMPORTING',         -- Đang nhập chính thức
    'COMPLETED',         -- Nhập hoàn tất
    'FAILED',            -- Thất bại
    'ROLLED_BACK'        -- Đã rollback
);

CREATE TYPE import_row_status AS ENUM (
    'VALID',             -- Hợp lệ
    'WARNING',           -- Có cảnh báo
    'ERROR'              -- Lỗi
);

CREATE TYPE import_error_level AS ENUM (
    'ERROR',
    'WARNING',
    'INFO'
);

CREATE TYPE transaction_type AS ENUM (
    'DEPOSIT',           -- Nạp tiền
    'PURCHASE',          -- Mua lượt thi / gói
    'REFUND',            -- Hoàn tiền
    'BONUS'              -- Thưởng / khuyến mãi
);

CREATE TYPE transaction_status AS ENUM (
    'PENDING',
    'COMPLETED',
    'FAILED',
    'REFUNDED'
);

CREATE TYPE package_type AS ENUM (
    'SINGLE_EXAM',       -- Lượt thi lẻ
    'SUBJECT_PACK',      -- Gói theo môn
    'COMBO_PACK',        -- Gói combo
    'SUBSCRIPTION'       -- Gói thời hạn (mở rộng sau)
);

CREATE TYPE notification_type AS ENUM (
    'REVIEW_RESULT',     -- Kết quả duyệt nội dung
    'TICKET_UPDATE',     -- Cập nhật ticket
    'SYSTEM',            -- Thông báo hệ thống
    'EXAM_PUBLISHED',    -- Đề mới xuất bản
    'ASSIGNMENT'         -- Phân công chủ đề
);

CREATE TYPE audit_action AS ENUM (
    'CREATE',
    'UPDATE',
    'DELETE',
    'PUBLISH',
    'UNPUBLISH',
    'APPROVE',
    'REJECT',
    'LOCK',
    'UNLOCK',
    'ASSIGN_ROLE',
    'REVOKE_ROLE',
    'IMPORT',
    'ROLLBACK',
    'LOGIN',
    'LOGOUT'
);

-- ============================================================================
-- 2. CORE TABLES — USERS & AUTH
-- ============================================================================

CREATE TABLE users (
    id                  BIGSERIAL PRIMARY KEY,
    email               VARCHAR(255) NOT NULL UNIQUE,
    password_hash       VARCHAR(255) NOT NULL,
    full_name           VARCHAR(150) NOT NULL,
    phone               VARCHAR(20),
    gender              gender_type,
    date_of_birth       DATE,
    avatar_url          VARCHAR(500),
    role                user_role NOT NULL DEFAULT 'STUDENT',
    status              user_status NOT NULL DEFAULT 'ACTIVE',
    email_verified      BOOLEAN NOT NULL DEFAULT FALSE,
    last_login_at       TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_status ON users(status);

-- Refresh tokens cho JWT authentication
CREATE TABLE refresh_tokens (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token               VARCHAR(500) NOT NULL UNIQUE,
    device_info         VARCHAR(255),          -- "Android/Samsung S24", "Web/Chrome"
    expires_at          TIMESTAMPTZ NOT NULL,
    revoked             BOOLEAN NOT NULL DEFAULT FALSE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_tokens_user ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens(token);

-- ============================================================================
-- 3. SUBJECT & TOPIC HIERARCHY
-- ============================================================================

CREATE TABLE subjects (
    id                  BIGSERIAL PRIMARY KEY,
    code                VARCHAR(20) NOT NULL UNIQUE,   -- 'MATH', 'ENG'
    name                VARCHAR(100) NOT NULL,          -- 'Toán', 'Tiếng Anh'
    description         TEXT,
    icon_url            VARCHAR(500),
    display_order       INT NOT NULL DEFAULT 0,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE topics (
    id                  BIGSERIAL PRIMARY KEY,
    subject_id          BIGINT NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,
    code                VARCHAR(50) NOT NULL UNIQUE,    -- 'MATH_ALGEBRA', 'ENG_READING'
    name                VARCHAR(150) NOT NULL,           -- 'Đại số', 'Đọc hiểu'
    description         TEXT,
    display_order       INT NOT NULL DEFAULT 0,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_topics_subject ON topics(subject_id);

CREATE TABLE subtopics (
    id                  BIGSERIAL PRIMARY KEY,
    topic_id            BIGINT NOT NULL REFERENCES topics(id) ON DELETE CASCADE,
    code                VARCHAR(50) NOT NULL UNIQUE,    -- 'MATH_ALGEBRA_EQ2'
    name                VARCHAR(150) NOT NULL,           -- 'Phương trình bậc 2'
    description         TEXT,
    display_order       INT NOT NULL DEFAULT 0,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_subtopics_topic ON subtopics(topic_id);

-- ============================================================================
-- 4. QUESTION BANK
-- ============================================================================

-- Nhóm câu hỏi / passage (dùng chung ngữ liệu)
CREATE TABLE question_groups (
    id                  BIGSERIAL PRIMARY KEY,
    code                VARCHAR(50) UNIQUE,             -- group_code từ Excel
    subject_id          BIGINT NOT NULL REFERENCES subjects(id),
    title               VARCHAR(300),                    -- Tiêu đề nhóm
    passage_text        TEXT,                             -- Đoạn văn / ngữ liệu chung
    passage_html        TEXT,                             -- Phiên bản HTML nếu cần format
    image_url           VARCHAR(500),                    -- Ảnh minh họa passage
    created_by          BIGINT REFERENCES users(id),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_question_groups_subject ON question_groups(subject_id);

-- Bảng câu hỏi chính
CREATE TABLE questions (
    id                  BIGSERIAL PRIMARY KEY,
    question_code       VARCHAR(50) NOT NULL UNIQUE,     -- Mã câu hỏi
    subject_id          BIGINT NOT NULL REFERENCES subjects(id),
    topic_id            BIGINT NOT NULL REFERENCES topics(id),
    subtopic_id         BIGINT REFERENCES subtopics(id),
    question_group_id   BIGINT REFERENCES question_groups(id), -- NULL nếu câu đơn
    difficulty          difficulty_level NOT NULL DEFAULT 'MEDIUM',
    question_type       question_type NOT NULL DEFAULT 'SINGLE_CHOICE',
    question_text       TEXT NOT NULL,                    -- Nội dung câu hỏi
    question_html       TEXT,                             -- Phiên bản HTML/LaTeX render
    image_url           VARCHAR(500),                    -- Ảnh minh họa câu hỏi
    explanation         TEXT,                             -- Lời giải
    explanation_html    TEXT,                             -- Lời giải dạng HTML
    source              VARCHAR(255),                    -- Nguồn (sách, đề thi gốc...)
    tags                VARCHAR(500),                    -- Tags phân loại, comma-separated
    status              question_status NOT NULL DEFAULT 'DRAFT',
    version             INT NOT NULL DEFAULT 1,
    order_in_group      INT,                              -- Thứ tự trong group (nếu có)
    created_by          BIGINT NOT NULL REFERENCES users(id),
    reviewed_by         BIGINT REFERENCES users(id),
    published_at        TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_questions_subject ON questions(subject_id);
CREATE INDEX idx_questions_topic ON questions(topic_id);
CREATE INDEX idx_questions_subtopic ON questions(subtopic_id);
CREATE INDEX idx_questions_group ON questions(question_group_id);
CREATE INDEX idx_questions_status ON questions(status);
CREATE INDEX idx_questions_difficulty ON questions(difficulty);
CREATE INDEX idx_questions_created_by ON questions(created_by);
CREATE INDEX idx_questions_code ON questions(question_code);

-- Đáp án (tách bảng riêng để linh hoạt mở rộng)
CREATE TABLE question_options (
    id                  BIGSERIAL PRIMARY KEY,
    question_id         BIGINT NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    option_label        VARCHAR(5) NOT NULL,              -- 'A', 'B', 'C', 'D'
    option_text         TEXT NOT NULL,                    -- Nội dung đáp án
    option_html         TEXT,                             -- Phiên bản HTML
    image_url           VARCHAR(500),                    -- Ảnh đáp án (nếu có)
    is_correct          BOOLEAN NOT NULL DEFAULT FALSE,
    display_order       INT NOT NULL DEFAULT 0,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_question_options_question ON question_options(question_id);

-- Lịch sử phiên bản câu hỏi
CREATE TABLE question_versions (
    id                  BIGSERIAL PRIMARY KEY,
    question_id         BIGINT NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    version             INT NOT NULL,
    question_text       TEXT NOT NULL,
    question_html       TEXT,
    explanation         TEXT,
    explanation_html    TEXT,
    options_snapshot    JSONB NOT NULL,                   -- Snapshot đáp án tại version đó
    changed_by          BIGINT NOT NULL REFERENCES users(id),
    change_summary      TEXT,                             -- Mô tả ngắn thay đổi
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_question_versions_question ON question_versions(question_id);

-- ============================================================================
-- 5. REVIEW / MODERATION WORKFLOW
-- ============================================================================

CREATE TABLE question_reviews (
    id                  BIGSERIAL PRIMARY KEY,
    question_id         BIGINT NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    reviewer_id         BIGINT NOT NULL REFERENCES users(id),
    action              review_action NOT NULL,
    comment             TEXT,                             -- Lý do duyệt / trả sửa / từ chối
    version_reviewed    INT NOT NULL,                     -- Review ở version nào
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_question_reviews_question ON question_reviews(question_id);
CREATE INDEX idx_question_reviews_reviewer ON question_reviews(reviewer_id);

-- Bình luận nội bộ (trao đổi giữa admin và CTV trên từng câu hỏi)
CREATE TABLE question_comments (
    id                  BIGSERIAL PRIMARY KEY,
    question_id         BIGINT NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
    user_id             BIGINT NOT NULL REFERENCES users(id),
    parent_comment_id   BIGINT REFERENCES question_comments(id), -- Reply thread
    content             TEXT NOT NULL,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_question_comments_question ON question_comments(question_id);

-- ============================================================================
-- 6. EXAM MANAGEMENT
-- ============================================================================

CREATE TABLE exams (
    id                  BIGSERIAL PRIMARY KEY,
    exam_code           VARCHAR(50) NOT NULL UNIQUE,     -- Mã đề
    title               VARCHAR(300) NOT NULL,            -- Tên đề
    subject_id          BIGINT NOT NULL REFERENCES subjects(id),
    description         TEXT,
    question_count      INT NOT NULL DEFAULT 0,           -- Tổng số câu
    duration_minutes    INT NOT NULL,                     -- Thời lượng (phút)
    difficulty          difficulty_level NOT NULL DEFAULT 'MEDIUM',
    pricing_type        exam_pricing_type NOT NULL DEFAULT 'FREE',
    price               DECIMAL(12,2) DEFAULT 0,         -- Giá lượt thi (VND)
    status              exam_status NOT NULL DEFAULT 'DRAFT',
    tags                VARCHAR(500),
    publish_date        TIMESTAMPTZ,
    created_by          BIGINT NOT NULL REFERENCES users(id),
    reviewed_by         BIGINT REFERENCES users(id),
    version             INT NOT NULL DEFAULT 1,
    total_attempts      INT NOT NULL DEFAULT 0,           -- Tổng lượt thi (cache counter)
    avg_score           DECIMAL(5,2),                     -- Điểm TB (cache)
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_exams_subject ON exams(subject_id);
CREATE INDEX idx_exams_status ON exams(status);
CREATE INDEX idx_exams_pricing ON exams(pricing_type);
CREATE INDEX idx_exams_code ON exams(exam_code);

-- Mapping câu hỏi vào đề thi
CREATE TABLE exam_questions (
    id                  BIGSERIAL PRIMARY KEY,
    exam_id             BIGINT NOT NULL REFERENCES exams(id) ON DELETE CASCADE,
    question_id         BIGINT NOT NULL REFERENCES questions(id),
    question_order      INT NOT NULL,                     -- Thứ tự câu trong đề
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    UNIQUE(exam_id, question_id),
    UNIQUE(exam_id, question_order)
);

CREATE INDEX idx_exam_questions_exam ON exam_questions(exam_id);
CREATE INDEX idx_exam_questions_question ON exam_questions(question_id);

-- ============================================================================
-- 7. EXAM SESSION & RESULTS
-- ============================================================================

-- Phiên thi (mỗi lần học viên bắt đầu làm bài)
CREATE TABLE exam_sessions (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL REFERENCES users(id),
    exam_id             BIGINT NOT NULL REFERENCES exams(id),
    mode                session_mode NOT NULL DEFAULT 'MOCK_EXAM',
    status              session_status NOT NULL DEFAULT 'IN_PROGRESS',
    started_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    submitted_at        TIMESTAMPTZ,
    time_spent_seconds  INT,                              -- Thời gian thực tế đã dùng
    total_questions     INT NOT NULL,
    answered_count      INT NOT NULL DEFAULT 0,
    correct_count       INT NOT NULL DEFAULT 0,
    wrong_count         INT NOT NULL DEFAULT 0,
    skipped_count       INT NOT NULL DEFAULT 0,
    score               DECIMAL(5,2),                     -- Điểm (thang 10 hoặc %)
    score_percentage    DECIMAL(5,2),                     -- Phần trăm đúng
    device_type         VARCHAR(20),                      -- 'ANDROID', 'WEB'
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_exam_sessions_user ON exam_sessions(user_id);
CREATE INDEX idx_exam_sessions_exam ON exam_sessions(exam_id);
CREATE INDEX idx_exam_sessions_status ON exam_sessions(status);
CREATE INDEX idx_exam_sessions_started ON exam_sessions(started_at);

-- Chi tiết đáp án từng câu trong phiên thi
CREATE TABLE session_answers (
    id                  BIGSERIAL PRIMARY KEY,
    session_id          BIGINT NOT NULL REFERENCES exam_sessions(id) ON DELETE CASCADE,
    question_id         BIGINT NOT NULL REFERENCES questions(id),
    question_order      INT NOT NULL,
    selected_option_id  BIGINT REFERENCES question_options(id), -- NULL nếu chưa chọn
    is_correct          BOOLEAN,
    is_bookmarked       BOOLEAN NOT NULL DEFAULT FALSE,   -- Đánh dấu câu xem lại
    time_spent_seconds  INT,                               -- Thời gian cho câu này
    answered_at         TIMESTAMPTZ,

    UNIQUE(session_id, question_id)
);

CREATE INDEX idx_session_answers_session ON session_answers(session_id);
CREATE INDEX idx_session_answers_question ON session_answers(question_id);

-- ============================================================================
-- 8. PRACTICE BY TOPIC (Luyện tập theo chủ đề yếu)
-- ============================================================================

-- Thống kê năng lực theo chủ đề của từng học viên
CREATE TABLE user_topic_stats (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    topic_id            BIGINT NOT NULL REFERENCES topics(id),
    total_attempted     INT NOT NULL DEFAULT 0,
    total_correct       INT NOT NULL DEFAULT 0,
    accuracy_rate       DECIMAL(5,2),                     -- % đúng
    last_practiced_at   TIMESTAMPTZ,
    is_weak_topic       BOOLEAN NOT NULL DEFAULT FALSE,   -- Đánh dấu chủ đề yếu
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    UNIQUE(user_id, topic_id)
);

CREATE INDEX idx_user_topic_stats_user ON user_topic_stats(user_id);
CREATE INDEX idx_user_topic_stats_weak ON user_topic_stats(is_weak_topic);

-- ============================================================================
-- 9. TICKET / ISSUE REPORT
-- ============================================================================

CREATE TABLE tickets (
    id                  BIGSERIAL PRIMARY KEY,
    ticket_code         VARCHAR(50) NOT NULL UNIQUE,      -- Mã ticket tự sinh
    reporter_id         BIGINT NOT NULL REFERENCES users(id),
    question_id         BIGINT REFERENCES questions(id),  -- Ticket liên quan câu hỏi
    exam_id             BIGINT REFERENCES exams(id),      -- Ticket liên quan đề thi
    ticket_type         ticket_type NOT NULL,
    status              ticket_status NOT NULL DEFAULT 'NEW',
    title               VARCHAR(300) NOT NULL,
    description         TEXT NOT NULL,
    assigned_to         BIGINT REFERENCES users(id),       -- Admin được phân công xử lý
    resolved_at         TIMESTAMPTZ,
    resolution_note     TEXT,                               -- Ghi chú cách xử lý
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_tickets_reporter ON tickets(reporter_id);
CREATE INDEX idx_tickets_question ON tickets(question_id);
CREATE INDEX idx_tickets_exam ON tickets(exam_id);
CREATE INDEX idx_tickets_status ON tickets(status);
CREATE INDEX idx_tickets_assigned ON tickets(assigned_to);

-- Lịch sử xử lý ticket
CREATE TABLE ticket_comments (
    id                  BIGSERIAL PRIMARY KEY,
    ticket_id           BIGINT NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    user_id             BIGINT NOT NULL REFERENCES users(id),
    content             TEXT NOT NULL,
    old_status          ticket_status,
    new_status          ticket_status,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ticket_comments_ticket ON ticket_comments(ticket_id);

-- ============================================================================
-- 10. EXCEL IMPORT (2 tầng: staging → question bank)
-- ============================================================================

-- Tầng 1: Batch import
CREATE TABLE import_batches (
    id                  BIGSERIAL PRIMARY KEY,
    batch_code          VARCHAR(50) NOT NULL UNIQUE,
    uploaded_by         BIGINT NOT NULL REFERENCES users(id),
    approved_by         BIGINT REFERENCES users(id),
    subject_id          BIGINT REFERENCES subjects(id),
    file_name           VARCHAR(255) NOT NULL,
    file_url            VARCHAR(500),
    file_size_bytes     BIGINT,
    status              import_batch_status NOT NULL DEFAULT 'UPLOADED',
    total_rows          INT NOT NULL DEFAULT 0,
    valid_rows          INT NOT NULL DEFAULT 0,
    warning_rows        INT NOT NULL DEFAULT 0,
    error_rows          INT NOT NULL DEFAULT 0,
    imported_rows       INT NOT NULL DEFAULT 0,            -- Số dòng đã nhập thành công
    error_summary       JSONB,                             -- Tóm tắt lỗi tổng
    approved_at         TIMESTAMPTZ,
    completed_at        TIMESTAMPTZ,
    rolled_back_at      TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_import_batches_uploaded_by ON import_batches(uploaded_by);
CREATE INDEX idx_import_batches_status ON import_batches(status);

-- Tầng 1: Từng dòng trong batch
CREATE TABLE import_rows (
    id                  BIGSERIAL PRIMARY KEY,
    batch_id            BIGINT NOT NULL REFERENCES import_batches(id) ON DELETE CASCADE,
    row_number          INT NOT NULL,                      -- Số dòng trong file Excel
    raw_data            JSONB NOT NULL,                    -- Dữ liệu gốc từ Excel
    status              import_row_status NOT NULL DEFAULT 'VALID',
    errors              JSONB,                             -- [{field, message, level}]
    question_id         BIGINT REFERENCES questions(id),   -- Liên kết sau khi import thành công
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_import_rows_batch ON import_rows(batch_id);
CREATE INDEX idx_import_rows_status ON import_rows(status);

-- ============================================================================
-- 11. COMMERCE — VÍ, GÓI, GIAO DỊCH
-- ============================================================================

-- Ví người dùng
CREATE TABLE wallets (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    balance             DECIMAL(15,2) NOT NULL DEFAULT 0,  -- Số dư (VND)
    total_deposited     DECIMAL(15,2) NOT NULL DEFAULT 0,
    total_spent         DECIMAL(15,2) NOT NULL DEFAULT 0,
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Gói đề thi
CREATE TABLE packages (
    id                  BIGSERIAL PRIMARY KEY,
    code                VARCHAR(50) NOT NULL UNIQUE,
    name                VARCHAR(200) NOT NULL,
    description         TEXT,
    package_type        package_type NOT NULL,
    subject_id          BIGINT REFERENCES subjects(id),    -- NULL nếu combo nhiều môn
    price               DECIMAL(12,2) NOT NULL,
    original_price      DECIMAL(12,2),                     -- Giá gốc (hiện giảm giá)
    exam_count          INT,                                -- Số đề trong gói
    duration_days       INT,                                -- Thời hạn sử dụng (NULL = vĩnh viễn)
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Đề nào thuộc gói nào
CREATE TABLE package_exams (
    id                  BIGSERIAL PRIMARY KEY,
    package_id          BIGINT NOT NULL REFERENCES packages(id) ON DELETE CASCADE,
    exam_id             BIGINT NOT NULL REFERENCES exams(id),
    UNIQUE(package_id, exam_id)
);

-- Lịch sử giao dịch
CREATE TABLE transactions (
    id                  BIGSERIAL PRIMARY KEY,
    transaction_code    VARCHAR(50) NOT NULL UNIQUE,
    user_id             BIGINT NOT NULL REFERENCES users(id),
    transaction_type    transaction_type NOT NULL,
    amount              DECIMAL(12,2) NOT NULL,
    balance_before      DECIMAL(15,2) NOT NULL,
    balance_after       DECIMAL(15,2) NOT NULL,
    status              transaction_status NOT NULL DEFAULT 'PENDING',
    -- Tham chiếu đối tượng giao dịch
    exam_id             BIGINT REFERENCES exams(id),
    package_id          BIGINT REFERENCES packages(id),
    description         VARCHAR(500),
    payment_method      VARCHAR(50),                       -- 'WALLET', 'MOMO', 'BANK'...
    external_ref        VARCHAR(255),                      -- Mã giao dịch bên ngoài
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_transactions_user ON transactions(user_id);
CREATE INDEX idx_transactions_status ON transactions(status);
CREATE INDEX idx_transactions_type ON transactions(transaction_type);

-- Quyền truy cập đề thi đã mua
CREATE TABLE user_exam_access (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    exam_id             BIGINT NOT NULL REFERENCES exams(id),
    transaction_id      BIGINT REFERENCES transactions(id),
    granted_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at          TIMESTAMPTZ,                        -- NULL = vĩnh viễn
    max_attempts        INT,                                -- NULL = không giới hạn
    used_attempts       INT NOT NULL DEFAULT 0,

    UNIQUE(user_id, exam_id)
);

CREATE INDEX idx_user_exam_access_user ON user_exam_access(user_id);

-- ============================================================================
-- 12. COLLABORATOR WORKSPACE
-- ============================================================================

-- Phân công chủ đề cho CTV
CREATE TABLE collaborator_assignments (
    id                  BIGSERIAL PRIMARY KEY,
    collaborator_id     BIGINT NOT NULL REFERENCES users(id),
    topic_id            BIGINT NOT NULL REFERENCES topics(id),
    assigned_by         BIGINT NOT NULL REFERENCES users(id),
    target_count        INT,                                -- Mục tiêu số câu cần đóng góp
    completed_count     INT NOT NULL DEFAULT 0,
    note                TEXT,
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    assigned_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deadline            TIMESTAMPTZ,

    UNIQUE(collaborator_id, topic_id)
);

CREATE INDEX idx_collab_assignments_collab ON collaborator_assignments(collaborator_id);

-- ============================================================================
-- 13. NOTIFICATIONS
-- ============================================================================

CREATE TABLE notifications (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    notification_type   notification_type NOT NULL,
    title               VARCHAR(300) NOT NULL,
    content             TEXT,
    -- Tham chiếu đối tượng liên quan
    reference_type      VARCHAR(50),                       -- 'QUESTION', 'EXAM', 'TICKET'
    reference_id        BIGINT,
    is_read             BOOLEAN NOT NULL DEFAULT FALSE,
    read_at             TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_user ON notifications(user_id);
CREATE INDEX idx_notifications_unread ON notifications(user_id, is_read);

-- ============================================================================
-- 14. AUDIT LOG
-- ============================================================================

CREATE TABLE audit_logs (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT REFERENCES users(id),       -- NULL nếu hệ thống tự động
    action              audit_action NOT NULL,
    entity_type         VARCHAR(50) NOT NULL,               -- 'QUESTION', 'EXAM', 'USER'...
    entity_id           BIGINT,
    old_value           JSONB,                              -- Giá trị cũ (nếu update)
    new_value           JSONB,                              -- Giá trị mới
    ip_address          VARCHAR(45),
    user_agent          VARCHAR(500),
    description         TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_logs_user ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_audit_logs_created ON audit_logs(created_at);

-- ============================================================================
-- 15. SYSTEM CONFIGURATION
-- ============================================================================

CREATE TABLE system_configs (
    id                  BIGSERIAL PRIMARY KEY,
    config_key          VARCHAR(100) NOT NULL UNIQUE,
    config_value        TEXT NOT NULL,
    description         VARCHAR(500),
    updated_by          BIGINT REFERENCES users(id),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Cấu hình ban đầu
INSERT INTO system_configs (config_key, config_value, description) VALUES
    ('exam.default_duration_minutes', '90', 'Thời lượng thi mặc định (phút)'),
    ('exam.auto_submit_on_timeout', 'true', 'Tự động nộp bài khi hết giờ'),
    ('import.max_file_size_mb', '10', 'Dung lượng tối đa file Excel import (MB)'),
    ('import.max_rows_per_batch', '500', 'Số dòng tối đa mỗi batch import'),
    ('wallet.min_deposit', '10000', 'Số tiền nạp tối thiểu (VND)'),
    ('app.maintenance_mode', 'false', 'Chế độ bảo trì hệ thống');

-- ============================================================================
-- 16. AUTO-UPDATE TIMESTAMPS TRIGGER
-- ============================================================================

CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Apply trigger cho tất cả bảng có updated_at
CREATE TRIGGER trg_users_updated_at BEFORE UPDATE ON users FOR EACH ROW EXECUTE FUNCTION update_updated_at();
CREATE TRIGGER trg_subjects_updated_at BEFORE UPDATE ON subjects FOR EACH ROW EXECUTE FUNCTION update_updated_at();
CREATE TRIGGER trg_topics_updated_at BEFORE UPDATE ON topics FOR EACH ROW EXECUTE FUNCTION update_updated_at();
CREATE TRIGGER trg_subtopics_updated_at BEFORE UPDATE ON subtopics FOR EACH ROW EXECUTE FUNCTION update_updated_at();
CREATE TRIGGER trg_questions_updated_at BEFORE UPDATE ON questions FOR EACH ROW EXECUTE FUNCTION update_updated_at();
CREATE TRIGGER trg_question_options_updated_at BEFORE UPDATE ON question_options FOR EACH ROW EXECUTE FUNCTION update_updated_at();
CREATE TRIGGER trg_question_groups_updated_at BEFORE UPDATE ON question_groups FOR EACH ROW EXECUTE FUNCTION update_updated_at();
CREATE TRIGGER trg_exams_updated_at BEFORE UPDATE ON exams FOR EACH ROW EXECUTE FUNCTION update_updated_at();
CREATE TRIGGER trg_tickets_updated_at BEFORE UPDATE ON tickets FOR EACH ROW EXECUTE FUNCTION update_updated_at();
CREATE TRIGGER trg_import_batches_updated_at BEFORE UPDATE ON import_batches FOR EACH ROW EXECUTE FUNCTION update_updated_at();
CREATE TRIGGER trg_wallets_updated_at BEFORE UPDATE ON wallets FOR EACH ROW EXECUTE FUNCTION update_updated_at();
CREATE TRIGGER trg_packages_updated_at BEFORE UPDATE ON packages FOR EACH ROW EXECUTE FUNCTION update_updated_at();
CREATE TRIGGER trg_transactions_updated_at BEFORE UPDATE ON transactions FOR EACH ROW EXECUTE FUNCTION update_updated_at();

-- ============================================================================
-- 17. SEED DATA — MÔN HỌC MVP
-- ============================================================================

INSERT INTO subjects (code, name, description, display_order) VALUES
    ('MATH', 'Toán', 'Toán học V-SAT', 1),
    ('ENG', 'Tiếng Anh', 'Tiếng Anh V-SAT', 2);
