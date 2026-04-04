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
│       │   │   ├── ApiClient.java     ← Retrofit singleton + JWT interceptor
│       │   │   ├── AuthApi.java       ← Auth endpoints
│       │   │   └── ExamApi.java       ← Exam + Session endpoints
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
│   │   └── src/main/java/com/vsatcompass/api/
│   │       ├── config/               ← Security, JPA, OpenAPI, DataInitializer
│   │       ├── controller/           ← auth/, admin/, collaborator/, student/
│   │       ├── dto/                  ← request/, response/, common/
│   │       ├── entity/               ← 16 entities + 13 enums
│   │       ├── repository/           ← 16 Spring Data JPA repos
│   │       ├── service/              ← 9 interfaces + 9 implementations
│   │       ├── security/             ← JWT utils, filter, UserDetails
│   │       ├── exception/            ← AppException, GlobalExceptionHandler
│   │       ├── mapper/               ← UserMapper
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

| Module | Base Path | Endpoints | Trạng thái |
|--------|-----------|-----------|------------|
| Auth | `/auth` | register, login, refresh, logout, getMe, updateProfile, changePassword | ✅ Done |
| Questions (Collaborator) | `/collaborator/questions` | CRUD + filter + options | ✅ Done |
| Questions (Admin) | `/admin/questions` | status, version | ✅ Done |
| Review Workflow | `/collaborator/questions/{id}/reviews` | create, list, comment | ✅ Done |
| Exams (Admin) | `/admin/exams` | CRUD + questions + status | ✅ Done |
| Exams (Public) | `/exams` | list published, detail | ✅ Done |
| Session Engine | `/sessions` | start, submit, answer | ✅ Done |
| Student Stats | `/my-stats` | topic stats, weak topics, history | ✅ Done |
| Tickets (Student) | `/tickets` | create, list, detail, comment | ✅ Done |
| Tickets (Admin) | `/admin/tickets` | list, assign, resolve, status | ✅ Done |
| Dashboard | `/admin/dashboard` | overview counts | ✅ Done |
| User Management | `/admin/users` | list, detail, role, status | ✅ Done |

---

## Android App — Hướng dẫn chạy

### Yêu cầu
- Android Studio Hedgehog+
- Android SDK 28+
- Emulator hoặc thiết bị thật

### Chạy

1. Mở thư mục `VSAT_COMPASS/` bằng Android Studio
2. Sync Gradle
3. Đảm bảo backend đang chạy ở `localhost:8080`
4. Run app trên emulator (kết nối qua `10.0.2.2:8080`)

> **Lưu ý:** Nếu test trên thiết bị thật, đổi `BASE_URL` trong `ApiClient.java` thành IP máy tính trong mạng LAN.

### Màn hình đã có

| Màn hình | Mô tả |
|----------|-------|
| Splash | Tự redirect theo trạng thái đăng nhập |
| Login | Đăng nhập bằng email/password |
| Register | Tạo tài khoản mới |
| Home | Dashboard: thống kê cá nhân |
| Exam List | Danh sách đề thi + filter |
| Exam Session | Làm bài thi với timer đếm ngược |
| Exam Result | Xem điểm và kết quả |
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

## Thống kê dự án

| Hạng mục | Số lượng |
|----------|----------|
| Bảng database | 27 |
| ENUM types PostgreSQL | 20 |
| API endpoints | ~52 MVP |
| Backend Java files | 126+ |
| Android Java files | 25 |
| Android XML layouts | 12 |

---

## Liên hệ

Dự án được phát triển phục vụ mục đích học thuật và nghiên cứu.
