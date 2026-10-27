# V-SAT Compass — Khảo sát nghiệp vụ và Đặc tả chức năng Module Quản trị Đề thi Web Admin

## 1. Tổng quan

Tài liệu này đặc tả nghiệp vụ và chức năng cho **Module Quản trị Đề thi trên Web App** của hệ thống **V-SAT Compass**.

### Kiến trúc hiện tại

- **Backend:** Spring Boot 3.5.x
- **Database:** PostgreSQL 17
- **Mobile Client:** Android Java
- **Web Admin:** React/Vue/Vite
- **Các bảng hiện có:** `exams`, `questions`, `question_options`, `exam_questions`, `exam_sessions`, `session_answers`, `question_groups`, `import_batches`, `import_rows`

Mục tiêu của module Web Admin:

- Biên soạn đề thi chuẩn V-SAT.
- Quản trị ngân hàng câu hỏi.
- Sinh đề thủ công hoặc tự động theo ma trận.
- Quản lý mã đề/phiên bản.
- Cấu hình thời gian thi, ca thi, lượt thi.
- Kiểm tra toàn vẹn đề trước khi xuất bản.
- Hỗ trợ LaTeX, hình ảnh và import hàng loạt.
- Tương thích với Android Client hiện tại.

---

# 2. KHẢO SÁT QUY CHUẨN ĐỀ THI V-SAT

## 2.1. Cấu trúc 7 môn thi

Theo cấu trúc V-SAT 2026 công bố, bảy môn:

- Toán
- Vật lí
- Hóa học
- Sinh học
- Lịch sử
- Địa lí
- Tiếng Anh

có cùng cấu trúc dạng thức câu hỏi.

| Môn | Thời gian | Đúng/Sai | MCQ 4 lựa chọn | Ghép hợp | Trả lời ngắn | Tổng câu | Điểm thô tối đa |
|---|---:|---:|---:|---:|---:|---:|---:|
| Toán | 90 phút | 9 | 6 | 5 | 5 | 25 | 150 |
| Vật lí | 60 phút | 9 | 6 | 5 | 5 | 25 | 150 |
| Hóa học | 60 phút | 9 | 6 | 5 | 5 | 25 | 150 |
| Sinh học | 60 phút | 9 | 6 | 5 | 5 | 25 | 150 |
| Lịch sử | 60 phút | 9 | 6 | 5 | 5 | 25 | 150 |
| Địa lí | 60 phút | 9 | 6 | 5 | 5 | 25 | 150 |
| Tiếng Anh | 60 phút | 9 | 6 | 5 | 5 | 25 | 150 |

### Phân bổ điểm theo dạng câu hỏi

| Dạng | Số câu | Số tiểu mục | Điểm tối đa |
|---|---:|---:|---:|
| Đúng/Sai | 9 | 36 | 54 |
| MCQ 4 lựa chọn | 6 | 24 lựa chọn | 36 |
| Ghép hợp | 5 | 20 | 30 |
| Trả lời ngắn | 5 | 5 | 30 |
| **Tổng** | **25** | — | **150** |

> Lưu ý: Không được hiểu 24 lựa chọn của phần MCQ là 24 đáp án độc lập. Mỗi câu MCQ vẫn chỉ có một đáp án đúng duy nhất và mỗi câu đúng được tính 6 điểm.

---

## 2.2. Quy tắc tính điểm

### 2.2.1. Dạng Đúng/Sai

Mỗi câu có 4 mệnh đề con: `a`, `b`, `c`, `d`.

Đặt:

```text
k = số mệnh đề được trả lời đúng
```

Điểm của một câu:

```text
scoreTF(k) =
    0 nếu k = 0
    1 nếu k = 1
    2 nếu k = 2
    3 nếu k = 3
    6 nếu k = 4
```

Tổng tối đa:

```text
9 × 6 = 54 điểm
```

> Công thức `0.1 → 0.25 → 0.5 → 1.0` không phải công thức chấm V-SAT 2026.

---

### 2.2.2. Trắc nghiệm 4 lựa chọn

Mỗi câu có 4 lựa chọn A/B/C/D, đúng duy nhất một phương án.

```text
Đúng = 6 điểm
Sai / bỏ = 0 điểm
```

Tổng tối đa:

```text
6 × 6 = 36 điểm
```

---

### 2.2.3. Ghép hợp

Mỗi câu có 4 cặp cần ghép đúng.

```text
Mỗi cặp đúng = 1.5 điểm
```

Một câu:

```text
4 × 1.5 = 6 điểm
```

Tổng tối đa:

```text
5 × 6 = 30 điểm
```

---

### 2.2.4. Trả lời ngắn

```text
Đúng = 6 điểm
Sai / bỏ = 0 điểm
```

Tổng tối đa:

```text
5 × 6 = 30 điểm
```

---

## 2.3. Tổng điểm

```text
54 + 36 + 30 + 30 = 150 điểm
```

V-SAT Compass nên lưu tối thiểu:

```text
raw_score
max_raw_score
score_percentage
scaled_score
```

Trong đó:

