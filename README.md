# V-SAT Compass

> Nền tảng mô phỏng thi và ôn luyện V-SAT đa nền tảng — Android Native + Spring Boot REST API

![Android](https://img.shields.io/badge/Android-Java-green?logo=android)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.9-brightgreen?logo=springboot)
![PostgreSQL](https://img.shields.io/badge/Database-Neon%20PostgreSQL-blue?logo=postgresql)
![Backend](https://img.shields.io/badge/backend-live-success)
![License](https://img.shields.io/badge/license-MIT-lightgrey)

---

## Tổng quan

**V-SAT Compass** là ứng dụng hỗ trợ học viên ôn thi V-SAT (Vietnam Scholastic Assessment Test), bao gồm:

- **Android App** (Java + XML) — giao diện người dùng, làm bài thi, xem kết quả
- **Backend API** (Spring Boot) — quản lý đề thi, câu hỏi, phiên thi, thống kê

---

## Tech Stack

### Backend
| Công nghệ | Phiên bản |
|-----------|-----------|
| Java | 17 |
| Spring Boot | 3.5.9 |
| PostgreSQL (Neon Serverless) | latest |
| Spring Security + JWT | jjwt 0.12.5 |
| Spring Data JPA + Hibernate | — |
| Lombok + MapStruct | — |
| Swagger / OpenAPI | springdoc 2.5.0 |
| Gradle | 8.7 |

### Android
| Công nghệ | Phiên bản |
|-----------|-----------|
| Java | 11 |
| Android SDK | 36 (min 28) |
| Retrofit 2 | 2.11.0 |
| OkHttp 3 | 4.12.0 |
| Gson | 2.11.0 |
| Glide | 4.16.0 |
| Jetpack Navigation | 2.8.5 |
| Lifecycle (ViewModel + LiveData) | 2.8.7 |
| CircleImageView | 3.1.0 |
| Material Components | 1.13.0 |

---

## Cấu trúc dự án

```
VSAT_COMPASS/                          ← Repository root
├── app/                               ← Android App
│   └── src/main/java/com/example/v_sat_compass/
│       ├── VsatApp.java               ← Application class
│       ├── MainActivity.java          ← Bottom nav + Navigation
│       ├── data/
│       │   ├── api/
│       │   │   ├── ApiClient.java     ← Retrofit singleton + JWT interceptor + feature flag
│       │   │   ├── AuthApi.java       ← Auth endpoints
│       │   │   └── ExamApi.java       ← Exam + Session endpoints (incl. client-submit)
│       │   ├── local/
│       │   │   └── LocalExamDataSource.java  ← Đọc sample_math_exam.json (fallback offline)
│       │   ├── model/                 ← Gson models (ApiResponse, Exam, Question…)
│       │   └── repository/            ← AuthRepository, Resource<T>
│       └── ui/
│           ├── SplashActivity.java    ← Auto-redirect theo login state
│           ├── auth/                  ← LoginActivity, RegisterActivity, AuthViewModel
│           ├── home/                  ← HomeFragment (dashboard student)
│           ├── exam/                  ← ExamFragment, ExamAdapter
│           │   └── session/           ← ExamSessionActivity, ExamResultActivity
│           └── profile/               ← ProfileFragment
│
├── VSAT/
│   ├── vsat-compass-api/              ← Backend Spring Boot (thư mục gốc)
│   │   ├── Dockerfile                 ← Multi-stage build (deploy Render.com)
│   │   ├── render.yaml                ← Render.com deploy config
│   │   └── src/main/java/com/vsatcompass/api/
│   │       ├── config/               ← Security, JPA, OpenAPI, DataInitializer
│   │       ├── controller/           ← auth/, student/ (Session)
│   │       ├── dto/                  ← request/, response/, common/
│   │       ├── entity/               ← User, RefreshToken, ExamSession + enums
│   │       ├── repository/           ← Spring Data JPA repos
│   │       ├── service/              ← AuthService, SessionService + impl
│   │       ├── security/             ← JWT utils, filter, UserDetails
│   │       ├── exception/            ← AppException, GlobalExceptionHandler
│   │       └── util/                 ← SecurityUtils
│   ├── vsat_database_schema.sql      ← 27 bảng + 20 ENUM types
│   └── ui/                           ← Tài liệu thiết kế
│
├── CHANGELOG.md
└── README.md                         ← File này
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

## API Modules

> **Trạng thái backend hiện tại (v0.9.4):** Production-ready trên Render.com — Auth + Session sync đã hardened; Subject/Topic/Subtopic read APIs, Question Bank write/review workflow, public Exam read API, Admin Exam CRUD metadata API, Admin Exam Composition + Publish Workflow, and Admin Exam DRAFT Discard đã live. v0.9.4 release smoke passed all 7 scripts: admin exam composition 17/0/0/0, admin exams 12/12 including DRAFT discard, exams 10/10, subjects 4/4, sessions 5/5, questions 32/32, and auth no-register 7/0/2. Android/admin UI cho content workflow còn deferred.

| Module | Base Path | Endpoints | Trạng thái |
|--------|-----------|-----------|------------|
| Auth | `/auth` | register, login, refresh, logout, getMe, updateProfile, changePassword | ✅ Verified prod |
| Session Engine | `/sessions` | start, **client-submit** | ✅ Verified prod |
| Subjects (Public) | `/subjects` | list subjects, topics, subtopics | ✅ Verified prod |
| Questions (Collaborator) | `/collaborator/questions` | create, list own, detail, update, submit-for-review | ✅ Verified prod |
| Questions (Admin) | `/admin/questions` | queue by status, approve, request revision, reject | ✅ Verified prod |
| Review Workflow | `/admin/questions/{id}/approve`, `/request-revision`, `/reject` | admin review actions + review history records | ✅ Verified prod |
| Exams (Admin) | `/admin/exams` | metadata CRUD, composition add/remove/reorder, publish workflow, DRAFT discard | ✅ Verified prod (C1.2c-1 / v0.9.4) |
| Exams (Public) | `/exams` | list `PUBLISHED` + `FREE`, detail with anti-leak 404 | ✅ Verified prod |
| Student Stats | `/my-stats` | topic stats, weak topics, history | 📋 Phase C |
| Tickets (Student) | `/tickets` | create, list, detail, comment | 📋 Phase C |
| Tickets (Admin) | `/admin/tickets` | list, assign, resolve, status | 📋 Phase C |
| Dashboard | `/admin/dashboard` | overview counts | 📋 Phase C |
| User Management | `/admin/users` | list, detail, role, status | 📋 Phase C |

### Question Bank API status

As of `v0.9.0`, backend Question Bank write workflow is available in production:

- Collaborator can create draft questions, list own questions, view own detail, update editable questions, and submit for review.
- Content admin can list review queue and approve, request revision, or reject submitted questions.
- Service-layer owner checks prevent one collaborator from editing another collaborator's draft.
- Invalid status transitions return `409 INVALID_STATE`.

Deferred:
- question version snapshots
- passage/question groups
- exam composition from approved questions
- publication scheduling
- Excel import
- Android/admin UI integration

### Exam API status

As of `v0.9.4`, the Exam API foundation, admin composition workflow, and DRAFT discard operation are available in production:

- `GET /exams` returns a paged `data.content[]` list of `PUBLISHED` + `FREE` exams.
- `GET /exams/{id}` returns public detail for a `PUBLISHED` + `FREE` exam.
- Non-existent, non-published, or non-free exams return `404 RESOURCE_NOT_FOUND` to avoid enumeration leaks.
- DTO responses expose only public fields; status, price, audit fields, questions, correct options, and explanations are not returned.
- `GET /admin/exams`, `GET /admin/exams/{id}`, `POST /admin/exams`, and `PUT /admin/exams/{id}` are verified for CONTENT_ADMIN/SUPER_ADMIN metadata CRUD.
- Admin exam create/update remains metadata-only: DRAFT/HIDDEN editing, FREE+price=0, server-controlled status/question count/version/audit fields.
- Admin DRAFT discard is verified in production:
  - `DELETE /admin/exams/{examId}` hard-deletes DRAFT exams only
  - non-DRAFT statuses return `409 INVALID_STATE`
  - missing exams return `404 RESOURCE_NOT_FOUND`
  - `CONTENT_ADMIN` and `SUPER_ADMIN` are allowed; anonymous requests return `401`; `STUDENT` returns `403`
- Admin composition endpoints are verified in production:
  - `POST /admin/exams/{examId}/questions`
  - `DELETE /admin/exams/{examId}/questions/{questionId}`
  - `PUT /admin/exams/{examId}/questions/reorder`
- Admin workflow endpoints are verified in production:
  - `POST /admin/exams/{examId}/submit-review`
  - `POST /admin/exams/{examId}/publish` (`SUPER_ADMIN` only)
  - `POST /admin/exams/{examId}/hide`
  - `POST /admin/exams/{examId}/archive`
  - `POST /admin/exams/{examId}/reject-review`
  - `POST /admin/exams/{examId}/return-to-draft`
- `smoke_admin_exams.sh` production PASS for v0.9.4: 12/12, including create-own-DRAFT -> discard -> GET 404.
- `smoke_admin_exam_composition.sh` production PASS for v0.9.4: 17 pass / 0 fail / 0 skip / 0 blocked.

Deferred:
- concurrency control via `@Version` until `Exam.version` semantics are verified
- paid/package pricing
- Android integration with the production Exam API

### Deployment & Operations

- **Task Tracker:** [`task.md`](task.md) — tiến độ Phase A/B/C/D, bug fixes, DB verification
- **Deploy Runbook:** [`docs/DEPLOY_RUNBOOK.md`](docs/DEPLOY_RUNBOOK.md) — deploy, rollback, secret rotation, incident triage
- **API Error Codes:** [`docs/API_ERROR_CODES.md`](docs/API_ERROR_CODES.md) — error catalog, response envelope, rate limits
- **Smoke Tests:** [`docs/SMOKE_CHECKLIST.md`](docs/SMOKE_CHECKLIST.md) — 15 Android manual checks; backend smoke scripts cover the current release smoke set
- **Smoke Scripts:** `docs/scripts/smoke_auth.sh`, `docs/scripts/smoke_sessions.sh`, `docs/scripts/smoke_subjects.sh`, `docs/scripts/smoke_admin_exams.sh`, `docs/scripts/smoke_admin_exam_composition.sh`, `VSAT/vsat-compass-api/docs/scripts/smoke_questions.sh`, `VSAT/vsat-compass-api/docs/scripts/smoke_exams.sh`

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
> - Danh sách đề, chi tiết đề, câu hỏi, timer và chấm điểm chạy ưu tiên trên thiết bị bằng dữ liệu cục bộ `sample_*.json`
> - Không còn phụ thuộc `sessions/start` hay `sessions/{id}/questions/{questionId}` để người dùng bắt đầu làm bài
> - Chỉ đồng bộ kết quả cuối lên server khi backend thực sự sẵn sàng (bootstrap session nền, không chặn luồng làm bài)

> **Backend mặc định (mới):**
> - Cả `debug` và `release` đều dùng backend public: `https://vsat-compass-api.onrender.com/api/v1/`
> - App vì vậy hoạt động trên mọi thiết bị Android có Wi-Fi hoặc dữ liệu di động, không phụ thuộc IP LAN của máy dev
>
> `LOCAL_LAN_HOST` vẫn được giữ lại trong `app/build.gradle.kts` chỉ để phục vụ dev local nếu sau này bạn chủ động bật lại `USE_LOCAL_BACKEND`.

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
- Metadata đề online: `GET /exams`, `GET /exams/{id}` (app vẫn có fallback local)

- **Không bắt buộc backend để làm bài trong chế độ client-side processing:**
- `GET /sessions/{sessionId}/questions/{questionId}`
- `POST /sessions/{sessionId}/answers`
- `POST /sessions/{sessionId}/submit`

### Quy ước mở rộng bộ đề local

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

## Thống kê dự án

| Hạng mục | Số lượng |
|----------|----------|
| Bảng database | 27 |
| ENUM types PostgreSQL | 20 |
| API endpoints (thiết kế) | ~52 MVP |
| Backend Java files (hiện tại) | 50+ (Auth + Session + Content foundations + infrastructure) |
| Android Java files | 35+ |
| Android XML layouts | 20+ |
| Android Drawable resources | 20+ |
| Local exam data (JSON assets) | 5 packs (2 Toán, 2 Tiếng Anh, 1 Vật lí) |
| Unit tests pass | 6/6 (ExamHistoryRepository) |
| Smoke test cases | 102 pass checks in v0.9.4 closeout scope (15 Android manual + 87 backend smoke pass checks; auth no-register also reports 2 expected skips) |
| Features live | Login, Register, Exam list, Session, Timer, Scoring, Result, Review, History, Rate limiting, Subject/Topic/Subtopic read APIs, Question CRUD workflow, Exam read-only public API, Admin Exam composition/workflow, Admin Exam DRAFT discard |

---

## Test & QA

### Unit Tests

```bash
./gradlew testDebugUnitTest
```

Các test case được viết cho `ExamHistoryRepository` (file I/O, cap 200 entries, corrupt recovery, stats):

| Test case | Mô tả |
|-----------|-------|
| `saveEntry_thenGetAll_returnsNewestFirst` | Thứ tự mới nhất trước |
| `saveEntry_exceedsMax_capsAt200` | Cap đúng 200 entries |
| `getByExamId_returnsOnlyMatchingSubject` | Filter môn học |
| `getStats_calculatesAverageCorrectly` | Tính điểm trung bình |
| `getAll_corruptFile_returnsEmptyAndCreatesCorruptBackup` | File bị corrupt → recover |

### Smoke Checklist

Xem [docs/SMOKE_CHECKLIST.md](docs/SMOKE_CHECKLIST.md) — 15 test case thủ công bao phủ toàn bộ student flow.

### Dev Tools (debug build only)

Vào ProfileFragment → **long-press tên hiển thị** → Dev menu:
- Inject 50 lịch sử mock (test stress scroll/stats)
- Xóa toàn bộ lịch sử (reset về empty state)

---

## Liên hệ

Dự án được phát triển phục vụ mục đích học thuật và nghiên cứu.
