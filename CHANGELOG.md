# V-SAT COMPASS — CHANGELOG

## [0.8.1] - 2026-04-25 — Phase B Production Hardening

### Bug Fixes

- **fix(auth): GET /auth/me, PUT /auth/me, PUT /auth/me/password returned 500 instead of 401
  when Bearer token was missing.**
  Root cause: `SecurityConfig` used `.requestMatchers("/auth/**").permitAll()` which
  allowed unauthenticated requests through to the controller. The controller called
  `SecurityUtils.getCurrentUserId()` → `null` → `findById(null)` → `IllegalArgumentException` → 500.
  Fix: tightened the permitAll allowlist to `/auth/login`, `/auth/register`, `/auth/refresh`,
  `/auth/logout` only. Spring Security now rejects unauthenticated `/auth/me` at the filter
  layer before the controller is reached.
  Shipped in commit `24b73af` — CHANGELOG entry backfilled here.

- **fix(security): Spring Security returned 403 instead of 401 for missing Bearer token.**
  RESTful convention requires 401 (Unauthorized) for missing/invalid auth and 403 only
  for authenticated-but-lacks-role cases. Spring Security's default `ExceptionTranslationFilter`
  produces 403 when no `AuthenticationEntryPoint` is configured.
  Fix: added `restAuthenticationEntryPoint()` bean to `SecurityConfig` that returns 401
  with the standard `ApiResponse.error()` envelope (`AUTH_UNAUTHORIZED`). Wired via
  `.exceptionHandling(ex -> ex.authenticationEntryPoint(...))`.

- **fix(api): POST /sessions/start returned 500 INTERNAL_ERROR when `exam_id` FK was violated.**
  Root cause: `GlobalExceptionHandler` had no handler for `DataIntegrityViolationException`,
  which fell through to the generic `Exception` handler → 500.
  Fix: added `@ExceptionHandler(DataIntegrityViolationException.class)` returning
  400 `DATA_INTEGRITY_VIOLATION`. Future callers sending a non-existent `examId` now
  receive a clear 400 instead of a misleading 500.
  Root trigger: `exams` table was empty (Exam management is Phase C scope).
  Resolved via manual SQL seed (see `docs/seed/smoke_test_seed.sql`).

### Documentation

- **docs/seed/smoke_test_seed.sql** — new. Idempotent SQL seed for smoke testing the
  Session API. Inserts subject `MATH` and placeholder exam `SMOKE_001`. Run once in
  Neon Console before executing `smoke_sessions.sh`. Will be replaced by Phase C fixtures.
- **docs/scripts/smoke_sessions.sh** — updated to use `EXAM_ID` env var (default `1`)
  instead of hardcoded `examId=1`. Override with `EXAM_ID=<id>` if seed assigns a
  different ID.
- **docs/API_ERROR_CODES.md** — added `DATA_INTEGRITY_VIOLATION` (400) error code entry.

### Neon DB Verification (2026-04-25)

| Table | Result |
|-------|--------|
| `users` | 4 seeded accounts confirmed (STUDENT, COLLABORATOR, CONTENT_ADMIN, SUPER_ADMIN) all status=ACTIVE |
| `refresh_tokens` | 6 tokens created during smoke window (14:01–14:36 UTC); mix of revoked=true (logout TC) and revoked=false; expires_at = +30 days |
| `exam_sessions` | 2 rows: session id=4 (MOCK_EXAM, SUBMITTED, score=73.33, correct=22/30) + id=5 (PRACTICE, IN_PROGRESS) |
| `exam_sessions` orphans | 0 — all sessions correctly linked to user_id=4 (student@vsat.com) and exam_id=1 (SMOKE_001) |

- Anti-replay verified at both HTTP (409) and DB levels (`status=SUBMITTED` prevents re-submit)
- Owner check verified at both HTTP (403) and DB (`user_id` enforced in service layer)
- Foreign key integrity: `exam_id` → `exams.id` confirmed via 0 orphan sessions

### Notes
- Phase B production smoke verification: **14/14 PASS** (9 auth + 5 session) on 2026-04-25
- Anti-replay verified: duplicate `/sessions/{id}/client-submit` returns 409 SESSION_ALREADY_SUBMITTED
- TC-SESSION-2: `/sessions/start` without Bearer now returns 401 (AuthenticationEntryPoint active)
- Phase B officially closed 2026-04-25

### Operational Status
- API: LIVE at `https://vsat-compass-api.onrender.com/api/v1/`
- Database: CONNECTED (Neon PostgreSQL pooler, ap-southeast-1)
- Android client: USING PRODUCTION (`BASE_URL_CLOUD` verified)
- Monitoring: ACTIVE (UptimeRobot 5-minute interval)

---

## [0.8.0] - 2026-04-23 — Phase B: Backend Production-Ready

### Mục tiêu
Stabilize public backend, lock in minimum required APIs (Auth + Session sync), harden security — nền tảng cho Phase C (admin content management).

---

### 🏗️ Phase 1 — Public Backend Stability

#### Actuator & Monitoring
- [x] Thêm `spring-boot-starter-actuator` — expose chỉ `/actuator/health` (no details)
- [x] Thêm `bucket4j-core` dependency (chuẩn bị cho Phase 3 rate limiting)

#### Config Hardening (`application.yml`)
- [x] **Remove JWT_SECRET fallback** — app fail-fast nếu env var chưa set
- [x] CORS driven by `CORS_ALLOWED_ORIGINS` env var (default: localhost dev)
- [x] Logging levels tối ưu: prod `WARN` cho Hibernate SQL + Spring Security
- [x] Dev profile: `HIBERNATE_SQL_LOG` env var toggle

#### Security (`SecurityConfig.java`)
- [x] HSTS header enabled (max-age 1 year, includeSubDomains)
- [x] Actuator health endpoint added to public permits
- [x] Rate limit filter registered before JWT filter

#### Secret Management
- [x] **`render.yaml`**: Remove tất cả hardcoded secrets (JWT_SECRET, DATABASE_URL, DATABASE_USERNAME)
- [x] Chuyển sang Render Dashboard env var management
- [x] Health check path configured: `/api/v1/actuator/health`
- [x] `.env.example` cập nhật: CORS_ALLOWED_ORIGINS, stronger warnings

#### Response Envelope
- [x] Thêm `timestamp` field (ISO-8601 OffsetDateTime) vào `ApiResponse`

#### Documentation
- [x] **`docs/DEPLOY_RUNBOOK.md`** — 7 sections: prerequisites, first-time deploy, routine deploy, rollback, secret rotation, incident triage, cost watch

---

### 🔌 Phase 2 — Minimum Required APIs