- `raw_score`: điểm thô, tối đa 150.
- `score_percentage`: tỷ lệ phần trăm.
- `scaled_score`: điểm năng lực chuẩn hóa nếu có mô hình IRT.

---

## 2.4. Chuẩn hóa điểm và IRT

Không được sử dụng các công thức tuyến tính như:

```text
scaled = raw / 150 × 100
```

hoặc:

```text
scaled = raw × constant
```

để gọi là "điểm V-SAT chuẩn hóa".

Điểm chuẩn hóa V-SAT được xây dựng dựa trên mô hình **Item Response Theory (IRT)**.

Hệ thống cần phân biệt:

```text
Raw Score
Scaled Score
```

Nếu chưa có tham số định cỡ câu hỏi chính thức:

```text
scaled_score = NULL
```

và giao diện chỉ hiển thị:

```text
Điểm thô: 126 / 150
Tỷ lệ: 84%
```

Không được gọi điểm tự quy đổi là "điểm năng lực V-SAT chính thức".

---

## 2.5. Tách Difficulty và Cognitive Level

Repo hiện tại có:

```java
EASY
MEDIUM
HARD
VERY_HARD
```

Đây là **độ khó**.

Trong khi ma trận khảo thí cần thêm **mức độ nhận thức**:

```text
RECOGNITION
UNDERSTANDING
APPLICATION
HIGH_APPLICATION
```

Tương ứng:

- Nhận biết
- Thông hiểu
- Vận dụng
- Vận dụng cao

Không nên ánh xạ:

```text
EASY = Nhận biết
MEDIUM = Thông hiểu
```

vì hai khái niệm này khác nhau về nghiệp vụ khảo thí.

---

# 3. CƠ CHẾ TẠO ĐỀ TRÊN WEB APP

Ba phương thức tạo đề:

1. Manual Composition
2. Matrix-based Auto Generation
3. Bulk Import

Tất cả đều hội tụ về một workflow chung:

```text
Exam Draft
   ↓
Composition
   ↓
Validation
   ↓
Review
   ↓
Publish
```

---

# 4. TẠO ĐỀ THỦ CÔNG — MANUAL COMPOSITION

## 4.1. Bộ lọc ngân hàng câu hỏi

Web Admin cần hỗ trợ:

```text
Subject
Topic
Subtopic
Question Type
Cognitive Level
Difficulty
Status
Tags
Source
Has Image
Has LaTeX
Question Group
```

### Question Type

```text
TRUE_FALSE
SINGLE_CHOICE
MATCHING
SHORT_ANSWER
```

### Question Status

Chỉ nên cho phép đưa vào đề chuẩn bị publish các câu:

```text
APPROVED
```

---

## 4.2. Giao diện Builder

Bố cục đề xuất:

```text
┌──────────────────────┬─────────────────────────┬───────────────────┐
│ QUESTION BANK        │ EXAM COMPOSITION        │ EXAM INSPECTOR    │
│                      │                         │                   │
│ Search               │ Section 1: True/False   │ Questions 25/25   │
│ Subject              │ [Q01]                   │ Raw 150/150       │
│ Topic                │ [Q02]                   │ Latex errors: 0   │
│ Type                 │ ...                     │                   │
│ Cognitive Level      │                         │ Matrix coverage   │
│ Difficulty           │ Section 2: MCQ          │ ██████████        │
│                      │ ...                     │                   │
└──────────────────────┴─────────────────────────┴───────────────────┘
```

Drag & Drop nên bị giới hạn theo section.

Ví dụ:

- TRUE_FALSE chỉ được kéo vào section Đúng/Sai.
- MATCHING chỉ được kéo vào section Ghép hợp.

---

# 5. WYSIWYG EDITOR

Editor cần hỗ trợ:

```text
Rich Text
Inline LaTeX
Block LaTeX
Image
Table
Chart
Passage
Shared Stimulus
```

Ví dụ:

```text
Inline math:
\(x^2 + 2x + 1\)
```

```text
Block:
\[
\int_0^1 x^2 dx
\]
```

Stack frontend đề xuất:

```text
React + TypeScript + Vite
TipTap
KaTeX
DOMPurify
```

Không render trực tiếp `question_html` chưa sanitize để tránh Stored XSS.

---

# 6. SINH ĐỀ TỰ ĐỘNG THEO MA TRẬN

Ma trận nên có ba chiều:

```text
Content
    Topic / Subtopic

Cognitive Level
    Recognition
    Understanding
    Application
    High Application

Question Type
    TRUE_FALSE
    SINGLE_CHOICE
    MATCHING
    SHORT_ANSWER
```

Ví dụ:

| Chủ đề | NB | TH | VD | VDC | Tổng |
|---|---:|---:|---:|---:|---:|
| Hàm số | 2 | 2 | 1 | 1 | 6 |
| Mũ - Log | 1 | 2 | 1 | 0 | 4 |
| Tích phân | 1 | 1 | 2 | 1 | 5 |
| Hình học | 1 | 2 | 2 | 1 | 6 |
| Xác suất | 1 | 1 | 1 | 1 | 4 |

