# V-SAT COMPASS — CHANGELOG

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
