# KẾ HOẠCH TRIỂN KHAI GIAI ĐOẠN TIẾP THEO (NEXT PHASE IMPLEMENTATION PLAN)
## V-SAT COMPASS: TECHNICAL DEBT, 5 FEATURE PROPOSALS & HOME SERVER ALIGNMENT

> Trạng thái: **ĐÃ HOÀN THÀNH 100%** (Toàn bộ Phase 1, Phase 2, Phase 3, Phase 4 — Nhóm A, B, C)
> Phiên bản phát hành: **v1.2.0** (Backend & Web Admin/CBT) | **android/v0.3.0** (`versionCode = 3`)

---

## MỤC LỤC
1. [Section 0: Hướng dẫn cho Phiên làm việc tiếp theo (Session Handoff & Rules)](#section-0-hướng-dẫn-cho-phiên-làm-việc-tiếp-theo-session-handoff--rules)
2. [Section 1: Phase 1 — Xử lý Nợ Kỹ Thuật (Technical Debt TD-1 .. TD-4)](#section-1-phase-1--xử-lý-nợ-kỹ-thuật-technical-debt-td-1--td-4)
3. [Section 2: Phase 2 — Chi tiết 5 Đề xuất Tính năng Mới (Proposals P1 .. P5)](#section-2-phase-2--chi-tiết-5-đề-xuất-tính-năng-mới-proposals-p1--p5)
4. [Section 3: Phase 3 — Triển khai Linux Home Server](#section-3-phase-3--triển-khai-linux-home-server)
5. [Section 4: Tiêu chuẩn Nghiệm thu Tự động (Verification & Acceptance Gates)](#section-4-tiêu-chuẩn-nghiệm-thu-tự-động-verification--acceptance-gates)
6. [Section 5: Phase 4 — Vòng lặp Nâng cấp Tính năng (Feature Expansions & Hardening)](#section-5-phase-4--vòng-lặp-nâng-cấp-tính-năng-feature-expansions--hardening)

---

## SECTION 0: HƯỚNG DẪN CHO PHIÊN LÀM VIỆC TIẾP THEO (SESSION HANDOFF & RULES)

### 0.1. Trạng thái Baseline Hiện tại
- **Android App (`app/`)**: 253/253 unit tests pass. Bản `release` APK đã ký tại `app/build/outputs/apk/release/app-release.apk` (dung lượng ~4.3 MB, bật R8 full shrinking & obfuscation, phiên bản `android/v0.3.0`, `versionCode = 3`).
- **Backend API (`VSAT/vsat-compass-api/`)**: 371/371 unit tests pass. Phiên bản `v1.2.0`, Spring Boot 3.5.9, Flyway `V0..V17`, Gradle build tool, Java 17 bytecode target (tương thích Java 21 runtime).
- **Web Admin & CBT (`web-admin/`)**: Phiên bản `v1.2.0`, React 19.2 + Vite 8.3 + TypeScript 6.0. Đã chạy thực tế 8 module: Blueprints (CRUD DB), ExamWizard (4 bước), QuestionBank, ReviewQueue (KaTeX math preview), ImportQuestions (Đa định dạng .xlsx/.json/.docx/.txt), Schedules (Ca thi), StudentCBT (Thi online), Users & Tickets.
- **CI / Testing Harness**: Workflow `.github/workflows/auto-acceptance.yml` và bộ harness clean-room độc lập `acceptance-harness/` đạt 126/126 checks passed (0 violations).

### 0.2. Ba Quy tắc Bắt buộc (Non-Negotiable Guardrails)
1. **Quy trình nghiệm thu tự động**: Sử dụng skill `/auto-acceptance-loop` (vòng lặp 9 bước chuẩn phối hợp giữa Fable, Opus, Sonnet) cho các đợt phát triển lớn.
2. **Quy chuẩn Git Commit**: TUYỆT ĐỐI KHÔNG thêm trailer `Co-Authored-By: Claude` (hoặc bất kỳ thông tin AI nào) vào commit message. User là tác giả duy nhất.
3. **Bảo vệ Môi trường Production**: KHÔNG tự ý `git push` lên branch chính khi chưa có yêu cầu; KHÔNG trỏ test tự động vào cơ sở dữ liệu Neon Cloud Production.

### 0.3. Thứ tự Thực hiện Khuyến nghị
```text
Phase 1: Technical Debt (TD-1 -> TD-2 -> TD-3 -> TD-4)
   │
   ▼
Phase 2: Feature Proposals (P1 -> P2 -> P3 -> P4 -> P5)
   │
   ▼
Phase 3: Linux Home Server Cutover (Chặng 0 -> 7 theo docs/LINUX_HOME_SERVER_ROADMAP.md)
```

---

## SECTION 1: PHASE 1 — XỬ LÝ NỢ KỸ THUẬT (TECHNICAL DEBT TD-1 .. TD-4) [x] ĐÃ HOÀN THÀNH (commit 5567aae)

### TD-1: Dọn dẹp Git Working Tree, Đồng bộ Tag & Xóa Branch Backup [x]
1. **Phân tách và commit working tree đang dở dang**:
   - *Cảnh báo*: Không dùng lệnh `git add -u` gộp chung.
   - Nhóm 1 (Tài liệu báo cáo cũ): 3 file `.docx` bị xóa unstaged (`Bao_cao_cuoi_ky_VSAT_Compass_Final*.docx`). Commit: `chore(docs): remove obsolete final docx reports`.
   - Nhóm 2 (Acceptance Harness & CI): 6 file modified (`.github/workflows/auto-acceptance.yml`, `acceptance-harness/run-all.mjs`, `lib/test-harness.mjs`, `stubs/llm-mock-server.mjs`, `suites/backend-public-student-collab.mjs`, `suites/helpers/backend-psc-helpers.mjs`). Commit: `ci: align acceptance harness suite helpers and workflow flags`.
2. **Gắn Tag phát hành mới**:
   - Hiện tại tag `android/v0.1.0` đang trỏ commit cũ `1c411ba` (sau đó 25 commits).
   - Tạo tag mới tại commit release mới nhất: `git tag -a android/v0.2.0 -m "Release APK v0.2.0 with production configs"` và `git tag -a v1.1.0 -m "Backend & Web Admin aligned release v1.1.0"`.
3. **Xóa branch backup cũ**:
   - Branch `backup-pre-batch-2b-20260430` (tạo ngày 2026-04-30, hạn giữ đến 2026-06-01) đã hết thời hạn lưu trữ.
   - Thực hiện xóa: `git branch -D backup-pre-batch-2b-20260430` và `git push origin --delete backup-pre-batch-2b-20260430`.

### TD-2: Tích hợp Flyway Migration vào Backend API [x]
1. **Đường dẫn dữ liệu SQL thực tế**:
   - 11 file migration nằm tại: `VSAT/vsat-compass-api/docs/sql/0001_*.sql` đến `0011_*.sql` (thư mục `docs/sql/` ở root không tồn tại).
2. **Khai báo dependencies trong `VSAT/vsat-compass-api/build.gradle`**:
   ```groovy
   implementation 'org.flywaydb:flyway-core:10.15.2'
   implementation 'org.flywaydb:flyway-database-postgresql:10.15.2'
   ```
3. **Tổ chức thư mục và cấu hình**:
   - Tạo thư mục: `VSAT/vsat-compass-api/src/main/resources/db/migration/`.
   - Sao chép và đổi tên chuẩn Flyway: `0001_create_tickets.sql` -> `V1__create_tickets.sql`, ..., `0011_add_exam_window_title.sql` -> `V11__add_exam_window_title.sql`.
   - Cập nhật `application.yml`:
     ```yaml
     spring:
       flyway:
         enabled: true
         baseline-on-migrate: true
         baseline-version: 0
         locations: classpath:db/migration
     ```
   - Chạy test kiểm chứng: `./gradlew test` đảm bảo DB in-memory H2 hoặc PostgreSQL test container chạy trơn tru qua toàn bộ 11 scripts.

### TD-3: Hoàn thiện 2 Module còn thiếu trên Web Admin (`web-admin/`) [x]
1. **Module 1: User Management (`web-admin/src/pages/Users/index.tsx`)**:
   - Backend endpoint đã có: `AdminUserController.java:28-75` (`GET /api/v1/admin/users`, `PATCH /api/v1/admin/users/{id}/role`, `PATCH /api/v1/admin/users/{id}/lock`, `PATCH /api/v1/admin/users/{id}/unlock`). Android đã chạy ổn định tại `AdminUserManagementFragment.java:25`.
   - Bổ sung API client trong `web-admin/src/services/api.ts`: `fetchAdminUsers`, `updateUserRole`, `lockUserAccount`, `unlockUserAccount`.
   - Xây dựng UI: Danh sách tài khoản người dùng, bộ lọc theo vai trò (`STUDENT`, `TEACHER`, `COLLABORATOR`, `ADMIN`), nút khóa/mở khóa tài khoản kèm modal xác nhận.
   - Khai báo routing & menu: Thêm tab `users` vào `Sidebar.tsx` và `App.tsx`.
2. **Module 2: Ticket Management (`web-admin/src/pages/Tickets/index.tsx`)**:
   - Backend endpoint đã có: `AdminTicketController.java:24-38` (`GET /api/v1/admin/tickets`, `PATCH /api/v1/admin/tickets/{id}`).
   - Bổ sung API client trong `web-admin/src/services/api.ts`: `fetchAdminTickets`, `updateTicketStatus`.
   - Xây dựng UI: Danh sách ticket báo lỗi câu hỏi từ học viên; bộ lọc trạng thái (`OPEN`, `IN_PROGRESS`, `RESOLVED`, `REJECTED`); render nội dung câu hỏi bị báo lỗi qua component `<MathView />` (KaTeX); khung nhập phản hồi xử lý của admin.
   - Thêm tab `tickets` vào `Sidebar.tsx` và `App.tsx`.

### TD-4: Kiểm soát Đồng thời (@Version Optimistic Locking) [x]
1. **Thiết kế kỹ thuật**: Áp dụng Option C trong `docs/design/DESIGN_EXAM_VERSION.md:47-50` (dùng trường `lock_version` độc lập, tránh xung đột với số phiên bản nghiệp vụ hiển thị cho người dùng).
2. **Migration cơ sở dữ liệu (`V12__add_lock_version.sql`)**:
   ```sql
   ALTER TABLE exams ADD COLUMN lock_version INTEGER NOT NULL DEFAULT 0;
   ALTER TABLE questions ADD COLUMN lock_version INTEGER NOT NULL DEFAULT 0;
   ```
3. **Cập nhật Entity**:
   - `Exam.java:88`: Thêm `@Version @Column(name = "lock_version", nullable = false) private Integer lockVersion;`.
   - `Question.java:79`: Thêm `@Version @Column(name = "lock_version", nullable = false) private Integer lockVersion;`.
4. **Xử lý Exception**:
   - Bổ sung handler trong `GlobalExceptionHandler.java`: Bắt `ObjectOptimisticLockingFailureException`, trả về HTTP `409 Conflict` kèm thông báo tiếng Việt: "Dữ liệu vừa được cập nhật bởi một phiên làm việc khác. Vui lòng tải lại trang."

---

## SECTION 2: PHASE 2 — CHI TIẾT 5 ĐỀ XUẤT TÍNH NĂNG MỚI (PROPOSALS P1 .. P5) [x] ĐÃ HOÀN THÀNH

### Đề xuất 1 (P1): Phân hệ Thi Trực Tuyến trên Máy tính (Web Student CBT) [x] (commit d0e65e5)
1. **Kiến trúc Giao diện**: Mở rộng trong `web-admin/` (hoặc switch layout theo vai trò `STUDENT` sau khi xác thực JWT tại `App.tsx`).
2. **Ba màn hình bắt buộc**:
   - **`StudentExamLobby`**: Tải danh sách đề thi tự do (`GET /api/v1/exams`) và ca thi tập trung (`GET /api/v1/exam-windows`), kiểm tra thời gian mở ca, nút "Bắt đầu làm bài".
   - **`StudentCBTWorkspace`**:
     - Đồng hồ đếm ngược (Web Worker chạy ngầm, không lệch giờ khi chuyển tab).
     - Split-pane layout: Bên trái ghim bài đọc hiểu (`QuestionGroup`), bên phải hiển thị danh sách câu hỏi con có thể cuộn độc lập.
     - Question Palette: Bản đồ câu hỏi với trạng thái Đã làm / Chưa làm / Đánh dấu xem lại.
     - Đầy đủ 3 dạng câu hỏi V-SAT: MCQ 4 lựa chọn, Đúng/Sai 4 mệnh đề, Trả lời ngắn điền số. Render công thức Toán/Lý/Hóa bằng `<MathView />` (KaTeX).
     - Phím tắt bàn phím: `ArrowLeft`/`ArrowRight` chuyển câu, phím `1-4` chọn đáp án, phím `F` gắn cờ đánh dấu.
     - Cơ chế Auto-save: Lưu `answer_payload` (JSONB) lên backend mỗi 15 giây hoặc ngay khi học viên chọn đáp án.
   - **`StudentScorecard & Review`**:
     - Hiển thị bảng điểm phân tầng V-SAT (điểm thô thang 150, quy đổi năng lực).
     - Xem lại chi tiết từng câu kèm lời giải mẫu; nút "Nhờ AI giải thích" gọi `POST /api/v1/ai/explain`.
     - Nút "Báo lỗi câu hỏi" mở modal gửi ticket trực tiếp qua `POST /api/v1/tickets`.

### Đề xuất 2 (P2): Dự đoán Trúng tuyển Đại học (Admission Matching Engine) [x] (commit a966b38)
1. **Migration cơ sở dữ liệu (`V13__create_admission_benchmarks.sql`)**:
   ```sql
   CREATE TABLE universities (
       id BIGSERIAL PRIMARY KEY,
       code VARCHAR(50) NOT NULL UNIQUE,
       name VARCHAR(255) NOT NULL,
       region VARCHAR(50)
   );
   CREATE TABLE admission_benchmarks (
       id BIGSERIAL PRIMARY KEY,
       university_id BIGINT NOT NULL REFERENCES universities(id) ON DELETE CASCADE,
       major_code VARCHAR(50) NOT NULL,
       major_name VARCHAR(255) NOT NULL,
       subject_combo VARCHAR(50) NOT NULL,
       year INTEGER NOT NULL,
       cutoff_score NUMERIC(5,2) NOT NULL,
       notes TEXT
   );
   CREATE INDEX idx_benchmarks_combo ON admission_benchmarks(subject_combo, year);
   ```
   - Seed dữ liệu điểm chuẩn V-SAT thực tế từ các trường: HUB, UEL, ĐH Cần Thơ, UFM, ĐH Thái Nguyên...
2. **Backend Service & Controller**:
   - `AdmissionBenchmark.java`, `AdmissionService.java`, `AdmissionController.java` (`GET /api/v1/admissions/benchmarks`, `POST /api/v1/admissions/predict`).
   - Thuật toán phân nhóm xác suất trúng tuyển dựa trên tổng điểm 3 môn từ lịch sử thi của học viên:
     - `SAFE` (An toàn): Điểm học viên $\ge$ Điểm chuẩn $+ 15.0$ điểm.
     - `FIT` (Vừa sức): Điểm học viên trong khoảng $[\text{Điểm chuẩn} - 10.0, \text{Điểm chuẩn} + 15.0)$.
     - `RISK` (Rủi ro): Điểm học viên $<$ Điểm chuẩn $- 10.0$ điểm.
3. **Phía Client**:
   - Android (`AdmissionPredictorActivity.java`): Màn hình chọn tổ hợp xét tuyển, lọc theo trường/khu vực, danh sách ngành chia 3 tab màu trực quan (Xanh/Vàng/Đỏ).
   - Web CBT: Tích hợp widget dự đoán ngay trên trang Dashboard của học viên.

### Đề xuất 3 (P3): Hỗ trợ Âm thanh / Listening cho Môn Tiếng Anh [x] (commit fe0e789)
1. **Migration cơ sở dữ liệu (`V14__add_audio_to_question_groups.sql`)**:
   ```sql
   ALTER TABLE question_groups
       ADD COLUMN audio_url VARCHAR(500),
       ADD COLUMN audio_duration_sec INTEGER,
       ADD COLUMN max_plays INTEGER DEFAULT 2;
   ```
2. **Cập nhật Backend**:
   - Entity `QuestionGroup.java:38-39` và DTOs tương ứng.
   - Bổ sung cột `audio_url`, `max_plays` vào sheet `Question Groups` trong `QuestionImportServiceImpl.java` để hỗ trợ import file Excel.
3. **Web Admin & Web CBT**:
   - Nhúng trình phát âm thanh HTML5 `<audio>` vào `ReviewQueue` và `QuestionBank`.
   - Trong `StudentCBTWorkspace`: Khóa thanh tua âm thanh (disable seeking), đếm ngược số lượt nghe còn lại theo giới hạn `max_plays`.
4. **Android Client (`app/`)**:
   - Tích hợp AndroidX `Media3 ExoPlayer` trong `ExamSessionActivity.java` và `ExamReviewActivity.java`.
   - Mở rộng `OfflineExamRepository.java:312-315`: Tải file audio về máy, mã hóa và lưu trữ offline cùng chuẩn **AES-128-GCM** với header `VSAT\x02`.

### Đề xuất 4 (P4): Chế độ Giám sát Thi Chống Gian lận (Anti-Cheating / Proctoring Mode) [x] (commit 310f00f)
1. **Khái niệm nghiệp vụ**: Kích hoạt tự động khi ca thi có `mode = 'SCHEDULED_MOCK'` (đã khai báo tại `ExamWindow.java:39`).
2. **Migration cơ sở dữ liệu (`V15__add_session_proctoring.sql`)**:
   ```sql
   ALTER TABLE exam_sessions
       ADD COLUMN violation_count INTEGER NOT NULL DEFAULT 0,
       ADD COLUMN proctoring_logs JSONB DEFAULT '[]'::jsonb;
   ```
3. **Backend Service**:
   - Endpoint: `POST /api/v1/sessions/{sessionId}/violations`.
   - Tiếp nhận các mã vi phạm: `FOCUS_LOST`, `APP_BACKGROUNDED`, `MULTI_WINDOW`, `LOCK_TASK_EXITED`, `FULLSCREEN_EXITED`.
   - Tự động khóa phiên và thu bài cưỡng chế nếu `violation_count >= 3`.
4. **Android Client (`ExamSessionActivity.java`)**:
   - Khi ca thi là `SCHEDULED_MOCK`: Kích hoạt `FLAG_SECURE` (chặn chụp/quay màn hình), kích hoạt chế độ ghim màn hình `startLockTask()`.
   - Bắt sự kiện thoát ứng dụng hoặc chia đôi màn hình qua `isInMultiWindowMode()`, `onWindowFocusChanged(false)`, `onStop()`. Hiển thị dialog cảnh báo và gửi log vi phạm về server.
5. **Web CBT**:
   - Bắt buộc vào chế độ toàn màn hình (`requestFullscreen()`); lắng nghe sự kiện `visibilitychange` và `window.onblur` để ghi nhận vi phạm rời khỏi bài thi.

### Đề xuất 5 (P5): Tối ưu Bộ nhớ Đệm (Redis Cache) & Connection Pooling [x] (commit e317eee)
1. **Tối ưu Connection Pooling (HikariCP)**:
   - File cấu hình `VSAT/vsat-compass-api/src/main/resources/application.yml:78-79,101-106` hiện đang cố định `maximum-pool-size: 5` (dành cho Neon Cloud).
   - Tham số hóa biến môi trường: `maximum-pool-size: ${DB_POOL_MAX_SIZE:5}`. Khi triển khai trên Linux Home Server (PostgreSQL 17 cục bộ), nâng giá trị lên `25`.
2. **Tích hợp Redis Cache**:
   - Thêm dependencies vào `VSAT/vsat-compass-api/build.gradle`:
     ```groovy
     implementation 'org.springframework.boot:spring-boot-starter-data-redis'
     implementation 'org.springframework.boot:spring-boot-starter-cache'
     ```
   - Cấu hình chuyển đổi linh hoạt trong `application.yml`:
     ```yaml
     spring:
       cache:
         type: ${CACHE_TYPE:simple} # 'simple' cho test/dev cục bộ, 'redis' cho production
       data:
         redis:
           host: ${REDIS_HOST:localhost}
           port: ${REDIS_PORT:6379}
     ```
3. **Phạm vi Caching**:
   - Bảng xếp hạng: `@Cacheable(value = "leaderboard", key = "#subjectId")` tại `LeaderboardService` (TTL 60s).
   - Ma trận đề thi: `@Cacheable(value = "blueprints")` tại `BlueprintService` (TTL 1 giờ, tự động evict khi admin sửa ma trận).
   - Danh sách đề thi & ca thi mở: Cache danh mục đề public `/api/v1/exams` (TTL 60s, evict khi có đề mới được publish).
4. **Bổ sung Container Redis vào Hạ tầng**:
   - Cập nhật Chặng 4 trong `docs/LINUX_HOME_SERVER_ROADMAP.md` và `docker-compose.prod.yml`: thêm service `vsat-redis` sử dụng image `redis:7-alpine`, giới hạn RAM 256MB.

---

## SECTION 3: PHASE 3 — TRIỂN KHAI LINUX HOME SERVER (GÓI PRE-LINUX INFRASTRUCTURE SẴN SÀNG) [x]

*Đã hoàn thiện đóng gói toàn bộ artifacts Pre-Linux Infrastructure:*
- `docker-compose.prod.yml`: PostgreSQL 17 + Redis 7 LRU + Spring Boot API + Web Admin SPA
- `.env.prod.example`: Cấu hình an toàn cho môi trường Linux Home Server
- `ops/scripts/run-migrations.sh`: Kiểm tra PostgreSQL connection & xác nhận bảng `flyway_schema_history`
- `ops/scripts/backup-db.sh`: Sao lưu tự động nén gzip, lưu 14 ngày, sync Rclone & bot Telegram
- `ops/scripts/health-monitor.sh`: Giám sát Actuator Health, CPU, RAM, Disk, tự động restart container
- `ops/scripts/smoke-test.sh`: Kiểm thử khói toàn diện 5/5 endpoints sau deploy

*Lộ trình triển khai chi tiết 8 chặng tại:*
[`docs/LINUX_HOME_SERVER_ROADMAP.md`](./LINUX_HOME_SERVER_ROADMAP.md)

1. **Chặng 0 - 1**: Cài Ubuntu Server 24.04 LTS, cấu hình BIOS Auto Power On, mask `sleep.target`, mạng tĩnh `192.168.1.240`, SSH Port `2222`, kích hoạt UFW.
2. **Chặng 2 - 3**: Cài đặt Docker Engine CE, giới hạn log rotation (20m/5 files), chạy PostgreSQL 17 container, mount volume cục bộ, thực thi tuần tự các file migration Flyway.
3. **Chặng 4 - 5**: Build ứng dụng nội bộ (Spring Boot API + Web Admin static SPA + Redis container), đấu nối Cloudflare Tunnel (`cloudflared`) với domain chính thức, bật Zero Trust OTP bảo vệ Web Admin.
4. **Chặng 6 - 7**: Cấu hình Rclone tự động đẩy backup PostgreSQL lên Google Drive mỗi ngày lúc 02:00 AM kèm bot Telegram báo sự cố; cập nhật URL backend cho Android APK; chạy toàn bộ bộ kiểm thử khói (Smoke Test).

---

## SECTION 4: TIÊU CHUẨN NGHIỆM THU TỰ ĐỘNG (VERIFICATION & ACCEPTANCE GATES)

Mọi phiên làm việc bắt buộc phải chạy và xác nhận đạt 100% các lệnh kiểm tra sau trước khi bàn giao:

```bash
# 1. Kiểm tra Backend API (Toàn bộ 298+ unit tests và Flyway migration)
cd "E:\TuanDuong\lap trinh kiem com\Java\VSAT_COMPASS\VSAT\vsat-compass-api"
./gradlew clean test --no-daemon

# 2. Kiểm tra Android Client (Toàn bộ 253+ unit tests)
cd "E:\TuanDuong\lap trinh kiem com\Java\VSAT_COMPASS"
./gradlew testDebugUnitTest --no-daemon

# 3. Kiểm tra Build Web Admin (Typecheck + Vite production bundle)
cd "E:\TuanDuong\lap trinh kiem com\Java\VSAT_COMPASS\web-admin"
npm run build

# 4. Kiểm tra Toàn trình Clean-room Acceptance Harness
cd "E:\TuanDuong\lap trinh kiem com\Java\VSAT_COMPASS"
node acceptance-harness/run-all.mjs
```

---

## SECTION 5: PHASE 4 — VÒNG LẶP NÂNG CẤP TÍNH NĂNG (FEATURE EXPANSIONS & HARDENING) [x] ĐÃ HOÀN THÀNH 100%

### 5.1. Cụm 1 (`PR #26`, commit `08a65f1`) [x] 100%
- Hỗ trợ đầy đủ dạng câu hỏi ghép hợp **V-SAT 2026 MATCHING** (`QuestionMatchingChoice`, bảng câu hỏi ghép hợp trên Web CBT `MatchingQuestionPanel.tsx`, chấm điểm và đáp án chi tiết trên `StudentScorecard`).
- Lưu trữ bền vững thông tin ngữ liệu nghe (`audioUrl`, `audioDurationSec`, `maxPlays`) xuyên suốt API response và tải về offline.
- Tối ưu hóa Redis cache serialization (`CacheConfig.java`) và kiểm thử đơn vị (`CacheConfigTest`, `LeaderboardServiceTest`, `SessionServiceTest`).

### 5.2. Cụm 2 & 3 (`PR #27`, commit `9d30aee`) [x] 100%
- Hoàn thiện các module **Web Admin**: Exam Wizard 4 bước (`Step1General`, `Step2Composition`, `Step3Preview`), tích hợp phát/kiểm tra Audio trong `QuestionBank`, `QuestionCreateModal`, `ReviewQueue`, và luồng xác thực `Login`, `Dashboard`, `Settings`.
- Hoàn thiện **Android Client**: Lưu trữ và phát Audio offline (`OfflineExamRepository`, `ExamSessionActivity`, `ExamReviewActivity`) và khóa ghim màn hình `lockTask` cho ca thi giám sát.

### 5.3. Vòng lặp 3 — Nhóm A: Đồng bộ Câu trả lời, Đếm Lượt nghe Audio & Phân tách Token [x] 100%
- **A1 (Server-side Session Answers Sync & Clear)**:
  - Bổ sung endpoint `GET /api/v1/sessions/{id}/answers` khôi phục trạng thái làm bài thí sinh qua `SessionAnswerResponse` (ẩn `isCorrect` và `rawPointsAwarded`).
  - Hỗ trợ xóa câu trả lời đã lưu khi client gửi `selectedOptionId == null` và `answerPayload` rỗng, tự động cập nhật `answeredCount`.
- **A2 (Audio Play Counter & Pessimistic Lock)**:
  - Migration `V16__add_session_audio_play_counts.sql` bổ sung `audio_play_counts JSONB` trên `exam_sessions`.
  - Khóa bi quan `@Lock(LockModeType.PESSIMISTIC_WRITE)` tại `ExamSessionRepository.findByIdForUpdate`.
  - Cung cấp `GET /api/v1/sessions/{id}/audio-plays` và `POST /api/v1/sessions/{id}/audio-plays`; chặn vượt quá `maxPlays` bằng HTTP `409 Conflict`.
- **A3 (Role-scoped JWT Storage)**:
  - Phân tách JWT storage keys theo vai trò trên Web: `vsat_admin_access_token` (`ADMIN`) và `vsat_student_access_token` (`STUDENT`) kèm cơ chế fallback tương thích ngược.

### 5.4. Vòng lặp 3 — Nhóm B: Blueprint DB CRUD (`WEB-3`) & Multi-format Question Import (`WEB-4`) [x] 100%
- **B1 / `WEB-3` (Blueprint DB CRUD)**:
  - Migration `V17__add_blueprint_cells_json.sql` thêm `cells_json JSONB` và seed ma trận chuẩn 7 môn thi V-SAT.
  - Bổ sung `ExamBlueprint`, `ExamBlueprintRepository`, `BlueprintUpsertRequest`, và các endpoint `POST/PUT/DELETE /api/v1/admin/blueprints` kèm `@Caching` evict `blueprints`/`blueprintDetail`.
  - Tích hợp Modal Tạo mới, Chỉnh sửa và Xóa ma trận trực tiếp trên `web-admin/src/pages/Blueprints/index.tsx`.
- **B2 / `WEB-4` (Multi-format Question Import & Audio Preview)**:
  - Mở rộng `QuestionImportServiceImpl` và giao diện `ImportQuestions/index.tsx` hỗ trợ 4 định dạng `.xlsx`, `.json`, `.docx`, `.txt`.
  - Bổ sung parser dạng câu hỏi `MATCHING` và hiển thị cột xem trước audio (`audioUrl`, `maxPlays`) trong bảng preview trước khi lưu.

### 5.5. Nhóm C & Kết quả Nghiệm thu Tổng thể (`v1.2.0` / `android/v0.3.0`) [x] 100%
- **Nhóm C (Docs Cleanup & Version Sync)**: Dọn dẹp toàn bộ báo cáo nháp và log tạm; đồng bộ chuẩn tài liệu hệ thống lên `v1.2.0` (Backend & Web Admin/CBT) và `android/v0.3.0` (`versionCode = 3`).
- **Kết quả nghiệm thu**: Backend JUnit **371/371 passed**, Android unit tests **253/253 passed**, Web Admin `npm run build` **pass**, Clean-room Acceptance Harness **126/126 passed** (`ACCEPTED`).