> Đây chỉ là ví dụ về cách biểu diễn ma trận, không phải ma trận nội dung chính thức của môn Toán.

---

## 6.1. Thuật toán sinh đề

Không nên dùng đơn thuần:

```java
Collections.shuffle(allQuestions);
take(25);
```

Pipeline đề xuất:

```text
Blueprint
   ↓
Build Candidate Pools
   ↓
Check Pool Capacity
   ↓
Allocate Scarce Cells First
   ↓
Weighted Random Selection
   ↓
Check Cross-cell Constraints
   ↓
Backtracking
   ↓
Integrity Validation
   ↓
Persist Composition
```

Candidate query:

```text
status = APPROVED
AND subject_id = ...
AND topic_id = ...
AND cognitive_level = ...
AND question_type = ...
AND question_id NOT IN selected
```

Có thể sử dụng trọng số:

```text
weight =
    base_weight
    × exposure_penalty
    × recency_penalty
```

để tránh câu hỏi bị sử dụng lặp lại quá nhiều.

---

# 7. TẠO MÃ ĐỀ / VARIANTS

## 7.1. Permutation Variant

Cùng một tập câu hỏi nhưng:

```text
shuffle question order
shuffle MCQ options
shuffle matching choices
```

Ví dụ:

```text
101
102
103
104
```

Đây là cơ chế nên triển khai trước.

---

## 7.2. Parallel-form Variant

Mỗi mã đề dùng câu hỏi khác nhau nhưng cùng blueprint.

Yêu cầu:

```text
content equivalence
difficulty equivalence
IRT information equivalence
```

Nếu Question Bank chưa được hiệu chuẩn psychometric thì không nên gọi các phiên bản này là "đề tương đương chuẩn hóa".

---

# 8. BULK IMPORT

Repo hiện tại đã có:

```text
import_batches
import_rows
```

nên không cần xây import subsystem khác.

Workflow:

```text
Upload
 ↓
Parse
 ↓
Staging
 ↓
Schema Validation
 ↓
Business Validation
 ↓
LaTeX Validation
 ↓
Preview
 ↓
Commit
```

---

## 8.1. Excel Format

Các cột đề xuất:

```text
question_code
subject_code
topic_code
subtopic_code
question_type
cognitive_level
difficulty
group_code
question_text
option_a
option_b
option_c
option_d
correct_answer
explanation
source
tags
```

True/False:

```text
statement_a
statement_a_answer
statement_b
statement_b_answer
statement_c
statement_c_answer
statement_d
statement_d_answer
```

---

## 8.2. Word Tagged Format

Ví dụ:

```text
[QUESTION]
CODE=MATH_0001
TYPE=TRUE_FALSE
TOPIC=CALCULUS
COGNITIVE=APPLICATION

[STEM]
Cho hàm số \(f(x)=...\)

[A:true]
...

[B:false]
...

[C:true]
...

[D:true]
...

[EXPLANATION]
...
[/QUESTION]
```

DOCX chỉ nên là định dạng biên soạn thuận tiện, không phải canonical format.

---

## 8.3. JSON Canonical Format

```json
{
  "questionCode": "MATH_TF_0001",
  "subjectCode": "MATH",
  "topicCode": "CALCULUS",
  "subtopicCode": "DERIVATIVE",
  "questionType": "TRUE_FALSE",
  "cognitiveLevel": "APPLICATION",
  "difficulty": "HARD",
  "stem": "Cho hàm số \\(f(x)=...\\)",
  "statements": [
    {
      "key": "a",
      "content": "...",
      "correct": true
    },
    {
      "key": "b",
      "content": "...",
      "correct": false
    },
    {
      "key": "c",
      "content": "...",
      "correct": true
    },
    {
      "key": "d",
      "content": "...",
      "correct": true
    }
  ],
  "explanation": "..."
}
```

---

## 8.4. Validation Error Format

```json
{
  "row": 18,
  "questionCode": "MATH_TF_0023",
  "status": "ERROR",
  "errors": [
    {
      "field": "statements",
      "code": "TF_REQUIRES_4_STATEMENTS",
      "message": "Câu hỏi Đúng/Sai phải có đúng 4 mệnh đề."
    },
    {
      "field": "questionText",
      "code": "LATEX_PARSE_ERROR",
      "message": "Unexpected token at position 42."
    }
  ]
}
```

---

# 9. TIMING & ACCESS RULES

Không nên lưu toàn bộ cấu hình ca thi trực tiếp trong `exams`.

Quan hệ đề xuất:

```text
exam
   ↓
exam_windows
```

Một đề có thể có nhiều ca.

---

## 9.1. Practice Mode

```text
mode = PRACTICE
start_at = NULL
end_at = NULL
attempt_limit = NULL
```

Có thể cấu hình:

```text
showAnswerImmediately = true
showExplanationImmediately = true
```

---

## 9.2. Scheduled Mock Exam

Ví dụ:

```text
start_at = 2026-10-20T08:00:00+07:00
end_at = 2026-10-20T09:30:00+07:00
grace_period_minutes = 15
```