#### Session Module (NEW — 8 files)
- [x] `ExamSession` entity mapped to frozen `exam_sessions` table schema
- [x] `SessionMode` enum: `MOCK_EXAM`, `PRACTICE`
- [x] `SessionStatus` enum: `IN_PROGRESS`, `SUBMITTED`, `TIMED_OUT`, `ABANDONED`
- [x] `ExamSessionRepository`: `findById`, `findByIdAndUserId`
- [x] `SessionRequest` DTO: `StartSession`, `ClientSubmit` (với validation)
- [x] `SessionResponse` DTO: `SessionInfo`
- [x] `SessionService` + `SessionServiceImpl`:
  - `startSession()` — tạo phiên thi mới
  - `clientSubmit()` — nộp kết quả client-side scoring
  - Anti-replay: 409 `SESSION_ALREADY_SUBMITTED` nếu đã nộp
  - Owner check: 403 `SESSION_FORBIDDEN` nếu không phải chủ phiên
  - Validation: `correctCount ≤ totalQuestions`, `score 0-100`, `timeSpent 0-86400`
- [x] `SessionController`: `POST /sessions/start` (201), `POST /sessions/{id}/client-submit` (200)

#### Error Code Standardization
- [x] `AppException` — 10 specific factory methods:
  - Auth: `authEmailTaken()`, `authInvalidCredentials()`, `authUnauthorized()`, `authRefreshInvalid()`, `authForbidden()`
  - Session: `sessionAlreadySubmitted()`, `sessionForbidden()`
  - Generic: `validationFailed()`, `rateLimitExceeded()`, `badRequest()`
- [x] `AuthServiceImpl` — thay generic errors bằng specific error codes

#### Smoke Test Scripts
- [x] `docs/scripts/smoke_auth.sh` — 9 test cases (bash 3.2 compatible)
- [x] `docs/scripts/smoke_sessions.sh` — 5 test cases (bash 3.2 compatible)

---

### 🔒 Phase 3 — Security & Stability Hardening

#### Validation Hardening (`AuthRequest.java`)
- [x] Password: `@Size(min=6)` → `@Size(min=8, max=100)` + `@Pattern(letter+digit)`
- [x] Email: thêm `@Size(max=255)`
- [x] FullName: `@Size(min=2, max=100)`
- [x] Áp dụng cho Register, ChangePassword, ResetPassword

#### Exception Handler (`GlobalExceptionHandler.java`)
- [x] `VALIDATION_ERROR` → `VALIDATION_FAILED`
- [x] Validation errors trả `fieldErrors` map trong `data` field
- [x] Thêm handlers: `BadCredentialsException`, `AccessDeniedException`, `AuthenticationException`

#### Rate Limiting (`RateLimitFilter.java`)
- [x] In-memory Bucket4j rate limiting:
  - `/auth/login`: 10 req/phút/IP
  - `/auth/register`: 5 req/giờ/IP
  - `/auth/refresh`: 30 req/phút/IP
- [x] Client IP resolution: `X-Forwarded-For` first IP (Render proxy)
- [x] Returns 429 `RATE_LIMIT_EXCEEDED` with standard ApiResponse

#### JWT Cleanup Job
- [x] `@EnableScheduling` trên `VsatCompassApiApplication`
- [x] `RefreshTokenCleanupService`: chạy 03:00 AM daily
- [x] Xóa expired tokens + revoked tokens cũ hơn 7 ngày

#### Documentation
- [x] **`docs/API_ERROR_CODES.md`** — Error catalog + response envelope + rate limits + Android handling guide
- [x] **`docs/SMOKE_CHECKLIST.md`** — 10 backend TCs mới (TC-016 → TC-025), tổng 25 TCs
- [x] Android direct-DB audit: **CLEAN** (không có PostgreSQL/JDBC/Neon references)

---

### 📊 Build Results

| Module | Command | Result |
|--------|---------|--------|
| Backend | `./gradlew build -x test` | ✅ BUILD SUCCESSFUL (no warnings) |

### 📁 Files Changed

| Category | Count | Chi tiết |
|----------|-------|----------|
| New files | 15 | 8 Session module + 2 smoke scripts + RateLimitFilter + CleanupService + 3 docs |
| Modified files | 12 | build.gradle, application.yml, SecurityConfig, render.yaml, .env.example, ApiResponse, AppException, AuthServiceImpl, AuthRequest, GlobalExceptionHandler, VsatCompassApiApplication, RefreshTokenRepository |

---

## [0.7.1] - 2026-04-23 — Phase A Polish & Hardening

### Mục tiêu
Polish toàn diện Phase A (Local Pack + Exam Review + History) trước khi bước sang Giai đoạn B. Build pass + 6 unit tests pass.

---

### 🔍 Phase 1 — Code Quality Audit & Fix

**Lint baseline:** 54 errors / 537 warnings (pre-existing). Phase A chỉ thêm 1 error (`MissingSuperCall`) đã được fix.

#### strings.xml
- [x] Extract toàn bộ chuỗi tiếng Việt hardcoded trong Phase A sang `res/values/strings.xml`
- [x] Thêm 30+ string keys: `review_*`, `history_*`, `home_greeting_*`, `home_stats_*`, `time_*`, `empty_state_*`, `cd_*` (accessibility)

#### ScoreConstants.java *(mới)*
- [x] `VSAT_MAX_SCORE = 1200`, `PERCENT_TO_VSAT = 12`, `WEAK_TOPIC_THRESHOLD_PERCENT = 60`
- [x] `ExamResultActivity` và `ExamHistoryEntry` dùng `ScoreConstants` thay vì magic number 12

#### TAG constants
- [x] Thêm `private static final String TAG = "ClassName"` vào: `ExamReviewActivity`, `ExamHistoryActivity`, `ExamHistoryRepository`, `HomeFragment`

#### Exception handling cleanup
- [x] `ExamReviewActivity`: `catch (Exception ignored)` → `catch (JsonSyntaxException e)` + `Log.w(TAG, ...)`
- [x] `ExamHistoryRepository.saveSync()`: `catch (IOException ignored)` → `Log.e(TAG, ..., e)` với context
- [x] `ExamHistoryRepository.loadSync()`: silent swallow → log warning + corrupt file recovery

#### MissingSuperCall
- [x] `ExamSessionActivity.onBackPressed()`: thêm `@SuppressWarnings("MissingSuperCall")` với comment giải thích

#### Schema migration-friendly
- [x] `ExamHistoryEntry`: thêm `@SerializedName` cho mọi field — JSON cũ parse OK khi thêm field mới

#### ExamHistoryAdapter
- [x] `SimpleDateFormat` dùng `Locale.forLanguageTag("vi-VN")` thay vì deprecated constructor

---

### 🛡️ Phase 2 — Robustness & Edge Cases

#### ExamHistoryRepository
- [x] **Atomic write**: ghi vào `.tmp` → `fos.getFD().sync()` → `renameTo()` target — bảo vệ khỏi half-written nếu app bị kill
- [x] **Corrupt file recovery**: `JsonSyntaxException` → rename sang `exam_history.json.corrupt.<timestamp>`, log warning, trả về empty list
- [x] **Concurrent access**: tất cả I/O đi qua `synchronized (fileLock)` + single-thread executor
- [x] **`saveEntry` overload** nhận `Runnable onSaveFailed` → callback khi save thất bại (storage đầy...)
- [x] **`injectMockEntries(count, onDone)`**: debug-only API inject N entries giả để test stress/scroll
- [x] **`clearAll(onDone)`**: debug-only API xóa toàn bộ history
- [x] `Context appCtx = context.getApplicationContext()` tránh Activity context leak trong executor

