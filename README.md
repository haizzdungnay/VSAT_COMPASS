# V-SAT Compass

> Nền tảng mô phỏng thi và ôn luyện V-SAT đa nền tảng — Android Native + Spring Boot REST API

![Android](https://img.shields.io/badge/Android-Java-green?logo=android)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen?logo=springboot)
![PostgreSQL](https://img.shields.io/badge/Database-Neon%20PostgreSQL-blue?logo=postgresql)
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
│   │       ├── controller/           ← auth/ (các module khác đang phát triển)
│   │       ├── dto/                  ← request/, response/, common/
│   │       ├── entity/               ← User, RefreshToken + enums
│   │       ├── repository/           ← Spring Data JPA repos
│   │       ├── service/              ← AuthService + impl
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

---

## API Modules

> **Trạng thái backend hiện tại:** Đang triển khai dần. Module Auth hoàn chỉnh; các module Exam/Session/Question chưa có — Android app tự fallback về dữ liệu cục bộ (`sample_math_exam.json`).

| Module | Base Path | Endpoints | Trạng thái |
|--------|-----------|-----------|------------|
| Auth | `/auth` | register, login, refresh, logout, getMe, updateProfile, changePassword | ✅ Done |
| Questions (Collaborator) | `/collaborator/questions` | CRUD + filter + options | 📋 Thiết kế xong |
| Questions (Admin) | `/admin/questions` | status, version | 📋 Thiết kế xong |
| Review Workflow | `/collaborator/questions/{id}/reviews` | create, list, comment | 📋 Thiết kế xong |
| Exams (Admin) | `/admin/exams` | CRUD + questions + status | 📋 Thiết kế xong |
| Exams (Public) | `/exams` | list published, detail | 📋 Thiết kế xong |
| Session Engine | `/sessions` | start, **client-submit**, answer | 📋 Thiết kế xong |
| Student Stats | `/my-stats` | topic stats, weak topics, history | 📋 Thiết kế xong |
| Tickets (Student) | `/tickets` | create, list, detail, comment | 📋 Thiết kế xong |
| Tickets (Admin) | `/admin/tickets` | list, assign, resolve, status | 📋 Thiết kế xong |
| Dashboard | `/admin/dashboard` | overview counts | 📋 Thiết kế xong |
| User Management | `/admin/users` | list, detail, role, status | 📋 Thiết kế xong |

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
> - Đăng nhập / danh sách đề gọi API thực. Nếu backend chưa có hoặc mất mạng → dùng `sample_math_exam.json`
> - Timer + chấm điểm chạy hoàn toàn trên thiết bị
> - Chỉ POST kết quả cuối `{score, correct, total, timeSpent}` lên server

> **Khi chạy với backend local:** đổi `BASE_URL` trong `ApiClient.java` sang `BASE_URL_LOCAL` và sửa IP thành IP máy tính trong LAN.

### Màn hình đã có

| Màn hình | Mô tả |
|----------|-------|
| Splash | Tự redirect theo trạng thái đăng nhập |
| Login | Đăng nhập bằng email/password |
| Register | Tạo tài khoản mới |
| Home | Dashboard: thống kê cá nhân |
| Exam List | Danh sách đề thi, tìm kiếm & lọc theo môn |
| Exam Detail | Chi tiết đề thi + thông tin cơ bản |
| Exam Session | Làm bài thi: timer, bookmark, grid câu hỏi |
| Exam Result | Circular score, thống kê theo môn, chủ đề cần cải thiện |
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
| Backend Java files (hiện tại) | ~25 (Auth module) |
| Android Java files | 30+ |
| Android XML layouts | 18+ |
| Android Drawable resources | 20+ |
| Local exam data (JSON assets) | 1 (sample_math_exam.json) |
| Features live | Login, Register, Exam list, Exam session, Timer, Scoring, Result |

---

## Liên hệ

Dự án được phát triển phục vụ mục đích học thuật và nghiên cứu.