Hạn cuối vào thi:

```text
08:15
```

---

## 9.3. Deadline Session

```text
deadline =
min(
    session.startedAt + duration,
    examWindow.endAt
)
```

Nếu:

```text
hard_stop_at_end = true
```

thì ca thi đóng lúc nào, mọi session cũng kết thúc muộn nhất tại thời điểm đó.

---

## 9.4. Đồng hồ thi

Không để client tự quyết định hoàn toàn bằng:

```javascript
Date.now()
```

Server cần trả:

```json
{
  "sessionId": 9876,
  "serverTime": "2026-10-20T08:02:14+07:00",
  "startedAt": "2026-10-20T08:02:14+07:00",
  "deadlineAt": "2026-10-20T09:30:00+07:00",
  "remainingSeconds": 5266
}
```

Client dùng server time làm authority.

---

## 9.5. Offline Mode

Phân biệt:

```text
Practice Offline
```

và:

```text
Scheduled Secure Exam
```

Practice offline có thể cho phép.

Scheduled Mock có yêu cầu chống gian lận thì không nên fully offline.

Nếu vẫn cần offline:

```text
signed exam manifest
notBefore
notAfter
content hash
encrypted answer key
```

---

## 9.6. Result Release Policy

Đề xuất enum:

```text
IMMEDIATE_SCORE
IMMEDIATE_FULL
AFTER_WINDOW_SCORE
AFTER_WINDOW_FULL
MANUAL
```

Practice:

```text
IMMEDIATE_FULL
```

Scheduled Mock:

```text
AFTER_WINDOW_FULL
```

---

# 10. DATABASE MIGRATION

## 10.1. Cognitive Level

```sql
CREATE TYPE cognitive_level AS ENUM (
    'RECOGNITION',
    'UNDERSTANDING',
    'APPLICATION',
    'HIGH_APPLICATION'
);

ALTER TABLE questions
ADD COLUMN cognitive_level cognitive_level;
```

Sau khi backfill:

```sql
ALTER TABLE questions
ALTER COLUMN cognitive_level SET NOT NULL;
```

---

# 11. TRUE/FALSE MODEL

```sql
CREATE TABLE question_true_false_items (
    id BIGSERIAL PRIMARY KEY,

    question_id BIGINT NOT NULL
        REFERENCES questions(id)
        ON DELETE CASCADE,

    item_key VARCHAR(5) NOT NULL,

    statement_text TEXT NOT NULL,
    statement_html TEXT,

    is_correct BOOLEAN NOT NULL,

    display_order INT NOT NULL,

    UNIQUE(question_id, item_key),
    UNIQUE(question_id, display_order)
);
```

---

# 12. MATCHING MODEL

```sql
CREATE TABLE question_matching_choices (
    id BIGSERIAL PRIMARY KEY,

    question_id BIGINT NOT NULL
        REFERENCES questions(id)
        ON DELETE CASCADE,

    choice_key VARCHAR(10) NOT NULL,

    choice_text TEXT NOT NULL,
    choice_html TEXT,

    display_order INT NOT NULL,

    UNIQUE(question_id, choice_key),
    UNIQUE(question_id, display_order)
);
```

```sql
CREATE TABLE question_matching_items (
    id BIGSERIAL PRIMARY KEY,

    question_id BIGINT NOT NULL
        REFERENCES questions(id)
        ON DELETE CASCADE,

    item_key VARCHAR(10) NOT NULL,

    item_text TEXT NOT NULL,
    item_html TEXT,

    correct_choice_key VARCHAR(10) NOT NULL,

    display_order INT NOT NULL,

    UNIQUE(question_id, item_key)
);
```

---

# 13. SHORT ANSWER MODEL

```sql
CREATE TYPE short_answer_value_type
AS ENUM (
    'TEXT',
    'NUMERIC'
);
```

```sql
CREATE TABLE question_short_answer_keys (
    question_id BIGINT PRIMARY KEY
        REFERENCES questions(id)
        ON DELETE CASCADE,

    value_type short_answer_value_type NOT NULL,

    accepted_answers JSONB NOT NULL DEFAULT '[]'::jsonb,

    numeric_value NUMERIC(20,8),

    numeric_tolerance NUMERIC(20,8),

    unit VARCHAR(50),

    case_sensitive BOOLEAN NOT NULL DEFAULT FALSE,

    trim_whitespace BOOLEAN NOT NULL DEFAULT TRUE
);
```

Ví dụ:

```json
{
  "valueType": "NUMERIC",
  "numericValue": 12.5,
  "numericTolerance": 0.01,
  "unit": null
}
```

---

# 14. SCORING PROFILE