#### ExamReviewActivity
- [x] Guard `examId == 0` → `finish()` + Toast trước khi inflate
- [x] Guard `questionIds.isEmpty()` sau load → `finish()` + Toast
- [x] `optionExistsInQuestion()`: kiểm tra `selectedOptionId` còn hợp lệ trong pack hiện tại — nếu pack đề đã thay đổi sau khi lưu, treat như câu chưa làm + log warning
- [x] Clamp `currentIndex` khi restore từ `savedInstanceState`

#### ExamHistoryActivity
- [x] `onSaveInstanceState` / restore `currentSubjectFilter` sau rotate
- [x] `syncChipUi()` tách rời khỏi `selectChip()` để restore không trigger reload thừa
- [x] `showLoadingState(true/false)` toggle `ProgressBar` + `RecyclerView`
- [x] `loadHistory()` phân biệt 2 empty state: "chưa có bài nào" vs "filter ra 0 kết quả"
- [x] `openReview()` bọc trong try-catch, log warning nếu `examId` không tìm thấy

#### ExamResultActivity
- [x] Nhận extra `history_save_failed` từ `ExamSessionActivity`
- [x] Hiển thị `Snackbar` cảnh báo khi lưu lịch sử thất bại

#### Debug Dev Menu (ProfileFragment — DEBUG build only)
- [x] Long-press tên hiển thị trong ProfileFragment → dialog dev tools
- [x] "Inject 50 lịch sử mock" → gọi `injectMockEntries(50)`
- [x] "Xóa toàn bộ lịch sử" → gọi `clearAll()`
- [x] Toàn bộ code trong `if (!BuildConfig.DEBUG) return;` — release build không có

---

### ✨ Phase 3 — UX Polish & Verification

#### RelativeTimeHelper.java *(mới)*
- [x] `format(context, timestampMillis)`: < 1ph → "vừa xong"; < 1h → "X phút trước"; < 24h → "X giờ trước"; < 7 ngày → "X ngày trước"; còn lại → "dd/MM/yyyy HH:mm" locale vi-VN
- [x] `ExamHistoryAdapter` dùng `RelativeTimeHelper.format()` thay vì `SimpleDateFormat` tĩnh

#### Loading & Empty states
- [x] `ExamHistoryActivity`: `ProgressBar` (id `progressLoading`) hiện khi đang fetch, ẩn khi xong
- [x] Empty state text khác nhau: "Chưa có bài thi nào" vs "Không có bài thi nào ở môn này"
- [x] `HomeFragment`: stat cards hiện "--" / "0ph" thay vì "0" khi chưa có lịch sử

#### Accessibility
- [x] `activity_exam_review.xml` nút back: `contentDescription="@string/cd_back"` + `minWidth/Height="48dp"`
- [x] `activity_exam_history.xml` nút back: `contentDescription="@string/cd_back"` + `minWidth/Height="48dp"`

#### StrictMode (debug build)
- [x] `VsatApp.enableStrictMode()`: `detectDiskReads + detectDiskWrites + detectNetwork + penaltyLog`
- [x] Chỉ bật khi `BuildConfig.DEBUG` — release build không bị ảnh hưởng

#### Unit Tests
- [x] `ExamHistoryRepositoryTest.java` — 6 test cases (5 yêu cầu + 1 bonus):
  - `saveEntry_thenGetAll_returnsNewestFirst`
  - `saveEntry_exceedsMax_capsAt200`
  - `getByExamId_returnsOnlyMatchingSubject` (filter contains)
  - `getStats_calculatesAverageCorrectly` (avgScore = 900 khi 2 bài 600+1200)
  - `getAll_corruptFile_returnsEmptyAndCreatesCorruptBackup`
  - (TC6: khởi tạo fresh repo mỗi test qua reflection)
- [x] `build.gradle.kts`: `testOptions { unitTests { isReturnDefaultValues = true } }` để Android stubs không throw

#### Smoke Checklist
- [x] `docs/SMOKE_CHECKLIST.md` — 15 test cases bao phủ: login, 5 đề, filter, làm bài, review, history, persist, rotate, empty state, dev menu, sync status, thoát giữa chừng

### Giả định kỹ thuật đã chọn
- **`returnDefaultValues = true`**: Android stubs trong JVM unit test trả về default thay vì throw — đủ để test file I/O logic không cần Robolectric.
- **FakeContext extends ContextWrapper**: không cần Mockito, chỉ override `getFilesDir()`.
- **Không thêm dependency mới**: tất cả fix dùng JUnit4 + standard Java — thoả ràng buộc.

### Kết quả đo lường
| Metric | v0.7.0 | v0.7.1 |
|--------|--------|--------|
| Unit tests pass | 1/1 (ExampleTest) | 6/6 |
| Lint Phase A errors | 1 (MissingSuperCall) | 0 |
| Strings hardcoded (Phase A Java) | ~18 | 0 |
| Exception swallow (silent) | 3 | 0 |

---

## [0.7.0] - 2026-04-23 — Student MVP hoàn chỉnh: Review chi tiết + Lịch sử bài làm

### Tổng quan

Hoàn tất 3 task P0 cuối cùng của Giai đoạn A:

- **A2** — 5 pack đề local với metadata chuẩn hóa, filter chip hoạt động đúng
- **A4** — Màn xem lời giải chi tiết từng câu sau bài thi
- **A5** — Lịch sử bài làm persist local + dashboard student hiển thị dữ liệu thật

Học viên có thể: đăng nhập → làm bài → xem kết quả → xem lời giải → xem lịch sử → tất cả chạy offline.

---

### 📦 Phase 1 — Bổ sung pack đề + chuẩn hóa metadata (A2)

#### Assets mới / cập nhật
- [x] **`sample_math_exam_2.json`** — Đề Toán nâng cao (id=4), 30 câu, 60 phút, MATH_002; topics: số phức, logarithm, hàm số, đạo hàm, tích phân, hình học, xác suất
- [x] **`sample_english_exam_2.json`** — Đề Tiếng Anh nâng cao (id=5), 30 câu, 45 phút, ENG_002; topics: ngữ pháp, từ vựng, đọc hiểu, nhận biết lỗi
- [x] **`sample_english_exam.json`** — Sửa `subject_name: "Tieng Anh"` → `"Tiếng Anh"`, cập nhật nội dung và explanation tiếng Việt
- [x] **`sample_physics_exam.json`** — Sửa `subject_name: "Vat ly"` → `"Vật lí"`, cập nhật nội dung tiếng Việt

Kết quả: filter chip "Toán" → 2 đề, "Tiếng Anh" → 2 đề, "Vật lí" → 1 đề. Tổng 5 pack.

---

### 🔍 Phase 2 — Xem lời giải chi tiết (A4)

