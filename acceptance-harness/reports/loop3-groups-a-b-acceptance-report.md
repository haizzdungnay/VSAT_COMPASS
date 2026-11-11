# Báo cáo Nghiệm thu Vòng lặp 3 (Nhóm A & Nhóm B)

## 1. Tổng quan Nghiệm thu

- **A1 — Đồng bộ & xóa câu trả lời phiên thi (Server)**:
  - Cung cấp `GET /api/v1/sessions/{sessionId}/answers` để khôi phục trạng thái làm bài của thí sinh.
  - Khi payload rỗng (`selectedOptionId == null && (payload == null || payload.isEmpty())`), xóa bản ghi `SessionAnswer` và cập nhật lại `answeredCount`.
  - Phản hồi `SessionAnswerResponse` ẩn hoàn toàn `isCorrect` và `rawPointsAwarded` nhằm chống gian lận.
- **A2 — Bộ đếm lượt nghe Audio có khóa bi quan**:
  - Migration `V16__add_session_audio_play_counts.sql` bổ sung cột `audio_play_counts JSONB`.
  - JPA `@Lock(LockModeType.PESSIMISTIC_WRITE)` khóa dòng `ExamSession` tại `findByIdForUpdate`.
  - Cung cấp `GET /sessions/{sessionId}/audio-plays` và `POST /sessions/{sessionId}/audio-plays`. Chặn vượt `maxPlays` bằng HTTP 409 Conflict.
- **A3 — Phân tách JWT Token theo Role STUDENT / ADMIN (Web)**:
  - Tách token lưu trữ độc lập: `vsat_student_access_token` và `vsat_admin_access_token`.
  - Điều hướng và tự động refresh theo role hiện tại trong `localStorage ('vsat_web_role')`.
- **B1 (WEB-3) — Lưu trữ & CRUD Ma trận đề thi trong CSDL**:
  - Migration `V17__add_blueprint_cells_json.sql` thêm cột `cells_json JSONB` và seed dữ liệu chuẩn 7 môn.
  - CRUD API tại `AdminBlueprintController` (GET/POST/PUT/DELETE) kèm `@Caching` evict cache `blueprints` và `blueprintDetail`.
  - Giao diện `Blueprints/index.tsx` tích hợp Modal Tạo mới, Chỉnh sửa và Xóa ma trận.
- **B2 (WEB-4) — Mở rộng Import câu hỏi đa định dạng & Xem trước Audio**:
  - Hỗ trợ 4 định dạng file: `.xlsx`, `.json`, `.docx`, `.txt`.
  - Parser hỗ trợ đầy đủ câu hỏi ghép hợp `MATCHING`.
  - Bảng Preview tại `ImportQuestions/index.tsx` hiển thị thông tin ngữ liệu nghe (`audioUrl`, `maxPlays`).

## 2. Bảng Đối chiếu Bằng chứng File & Line

