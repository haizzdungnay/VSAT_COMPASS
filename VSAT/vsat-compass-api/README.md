# V-SAT Compass API

Backend API cho nền tảng mô phỏng thi và hỗ trợ ôn tập V-SAT đa nền tảng (Android + Web).

## Tech Stack

- **Java 17** + **Spring Boot 3.2.5**
- **PostgreSQL** (Neon Serverless)
- **Spring Security** + **JWT** (jjwt 0.12.5)
- **Spring Data JPA** + **Hibernate**
- **MapStruct** + **Lombok**
- **Swagger/OpenAPI** (springdoc 2.5.0)
- **Gradle** build tool

## Project Structure

```
src/main/java/com/vsatcompass/api/
├── config/              # Security, CORS, OpenAPI, JPA configs
├── controller/
│   ├── auth/            # Authentication endpoints
│   ├── student/         # Student-facing endpoints
│   ├── admin/           # Admin endpoints (Content Admin, Super Admin)
│   └── collaborator/    # Collaborator workspace endpoints
├── dto/
│   ├── request/         # Request DTOs with validation
│   ├── response/        # Response DTOs
│   └── common/          # ApiResponse wrapper, pagination
├── entity/
│   └── enums/           # JPA entities & enum types
├── exception/           # AppException + GlobalExceptionHandler
├── mapper/              # Entity ↔ DTO mappers
├── repository/          # Spring Data JPA repositories
├── security/
│   ├── jwt/             # JWT utility
│   ├── filter/          # JWT authentication filter
│   └── service/         # UserDetails service
├── service/
│   └── impl/            # Business logic
└── util/                # SecurityUtils, helpers
```

## Getting Started

### 1. Clone & setup environment

```bash
cp .env.example .env
# Edit .env with your Neon PostgreSQL credentials & JWT secret
```

### 2. Setup database

Chạy file `vsat_database_schema.sql` trên Neon PostgreSQL dashboard hoặc psql:

```bash
psql $DATABASE_URL -f vsat_database_schema.sql
```

### 3. Run

```bash
./gradlew bootRun
```

### 4. Test API

- Swagger UI: http://localhost:8080/api/v1/swagger-ui.html
- API Docs: http://localhost:8080/api/v1/api-docs

## API Modules

| Module          | Base Path              | Status |
|-----------------|------------------------|--------|
| Auth            | `/auth`                | ✅ Done |
| Subjects        | `/subjects`            | 🔲 TODO |
| Questions       | `/questions`           | 🔲 TODO |
| Reviews         | `/reviews`             | 🔲 TODO |
| Exams           | `/exams`               | 🔲 TODO |
| Exam Sessions   | `/sessions`            | 🔲 TODO |
| Student Stats   | `/me/stats`            | 🔲 TODO |
| Tickets         | `/tickets`             | 🔲 TODO |
| Dashboard       | `/admin/dashboard`     | 🔲 TODO |
| User Management | `/admin/users`         | 🔲 TODO |
| Import Excel    | `/import`              | 🔲 Phase 2 |
| Notifications   | `/me/notifications`    | 🔲 Phase 2 |
| Commerce        | `/me/wallet`           | 🔲 Defer |

## Environments

| Profile | Database         | Logging |
|---------|------------------|---------|
| `dev`   | Local/Neon (5 pool) | DEBUG |
| `prod`  | Neon (20 pool)      | INFO  |
