# V-SAT Compass

> Nền tảng mô phỏng thi và ôn luyện V-SAT đa nền tảng — Android Native + Spring Boot REST API

![Android](https://img.shields.io/badge/Android-Java-green?logo=android)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen?logo=springboot)
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
| Spring Boot | 3.2.5 |
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

> **Trạng thái backend hiện tại (v0.8.0+):** Production-ready trên Render.com — Auth + Session sync đã hardened (rate limiting Bucket4j, HSTS, JWT cleanup job, error codes chuẩn hoá). Smoke 25 TCs pass. Các module quản trị nội dung (C/D scope) — Android app tự fallback về dữ liệu cục bộ khi chưa có.

| Module | Base Path | Endpoints | Trạng thái |
|--------|-----------|-----------|------------|
| Auth | `/auth` | register, login, refresh, logout, getMe, updateProfile, changePassword | ✅ Verified prod |
| Session Engine | `/sessions` | start, **client-submit** | ✅ Verified prod |
| Questions (Collaborator) | `/collaborator/questions` | CRUD + filter + options | 📋 Phase C |
| Questions (Admin) | `/admin/questions` | status, version | 📋 Phase C |
| Review Workflow | `/collaborator/questions/{id}/reviews` | create, list, comment | 📋 Phase C |
| Exams (Admin) | `/admin/exams` | CRUD + questions + status | 📋 Phase C |
| Exams (Public) | `/exams` | list published, detail | 📋 Phase C |
| Student Stats | `/my-stats` | topic stats, weak topics, history | 📋 Phase C |
| Tickets (Student) | `/tickets` | create, list, detail, comment | 📋 Phase C |
| Tickets (Admin) | `/admin/tickets` | list, assign, resolve, status | 📋 Phase C |
| Dashboard | `/admin/dashboard` | overview counts | 📋 Phase C |
| User Management | `/admin/users` | list, detail, role, status | 📋 Phase C |

### Deployment & Operations

- **Task Tracker:** [`TASKS.md`](TASKS.md) — tiến độ Phase A/B/C/D, bug fixes, DB verification
- **Deploy Runbook:** [`docs/DEPLOY_RUNBOOK.md`](docs/DEPLOY_RUNBOOK.md) — deploy, rollback, secret rotation, incident triage
- **API Error Codes:** [`docs/API_ERROR_CODES.md`](docs/API_ERROR_CODES.md) — error catalog, response envelope, rate limits
- **Smoke Tests:** [`docs/SMOKE_CHECKLIST.md`](docs/SMOKE_CHECKLIST.md) — 25 TCs (15 Android + 10 Backend)
- **Smoke Scripts:** `docs/scripts/smoke_auth.sh`, `docs/scripts/smoke_sessions.sh`

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
| Backend Java files (hiện tại) | 40+ (Auth + Session + infrastructure) |
| Android Java files | 35+ |
| Android XML layouts | 20+ |
| Android Drawable resources | 20+ |
| Local exam data (JSON assets) | 5 packs (2 Toán, 2 Tiếng Anh, 1 Vật lí) |
| Unit tests pass | 6/6 (ExamHistoryRepository) |
| Smoke test cases | 25 TCs (15 Android + 10 Backend) |
| Features live | Login, Register, Exam list, Session, Timer, Scoring, Result, Review, History, Rate limiting |

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