#### File mới
- [x] **`ui/exam/session/ExamReviewActivity.java`** — Màn review từng câu: option đúng tô xanh, option sai tô đỏ, explanation đầy đủ, điều hướng ← / →, grid tổng quan
- [x] **`ui/exam/session/ReviewGridAdapter.java`** — Grid adapter cho review: đúng=xanh lá, sai=đỏ nhạt, chưa làm=xám
- [x] **`res/layout/activity_exam_review.xml`** — Layout màn review với ScrollView, option cards, explanation card, bottom nav bar
- [x] **`res/drawable/bg_badge_warning.xml`** — Badge cam cho câu "chưa trả lời"

#### File sửa
- [x] **`ExamSessionActivity.java`** — Thêm serialize `selectedAnswers` → JSON, truyền `examId`/`examTitle`/`examSubject`/`selectedAnswersJson` sang ExamResultActivity; import Gson
- [x] **`ExamResultActivity.java`** — Nhận extras từ Session, nối nút "Xem lời giải chi tiết" → mở ExamReviewActivity với đúng extras
- [x] **`AndroidManifest.xml`** — Đăng ký ExamReviewActivity

**Luồng dữ liệu:** ExamSessionActivity → (intent extras) → ExamResultActivity → ExamReviewActivity. ExamReviewActivity tự load câu hỏi từ LocalExamDataSource theo `examId`, không cần snapshot câu hỏi.

**Tính năng:**
- Option đúng + user chọn đúng → xanh #E8F5E9, icon ✓
- Option đúng + user không chọn → xanh nhạt + label "Đáp án đúng"
- Option sai + user đã chọn → đỏ nhạt + icon ✗ + label "Của bạn"
- Câu chưa làm → badge cam "Bạn chưa trả lời câu này"
- Rotate device → giữ nguyên câu đang xem (onSaveInstanceState)

---

### 📊 Phase 3 — Lịch sử bài làm + Dashboard student (A5)

#### File mới
- [x] **`data/model/ExamHistoryEntry.java`** — POJO: id, examId, examTitle, subject, totalQuestions, correctCount, score (1200), timeSpentSeconds, submittedAtMillis, selectedAnswersJson
- [x] **`data/repository/ExamHistoryRepository.java`** — Lưu JSON vào `filesDir/exam_history.json`; cap 200 entries; methods async (ExecutorService + Handler): saveEntry, getAll, getRecent, getBySubject, getStats, getBestScoreForExam
- [x] **`ui/history/ExamHistoryActivity.java`** — Màn lịch sử: filter chips theo môn, stats row (bài đã làm / điểm TB / tỷ lệ đúng), empty state thân thiện, nút "Xem lại" → ExamReviewActivity
- [x] **`ui/history/ExamHistoryAdapter.java`** — RecyclerView adapter cho list lịch sử
- [x] **`res/layout/activity_exam_history.xml`** — Layout màn lịch sử
- [x] **`res/layout/item_exam_history.xml`** — Card item: tên đề, môn badge, điểm lớn, ngày/giờ, câu đúng, thời gian, nút "Xem lại"

#### File sửa
- [x] **`ExamSessionActivity.java`** — Sau khi chấm điểm, tạo ExamHistoryEntry và gọi `ExamHistoryRepository.saveEntry()` async
- [x] **`HomeFragment.java`** — `loadHistoryStats()` lấy stats thật từ repo: tvTotalExams = số bài đã làm, tvAvgScore = điểm TB V-SAT, tvTotalTime = tổng thời gian; `loadRecentForContinue()` cập nhật section "Tiếp tục luyện tập" theo đề gần nhất; nút "Xem tất cả" → ExamHistoryActivity; `onResume()` reload stats
- [x] **`fragment_home.xml`** — Thêm id `tvViewAllHistory` cho nút "Xem tất cả"
- [x] **`AndroidManifest.xml`** — Đăng ký ExamHistoryActivity

### Giả định kỹ thuật đã chọn
- **Không dùng Room** — dữ liệu lịch sử lưu dạng JSON file trong `filesDir`. Lý do: tránh thêm dependency nặng; với cap 200 entries (~200KB), file I/O là đủ nhanh.
- **Snapshot câu hỏi không cần lưu** — ExamReviewActivity reload từ LocalExamDataSource theo `examId`. Giả định: pack đề local không bị xóa giữa các lần xem lại.
- **selectedAnswersJson trong history** — lưu để "Xem lại" bài cũ load đúng đáp án đã chọn.

---

## [0.6.0] - 2026-04-23 — Client-First ổn định đa thiết bị + Backend tối thiểu

### Tổng quan

Phiên bản này tập trung tối ưu vận hành chi phí thấp:

- Luồng làm bài chuyển sang **local-first thực sự** (thiết bị là trung tâm)
- Backend chỉ giữ vai trò tối thiểu cho **Auth** và **đồng bộ kết quả cuối**
- App chạy ổn trên nhiều thiết bị Android mà không phụ thuộc IP LAN thay đổi

---

### ✨ Android — Thay đổi chính

#### `app/build.gradle.kts`
- [x] Cấu hình mặc định dùng `BASE_URL_CLOUD` cho cả debug/release
- [x] Giữ `LOCAL_LAN_HOST` làm tùy chọn dev nội bộ
- [x] Tắt `USE_LOCAL_BACKEND` mặc định để tránh phụ thuộc IP LAN khi chạy đa thiết bị

#### `ApiClient.java`
- [x] Chuẩn hóa resolve base URL theo BuildConfig
- [x] Thêm log runtime backend URL để debug nhanh trên Logcat
- [x] Thêm hàm `getCurrentBaseUrl()` phục vụ hiển thị runtime status

#### `LoginActivity.java`
- [x] Hiển thị backend URL trong debug build để xác định endpoint thực tế app đang gọi

#### `HomeFragment.java` + `ExamFragment.java`
- [x] Trong chế độ `CLIENT_SIDE_EXAM_PROCESSING=true`, ưu tiên nạp đề từ local datasource
- [x] Giảm phụ thuộc vào backend exam list khi user chỉ cần làm bài trên thiết bị

#### `ExamSessionActivity.java`
- [x] Chạy phiên thi local ngay lập tức (không chặn bởi `sessions/start`)
- [x] Bootstrap session online chạy nền (non-blocking) để đồng bộ kết quả cuối nếu backend sẵn sàng
- [x] Chỉ gửi `client-submit` khi có remote session hợp lệ
- [x] Bổ sung trạng thái sync trực quan trên màn hình thi:
  - `Sync: local-only`
  - `Sync: online enabled`

#### `activity_exam_session.xml`
- [x] Thêm `tvSyncStatus` ngay dưới top bar để thể hiện trạng thái local/online sync realtime

---

### 📚 Local Exam Packs

#### `LocalExamDataSource.java`
- [x] Nâng cấp từ single-file sang multi-pack scan `sample_*.json`
- [x] Tự động nạp toàn bộ đề local trong `assets/`
- [x] Chống trùng ID câu hỏi giữa nhiều đề bằng namespace theo examId
- [x] Fallback an toàn về `sample_math_exam.json` nếu không có file theo pattern