```sql
CREATE TABLE scoring_profiles (
    id BIGSERIAL PRIMARY KEY,

    code VARCHAR(50) NOT NULL UNIQUE,

    name VARCHAR(200) NOT NULL,

    max_raw_score NUMERIC(7,2) NOT NULL,

    rules JSONB NOT NULL,

    scaled_method VARCHAR(30),

    scaled_mean NUMERIC(8,2),

    scaled_sd NUMERIC(8,2),

    version INT NOT NULL DEFAULT 1,

    is_active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

Ví dụ rule:

```json
{
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
}
```

Liên kết exam:

```sql
ALTER TABLE exams
ADD COLUMN scoring_profile_id BIGINT
REFERENCES scoring_profiles(id);
```

---

# 15. EXAM SECTION

```sql
CREATE TABLE exam_sections (
    id BIGSERIAL PRIMARY KEY,

    exam_id BIGINT NOT NULL
        REFERENCES exams(id)
        ON DELETE CASCADE,

    question_type question_type NOT NULL,

    section_order INT NOT NULL,

    required_question_count INT NOT NULL,

    max_raw_score NUMERIC(7,2) NOT NULL,

    UNIQUE(exam_id, section_order),

    UNIQUE(exam_id, question_type)
);
```

```sql
ALTER TABLE exam_questions
ADD COLUMN section_id BIGINT
REFERENCES exam_sections(id);
```

```sql
ALTER TABLE exam_questions
ADD COLUMN max_raw_score NUMERIC(7,2);
```

---

# 16. EXAM WINDOW

```sql
CREATE TYPE exam_delivery_mode AS ENUM (
    'PRACTICE',
    'SCHEDULED_MOCK'
);
```

```sql
CREATE TYPE result_release_policy AS ENUM (
    'IMMEDIATE_SCORE',
    'IMMEDIATE_FULL',
    'AFTER_WINDOW_SCORE',
    'AFTER_WINDOW_FULL',
    'MANUAL'
);
```

```sql
CREATE TABLE exam_windows (
    id BIGSERIAL PRIMARY KEY,

    exam_id BIGINT NOT NULL
        REFERENCES exams(id)
        ON DELETE CASCADE,

    window_code VARCHAR(50) NOT NULL,

    mode exam_delivery_mode NOT NULL,

    start_at TIMESTAMPTZ,

    end_at TIMESTAMPTZ,

    grace_period_minutes INT NOT NULL DEFAULT 0,

    max_attempts INT,

    result_policy result_release_policy NOT NULL,

    hard_stop_at_end BOOLEAN NOT NULL DEFAULT TRUE,

    allow_offline BOOLEAN NOT NULL DEFAULT FALSE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    UNIQUE(exam_id, window_code),

    CHECK (
        mode = 'PRACTICE'
        OR (
            start_at IS NOT NULL
            AND end_at IS NOT NULL
            AND end_at > start_at
        )
    ),

    CHECK (
        max_attempts IS NULL OR max_attempts > 0
    )
);
```

---

# 17. SESSION ANSWER — MỞ RỘNG

Schema hiện tại chỉ có:

```text
selected_option_id
```

nên chưa đủ cho True/False, Matching và Short Answer.

Bổ sung:

```sql
ALTER TABLE session_answers
ADD COLUMN answer_payload JSONB;
```

```sql
ALTER TABLE session_answers
ADD COLUMN correct_subitems INT;
```

```sql
ALTER TABLE session_answers
ADD COLUMN total_subitems INT;
```

```sql
ALTER TABLE session_answers
ADD COLUMN raw_points_awarded NUMERIC(7,2);
```

True/False:

```json
{
  "a": true,
  "b": false,
  "c": true,
  "d": true
}
```

Matching:

```json
{
  "1": "C",
  "2": "A",
  "3": "F",
  "4": "B"
}
```

Short Answer:

```json
{
  "value": "12.5"
}
```

Single Choice vẫn giữ `selected_option_id` để tương thích Android hiện tại.

---

# 18. SESSION SCORE — MỞ RỘNG

```sql
ALTER TABLE exam_sessions
ADD COLUMN raw_score NUMERIC(7,2);
```

```sql
ALTER TABLE exam_sessions
ADD COLUMN max_raw_score NUMERIC(7,2);
```

```sql
ALTER TABLE exam_sessions
ADD COLUMN scaled_score NUMERIC(8,2);
```

```sql
ALTER TABLE exam_sessions
ADD COLUMN scoring_profile_code VARCHAR(50);
```

```sql
ALTER TABLE exam_sessions
ADD COLUMN scoring_model_version VARCHAR(50);
```

```sql
ALTER TABLE exam_sessions
ADD COLUMN deadline_at TIMESTAMPTZ;
```

```sql
ALTER TABLE exam_sessions
ADD COLUMN exam_window_id BIGINT
REFERENCES exam_windows(id);
```

---

# 19. EXAM VARIANTS

```sql
CREATE TABLE exam_variants (
    id BIGSERIAL PRIMARY KEY,

    exam_id BIGINT NOT NULL
        REFERENCES exams(id)
        ON DELETE CASCADE,

    variant_code VARCHAR(30) NOT NULL,

    random_seed VARCHAR(100) NOT NULL,

    content_hash VARCHAR(64),

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    UNIQUE(exam_id, variant_code)
);
```

```sql
CREATE TABLE exam_variant_questions (
    id BIGSERIAL PRIMARY KEY,

    variant_id BIGINT NOT NULL
        REFERENCES exam_variants(id)
        ON DELETE CASCADE,

    exam_question_id BIGINT NOT NULL
        REFERENCES exam_questions(id),

    question_order INT NOT NULL,

    option_order JSONB,

    subitem_order JSONB,

    UNIQUE(variant_id, question_order)
);
```

---

# 20. EXAM BLUEPRINT

```sql
CREATE TABLE exam_blueprints (
    id BIGSERIAL PRIMARY KEY,

    code VARCHAR(50) NOT NULL UNIQUE,

    name VARCHAR(200) NOT NULL,

    subject_id BIGINT NOT NULL
        REFERENCES subjects(id),

    scoring_profile_id BIGINT
        REFERENCES scoring_profiles(id),

    total_questions INT NOT NULL,

    max_raw_score NUMERIC(7,2) NOT NULL,

    version INT NOT NULL DEFAULT 1,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

```sql
CREATE TABLE exam_blueprint_cells (
    id BIGSERIAL PRIMARY KEY,

    blueprint_id BIGINT NOT NULL
        REFERENCES exam_blueprints(id)
        ON DELETE CASCADE,

    topic_id BIGINT REFERENCES topics(id),

    subtopic_id BIGINT REFERENCES subtopics(id),

    question_type question_type NOT NULL,

    cognitive_level cognitive_level,

    difficulty difficulty_level,

    question_count INT NOT NULL,

    CHECK(question_count > 0)
);
```

---

# 21. IRT / PSYCHOMETRIC MODEL

Nếu sau này hệ thống cần scaled score:

```sql
CREATE TABLE question_psychometrics (
    question_id BIGINT PRIMARY KEY
        REFERENCES questions(id)
        ON DELETE CASCADE,

    irt_model VARCHAR(20) NOT NULL,

    discrimination_a NUMERIC(10,6),

    difficulty_b NUMERIC(10,6),

    guessing_c NUMERIC(10,6),

    calibration_sample_size INT,

    calibration_version VARCHAR(50),

    calibrated_at TIMESTAMPTZ
);
```

Ví dụ:

```text
IRT_MODEL = 3PL

a = discrimination
b = difficulty
c = guessing
```

---

# 22. REST API CONTRACT

## 22.1. Create Exam

```http
POST /api/v1/admin/exams
```

Payload:

```json
{
  "examCode": "VSAT_MATH_2026_MOCK_001",
  "title": "V-SAT Toán - Đề thi thử số 01",
  "subjectId": 1,
  "description": "Đề mô phỏng cấu trúc V-SAT 2026",
  "durationMinutes": 90,
  "difficulty": "HARD",
  "pricingType": "FREE",
  "price": 0,
  "tags": "VSAT,2026,MOCK",
  "scoringProfileCode": "VSAT_2026_RAW_150"
}
```

Response:

```json
{
  "data": {
    "id": 123,
    "examCode": "VSAT_MATH_2026_MOCK_001",
    "status": "DRAFT",
    "questionCount": 0
  }
}
```

---

## 22.2. Generate by Matrix

```http
POST /api/v1/admin/exams/generate-by-matrix
```

Payload:

```json
{
  "exam": {
    "examCode": "VSAT_PHY_AUTO_001",
    "title": "V-SAT Vật lí - Auto 01",
    "subjectId": 2,
    "durationMinutes": 60
  },
  "blueprintId": 17,
  "variantCount": 4,
  "generation": {
    "avoidPreviouslyUsedQuestions": true,
    "maxExposureRate": 0.3
  }
}
```

Response:

```json
{
  "data": {
    "examId": 180,
    "selectedQuestions": 25,
    "rawScore": 150,
    "variants": [
      "101",
      "102",
      "103",
      "104"
    ],
    "generationWarnings": []
  }
}
```

Có thể bổ sung:

```http
POST /api/v1/admin/exams/{id}/generate-by-matrix
```

để sinh câu hỏi vào một Draft đã tồn tại.

---

## 22.3. Import

```http
POST /api/v1/admin/exams/{id}/import
Content-Type: multipart/form-data
```

Fields:

```text
file
format=XLSX
mode=APPEND
dryRun=true
```

Response:

```json
{
  "data": {
    "batchId": 991,
    "totalRows": 25,
    "validRows": 23,
    "warningRows": 1,
    "errorRows": 1,
    "canCommit": false
  }
}
```

Commit:

```http
POST /api/v1/admin/import-batches/991/commit
```

---

## 22.4. Validate Exam

```http
POST /api/v1/admin/exams/{id}/validate
```

Response:

```json
{
  "data": {
    "valid": false,
    "questionCount": {
      "actual": 24,
      "required": 25
    },
    "rawScore": {
      "actual": 144,
      "required": 150
    },
    "errors": [
      {
        "code": "SHORT_ANSWER_COUNT_INVALID",
        "expected": 5,
        "actual": 4
      }
    ]
  }
}
```

---

## 22.5. Publish

Backend hiện tại của VSAT Compass đang sử dụng:

```http
POST /api/v1/admin/exams/{id}/publish
```

Nên giữ contract này để tránh break client hiện hữu.

Publish validator phải kiểm tra:

```text
Status hợp lệ

25 questions

9 TRUE_FALSE
6 SINGLE_CHOICE
5 MATCHING
5 SHORT_ANSWER

TRUE_FALSE = đúng 4 statements
SINGLE_CHOICE = đúng 4 options + đúng 1 correct
MATCHING = đúng cấu trúc
SHORT_ANSWER = có answer key

Raw score = 150

Questions APPROVED

Subject đồng nhất

LaTeX valid

Image references valid

No duplicate question

Scoring profile hợp lệ

Exam window hợp lệ
```

---

# 23. WEB ADMIN UI/UX

Frontend đề xuất:

```text
React
TypeScript
Vite
TanStack Query
React Router
React Hook Form
Zod
dnd-kit
TipTap
KaTeX
DOMPurify
```

---

# 24. EXAM LIST SCREEN

```text
┌───────────────────────────────────────────────────────────────┐
│ V-SAT Compass                         [ + TẠO ĐỀ THI ]       │
├───────────────────────────────────────────────────────────────┤
│ Đề thi                                                       │
│                                                               │
│ [Search................] [Môn ▼] [Status ▼] [Mode ▼]        │
│ [Difficulty ▼] [Ngày tạo ▼]                                 │
├───────────────────────────────────────────────────────────────┤
│ Mã đề          Tên           Môn     Câu   Time   Status      │
│ ───────────────────────────────────────────────────────────── │
│ MATH-001       Toán 01       Toán    25    90m    DRAFT       │
│ PHY-001        Vật lí 01     Lý      25    60m    PUBLISHED   │
│ CHEM-001       Hóa 01        Hóa     24    60m    ERROR       │
│                                                               │
│                                        < 1 2 3 ... >         │
└───────────────────────────────────────────────────────────────┘
```

Actions:

```text
Edit
Duplicate
Preview
Validate
Submit Review
Publish
Hide
Archive
```

---

# 25. CREATE EXAM WIZARD

## Bước 1 — General & Timing

```text
Thông tin chung

Tên đề
Mã đề
Môn
Mô tả
Tag
Scoring profile

Thời lượng:
[90] phút

Chế độ:
(•) Practice
( ) Scheduled Mock

Nếu Scheduled:

Start
End
Grace period
Attempts
Result policy
```

Gợi ý duration:

```text
MATH → 90 phút
PHY → 60 phút
CHEM → 60 phút
BIO → 60 phút
HISTORY → 60 phút
GEOGRAPHY → 60 phút
ENGLISH → 60 phút
```

---

## Bước 2 — Composition

Ba tab:

```text
[ Manual ]
[ Generate by Matrix ]
[ Import ]
```

Matrix preview:

```text
┌────────────────────────────────────────────┐
│          NB    TH    VD    VDC    Total   │
├────────────────────────────────────────────┤
│ Topic A   1     2     2      1       6    │
│ Topic B   2     1     1      1       5    │
│ Topic C   ...                            │
└────────────────────────────────────────────┘
```

Realtime inspector:

```text
Questions       25 / 25
Raw Score      150 / 150
TF               9 / 9
MCQ              6 / 6
Matching         5 / 5
Short Answer     5 / 5
```

Candidate capacity:

```text
✅ TF: 143 available
✅ MCQ: 89 available
⚠ Matching: 7 available
```

---

## Bước 3 — Preview & Validation

```text
┌────────────────────────────────┬───────────────────────┐
│ EXAM PREVIEW                   │ VALIDATION            │
│                                │                       │
│ Câu 1                          │ ✅ Structure          │
│ ...                            │ ✅ Scoring            │
│                                │ ✅ Images             │
│ \( f(x) = ... \)               │ ❌ Latex Q18          │
│                                │                       │
│ a. ... [Đ][S]                  │ ⚠ Missing explanation│
│ b. ... [Đ][S]                  │                       │
└────────────────────────────────┴───────────────────────┘
```

Preview modes:

```text
Desktop
Android
Print
```

---

## Bước 4 — Variants & Publish

```text
Số phiên bản:
[4]

☑ Shuffle question order
☑ Shuffle MCQ options
☑ Shuffle matching choices
☐ Shuffle TF statements

Variants:
101
102
103
104
```

Integrity:

```text
✅ 25 questions
✅ 150 points
✅ 9/6/5/5 structure
✅ all questions approved
✅ LaTeX valid
✅ answer keys valid
✅ no duplicated questions
```

Buttons:

```text
[ SUBMIT FOR REVIEW ]
```

SUPER_ADMIN:

```text
[ PUBLISH ]
```

---

# 26. EXAM WORKFLOW

Giữ workflow hiện tại của backend:

```text
DRAFT
  │
  ├─ edit
  ├─ import
  ├─ manual compose
  └─ generate matrix
       │
       ▼
COMPOSING
       │
       ▼
PENDING_REVIEW
       │
       ├──── reject ───→ DRAFT
       │
       ▼
PUBLISHED
       │
       ▼
HIDDEN
       │
       ├─ return → DRAFT
       │
       └─ archive
       ▼
ARCHIVED
```

---

# 27. TƯƠNG THÍCH VỚI ANDROID HIỆN TẠI

Các file local demo như:

```text
sample_math_exam.json
sample_math_exam_2.json
sample_english_exam.json
sample_english_exam_2.json
sample_physics_exam.json
```

không nên được dùng làm source of truth cho Web Admin V-SAT 2026.

Sau khi module mới được triển khai:

```text
Scoring Profile
+
Blueprint
+
Published Exam Release
```

phải trở thành source of truth.

Local JSON chỉ giữ vai trò:

```text
offline demo
legacy fallback
development fixture
```

---

# 28. KIẾN TRÚC TỔNG THỂ ĐỀ XUẤT

```text
                     ┌──────────────────────┐
                     │    WEB ADMIN         │
                     │ React / Vue / Vite   │
                     └──────────┬───────────┘
                                │
                         REST / JWT
                                │
                    ┌───────────▼────────────┐
                    │   SPRING BOOT API      │
                    │                        │
                    │ Question Bank          │
                    │ Exam Builder           │
                    │ Blueprint Engine       │
                    │ Variant Generator      │
                    │ Import Pipeline        │
                    │ Scoring Engine         │
                    │ Scheduling Engine      │
                    └───────────┬────────────┘
                                │
                    ┌───────────▼────────────┐
                    │ PostgreSQL 17          │
                    │                        │
                    │ questions              │
                    │ question_*             │
                    │ exams                  │
                    │ exam_sections          │
                    │ exam_questions         │
                    │ exam_blueprints        │
                    │ exam_windows           │
                    │ exam_variants          │
                    │ scoring_profiles       │
                    └───────────┬────────────┘
                                │
                          Published Exam
                                │
                     ┌──────────▼─────────┐
                     │ Android Student    │
                     │                    │
                     │ Online             │
                     │ Offline fallback   │
                     └────────────────────┘
```

---

# 29. LỘ TRÌNH TRIỂN KHAI ĐỀ XUẤT

## Phase WEB-1 — Question Model & Scoring

- Thêm `cognitive_level`
- Hoàn thiện `TRUE_FALSE`
- Thêm `MATCHING`
- Chuẩn hóa `SHORT_ANSWER`
- Thêm `scoring_profiles`
- Mở rộng `session_answers`
- Mở rộng `exam_sessions`

---

## Phase WEB-2 — Manual Exam Builder

- Exam list
- Exam wizard
- Manual composition
- Drag & Drop
- Exam sections
- Validation
- WYSIWYG preview
- LaTeX rendering

---

## Phase WEB-3 — Matrix Generator & Variants

- Blueprint
- Blueprint cells
- Candidate capacity check
- Auto generation
- Exposure control
- Variant generation
- Shuffle engine

---

## Phase WEB-4 — Import Pipeline

- Excel template
- DOCX tagged parser
- JSON import
- Staging
- Validation
- Preview
- Commit / rollback

---

## Phase WEB-5 — Scheduling & Result Policies

- Exam windows
- Practice mode
- Scheduled mock exam
- Grace period
- Attempt limits
- Result release rules
- Deadline enforcement
- Offline delivery rules

---

# 30. LƯU Ý MIGRATION

Không nên chỉnh trực tiếp file baseline:

```text
VSAT/vsat_database_schema.sql
```

nếu file này đã được xác định là frozen trong quy trình dự án.

Nên triển khai bằng migration versioned, ví dụ:

```text
V20261006_01__add_cognitive_level.sql
V20261006_02__add_vsat_question_types.sql
V20261006_03__add_scoring_profiles.sql
V20261006_04__add_exam_windows.sql
V20261006_05__add_exam_blueprints.sql
V20261006_06__add_exam_variants.sql
```

Khuyến nghị tích hợp **Flyway** với Spring Boot để:

- Version hóa schema.
- Upgrade production DB an toàn.
- Theo dõi lịch sử migration.
- Tránh sửa thủ công Neon/PostgreSQL.
- Phối hợp backend/web/admin dễ hơn.

---

# 31. KẾT LUẬN

Module Quản trị Đề thi Web App của V-SAT Compass không nên chỉ là một trang CRUD đề thi.

Kiến trúc nên phát triển thành một **Exam Authoring & Delivery System**, bao gồm:

```text
Question Bank
Exam Builder
Blueprint Engine
Scoring Engine
Import Pipeline
Variant Generator
Scheduling Engine
Validation Engine
Publishing Workflow
```

Thiết kế này tận dụng tối đa backend và schema hiện có, đồng thời mở rộng theo hướng:

- Phù hợp cấu trúc V-SAT 2026.
- Không phá Android Client hiện tại.
- Dễ mở rộng cho các quy chuẩn V-SAT mới.
- Có khả năng tích hợp IRT trong tương lai.
- Hỗ trợ Web Admin chuyên nghiệp cho giáo viên/quản trị viên.
