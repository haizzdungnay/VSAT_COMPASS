# V-SAT Compass API

Backend API cho nền tảng mô phỏng thi và hỗ trợ ôn tập V-SAT đa nền tảng (Android + Web).

Phiên bản: **v1.2.0** | Spring Boot: **3.5.9** | Java: **17** | PostgreSQL: **17**

---

## Tech Stack

- **Java 17** + **Spring Boot 3.5.9**
- **PostgreSQL 17** (Neon Serverless / Docker Production Stack)
- **Flyway Migrations** (`V0`..`V17`)
- **Spring Security** + **JWT** (jjwt 0.12.5) với phân tách token theo role
- **Spring Data JPA** + **Hibernate** + **Pessimistic Locking** (`PESSIMISTIC_WRITE`)
- **Redis Cache** (Spring Data Redis, TTL 60s - 1h, LRU eviction)
- **Apache POI 5.3.0** (.xlsx parser) + DOCX/JSON/TXT multi-format parser
- **MapStruct** + **Lombok**
- **Swagger / OpenAPI** (springdoc 2.5.0)
- **Gradle 8.7** build tool

---

## Project Structure

```
src/main/java/com/vsatcompass/api/
├── config/              # Security, Cache, Redis, CORS, OpenAPI, JPA configs
├── controller/
│   ├── auth/            # Authentication endpoints (Admin & Student)
│   ├── student/         # Student-facing endpoints (Session, CBT, Answers, Audio, Admissions)
│   ├── admin/           # Admin endpoints (Blueprints, Exams, Schedules, Questions, Users, Tickets)
│   └── collaborator/    # Collaborator workspace endpoints
├── dto/
│   ├── request/         # Request DTOs with validation
│   ├── response/        # Response DTOs
│   └── common/          # ApiResponse wrapper, pagination
├── entity/
│   └── enums/           # JPA entities & enum types
├── exception/           # AppException + GlobalExceptionHandler (409 Conflict / LockVersion)
├── mapper/              # Entity <-> DTO mappers
├── repository/          # Spring Data JPA repositories with pessimistic lock queries
├── security/
│   ├── jwt/             # JWT utility
│   ├── filter/          # JWT authentication filter + RateLimitFilter
│   └── service/         # UserDetails service
├── service/
│   └── impl/            # Business logic
└── util/                # SecurityUtils, helpers
```

---

## Getting Started

### 1. Clone & setup environment

```bash
cp .env.example .env
# Chỉnh sửa .env với thông tin kết nối DB và JWT secret
```

### 2. Database Migrations

Cơ sở dữ liệu được tự động đồng bộ qua Flyway khi khởi động ứng dụng:
- Thư mục migrations: `src/main/resources/db/migration/`
- Phiên bản: `V0__base_schema.sql` đến `V17__add_blueprint_cells_json.sql`

```bash
# Chạy thủ công qua psql nếu cần
psql $DATABASE_URL -f src/main/resources/db/migration/V0__base_schema.sql
```

### 3. Run

```bash
./gradlew bootRun
```

### 4. Test API

- Swagger UI: `http://localhost:8080/api/v1/swagger-ui.html`
- Health check: `http://localhost:8080/api/v1/actuator/health`

---

## API Modules Status (100% Done)

| Module | Base Path | Tính năng chính | Trạng thái |
|--------|-----------|-----------------|------------|
| Auth | `/auth` | Register, login, refresh, logout, getMe, role-scoped token storage | ✅ 100% Done |
| Subjects | `/subjects` | Public taxonomy: danh mục môn học, chủ đề, tiểu mục | ✅ 100% Done |
| Questions | `/collaborator/questions` | CRUD câu hỏi CTV, nộp duyệt, upload audio | ✅ 100% Done |
| Question Review | `/admin/questions` | Hàng đợi duyệt, approve, request-revision, reject, picker | ✅ 100% Done |
| Exams | `/exams` & `/admin/exams` | Soạn đề, composition, reorder, publish, lockVersion 409 | ✅ 100% Done |
| Blueprints | `/admin/blueprints` | DB-backed ma trận 3 chiều, CRUD (`WEB-3`), cache evict | ✅ 100% Done |
| Schedules | `/admin/exam-windows` | Ca thi, phòng thi, đếm lượt thi, mở/đóng ca | ✅ 100% Done |
| Exam Sessions | `/sessions` | Start, client/server submit, get questions, answers, answer-keys | ✅ 100% Done |
| Session Answers | `/sessions/{id}/answers` | Lưu từng câu, trả lời hàng loạt, khôi phục trạng thái, xóa câu trả lời | ✅ 100% Done |
| Audio Plays | `/sessions/{id}/audio-plays` | Đếm lượt nghe, khóa bi quan PESSIMISTIC_WRITE, chặn vượt maxPlays (409) | ✅ 100% Done |
| Student Stats | `/my-stats` | Thống kê môn học (`/topics`), chủ đề yếu (`/weak-topics`) | ✅ 100% Done |
| Admissions | `/admissions` | Tra cứu điểm chuẩn, dự đoán trúng tuyển (Safe/Fit/Risk) | ✅ 100% Done |
| Tickets | `/tickets` & `/admin/tickets` | Báo lỗi câu hỏi, tiếp nhận, cập nhật trạng thái | ✅ 100% Done |
| Dashboard | `/admin/stats` | KPI tổng thể hệ thống | ✅ 100% Done |
| User Management | `/admin/users` | Quản lý người dùng, phân quyền, khóa/mở khóa | ✅ 100% Done |
| Multi-format Import | `/admin/questions/import` | Nạp câu hỏi .xlsx, .json, .docx, .txt có xem trước KaTeX & audio (`WEB-4`) | ✅ 100% Done |

---

## Environments

| Profile | Database | Cache | Logging |
|---------|----------|-------|---------|
| `dev` | Local/Neon PostgreSQL | Simple (In-memory) | DEBUG |
| `prod` | PostgreSQL 17 Container / Neon | Redis 7 LRU | WARN / INFO |