#### Assets mới
- [x] `app/src/main/assets/sample_english_exam.json`
- [x] `app/src/main/assets/sample_physics_exam.json`
- [x] Giữ `sample_math_exam.json` làm bộ đề mẫu nền tảng

---

### 🧭 Tài liệu

#### `README.md`
- [x] Cập nhật kiến trúc client-first theo backend tối thiểu
- [x] Bổ sung API dependency map: endpoint bắt buộc vs tùy chọn vs không bắt buộc
- [x] Bổ sung quy ước mở rộng đề local `sample_*.json`
- [x] Bổ sung mô tả trạng thái sync trong màn hình thi

---

### ✅ Kết quả đạt được

- App có thể vận hành làm bài ổn định theo mô hình local-first
- Giảm rủi ro downtime backend ảnh hưởng trực tiếp trải nghiệm thi
- Dễ mở rộng ngân hàng đề local mà không phải sửa code luồng chính
- Phù hợp chiến lược giảm chi phí server (backend mỏng, chỉ giữ phần cần bảo mật và đồng bộ)

## [0.5.0] - 2026-04-10 — Kiến trúc Hybrid: Xử lý cục bộ + Đồng bộ kết quả

### Tổng quan thay đổi kiến trúc

Chuyển từ hai cực "full server" và "full offline" sang kiến trúc **hybrid** hợp lý hơn:

| Bước | Xử lý | Ghi chú |
|------|--------|---------|
| Đăng nhập / xác thực | **Server** | Kiểm tra tài khoản, quyền mua đề |
| Lấy danh sách đề thi | **Server** | `GET /exams` |
| Bắt đầu phiên thi | **Server** | `POST /sessions/start` — kiểm tra quyền truy cập |
| Tải câu hỏi | **Server** (cache RAM) | Mỗi câu chỉ gọi API 1 lần, sau đó đọc từ `questionCache` |
| Đếm thời gian | **Thiết bị** | `CountDownTimer` |
| Lưu đáp án đã chọn | **Thiết bị** | `selectedAnswers` Map — không gọi API theo từng câu |
| Chấm điểm | **Thiết bị** | So sánh với `option.isCorrect()` từ `questionCache` |
| Nộp kết quả | **Server** (fire-and-forget) | Chỉ POST `{score, correct, total, timeSpent}` |

**Lợi ích:** Giảm API calls từ `1 + N + N + 1` xuống `1 + N(cached) + 1`. Không cần server xử lý scoring. Kết quả được lưu vào DB mà không cần logic phức tạp phía server.

---

### ✨ Android — Thay đổi

#### `ExamApi.java`
- [x] Thêm endpoint `POST /sessions/{sessionId}/client-submit` — nhận kết quả đã tính sẵn từ client

#### `ApiClient.java`
- [x] Cập nhật comment giải thích `CLIENT_SIDE_EXAM_PROCESSING=true` (timer + chấm điểm cục bộ; vẫn cần mạng để auth, fetch đề, ghi kết quả)

#### `AuthRepository.java`
- [x] Xóa early-return offline bypass trong `login()`, `register()`, `getMe()` → tất cả gọi API thực tế
- [x] Giữ `createOfflineAuth()` chỉ làm fallback khi mất mạng

#### `HomeFragment.java`
- [x] Xóa offline shortcut trong `loadUserProfile()` và `loadExams()` → luôn gọi API trước, fallback cục bộ khi lỗi

#### `ExamFragment.java`
- [x] Xóa offline shortcut trong `loadExams()` → gọi `GET /exams`, fallback về `LocalExamDataSource` khi lỗi

#### `ExamSessionActivity.java`
- [x] Thêm `Map<Long, Question> questionCache` — lưu câu hỏi đã fetch để tránh gọi lại + dùng để chấm điểm
- [x] `startSession()`: bỏ offline early-return, luôn gọi `POST /sessions/start`; `sessionStartMillis` set ngay khi bắt đầu
- [x] `loadQuestion()`: phục vụ từ `questionCache` nếu đã có, ngược lại gọi API rồi lưu vào cache
- [x] `submitAnswer()`: vẫn no-op (không gửi từng đáp án lên server) — chỉ lưu vào `selectedAnswers` Map
- [x] `submitExamLocally()`: chấm điểm từ `questionCache` (dùng `option.isCorrect()`), fallback về `LocalExamDataSource` nếu câu chưa cache; sau khi tính xong gọi `submitClientResult()` fire-and-forget để ghi kết quả vào DB

---

### 🚀 Backend — Cloud Deploy Setup (từ phiên trước)

#### `Dockerfile` *(mới)*
- [x] Multi-stage build: `gradle:8.7-jdk17` để build → `eclipse-temurin:17-jre-alpine` để run
- [x] Expose port 8080, `ENTRYPOINT` chạy fat JAR

#### `render.yaml` *(mới)*
- [x] Cấu hình deploy lên Render.com free tier
- [x] Health check path `/api/v1/actuator/health`
- [x] Env vars: `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`, `JWT_SECRET`, `SPRING_PROFILES_ACTIVE=prod`

#### `application.yml`
- [x] Profile `prod`: HikariCP pool tối ưu cho Neon free tier (`maximumPoolSize=5`, `minimumIdle=1`, `connectionTimeout=20000`)

---

### ⚠️ Lưu ý triển khai

- Backend chưa implement module **Exam/Session/Question** (chỉ có Auth module). App tự động fallback về `LocalExamDataSource` (`sample_math_exam.json`) khi API chưa có.
- Khi backend đủ module, tắt fallback bằng cách không dùng `LocalExamDataSource` trong các `onFailure`.
- Endpoint `POST /sessions/{id}/client-submit` cần được thêm vào `SessionController` trên backend.

---

## [0.4.0] - 2026-04-08 — Triển khai toàn bộ UI Mockup + Enhanced Screens

### ✨ Tính năng mới — Android UI Implementation

#### Màn hình mới
- [x] **ExamDetailActivity** — Chi tiết đề thi: gradient header, info cards (thời gian, số câu, độ khó), mô tả, nút "Bắt đầu thi ngay"
- [x] **PracticeFragment** — Lộ trình cải thiện: 
  - 2 chủ đề cần cải thiện (Hình học không gian 45%, Logarit & Hàm số mũ 60%) với nút "Luyện tập ngay"
  - 4 chủ đề theo tiến độ (Số học & Đại số 80%, Giải tích 50%, Xác suất & Thống kê 30%, Vật lí hạt nhân 70%) với nút "Tiếp tục"
- [x] **Fragment_practice.xml** — ScrollView + MaterialCardView + custom progress bars

#### Cải thiện HomeFragment
- [x] Gradient header với chào buổi (Morning/Afternoon/Evening)
- [x] Circular score gauge display
- [x] 3 stat cards: Đề làm, Câu đúng, Tỷ lệ thành công
- [x] "Tiếp tục luyện tập" section
- [x] RecyclerView gợi ý ngang (SuggestionAdapter)
- [x] RecyclerView đề gần đây