| Hạng mục | Thành phần | Đường dẫn File trên đĩa | Dòng chính xác |
|---|---|---|---|
| **A1** | REST Endpoint | `VSAT/vsat-compass-api/src/main/java/com/vsatcompass/api/controller/student/SessionController.java` | L101-108 |
| **A1** | Service Interface | `VSAT/vsat-compass-api/src/main/java/com/vsatcompass/api/service/SessionService.java` | L48 |
| **A1** | Logic xóa câu trả lời | `VSAT/vsat-compass-api/src/main/java/com/vsatcompass/api/service/impl/SessionServiceImpl.java` | L291-300 |
| **A1** | Logic giấu điểm/đúng sai | `VSAT/vsat-compass-api/src/main/java/com/vsatcompass/api/service/impl/SessionServiceImpl.java` | L603-621 |
| **A1** | Safe Response DTO | `VSAT/vsat-compass-api/src/main/java/com/vsatcompass/api/dto/response/SessionAnswerResponse.java` | L15-25 |
| **A1** | Restore answers (FE) | `web-admin/src/pages/StudentCBT/StudentCBTWorkspace.tsx` | L268-290 |
| **A2** | Migration SQL | `VSAT/vsat-compass-api/src/main/resources/db/migration/V16__add_session_audio_play_counts.sql` | L1-4 |
| **A2** | Entity Column | `VSAT/vsat-compass-api/src/main/java/com/vsatcompass/api/entity/ExamSession.java` | L104-108 |
| **A2** | Khóa bi quan JPA | `VSAT/vsat-compass-api/src/main/java/com/vsatcompass/api/repository/ExamSessionRepository.java` | L22-25 |
| **A2** | REST Endpoints Audio | `VSAT/vsat-compass-api/src/main/java/com/vsatcompass/api/controller/student/SessionController.java` | L110-127 |
| **A2** | Logic đếm & chặn 409 | `VSAT/vsat-compass-api/src/main/java/com/vsatcompass/api/service/impl/SessionServiceImpl.java` | L635-728 |
| **A2** | UI Audio Player hook | `web-admin/src/pages/StudentCBT/StudentCBTWorkspace.tsx` | L291-320 |
| **A3** | Phân tách Token Key | `web-admin/src/services/api.ts` | L34-36 |
| **A3** | Token Resolver & Store | `web-admin/src/services/api.ts` | L52-74 |
| **A3** | Role Switch Login | `web-admin/src/pages/Login/index.tsx` | L20-43 |
| **B1** | Migration SQL Cells | `VSAT/vsat-compass-api/src/main/resources/db/migration/V17__add_blueprint_cells_json.sql` | L1-21 |
| **B1** | Blueprint Entity | `VSAT/vsat-compass-api/src/main/java/com/vsatcompass/api/entity/ExamBlueprint.java` | L58-61 |
| **B1** | Admin Controller CRUD | `VSAT/vsat-compass-api/src/main/java/com/vsatcompass/api/controller/admin/AdminBlueprintController.java` | L25-68 |
| **B1** | Service Cache Evict | `VSAT/vsat-compass-api/src/main/java/com/vsatcompass/api/service/impl/AdminBlueprintServiceImpl.java` | L117-215 |
| **B1** | Modal Tạo/Sửa/Xóa | `web-admin/src/pages/Blueprints/index.tsx` | L102-161, L458-675 |
| **B2** | Multi-format Validator | `VSAT/vsat-compass-api/src/main/java/com/vsatcompass/api/service/impl/QuestionImportServiceImpl.java` | L165-183 |
| **B2** | MATCHING Type Parse | `VSAT/vsat-compass-api/src/main/java/com/vsatcompass/api/service/impl/QuestionImportServiceImpl.java` | L798 |
| **B2** | UI Import Formats | `web-admin/src/pages/ImportQuestions/index.tsx` | L122, L249-260 |
| **B2** | UI Audio Preview Table | `web-admin/src/pages/ImportQuestions/index.tsx` | L384-394 |

## 3. Kết quả Kiểm thử Thực tế

- **Backend JUnit Test**:
  - Lệnh: `./gradlew test --no-daemon`
  - Kết quả: **371/371 passed**, 0 failures, 0 ignored, 100% success rate, duration 6.069s. Exit code: `0`.
- **Web Admin Production Build**:
  - Lệnh: `npm run build` (`tsc -b && vite build`)
  - Kết quả: Build thành công không phát sinh type error hay bundle error. Exit code: `0`.
- **Clean-Room Acceptance Harness**:
  - Lệnh: `node acceptance-harness/run-all.mjs`
  - Kết quả: **126/126 passed** (0 failures, 0 violations). Verdict: **ACCEPTED**.
  - Chi tiết từng bộ kiểm thử:
    - `backend_endpoints`: 76/76 passed (`API-001` .. `API-076`)
    - `backend_jobs_and_integrations`: 2/2 passed (`JOB-001` .. `JOB-002`)
    - `web_admin_pages`: 10/10 passed (`WEB-001` .. `WEB-010`)
    - `android_screens_and_flows`: 35/35 passed (`AND-001` .. `AND-035`)
    - `android_workers_and_services`: 3/3 passed (`WRK-001` .. `WRK-003`)
    - `phase2-features-check`: passed (P2 Admission Engine, TD-4 Lock 409, P4 Anti-Cheat 3 violations, P1 Student CBT Portal).
