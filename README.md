# V-SAT Compass

> Nền tảng mô phỏng thi, đánh giá năng lực và ôn luyện V-SAT đa nền tảng — Backend Spring Boot + Web Admin/Student CBT + Android Native

**Phiên bản hiện hành:** `v1.2.0` (Backend & Web Admin/CBT) | `android/v0.3.0` (`versionCode = 3`)

![Version](https://img.shields.io/badge/Version-v1.2.0-blue)
![Android](https://img.shields.io/badge/Android-v0.3.0%20(Java)-green?logo=android)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.9-brightgreen?logo=springboot)
![React](https://img.shields.io/badge/Web%20Admin%20%26%20CBT-React%2019.2-61dafb?logo=react)
![PostgreSQL](https://img.shields.io/badge/Database-PostgreSQL%2017-blue?logo=postgresql)
![License](https://img.shields.io/badge/license-MIT-lightgrey)

---

## Tổng quan

**V-SAT Compass** là hệ sinh thái ôn luyện và thi trực tuyến chuẩn hóa theo kỳ thi V-SAT 2026 (Vietnam Scholastic Assessment Test), gồm 3 phân hệ đồng bộ:

- **Backend API (`VSAT/vsat-compass-api/`)** — Spring Boot 3.5.9 REST API, quản lý ngân hàng câu hỏi 4 định dạng, ma trận đề thi 3 chiều, ca thi tập trung, chấm điểm phân tầng thang 150, audio play pessimistic locking, và Redis Cache.
- **Web Admin & Student CBT (`web-admin/`)** — React 19 + Vite 8 + TypeScript 6: Cổng quản trị (Blueprints CRUD, ExamWizard 4 bước, Import đa định dạng `.xlsx/.json/.docx/.txt`, ReviewQueue KaTeX/Audio) và Giao diện thi máy tính (StudentCBTWorkspace, StudentScorecard, AdmissionPredictor).
- **Android App (`app/`)** — Java + XML: Giao diện học viên và quản trị di động, mã hóa đề thi & file nghe offline `.vsa` AES-128-GCM, Kiosk mode `startLockTask()` chống gian lận, dự đoán trúng tuyển đại học.

---

## Tech Stack

### Backend
| Công nghệ | Phiên bản |
|-----------|-----------|
| Java | 17 |
| Spring Boot | 3.5.9 |
| PostgreSQL (Neon / Docker) | 17 |
| Flyway Migrations | V0..V17 |
| Redis Cache | 7 (LRU) |
| Spring Security + JWT | jjwt 0.12.5 |
| Apache POI (Excel import) | 5.3.0 |
| Lombok + MapStruct | — |
| Swagger / OpenAPI | springdoc 2.5.0 |
| Gradle | 8.7 |

### Web Admin & Student CBT
| Công nghệ | Phiên bản |
|-----------|-----------|
| React | 19.2.8 |
| TypeScript | ~6.0.2 |
| Vite | 8.3.0 |
| Tailwind CSS | 3.4.19 |
| KaTeX | 0.19.0 |
| Oxlint | 1.81.0 |

### Android
| Công nghệ | Phiên bản |
|-----------|-----------|
| Java | 11 |
| Android SDK | 36 (min 28) |
| Retrofit 2 / OkHttp 3 | 2.11.0 / 4.12.0 |
| Gson / Glide | 2.11.0 / 4.16.0 |
| Media3 ExoPlayer / WorkManager | Offline Audio + Background Sync |
| Jetpack Navigation / Lifecycle | 2.8.5 / 2.8.7 |
| Material Components | 1.13.0 |

---

## Cấu trúc dự án

```
VSAT_COMPASS/                          ← Repository root
├── app/                               ← Android App (v0.3.0, versionCode = 3)
│   └── src/main/java/com/example/v_sat_compass/
│       ├── VsatApp.java               ← Application class
│       ├── MainActivity.java          ← Bottom nav + Navigation
│       ├── data/
│       │   ├── api/                   ← ApiClient, AuthApi, ExamApi, AdminApi, AdminQuestionApi
│       │   ├── local/                 ← LocalExamDataSource, OfflineExamRepository (AES-128-GCM .vsa)
│       │   ├── model/                 ← Gson models (QuestionGroup, Matching, Admission…)
│       │   └── repository/            ← Auth, SessionContent, ExamHistory, Gamification, Flashcard
│       └── ui/
│           ├── auth/                  ← Splash, Login, Register
│           ├── home/                  ← HomeFragment (dashboard student)
│           ├── exam/                  ← ExamFragment, ExamSession (Kiosk LockTask), ExamReview, History
│           ├── practice/              ← PracticeFragment, Flashcards, Progress, AdmissionPredictor
│           └── admin/                 ← AdminActivity, Exam/Question/User management
│
├── web-admin/                         ← Web Admin & Student CBT Portal (v1.2.0)
│   ├── package.json
│   └── src/pages/
│       ├── Dashboard/                 ← KPI & tổng quan hệ thống
│       ├── QuestionBank/              ← Quản lý ngân hàng câu hỏi + audio
│       ├── Blueprints/                ← DB-backed ma trận đề 3 chiều CRUD (WEB-3)
│       ├── ExamWizard/                ← Trình soạn đề 4 bước
│       ├── ImportQuestions/           ← Nạp đa định dạng .xlsx/.json/.docx/.txt (WEB-4)
│       ├── ReviewQueue/               ← Duyệt câu hỏi KaTeX & Audio preview
│       ├── Schedules/                 ← Quản lý ca thi tập trung (exam_windows)
│       ├── StudentCBT/                ← Sảnh thi, StudentCBTWorkspace, StudentScorecard, AdmissionPredictor
│       ├── Users/ & Tickets/          ← Quản lý người dùng & xử lý báo lỗi
│
├── VSAT/
│   ├── vsat-compass-api/              ← Backend Spring Boot 3.5.9 (v1.2.0)
│   │   ├── Dockerfile                 ← Multi-stage build
│   │   └── src/main/
│   │       ├── java/com/vsatcompass/api/
│   │       │   ├── config/            ← Security, Redis Cache, CORS, OpenAPI
│   │       │   ├── controller/        ← auth/, student/, admin/, collaborator/
│   │       │   ├── entity/            ← Entities + enums (QuestionGroup, ExamBlueprint, ExamWindow…)
│   │       │   ├── repository/        ← JPA repos + PESSIMISTIC_WRITE locks
│   │       │   ├── service/impl/      ← Session, AudioPlays, Blueprint, Multi-format Import, Admission
│   │       │   └── exception/         ← AppException, GlobalExceptionHandler (409 LockVersion)
│   │       └── resources/db/migration/ ← Flyway SQL migrations (V0..V17)
│   └── vsat_database_schema.sql       ← Base schema reference
│
├── acceptance-harness/                ← Clean-Room Acceptance Testing Harness (126 checks)
├── ops/                               ← Linux Home Server deployment & backup scripts
├── docs/                              ← Tài liệu kiến trúc, runbooks, API error codes
├── docker-compose.prod.yml            ← Production stack (Postgres 17 + Redis 7 + API + Web)
├── CHANGELOG.md
└── README.md                          ← File này
```

---

## Database

- **27 bảng**, **20 custom ENUM types**, indexes, triggers
- Hosted trên **Neon Serverless PostgreSQL** (ap-southeast-1)

### Chạy schema lần đầu

1. Vào [Neon Dashboard](https://console.neon.tech) → SQL Editor
2. Paste toàn bộ nội dung `VSAT/vsat_database_schema.sql`
3. Chạy → tạo toàn bộ schema

---

## Backend — Hướng dẫn chạy

### Yêu cầu
- Java 17+
- Gradle 8.7+

### Cài đặt

```bash
cd VSAT/vsat-compass-api

# Tạo file .env từ template
cp .env.example .env
```

Chỉnh sửa `.env`:

```env
DATABASE_URL=jdbc:postgresql://ep-xxx.ap-southeast-1.aws.neon.tech/neondb?sslmode=require
DATABASE_USERNAME=neondb_owner
DATABASE_PASSWORD=your_password
JWT_SECRET=your-256bit-secret-key
SPRING_PROFILES_ACTIVE=dev
```

### Chạy

```bash
./gradlew bootRun
# Windows:
gradlew.bat bootRun
```

Backend khởi động tại: `http://localhost:8080/api/v1`

### Tài khoản test (tự tạo khi khởi động)

| Email | Mật khẩu | Vai trò |
|-------|----------|---------|
| `student@vsat.com` | `Student@123` | STUDENT |
| `collab@vsat.com` | `Admin@123` | COLLABORATOR |
| `content@vsat.com` | `Admin@123` | CONTENT_ADMIN |
| `admin@vsat.com` | `Admin@123` | SUPER_ADMIN |

### Swagger UI

```
http://localhost:8080/api/v1/swagger-ui.html
```

### API URL

**Production:** https://vsat-compass-api.onrender.com/api/v1/
**Health:** https://vsat-compass-api.onrender.com/api/v1/actuator/health

---

## API Modules (v1.2.0 — 100% Implemented & Verified)

Hệ thống API đã hoàn thiện toàn bộ các phân hệ nghiệp vụ, tích hợp đồng bộ giữa Spring Boot 3.5.9, Web Admin/CBT, và Android Client:

| Module | Base Path | Endpoints Chính | Trạng thái |
|--------|-----------|-----------------|------------|
| Auth | `/auth` | register, login, refresh, logout, getMe, updateProfile, changePassword | ✅ 100% Done |
| Session Engine | `/sessions` | start (`orderedQuestionIds`), client-submit, server-submit, get-question, answer-keys | ✅ 100% Done |
| Session Answers | `/sessions/{id}/answers` | lưu từng câu, khôi phục trạng thái làm bài, xóa đáp án đã lưu | ✅ 100% Done |
| Audio Plays | `/sessions/{id}/audio-plays` | đếm lượt nghe, khóa bi quan `PESSIMISTIC_WRITE`, chặn quá `maxPlays` (409) | ✅ 100% Done |
| Subjects (Public) | `/subjects` | danh mục môn học, chủ đề, tiểu mục | ✅ 100% Done |
| Questions (Collaborator) | `/collaborator/questions` | soạn câu hỏi, danh sách, chi tiết, cập nhật, nộp duyệt | ✅ 100% Done |
| Questions (Admin) | `/admin/questions` | hàng đợi duyệt theo trạng thái, picker lọc ma trận, approve, request revision, reject | ✅ 100% Done |
| Blueprints (Admin) | `/admin/blueprints` | DB-backed ma trận đề thi 3 chiều CRUD (`WEB-3`), cache evict | ✅ 100% Done |
| Schedules / Windows | `/admin/exam-windows` | quản lý ca thi, phòng thi tập trung, đếm thí sinh, mở/đóng ca | ✅ 100% Done |
| Exams (Admin) | `/admin/exams` | metadata CRUD, composition add/remove/reorder, publish workflow, DRAFT discard, lockVersion 409 | ✅ 100% Done |
| Exams (Public) | `/exams` | danh sách đề `PUBLISHED` + `FREE`, chi tiết đề chống leak 404 | ✅ 100% Done |
| Student Stats | `/my-stats` | thống kê môn học (`/topics`), nhận diện chủ đề yếu (`/weak-topics`) | ✅ 100% Done |
| Admissions | `/admissions` | tra cứu điểm chuẩn đại học, dự đoán trúng tuyển (Safe/Fit/Risk) | ✅ 100% Done |
| Tickets | `/tickets` & `/admin/tickets` | báo lỗi câu hỏi từ thí sinh, admin phân loại, xử lý, cập nhật trạng thái | ✅ 100% Done |
| Dashboard | `/admin/stats` | tổng quan KPI số liệu thời gian thực | ✅ 100% Done |
| User Management | `/admin/users` | danh sách người dùng, đổi vai trò, khóa/mở khóa tài khoản | ✅ 100% Done |
| Multi-format Import | `/admin/questions/import` | nạp câu hỏi .xlsx, .json, .docx, .txt có xem trước KaTeX & audio (`WEB-4`) | ✅ 100% Done |
| Question Groups | `/admin/questions` | quản lý chùm câu hỏi đọc hiểu / ngữ liệu chung kèm audio | ✅ 100% Done |

### Deployment & Operations

- **Kế hoạch triển khai & Nghiệm thu:** [`docs/NEXT_PHASE_IMPLEMENTATION_PLAN.md`](docs/NEXT_PHASE_IMPLEMENTATION_PLAN.md) — tiến độ hoàn thành 100% các Phase và bằng chứng nghiệm thu
- **Deploy Runbook:** [`docs/DEPLOY_RUNBOOK.md`](docs/DEPLOY_RUNBOOK.md) — deploy Render/Docker, rollback, secret rotation, incident triage
- **API Error Codes:** [`docs/API_ERROR_CODES.md`](docs/API_ERROR_CODES.md) — catalog mã lỗi, response envelope, rate limits, xử lý 409 Conflict
- **Android Release Runbook:** [`docs/ANDROID_RELEASE.md`](docs/ANDROID_RELEASE.md) — quy trình ký số APK release `android/v0.3.0`
- **Linux Home Server Roadmap:** [`docs/LINUX_HOME_SERVER_ROADMAP.md`](docs/LINUX_HOME_SERVER_ROADMAP.md) — lộ trình tự chủ hạ tầng 8 chặng
- **Pre-Linux Docker Stack:** `docker-compose.prod.yml`, `.env.prod.example`, `ops/scripts/` (backup, health, migrations, smoke)

---

## 🌐 Production Endpoints

| | |
|---|---|
| **Base URL** | `https://vsat-compass-api.onrender.com/api/v1/` |
| **Health** | `https://vsat-compass-api.onrender.com/api/v1/actuator/health` |
| **Region** | Singapore (Render.com, Free tier, Docker runtime) |
| **Database** | Neon PostgreSQL (ap-southeast-1) |
| **Monitoring** | UptimeRobot — health check every 5 minutes |

> **Note:** Render free tier spins down after 15 minutes of inactivity. Cold start takes ~60-90 seconds.
> UptimeRobot keep-alive prevents this during active hours.
> After a fresh deploy, newly added endpoints may need an extra 3-5 minute JVM/ApplicationContext warm-up window even after actuator health already returns 200.

For deploy and rollback procedures, see [`docs/DEPLOY_RUNBOOK.md`](docs/DEPLOY_RUNBOOK.md).
For error codes and response envelope, see [`docs/API_ERROR_CODES.md`](docs/API_ERROR_CODES.md).

---

## Android App — Hướng dẫn chạy

### Yêu cầu
- Android Studio Hedgehog+
- Android SDK 28+
- Emulator hoặc thiết bị thật

### Chạy

1. Mở thư mục `VSAT_COMPASS/` bằng Android Studio
2. Sync Gradle
3. Run app — **không cần backend đang chạy**

> **Chế độ mặc định (`CLIENT_SIDE_EXAM_PROCESSING = true` trong `ApiClient.java`):**
> - Đăng nhập / hồ sơ cá nhân vẫn đi qua API
> - Từ C1.6-B, exam content là backend-first khi `USE_BACKEND_EXAM_CONTENT = true`: danh sách đề, chi tiết đề và nội dung câu hỏi ưu tiên backend production
> - Local `sample_*.json` chỉ dùng làm fallback/offline continuity khi backend content lỗi hoặc khi tắt flag backend content
> - Timer và chấm điểm vẫn chạy trên thiết bị; app không chặn luồng làm bài nếu backend content tạm thời không sẵn sàng
> - Chỉ đồng bộ kết quả cuối lên server khi backend thực sự sẵn sàng (bootstrap session nền, không chặn luồng làm bài)

> **Backend mặc định (mới):**
> - Cả `debug` và `release` đều dùng backend public: `https://vsat-compass-api.onrender.com/api/v1/`
> - App vì vậy hoạt động trên mọi thiết bị Android có Wi-Fi hoặc dữ liệu di động, không phụ thuộc IP LAN của máy dev
>
> `LOCAL_LAN_HOST` vẫn được giữ lại trong `app/build.gradle.kts` chỉ để phục vụ dev local nếu sau này bạn chủ động bật lại `USE_LOCAL_BACKEND`.

Student exam content mode:
- `USE_BACKEND_EXAM_CONTENT = true` makes the Android student flow load exam list/detail/session content from the backend first.
- The app uses `orderedQuestionIds` from `POST /sessions/start` to fetch per-question backend content.
- Local `sample_*.json` packs are retained as offline fallback for list/detail failures and as local mode data when the flag is disabled.
- `CLIENT_SIDE_EXAM_PROCESSING` remains independent and controls client-side scoring/session behavior.

### Backend tối thiểu nên giữ

Để giảm chi phí vận hành nhưng vẫn an toàn với NeonDB, app này chỉ nên bắt buộc backend cho các nhóm API sau:

- `auth/login`, `auth/register`, `auth/refresh`, `auth/logout`, `auth/me`
- Đồng bộ kết quả cuối bài thi nếu bạn muốn lưu lịch sử online
- Metadata đề thi online nếu sau này bạn muốn phân phối thêm đề mới ngoài gói cục bộ

Các phần timer, chọn đáp án, bookmark, chấm điểm, hiển thị kết quả và fallback đề mẫu đã được đẩy về thiết bị.

### API Dependency Map (Tối ưu hiện tại)

- **Bắt buộc backend:**
- Auth: `POST /auth/login`, `POST /auth/register`, `POST /auth/refresh`, `POST /auth/logout`, `GET /auth/me`

- **Tùy chọn backend (không chặn làm bài):**
- Đồng bộ kết quả cuối: `POST /sessions/start` (bootstrap nền), `POST /sessions/{sessionId}/client-submit`
- Nội dung câu hỏi theo phiên: `GET /sessions/{sessionId}/questions/{questionId}`
- Đáp án sau khi nộp bài: `GET /sessions/{sessionId}/answer-keys`
- Metadata đề online: `GET /exams`, `GET /exams/{id}` (app vẫn có fallback local)

- **Không bắt buộc backend để làm bài trong chế độ client-side processing:**
- `POST /sessions/{sessionId}/answers`
- `POST /sessions/{sessionId}/submit`

### Quy ước mở rộng bộ đề local

- Từ C1.6-B trở đi, `sample_*.json` là offline fallback only; khi `USE_BACKEND_EXAM_CONTENT = true`, backend là nguồn nội dung chính cho danh sách đề, chi tiết đề và phiên làm bài.
- C1.6-C soft-retire giữ các pack local vì mạng Việt Nam có thể không ổn định, tổng dung lượng khoảng 50KB, và giá trị UX offline vẫn hữu ích; hard deletion hoãn đến quyết định ship APK.
- Đặt nhiều file đề trong `app/src/main/assets/` theo pattern `sample_*.json`
- App sẽ tự scan và nạp toàn bộ các file này vào danh sách đề local
- Mỗi đề cần có field `explanation` cho từng câu hỏi để màn Review hiển thị lời giải
- Pack đề hiện tại (5 pack):
     - `sample_math_exam.json` — Toán cơ bản (50 câu, 90 phút)
     - `sample_math_exam_2.json` — Toán nâng cao (30 câu, 60 phút)
     - `sample_english_exam.json` — Tiếng Anh cơ bản (8 câu, 20 phút)
     - `sample_english_exam_2.json` — Tiếng Anh nâng cao (30 câu, 45 phút)
     - `sample_physics_exam.json` — Vật lí cơ bản (8 câu, 20 phút)

### Trạng thái sync khi làm bài

- Trên màn hình thi (`ExamSessionActivity`) có nhãn trạng thái:
     - `Sync: local-only` → đang làm bài hoàn toàn local, không có session online
     - `Sync: online enabled` → đã bootstrap được session online, có thể đồng bộ kết quả cuối

### Màn hình đã có

| Màn hình | Mô tả |
|----------|-------|
| Splash | Tự redirect theo trạng thái đăng nhập |
| Login | Đăng nhập bằng email/password |
| Register | Tạo tài khoản mới |
| Home | Dashboard: thống kê cá nhân từ lịch sử thật (số bài, điểm TB, thời gian) |
| Exam List | Danh sách đề thi, tìm kiếm & lọc theo môn |
| Exam Detail | Chi tiết đề thi + thông tin cơ bản |
| Exam Session | Làm bài thi: timer, bookmark, grid câu hỏi |
| Exam Result | Circular score, thống kê theo môn, chủ đề cần cải thiện |
| Exam Review | Xem lời giải chi tiết từng câu: đúng/sai/chưa làm, explanation, grid tổng quan |
| Exam History | Lịch sử bài làm: filter theo môn, stats, nút "Xem lại" từng bài |
| Practice (Topics) | Lộ trình cải thiện theo chủ đề với tiến độ |
| Profile | Thông tin cá nhân + đăng xuất |

### Android Release

Xem chi tiết tại [docs/ANDROID_RELEASE.md](docs/ANDROID_RELEASE.md).

- Phiên bản Android hiện hành: **`android/v0.3.0`** (`versionCode = 3`, `versionName = "0.3.0"`).
- Đã hoàn thiện: Ký số release APK (R8 full shrinking, ProGuard), tích hợp Media3 ExoPlayer phát audio offline mã hóa `.vsa` AES-128-GCM, Kiosk mode `startLockTask()` cho ca thi giám sát, dự đoán trúng tuyển đại học `AdmissionPredictorActivity`, hỗ trợ 4 dạng câu hỏi V-SAT 2026.
- Bản dựng release: `app/build/outputs/apk/release/app-release.apk` (~4.3 MB).

---

## Phân quyền

| Role | Quyền |
|------|-------|
| `STUDENT` | Làm bài thi, xem kết quả, gửi ticket |
| `COLLABORATOR` | Soạn câu hỏi, xét duyệt nội bộ |
| `CONTENT_ADMIN` | Quản lý đề thi, phê duyệt câu hỏi |
| `SUPER_ADMIN` | Toàn quyền, quản lý users |

---

## Kiến trúc xử lý đề thi (Hybrid)

```
[Android App]                         [Backend / Neon DB]
     │
     ├─ Login / Register         →    Auth API (JWT)
     ├─ GET /exams                →    Danh sách đề (quyền truy cập)
     ├─ POST /sessions/start      →    Tạo session, kiểm tra mua đề
     ├─ GET /sessions/{id}/questions/{qId}  →  Câu hỏi (cache vào RAM)
     │
     │  [Trên thiết bị]
     ├─ CountDownTimer            ←    Không cần server
     ├─ selectedAnswers Map       ←    Đáp án lưu RAM
     ├─ Chấm điểm (isCorrect)     ←    Dùng questionCache
     │
     └─ POST /sessions/{id}/client-submit  →  Ghi kết quả {score, correct, total, time}
```

**Fallback offline:** Nếu bất kỳ bước nào thất bại → `LocalExamDataSource` đọc `sample_math_exam.json` trong assets.

---

## Thống kê dự án & Kiểm chuẩn Thực tế

| Hạng mục | Thực tế (v1.2.0 / android/v0.3.0) | Ghi chú |
|----------|-----------------------------------|---------|
| Backend Unit Tests | **371 / 371 passed** (100%) | JUnit 5 + Mockito + Spring Security Test |
| Android Unit Tests | **253 / 253 passed** (100%) | Robolectric + JVM unit tests |
| Clean-Room Acceptance Checks | **126 / 126 passed** (100%) | Bộ harness độc lập `acceptance-harness/` |
| Web Admin / CBT Build | **PASS** (100%) | `tsc -b && vite build` (React 19 + TypeScript 6) |
| Web Admin / CBT Lint | **0 errors / 0 warnings** | Oxlint fast linter |
| Flyway Migrations | **V0 -> V17** (18 migrations) | PostgreSQL 17 baseline & feature migrations |
| Bảng database | 32+ bảng, 20 ENUM types | Tickets, blueprints, exam_windows, admission_benchmarks, sessions... |
| Local Exam Packs | 5 bộ đề chuẩn hóa | Toán cơ bản/nâng cao, Tiếng Anh cơ bản/nâng cao, Vật lí |

---

## Test & QA — Hướng dẫn Kiểm chuẩn

### 1. Backend API Tests (371 tests)

```bash
cd VSAT/vsat-compass-api
./gradlew test --no-daemon
```

### 2. Android Unit Tests (253 tests)

```bash
./gradlew testDebugUnitTest --no-daemon
```

### 3. Web Admin / CBT Build & Lint

```bash
cd web-admin
npm run lint
npm run build
```

### 4. Clean-Room Acceptance Harness (126 checks)

```bash
node acceptance-harness/run-all.mjs
```

### 5. Dev Tools (Android debug build only)

Vào ProfileFragment → **long-press tên hiển thị** → Dev menu:
- Inject 50 lịch sử mock (test stress scroll/stats)
- Xóa toàn bộ lịch sử (reset về empty state)

---

## Liên hệ

Dự án được phát triển phục vụ mục đích học thuật và nghiên cứu.