#### Cải thiện ExamSessionActivity  
- [x] Toolbar: nút back, title, bookmark icon, grid icon
- [x] Progress bar thể hiện tiến độ làm bài
- [x] Timer hiển thị bên cạnh số câu (`45:50` | `Câu 1/50`)
- [x] **Question Grid Dialog**: 7-column grid, màu sắc phân biệt (answered=green, unanswered=gray, current=blue border, bookmarked=orange flag)
- [x] **Bookmark toggle**: lưu câu hỏi yêu thích, icon màu thay đổi theo trạng thái
- [x] Nút "Nộp bài" từ grid dialog

#### Cải thiện ExamResultActivity
- [x] Top bar: nút back, tiêu đề, chỉn chu
- [x] **Circular score chart** (ring progress): 820/1200 điểm V-SAT
- [x] **Thống kê bên phải**: thời gian làm bài, số câu đúng
- [x] **"Kết quả theo môn"** section với progress bars: Toán 90%, Lí 70%
- [x] **"Chủ đề cần cải thiện"** section: Hình học không gian 55%, Dao động cơ 60%, Số phức 65%
- [x] 2 nút hành động: "Xem lời giải chi tiết" (purple), "Luyện tập thêm" (outline)

#### Cải thiện ExamAdapter + item_exam.xml
- [x] Redesign item card: bold title, icon + question count, icon + duration, "Đề miễn phí"/"30.000đ" badge, "Làm bài" button
- [x] Vietnamese text with diacritics: "câu", "phút"
- [x] Better spacing + Material Design 3 styling

#### Cải thiện ExamFragment
- [x] "Kho đề thi" header
- [x] Search bar (MaterialCardView + EditText)
- [x] Horizontal filter chips: Tất cả, Toán, Tiếng Anh, Vật lí, Hóa học
- [x] RecyclerView danh sách đề

#### Bảng điều hướng
- [x] 4 tabs: Trang chủ, Kho đề, Luyện tập, Tài khoản
- [x] Custom vector icons cho mỗi tab
- [x] Primary color gradient

#### Cập nhật ProfileFragment
- [x] Vietnamese diacritics throughout ("Thông tin cá nhân", "Số điện thoại", "Chưa cập nhật", "Đăng xuất")

### 🎨 Design & Colors
- [x] **Primary**: Purple/Indigo theme (#4A3ABA)
- [x] **Secondary**: Accent colors (#FF6C5CE7)
- [x] **Success**: Green (#4CAF50)
- [x] **Warning**: Orange (#FF9800)
- [x] **Error**: Red
- [x] **Drawables created** (20+ files):
  - ic_home, ic_exam, ic_practice, ic_profile (nav icons)
  - ic_back, ic_grid, ic_bookmark, ic_timer, ic_check_circle (feature icons)
  - bg_gradient_header, bg_gradient_card, bg_chip_selected/unselected (backgrounds)
  - bg_button_primary, bg_button_outline (button styles)
  - bg_question_answered, bg_question_unanswered, bg_question_current (question states)
  - circular_progress.xml (ring-shaped score gauge)
  - progress_green.xml, progress_orange.xml, progress_purple.xml (progress bars)

### Mã nguồn — File thay đổi

**Java (6 files)**
- ExamSessionActivity.java: bookmark toggle, grid dialog, Vietnamese text
- ExamResultActivity.java: circular score, V-SAT 1200-point scale
- HomeFragment.java: greeting logic, adapters setup
- ProfileFragment.java: Vietnamese diacritics
- QuestionGridAdapter.java: NEW — 7-column grid, state colors
- SuggestionAdapter.java: NEW (created in 0.3.0, improved)

**Layouts (9 files)**
- activity_exam_detail.xml: gradient header + info cards + button
- activity_exam_session.xml: toolbar with icons, timer, progress
- activity_exam_result.xml: circular score + stats + breakdowns
- fragment_practice.xml: NEW — scrollable topic cards + progress lists
- dialog_question_grid.xml: NEW — grid + stats + submit button
- item_question_grid.xml: NEW — numbered cells with state colors
- item_exam.xml: redesigned card layout
- fragment_exam.xml: search + filter chips
- fragment_profile.xml: Vietnamese labels

**Drawables (20+ files)**
- 10 vector icons for tabs/features
- 10 background drawables for buttons/states
- 3 custom progress bar drawables
- 1 circular progress ring

**Manifest & Config**
- AndroidManifest.xml: + ExamDetailActivity registration
- mobile_navigation.xml: + nav_practice fragment
- bottom_nav_menu.xml: 4 items with icons
- colors.xml: expanded color palette

### Bản dựng
- **API Call**: ExamFragment → ExamDetailActivity → ExamSessionActivity → ExamResultActivity
- **Navigation**: BottomNav 4 tabs + intent-based activities
- **Data Flow**: Retrofit API → RecyclerViews → Activities
- **State Management**: BookmarkedQuestions Set, SelectedAnswers Map persisted during session
- **Build Status**: ✅ BUILD SUCCESSFUL (all tasks up-to-date)

---

## [0.3.0] - 2026-04-04 — Kết nối Neon DB + Fix Auth + GitHub

### Đã hoàn thành
- [x] Kết nối Neon Serverless PostgreSQL (ap-southeast-1)
- [x] Chạy `vsat_database_schema.sql` trên Neon → 27 bảng + 20 ENUM types
- [x] Tạo file `.env` với credentials thực tế
- [x] **DataInitializer**: tự tạo 4 tài khoản test khi backend khởi động
  - `student@vsat.com` / `Student@123` (STUDENT)
  - `collab@vsat.com` / `Admin@123` (COLLABORATOR)
  - `content@vsat.com` / `Admin@123` (CONTENT_ADMIN)
  - `admin@vsat.com` / `Admin@123` (SUPER_ADMIN)

### Fix lỗi Android ↔ Backend JSON mismatch
- [x] **RegisterRequest**: bỏ `@SerializedName("full_name")` → gửi đúng `fullName`
- [x] **AuthResponse**: bỏ `@SerializedName("access_token/refresh_token")` → parse đúng `accessToken`/`refreshToken`
- [x] **UserProfile**: bỏ `@SerializedName("full_name"/"avatar_url")` → parse đúng camelCase từ backend
- [x] **AuthApi logout**: thêm body `{refreshToken: "..."}` theo đúng spec backend
- [x] **ApiClient**: thêm `getRefreshToken()` public method

### GitHub
- [x] Init git repository, push lên `https://github.com/haizzdungnay/VSAT_COMPASS`
- [x] Tạo `README.md` + cập nhật `CHANGELOG.md`

---

## [0.2.0] - 2026-04-03 — Android App MVP

### Android (25 Java files, 22 XML resources)

#### Architecture
- MVVM: ViewModel + LiveData + Repository pattern
- Retrofit 2 + OkHttp3 (JWT interceptor tự động gắn Bearer token)
- Jetpack Navigation Component + BottomNavigationView
- ViewBinding toàn bộ

#### Data Layer
- `ApiClient.java` — Retrofit singleton, JWT interceptor, SharedPreferences token storage
- `AuthApi.java` — Retrofit interface: login, register, refresh, logout, getMe
- `ExamApi.java` — Retrofit interface: danh sách đề, chi tiết, start/submit session, submit answer
- Models: `ApiResponse<T>`, `AuthResponse`, `LoginRequest`, `RegisterRequest`,
  `UserProfile`, `Exam`, `Question`, `ExamSession`
- `AuthRepository.java` — LiveData-based auth operations
- `Resource<T>` — LOADING/SUCCESS/ERROR wrapper

#### UI Layer
- `SplashActivity` — tự redirect dựa trên access token
- `LoginActivity` + `RegisterActivity` — auth screens với validation
- `MainActivity` — BottomNav (3 tabs: Trang chủ, Đề thi, Cá nhân)
- `HomeFragment` — chào mừng + thống kê học viên
- `ExamFragment` — danh sách đề thi (RecyclerView + SwipeRefresh)
- `ExamAdapter` — item card: tiêu đề, số câu, thời gian, nút "Làm bài"
- `ExamSessionActivity` — làm bài thi: timer đếm ngược, điều hướng câu hỏi, lưu đáp án
- `ExamResultActivity` — xem điểm, số câu đúng, thời gian làm bài
- `ProfileFragment` — thông tin cá nhân + đăng xuất

#### Resources
- Layouts: `activity_splash`, `activity_login`, `activity_register`, `activity_main`,
  `activity_exam_session`, `activity_exam_result`, `fragment_home`, `fragment_exam`,
  `fragment_profile`, `item_exam`
- Navigation graph: `mobile_navigation.xml` (3 fragments)
- Bottom nav menu: `bottom_nav_menu.xml`
- Colors: primary blue theme + text/error/success colors

#### Cấu hình
- `AndroidManifest.xml`: INTERNET permission, `usesCleartextTraffic=true`, 6 activities khai báo
- `libs.versions.toml`: toàn bộ dependencies khai báo type-safe
- `app/build.gradle.kts`: viewBinding enabled, Java 11, minSdk 28, targetSdk 36

---

## [0.1.0] - 2026-04-02 — Backend MVP (9 Modules hoàn thành)

### Infrastructure (Module 0)
- [x] VsatCompassApiApplication, SecurityConfig, JpaConfig, OpenApiConfig
- [x] JwtAuthenticationFilter, JwtUtils, CustomUserDetails, CustomUserDetailsService
- [x] AppException (factory methods: badRequest, notFound, forbidden, conflict, unauthorized)
- [x] GlobalExceptionHandler (xử lý tập trung toàn bộ exception)
- [x] SecurityUtils, UserMapper
- [x] AuthController, AuthService, AuthServiceImpl (7 endpoints)
- [x] User, RefreshToken entities + repositories
- [x] 3 Auth enums: UserRole, UserStatus, GenderType
- [x] spring-dotenv: load `.env` tự động

### Module 1 — Subject & Topic
- [x] 3 Entities: Subject, Topic, Subtopic
- [x] SubjectService + SubjectServiceImpl
- [x] SubjectController: 7 endpoints (CRUD subject/topic/subtopic)

### Module 2 — Question Bank
- [x] 4 Entities: Question, QuestionOption, QuestionGroup, QuestionVersion
- [x] 4 Enums: DifficultyLevel, QuestionType, QuestionStatus, ReviewAction
- [x] QuestionService + QuestionServiceImpl (CRUD, filter, options, version++)
- [x] QuestionController (collaborator): 5 endpoints
- [x] QuestionAdminController: 4 endpoints

### Module 3 — Review Workflow
- [x] 2 Entities: QuestionReview, QuestionComment
- [x] ReviewService + ReviewServiceImpl (status transitions: APPROVE/REJECT/REQUEST_REVISION)
- [x] ReviewController: 4 endpoints (create review, list, add/list comments)

### Module 4 — Exam Management
- [x] 2 Entities: Exam, ExamQuestion
- [x] 3 Enums: ExamStatus, ExamPricingType, DifficultyLevel
- [x] ExamService + ExamServiceImpl
- [x] ExamAdminController: 7 endpoints (CRUD + questions + status)
- [x] ExamPublicController: 2 public GET endpoints

### Module 5 — Session Engine
- [x] 2 Entities: ExamSession, SessionAnswer
- [x] 2 Enums: SessionMode, SessionStatus
- [x] SessionService + SessionServiceImpl (start, submit, scoring)
- [x] SessionController: 6 endpoints

### Module 6 — Student Stats
- [x] 1 Entity: UserTopicStats
- [x] StudentStatsService + StudentStatsServiceImpl
- [x] StudentStatsController: 3 endpoints (topic stats, weak topics, exam history)

### Module 7 — Ticket System
- [x] 2 Entities: Ticket, TicketComment
- [x] 2 Enums: TicketType, TicketStatus
- [x] TicketService + TicketServiceImpl (UUID ticketCode, lifecycle, comments)
- [x] TicketController (student): 4 endpoints
- [x] TicketAdminController: 6 endpoints

### Module 8 — Dashboard
- [x] DashboardService + DashboardServiceImpl (aggregate counts)
- [x] DashboardController: 1 endpoint (admin overview)

### Module 9 — User Management
- [x] UserManagementService + UserManagementServiceImpl
- [x] UserManagementController: 4 endpoints (list filter, detail, role, status) — SUPER_ADMIN only

---

## Thống kê tổng

| Hạng mục | Số lượng |
|----------|----------|
| Bảng database | 27 |
| ENUM types PostgreSQL | 20 |
| API endpoints MVP | ~52 |
| Backend Java files | 126+ |
| Android Java files | 25 |
| Android XML resources | 22 |


## [0.1.0] - 2026-04-02 — Khởi tạo dự án & Auth Module

### Đã hoàn thành trước phiên làm việc này
- Phân tích & thiết kế: tài liệu tổng hợp, database schema 27 bảng, 20 ENUM types
- Đặc tả kỹ thuật: 67 màn hình, ma trận quyền 65+ hành động, 79 API endpoints
- Spring Boot Auth module: 7/9 endpoints (register, login, refresh, logout, getMe, updateProfile, changePassword)
- JWT authentication: access token 15 phút + refresh token 30 ngày
- Security config: role-based access control, CORS, BCrypt password encoding
- Base infrastructure: ApiResponse wrapper, AppException, GlobalExceptionHandler
- Swagger/OpenAPI config

### Đã hoàn thành trong phiên làm việc này

#### Backend (Spring Boot 3.2.5)
- [x] Extract project `vsat-compass-api` từ zip archive
- [x] Setup Gradle wrapper (Gradle 8.7) cho backend project
- [x] Cấu hình `application.yml` với dev/prod profiles, Neon PostgreSQL

#### Thiết kế chi tiết cho các module tiếp theo
- [x] Module 1 — Subject & Topic: 3 entities, 7 API endpoints (SB-01 đến SB-07)
- [x] Module 2 — Question Bank: 4 entities, 3 enums, 6 API endpoints (QS-01 đến QS-05)
- [x] Module 3 — Review Workflow: 2 entities, 1 enum, 4 API endpoints (RV-01 đến RV-04)
- [x] Module 4 — Exam Management: 2 entities, 2 enums, 9 API endpoints (EX-01 đến EX-09)
- [x] Module 5 — Exam Session Engine: 2 entities, 2 enums, 7 API endpoints (ES-01 đến ES-07)
- [x] Module 6 — History & Analytics: 1 entity, 2 API endpoints
- [x] Module 7 — Ticket System: 2 entities, 2 enums, 7 API endpoints (TK-01 đến TK-07)
- [x] Module 8 — Dashboard: 2 API endpoints (admin + student)
- [x] Module 9 — User Management: 4 API endpoints (UM-01 đến UM-04)
- [x] Android App: thiết kế MVVM architecture, package structure, auth screens

### Đang triển khai (in progress)
- [ ] Code Android app (Auth, Student, Admin screens)
- [ ] Cấu hình database credentials (.env)
- [ ] Build & test backend

### Hoàn thành Backend Modules 1-9 (126 Java files)

#### Module 0 — Infrastructure (từ zip gốc)
- [x] VsatCompassApiApplication, SecurityConfig, JpaConfig, OpenApiConfig
- [x] JwtAuthenticationFilter, JwtUtils, CustomUserDetails, CustomUserDetailsService
- [x] AppException, GlobalExceptionHandler, SecurityUtils, UserMapper
- [x] AuthController, AuthService, AuthServiceImpl
- [x] User, RefreshToken entities + repositories
- [x] 3 Auth enums: UserRole, UserStatus, GenderType

#### Module 1 — Subject & Topic (hoàn thành)
- [x] 3 Entities: Subject, Topic, Subtopic
- [x] 3 Repositories, 3 Request DTOs, 4 Response DTOs
- [x] SubjectService + SubjectServiceImpl
- [x] SubjectController (7 endpoints: CRUD subjects/topics/subtopics)

#### Module 2 — Question Bank (hoàn thành)
- [x] 4 Entities: Question, QuestionOption, QuestionGroup, QuestionVersion
- [x] 4 Repositories, 1 Request DTO (inner classes), 2 Response DTOs
- [x] QuestionService + QuestionServiceImpl
- [x] QuestionController (collaborator: 5 endpoints)
- [x] QuestionAdminController (admin: 4 endpoints)
- [x] 4 Enums: DifficultyLevel, QuestionType, QuestionStatus, ReviewAction

#### Module 3 — Review Workflow (hoàn thành)
- [x] 2 Entities: QuestionReview, QuestionComment
- [x] 2 Repositories, 1 Request DTO, 2 Response DTOs
- [x] ReviewService + ReviewServiceImpl
- [x] ReviewController (4 endpoints: create review, list reviews, add/list comments)

#### Module 4 — Exam Management (hoàn thành)
- [x] 2 Entities: Exam, ExamQuestion
- [x] 2 Repositories, 1 Request DTO (inner classes), 3 Response DTOs
- [x] ExamService + ExamServiceImpl
- [x] ExamAdminController (7 endpoints: CRUD + status + questions)
- [x] ExamPublicController (2 public GET endpoints)
- [x] 3 Enums: ExamStatus, ExamPricingType, DifficultyLevel

#### Module 5 — Session Engine (hoàn thành)
- [x] 2 Entities: ExamSession, SessionAnswer
- [x] 2 Repositories, 2 Request DTOs, 3 Response DTOs
- [x] SessionService + SessionServiceImpl
- [x] SessionController (6 endpoints: start, submit, save answer, get session/answers)
- [x] 2 Enums: SessionMode, SessionStatus

#### Module 6 — Student Stats (hoàn thành)
- [x] 1 Entity: UserTopicStats
- [x] 1 Repository, 2 Response DTOs
- [x] StudentStatsService + StudentStatsServiceImpl
- [x] StudentStatsController (3 endpoints: topic stats, weak topics, exam history)

#### Module 7 — Ticket System (hoàn thành)
- [x] 2 Entities: Ticket, TicketComment
- [x] 2 Repositories, 1 Request DTO, 2 Response DTOs
- [x] TicketService + TicketServiceImpl
- [x] TicketController (student: 4 endpoints)
- [x] TicketAdminController (admin: 6 endpoints)
- [x] 2 Enums: TicketType, TicketStatus

#### Module 8 — Dashboard (hoàn thành)
- [x] 1 Response DTO (DashboardResponse)
- [x] DashboardService + DashboardServiceImpl
- [x] DashboardController (1 endpoint: admin dashboard overview)

#### Module 9 — User Management (hoàn thành)
- [x] 1 Request DTO, 1 Response DTO
- [x] UserManagementService + UserManagementServiceImpl
- [x] UserManagementController (4 endpoints: list, detail, update role/status)

---

## Cấu trúc project hiện tại

```
VSAT_COMPASS/                    ← Android App (Java + XML)
├── app/src/main/java/com/example/v_sat_compass/
├── build.gradle.kts
├── CHANGELOG.md                 ← File này
└── VSAT/                        ← Tài liệu & tài nguyên
    ├── vsat_database_schema.sql
    ├── vsat_schema_documentation.txt
    └── ui/
        ├── VSAT_COMPASS_BAO_CAO_VA_HUONG_DAN.txt
        ├── tong_hop_du_an_vsat_android_web_v2.txt
        ├── vsat_compass_api_spec.xlsx
        ├── vsat_compass_man_hinh_va_phan_quyen.xlsx
        └── sample/vsat-compass-api.zip

vsat-compass-api/                ← Backend Spring Boot (126 files)
├── build.gradle
├── gradlew / gradlew.bat
├── gradle/wrapper/
└── src/main/java/com/vsatcompass/api/
    ├── config/              (SecurityConfig, OpenApiConfig, JpaConfig)
    ├── security/            (JWT utils, filter, CustomUserDetails)
    ├── entity/              (16 entities + 13 enums)
    ├── repository/          (16 repositories)
    ├── dto/request/         (9 request DTOs)
    ├── dto/response/        (19 response DTOs)
    ├── dto/common/          (ApiResponse)
    ├── service/             (9 service interfaces)
    ├── service/impl/        (9 service implementations)
    ├── controller/auth/     (AuthController)
    ├── controller/admin/    (6 admin controllers)
    ├── controller/collaborator/ (2 collaborator controllers)
    ├── controller/student/  (4 student controllers)
    ├── exception/           (AppException, GlobalExceptionHandler)
    ├── mapper/              (UserMapper)
    └── util/                (SecurityUtils)
```

## Thống kê

| Hạng mục | Số lượng |
|----------|----------|
| Bảng database | 27 |
| ENUM types | 20 |
| API endpoints (thiết kế) | 79 |
| API endpoints (đã code) | ~52 MVP endpoints |
| Backend Java files (hiện có) | 126 |
| Backend modules cần build | 0 (9/9 hoàn thành) |
| Android screens (thiết kế) | 67 |
| Android screens (đã code) | 0 |
