# KẾ HOẠCH THỬ NGHIỆM, TEST ĐỘC LẬP & NGHIỆM THU TỰ ĐỘNG TOÀN DIỆN (100% ZERO-HUMAN MACHINE QA)
**Hệ thống**: V-SAT Compass (Toán, Tiếng Anh, Vật lí, Hóa học, Sinh học, Lịch sử, Địa lí - Chuẩn GDPT 2018 & V-SAT 2026)  
**Tài liệu tham chiếu nguồn**: `docs/testing/feature-inventory.json` (126 IDs toàn hệ thống)  
**Phương pháp nghiệm thu**: Tự động hóa 100% bằng máy thực thi (Clean-Room Autonomous Harness). Tuyệt đối không can thiệp bằng mắt, không thao tác bởi con người.

---

## 1. NGUYÊN TẮC CỐT LÕI & CÔ LẬP PHÒNG SẠCH (CLEAN-ROOM INDEPENDENCE)

1. **Nghiệm thu 100% bằng máy (Zero-Human Machine Verdict)**:
   - Toàn bộ 126 hạng mục tính năng được kiểm thử và nghiệm thu hoàn toàn tự động thông qua mã kịch bản.
   - Kết quả phân định bằng mã thoát (`exit 0` khi đạt, `exit 1` khi thất bại) kèm báo cáo cấu trúc máy đọc (`results/backend-report.json`, `results/web-report.json`, `results/android-report.xml`).
   - Mọi tiêu chuẩn đánh giá dựa trên Assertions đối chiếu cơ sở dữ liệu, JSON schema, và trạng thái giao diện headless. Tuyệt đối không có bất kỳ bước kiểm tra bằng mắt nào của con người.

2. **Cách ly phòng sạch (Clean-Room Architecture)**:
   - Toàn bộ hạ tầng kiểm thử độc lập được đặt tập trung trong thư mục mới: `acceptance-harness/`.
   - Tuyệt đối không tái sử dụng, không import, không gọi lại các tệp test hoặc kịch bản cũ của dự án (`src/test`, `app/src/test`, `app/src/androidTest`, `scripts/*.mjs`, `docs/scripts/smoke_*.sh`, `docs/seed/`, `docs/SMOKE_CHECKLIST.md`).
   - Mọi bộ sinh dữ liệu mẫu (fixtures), mã kiểm thử API, mã kiểm thử E2E web, và mã kiểm thử Android instrumented đều được viết mới hoàn toàn độc lập trong `acceptance-harness/`.

3. **Cơ chế ngăn ngừa Flaky Test**:
   - Tối đa 1 lần chạy lại tự động (retry) cho từng kịch bản đơn lẻ nếu gặp độ trễ mạng ảo nội bộ, có ghi nhật ký cảnh báo chi tiết.
   - Không sử dụng sleep cứng; dùng cơ chế polling điều kiện có timeout tất định (deterministic polling).

---

## 2. KIẾN TRÚC HẠ TẦNG CÔ LẬP & CHỐT CHẶN AN TOÀN (SAFETY PRE-FLIGHT GATE)

### 2.1 Chốt chặn an toàn môi trường (Safety Pre-flight Gate)
- Kịch bản cổng an toàn: `acceptance-harness/safety-preflight.mjs`.
- Bắt buộc kích hoạt đầu tiên trước khi khởi động bất kỳ tiến trình kiểm thử nào.
- Kiểm tra toàn bộ biến môi trường hệ thống:
  - `DATABASE_URL`
  - `SPRING_DATASOURCE_URL`
  - `API_BASE_URL`
  - `VITE_API_BASE_URL`
  - `LLM_BASE_URL`
- **Quy tắc dừng khẩn cấp**: Nếu phát hiện chuỗi kết nối chứa bất kỳ địa chỉ nào khác `localhost` hoặc `127.0.0.1` (đặc biệt các tên miền đám mây như `onrender.com`, Neon, Supabase, AWS), kịch bản lập tức hủy chạy với `exit 1` và in thông báo vi phạm an toàn.
- Tuyệt đối không chứa hoặc thực thi lệnh `git push` trong toàn bộ quy trình.

### 2.2 Cơ sở dữ liệu dùng một lần (Disposable PostgreSQL 16)
- Khởi chạy container PostgreSQL 16 riêng biệt trên cổng cô lập `55432` (`vsat-clean-pg`):
  ```bash
  docker run -d --name vsat-clean-pg -p 55432:5432 -e POSTGRES_DB=vsat_compass_acceptance -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres postgres:16-alpine
  ```
- Nạp schema nền tảng từ `VSAT/vsat_database_schema.sql`.
- Thực thi lần lượt 11 tệp migration tăng dần (`0001` đến `0011`):
  - `MIG-001`: `0001_create_tickets.sql`
  - `MIG-002`: `0002_add_leaderboard_opt_in.sql`
  - `MIG-003`: `0003_add_vsat_domain_types.sql`
  - `MIG-004`: `0004_extend_questions_for_vsat.sql`
  - `MIG-005`: `0005_create_vsat_question_tables.sql`
  - `MIG-006`: `0006_create_vsat_scoring_and_sections.sql`
  - `MIG-007`: `0007_create_exam_blueprints_and_variants.sql`
  - `MIG-008`: `0008_backfill_legacy_data.sql`
  - `MIG-009`: `0009_add_vsat_constraints.sql`
  - `MIG-010`: `0010_seed_vsat_blueprints.sql`
  - `MIG-011`: `0011_add_exam_window_title.sql`
- Cấu hình Spring Boot kiểm tra tính tương thích: `spring.jpa.hibernate.ddl-auto=validate`.
- Nạp bộ Clean-Room Fixtures chuẩn hóa cho 4 vai trò người dùng:
  - `STUDENT`: `student_harness@vsat.local` / `Pass@123456`
  - `COLLABORATOR`: `colab_harness@vsat.local` / `Pass@123456`
  - `CONTENT_ADMIN`: `content_admin_harness@vsat.local` / `Pass@123456`
  - `SUPER_ADMIN`: `super_admin_harness@vsat.local` / `Pass@123456`

### 2.3 Máy chủ giả lập dịch vụ ngoài (Local Mock/Stub Services)
- Kịch bản stub: `acceptance-harness/stubs/llm-mock-server.mjs` chạy trên cổng cục bộ `18081`.
- Cung cấp giao thức tương thích OpenAI `/v1/chat/completions` phục vụ kiểm thử `JOB-002` và `API-060`.
- Hỗ trợ phản hồi tất định 200 OK với cấu trúc JSON giải thích sư phạm chuẩn, cùng chế độ giả lập phản hồi lỗi 500 và timeout để kiểm tra khả năng phục hồi của hệ thống.

### 2.4 Khai báo công cụ thực thi cục bộ (Local Toolchain Specification)
- **Hệ điều hành**: Windows 11 Home Single Language 10.0.26300
- **Shell**: Git Bash (POSIX sh)
- **Docker CLI**: `C:\Program Files\Docker\Docker\resources\bin\docker.exe` (Docker version 28.5.2)
- **Node.js**: `C:\Program Files\nodejs\node.exe` (v24.16.0)
- **NPM / NPX**: `C:\Program Files\nodejs\npm.cmd` (11.2.0), `C:\Program Files\nodejs\npx.cmd`
- **JDK (JBR Android Studio)**: `C:\Program Files\Android\Android Studio\jbr` (JetBrains Runtime 17/21)
- **Android SDK Root**: `C:\Users\PC\AppData\Local\Android\Sdk`
- **ADB**: `C:\Users\PC\AppData\Local\Android\Sdk\platform-tools\adb.exe`
- **Android Emulator**: `C:\Users\PC\AppData\Local\Android\Sdk\emulator\emulator.exe`
- **AVD Target**: `Pixel_10_Pro`
- **Lệnh chạy Emulator không giao diện**:
  ```bash
  emulator -avd Pixel_10_Pro -no-window -no-audio -no-boot-anim -gpu swiftshader_indirect
  ```
- **Ánh xạ cổng mạng vào Emulator**:
  ```bash
  adb reverse tcp:18080 tcp:18080
  ```

---

## 3. MA TRẬN NGHIỆM THU TỰ ĐỘNG 100% TÍNH NĂNG (FULL FEATURE ACCEPTANCE MATRIX)
Danh mục kiểm kê toàn diện gồm chính xác 126 mã định danh không gộp, phủ kín 100% kiến trúc phần mềm.

### 3.1 Backend Endpoints (76 Mã Định Danh: API-001 đến API-076)

| ID | Method & Path | Phân quyền RBAC (2xx vs 401/403) | Kịch bản biên & Âm tính | Tự động xác minh (DB / JSON Oracle) |
|---|---|---|---|---|
| `API-001` | GET `/api/v1/admin/blueprints` | CONTENT_ADMIN, SUPER_ADMIN: 200; STUDENT, COLLABORATOR: 403; Chưa login: 401 | Gọi khi DB rỗng trả danh sách rỗng status 200 | JSON chứa mảng blueprint GDPT 2018 (Toán, Anh, Lí). Kiểm tra trường code, totalQuestions=40. |
| `API-002` | GET `/api/v1/admin/blueprints/{id}` | CONTENT_ADMIN, SUPER_ADMIN: 200; STUDENT, COLLABORATOR: 403; Chưa login: 401 | ID không tồn tại `999999` trả 404 NOT_FOUND | JSON trả về đối tượng BlueprintResponse, kiểm tra danh sách cells có đủ định dạng MCQ, TRUE_FALSE. |
| `API-003` | GET `/api/v1/admin/stats` | CONTENT_ADMIN, SUPER_ADMIN: 200; STUDENT, COLLABORATOR: 403; Chưa login: 401 | Hệ thống mới nạp dữ liệu rỗng trả các bộ đếm số bằng 0 | Đối chiếu trực tiếp số đếm totalUsers, totalExams với lệnh SQL `SELECT COUNT(*) FROM users;` trong DB. |
| `API-004` | GET `/api/v1/admin/exams` | CONTENT_ADMIN, SUPER_ADMIN: 200; STUDENT, COLLABORATOR: 403; Chưa login: 401 | Lọc theo status không hợp lệ hoặc keyword rác trả mảng trống trang đầu | JSON trả về Page<AdminExamSummaryResponse> có phân trang, kiểm tra totalElements và size. |
| `API-005` | GET `/api/v1/admin/exams/{id}` | CONTENT_ADMIN, SUPER_ADMIN: 200; STUDENT, COLLABORATOR: 403; Chưa login: 401 | ID đề thi âm hoặc `999999` trả 404 NOT_FOUND | Khớp trường title, subjectCode, status trong DB table `exams`. |
| `API-006` | POST `/api/v1/admin/exams` | CONTENT_ADMIN, SUPER_ADMIN: 201/200; STUDENT, COLLABORATOR: 403; Chưa login: 401 | Gửi payload thiếu title hoặc duration <= 0 trả 400 BAD_REQUEST | Kiểm tra bản ghi mới được chèn trong bảng `exams` với status='DRAFT'. |
| `API-007` | PUT `/api/v1/admin/exams/{id}` | CONTENT_ADMIN, SUPER_ADMIN: 200; STUDENT, COLLABORATOR: 403; Chưa login: 401 | Sửa đề thi đã ở trạng thái PUBLISHED trả 409 CONFLICT | Kiểm tra trường title và duration cập nhật chính xác trong bảng `exams`. |
| `API-008` | DELETE `/api/v1/admin/exams/{examId}` | CONTENT_ADMIN, SUPER_ADMIN: 200; STUDENT, COLLABORATOR: 403; Chưa login: 401 | Xóa đề thi đã PUBLISHED hoặc có phiên thi đang chạy trả 409 CONFLICT | Truy vấn SQL xác nhận `SELECT COUNT(*) FROM exams WHERE id=:id` bằng 0. |
| `API-009` | POST `/api/v1/admin/exams/{examId}/questions` | CONTENT_ADMIN, SUPER_ADMIN: 200; STUDENT, COLLABORATOR: 403; Chưa login: 401 | Thêm câu hỏi chưa được duyệt (status='DRAFT') trả 400 BAD_REQUEST | Kiểm tra bản ghi liên kết mới trong bảng `exam_questions`. |
| `API-010` | DELETE `/api/v1/admin/exams/{examId}/questions/{questionId}` | CONTENT_ADMIN, SUPER_ADMIN: 200; STUDENT, COLLABORATOR: 403; Chưa login: 401 | Xóa câu hỏi không liên kết với đề thi trả 404 NOT_FOUND | Kiểm tra bảng `exam_questions` không còn cặp examId và questionId này. |
| `API-011` | PUT `/api/v1/admin/exams/{examId}/questions/reorder` | CONTENT_ADMIN, SUPER_ADMIN: 200; STUDENT, COLLABORATOR: 403; Chưa login: 401 | Danh sách thứ tự thiếu câu hỏi hoặc trùng lặp số thứ tự trả 400 | Truy vấn trường order_index trong `exam_questions` khớp với mảng gửi lên. |
| `API-012` | POST `/api/v1/admin/exams/{examId}/submit-review` | CONTENT_ADMIN, SUPER_ADMIN: 200; STUDENT, COLLABORATOR: 403; Chưa login: 401 | Gửi duyệt khi đề thi chưa có đủ số lượng câu hỏi theo ma trận trả 400 | Trạng thái chuyển thành PENDING_REVIEW trong bảng `exams`. |
| `API-013` | POST `/api/v1/admin/exams/{examId}/publish` | CONTENT_ADMIN, SUPER_ADMIN: 200; STUDENT, COLLABORATOR: 403; Chưa login: 401 | Xuất bản đề chưa qua PENDING_REVIEW trả 400 BAD_REQUEST | Trạng thái chuyển thành PUBLISHED, trường published_at có giá trị khác null trong DB. |
| `API-014` | POST `/api/v1/admin/exams/{examId}/hide` | CONTENT_ADMIN, SUPER_ADMIN: 200; STUDENT, COLLABORATOR: 403; Chưa login: 401 | Ẩn đề thi đang ở trạng thái DRAFT trả 400 BAD_REQUEST | Cột status trong bảng `exams` đổi thành HIDDEN. |
| `API-015` | POST `/api/v1/admin/exams/{examId}/archive` | CONTENT_ADMIN, SUPER_ADMIN: 200; STUDENT, COLLABORATOR: 403; Chưa login: 401 | Lưu trữ đề thi không tồn tại trả 404 | Cột status trong bảng `exams` chuyển sang ARCHIVED. |
| `API-016` | POST `/api/v1/admin/exams/{examId}/reject-review` | CONTENT_ADMIN, SUPER_ADMIN: 200; STUDENT, COLLABORATOR: 403; Chưa login: 401 | Từ chối đề không ở trạng thái PENDING_REVIEW trả 400 | Trạng thái chuyển về REJECTED kèm ghi chú lý do trong DB. |
| `API-017` | POST `/api/v1/admin/exams/{examId}/return-to-draft` | CONTENT_ADMIN, SUPER_ADMIN: 200; STUDENT, COLLABORATOR: 403; Chưa login: 401 | Chuyển về nháp khi đề đang PUBLISHED trả 409 | Cột status trong bảng `exams` đổi thành DRAFT. |
| `API-018` | GET `/api/v1/admin/exams/{examId}/questions` | CONTENT_ADMIN, SUPER_ADMIN: 200; STUDENT, COLLABORATOR: 403; Chưa login: 401 | Exam ID không tồn tại trả 404 | JSON trả về đủ danh sách câu hỏi kèm đáp án đúng `isCorrect=true`. |
| `API-019` | GET `/api/v1/admin/exams/{examId}/variants` | CONTENT_ADMIN, SUPER_ADMIN: 200; STUDENT, COLLABORATOR: 403; Chưa login: 401 | Đề thi chưa tạo mã hoán vị nào trả danh sách rỗng 200 | Đối chiếu số bản ghi trong bảng `exam_variants` theo exam_id. |
| `API-020` | POST `/api/v1/admin/exams/{examId}/variants/generate` | CONTENT_ADMIN, SUPER_ADMIN: 200; STUDENT, COLLABORATOR: 403; Chưa login: 401 | Yêu cầu số biến thể variantCount <= 0 hoặc > 20 trả 400 | Sinh đúng N mã đề hoán vị; kiểm tra bảng `exam_variants` và `exam_variant_questions`. |
| `API-021` | POST `/api/v1/admin/exams/generate-by-matrix` | CONTENT_ADMIN, SUPER_ADMIN: 200; STUDENT, COLLABORATOR: 403; Chưa login: 401 | Ngân hàng không đủ câu hỏi thỏa mãn ma trận trả 409 | Tạo đề thi mới và tự động gán đủ 40 câu hỏi khớp các cell ma trận. |
| `API-022` | POST `/api/v1/admin/exams/{examId}/generate-by-matrix` | CONTENT_ADMIN, SUPER_ADMIN: 200; STUDENT, COLLABORATOR: 403; Chưa login: 401 | Điền vào đề thi đã PUBLISHED trả 409 CONFLICT | Đề thi nháp được bổ sung các câu hỏi từ ngân hàng chuẩn hóa. |
| `API-023` | GET `/api/v1/admin/exam-windows` | CONTENT_ADMIN, SUPER_ADMIN: 200; STUDENT, COLLABORATOR: 403; Chưa login: 401 | Lọc status không hợp lệ trả danh sách rỗng 200 | JSON chứa danh sách ca thi; khớp dữ liệu bảng `exam_windows`. |
| `API-024` | GET `/api/v1/admin/exam-windows/{id}` | CONTENT_ADMIN, SUPER_ADMIN: 200; STUDENT, COLLABORATOR: 403; Chưa login: 401 | ID không tồn tại trả 404 | Trả về chi tiết ca thi có startTime, endTime, targetExamId. |
| `API-025` | POST `/api/v1/admin/exam-windows` | CONTENT_ADMIN, SUPER_ADMIN: 201/200; STUDENT, COLLABORATOR: 403; Chưa login: 401 | startTime lớn hơn endTime trả 400 BAD_REQUEST | Bản ghi mới xuất hiện trong bảng `exam_windows` với status='UPCOMING'. |
| `API-026` | PUT `/api/v1/admin/exam-windows/{id}` | CONTENT_ADMIN, SUPER_ADMIN: 200; STUDENT, COLLABORATOR: 403; Chưa login: 401 | Sửa ca thi đã ở trạng thái CLOSED trả 409 | Cập nhật trường title, startTime, endTime thành công trong DB. |
| `API-027` | PATCH `/api/v1/admin/exam-windows/{id}/status` | CONTENT_ADMIN, SUPER_ADMIN: 200; STUDENT, COLLABORATOR: 403; Chưa login: 401 | Chuyển đổi trạng thái trái luồng logic trả 400 | Cột status trong bảng `exam_windows` cập nhật đúng giá trị mới (RUNNING/CLOSED). |
| `API-028` | DELETE `/api/v1/admin/exam-windows/{id}` | CONTENT_ADMIN, SUPER_ADMIN: 200; STUDENT, COLLABORATOR: 403; Chưa login: 401 | Xóa ca thi đang có thí sinh làm bài trả 409 CONFLICT | Truy vấn `SELECT COUNT(*) FROM exam_windows WHERE id=:id` bằng 0. |
| `API-029` | GET `/api/v1/admin/questions` | CONTENT_ADMIN, SUPER_ADMIN: 200; STUDENT, COLLABORATOR: 403; Chưa login: 401 | Lọc kết hợp định dạng câu hỏi và môn học không có kết quả trả mảng trống | JSON Page<QuestionListItemResponse> phân trang đầy đủ thông tin format, status. |
| `API-030` | GET `/api/v1/admin/questions/picker` | CONTENT_ADMIN, SUPER_ADMIN: 200; STUDENT, COLLABORATOR: 403; Chưa login: 401 | Tìm kiếm chuỗi ký tự đặc biệt trả 200 không gây lỗi SQL | Danh sách chỉ trả về các câu hỏi có status='APPROVED'. |
| `API-031` | GET `/api/v1/admin/questions/{id}` | CONTENT_ADMIN, SUPER_ADMIN: 200; STUDENT, COLLABORATOR: 403; Chưa login: 401 | ID không tồn tại trả 404 | Kiểm tra toàn bộ sub-items định dạng TRUE_FALSE hoặc MATCHING trong DB. |
| `API-032` | POST `/api/v1/admin/questions/{id}/approve` | CONTENT_ADMIN, SUPER_ADMIN: 200; STUDENT, COLLABORATOR: 403; Chưa login: 401 | Duyệt câu hỏi đã duyệt rồi trả 400/409 | Cột status trong bảng `questions` đổi thành APPROVED, tạo log trong `question_reviews`. |
| `API-033` | POST `/api/v1/admin/questions/{id}/request-revision` | CONTENT_ADMIN, SUPER_ADMIN: 200; STUDENT, COLLABORATOR: 403; Chưa login: 401 | Gửi yêu cầu sửa nhưng thiếu nội dung bình luận feedback trả 400 | Trạng thái đổi thành REVISION_REQUESTED, bình luận lưu vào `question_comments`. |
| `API-034` | POST `/api/v1/admin/questions/{id}/reject` | CONTENT_ADMIN, SUPER_ADMIN: 200; STUDENT, COLLABORATOR: 403; Chưa login: 401 | Từ chối câu hỏi thiếu lý do từ chối trả 400 | Cột status đổi thành REJECTED trong bảng `questions`. |
| `API-035` | GET `/api/v1/admin/questions/import/template` | CONTENT_ADMIN, SUPER_ADMIN: 200; STUDENT, COLLABORATOR: 403; Chưa login: 401 | Không có tham số, luôn trả file nhị phân | Header Content-Type là application/vnd.openxmlformats-officedocument.spreadsheetml.sheet; độ dài byte > 1000. |
| `API-036` | POST `/api/v1/admin/questions/import` | CONTENT_ADMIN, SUPER_ADMIN: 200; STUDENT, COLLABORATOR: 403; Chưa login: 401 | Upload file rỗng, sai định dạng .txt hoặc sai cấu trúc cột trả 400 | Nhận diện số dòng thành công, bảng `import_batches` ghi nhận số bản ghi mới. |
| `API-037` | GET `/api/v1/admin/tickets` | CONTENT_ADMIN, SUPER_ADMIN: 200; STUDENT, COLLABORATOR: 403; Chưa login: 401 | Lọc status ticket không tồn tại trả mảng trống 200 | Phân trang danh sách ticket từ bảng `tickets`. |
| `API-038` | PATCH `/api/v1/admin/tickets/{id}` | CONTENT_ADMIN, SUPER_ADMIN: 200; STUDENT, COLLABORATOR: 403; Chưa login: 401 | ID ticket không tồn tại trả 404 | Cập nhật resolution_status (RESOLVED/CLOSED) và note trong bảng `tickets`. |
| `API-039` | GET `/api/v1/admin/users` | SUPER_ADMIN: 200; CONTENT_ADMIN, COLLABORATOR, STUDENT: 403; Chưa login: 401 | Phân trang page < 0 trả 400 | Trả về Page<UserSummaryResponse> khớp tổng số lượng người dùng trong DB. |
| `API-040` | PATCH `/api/v1/admin/users/{id}/role` | SUPER_ADMIN: 200; CONTENT_ADMIN, COLLABORATOR, STUDENT: 403; Chưa login: 401 | Cập nhật vai trò không hợp lệ trả 400; tự hạ vai trò chính mình trả 409 | Cột role trong bảng `users` được cập nhật chính xác giá trị mới. |
| `API-041` | PATCH `/api/v1/admin/users/{id}/lock` | SUPER_ADMIN: 200; CONTENT_ADMIN, COLLABORATOR, STUDENT: 403; Chưa login: 401 | Khóa tài khoản đang đăng nhập của chính mình trả 409 | Cột status trong bảng `users` đổi thành LOCKED. |
| `API-042` | PATCH `/api/v1/admin/users/{id}/unlock` | SUPER_ADMIN: 200; CONTENT_ADMIN, COLLABORATOR, STUDENT: 403; Chưa login: 401 | Mở khóa tài khoản đã mở sẵn trả 200 idempotent | Cột status trong bảng `users` đổi lại thành ACTIVE. |
| `API-043` | GET `/api/v1/` | Toàn quyền PUBLIC: 200 | Truy cập không cần Header Authorization | JSON trả về status="UP", version hệ thống, serverTime. |
| `API-044` | POST `/api/v1/auth/register` | Toàn quyền PUBLIC: 201/200 | Đăng ký trùng email hoặc mật khẩu dưới 6 ký tự trả 400/409 | Người dùng mới được thêm vào bảng `users` với role='STUDENT', password được mã hóa BCrypt. |
| `API-045` | POST `/api/v1/auth/login` | Toàn quyền PUBLIC: 200 | Sai mật khẩu hoặc email không tồn tại trả 401 UNAUTHORIZED | Trả về accessToken và refreshToken; bản ghi mới được lưu trong `refresh_tokens`. |
| `API-046` | POST `/api/v1/auth/refresh` | Toàn quyền PUBLIC: 200 | Gửi refresh token giả mạo, hết hạn hoặc đã thu hồi trả 401 | Cấp accessToken mới, cơ chế xoay vòng token (rotation) cập nhật token mới vào DB. |
| `API-047` | POST `/api/v1/auth/logout` | Toàn quyền PUBLIC: 200 | Refresh token không tồn tại trả 200 không lỗi | Cột revoked trong bảng `refresh_tokens` chuyển thành true. |
| `API-048` | GET `/api/v1/auth/me` | STUDENT, COLLABORATOR, CONTENT_ADMIN, SUPER_ADMIN: 200; Chưa login: 401 | Header Bearer token bị sai lệch 1 ký tự trả 401 | Trả về email, fullName, role khớp chính xác user trong DB. |
| `API-049` | PUT `/api/v1/auth/me` | STUDENT, COLLABORATOR, CONTENT_ADMIN, SUPER_ADMIN: 200; Chưa login: 401 | Gửi fullName rỗng trả 400 BAD_REQUEST | Trường full_name và phone_number cập nhật trong bảng `users`. |
| `API-050` | PUT `/api/v1/auth/me/password` | STUDENT, COLLABORATOR, CONTENT_ADMIN, SUPER_ADMIN: 200; Chưa login: 401 | Sai mật khẩu cũ (currentPassword) trả 400 BAD_REQUEST | Trường password_hash thay đổi trong DB, đăng nhập lại bằng pass mới thành công. |
| `API-051` | GET `/api/v1/auth/me/leaderboard` | STUDENT, COLLABORATOR, CONTENT_ADMIN, SUPER_ADMIN: 200; Chưa login: 401 | Không có tham số, kiểm tra trạng thái hiện tại | Trả về cờ boolean optIn từ cột `leaderboard_opt_in` của bảng `users`. |
| `API-052` | PATCH `/api/v1/auth/me/leaderboard` | STUDENT, COLLABORATOR, CONTENT_ADMIN, SUPER_ADMIN: 200; Chưa login: 401 | Payload thiếu trường optIn trả 400 | Cột `leaderboard_opt_in` trong bảng `users` đảo giá trị đúng theo request. |
| `API-053` | POST `/api/v1/collaborator/questions` | COLLABORATOR, CONTENT_ADMIN, SUPER_ADMIN: 201/200; STUDENT: 403; Chưa login: 401 | Thiếu nội dung câu hỏi hoặc thiếu danh sách đáp án trả 400 | Bản ghi câu hỏi mới chèn vào `questions` với status='DRAFT' và created_by là user ID hiện tại. |
| `API-054` | GET `/api/v1/collaborator/questions` | COLLABORATOR, CONTENT_ADMIN, SUPER_ADMIN: 200; STUDENT: 403; Chưa login: 401 | Lọc status không hợp lệ trả danh sách rỗng 200 | Chỉ trả về danh sách câu hỏi do chính cộng tác viên đó tạo. |
| `API-055` | GET `/api/v1/collaborator/questions/{id}` | COLLABORATOR, CONTENT_ADMIN, SUPER_ADMIN: 200; STUDENT: 403; Chưa login: 401 | Xem câu hỏi do người khác tạo trả 403 FORBIDDEN | Trả về chi tiết câu hỏi kèm toàn bộ options và sub-items. |
| `API-056` | PUT `/api/v1/collaborator/questions/{id}` | COLLABORATOR, CONTENT_ADMIN, SUPER_ADMIN: 200; STUDENT: 403; Chưa login: 401 | Sửa câu hỏi đã APPROVED trả 409 CONFLICT | Cập nhật thành công nội dung câu hỏi trong bảng `questions`. |
| `API-057` | POST `/api/v1/collaborator/questions/{id}/submit-for-review` | COLLABORATOR, CONTENT_ADMIN, SUPER_ADMIN: 200; STUDENT: 403; Chưa login: 401 | Gửi duyệt câu hỏi không ở trạng thái DRAFT/REVISION_REQUESTED trả 400 | Trạng thái chuyển thành PENDING_REVIEW trong bảng `questions`. |
| `API-058` | GET `/api/v1/exams` | Toàn quyền PUBLIC: 200 | Lọc theo môn học không có đề thi nào trả mảng trống 200 | Chỉ trả về các đề thi có status='PUBLISHED'. |
| `API-059` | GET `/api/v1/exams/{id}` | Toàn quyền PUBLIC: 200 | Xem đề thi ở trạng thái DRAFT khi chưa đăng nhập trả 404 | Trả về tổng quan đề thi không kèm đáp án đúng. |
| `API-060` | POST `/api/v1/ai/explain` | STUDENT, CONTENT_ADMIN, SUPER_ADMIN: 200; Chưa login: 401 | Gửi questionId không tồn tại hoặc nội dung rỗng trả 400 | Kết nối local mock LLM cổng 18081, trả về văn bản giải thích sư phạm chi tiết. |
| `API-061` | GET `/api/v1/exams/{id}/download-bundle` | STUDENT: 200; COLLABORATOR, CONTENT_ADMIN, SUPER_ADMIN: 403; Chưa login: 401 | Tải đề thi đang DRAFT trả 404 | Trả về gói dữ liệu toàn bộ câu hỏi và mã xác thực phục vụ mã hóa AES-128-GCM phía client. |
| `API-062` | GET `/api/v1/leaderboard` | STUDENT: 200; COLLABORATOR, CONTENT_ADMIN, SUPER_ADMIN: 403; Chưa login: 401 | Truy vấn examId không có thí sinh làm bài trả danh sách rỗng | Chỉ hiển thị các thí sinh có điểm cao và đã bật `leaderboard_opt_in=true`. |
| `API-063` | GET `/api/v1/my-stats/topics` | STUDENT: 200; Chưa login: 401 | Người dùng mới chưa làm câu hỏi nào trả mảng rỗng 200 | Trả về danh sách chuyên đề kèm tỷ lệ chính xác từ bảng `user_topic_stats`. |
| `API-064` | GET `/api/v1/my-stats/weak-topics` | STUDENT: 200; Chưa login: 401 | Người dùng đạt 100% mọi chuyên đề trả mảng rỗng | Danh sách chuyên đề có accuracy < 60% sắp xếp tăng dần. |
| `API-065` | GET `/api/v1/practice/questions` | STUDENT: 200; Chưa login: 401 | Yêu cầu số lượng limit <= 0 hoặc > 50 trả 400 | Trả về danh sách câu hỏi ngẫu nhiên không lộ đáp án đúng. |
| `API-066` | POST `/api/v1/practice/grade` | STUDENT: 200; Chưa login: 401 | Gửi questionId sai lệch hoặc format câu trả lời không đúng chuẩn trả 400 | Chấm điểm tức thì, trả về isCorrect và cập nhật bảng `user_topic_stats`. |
| `API-067` | POST `/api/v1/sessions/start` | STUDENT: 200; Chưa login: 401 | Bắt đầu thi với đề đang DRAFT hoặc ngoài ca thi quy định trả 400/409 | Bản ghi mới tạo trong bảng `exam_sessions` với status='IN_PROGRESS'. |
| `API-068` | POST `/api/v1/sessions/{sessionId}/client-submit` | STUDENT: 200; Chưa login: 401 | Nộp phiên đã finalized trước đó trả 409 CONFLICT | Đồng bộ toàn bộ đáp án offline vào `session_answers`, tính điểm chuẩn V-SAT 150 điểm. |
| `API-069` | POST `/api/v1/sessions/{sessionId}/answers` | STUDENT: 200; Chưa login: 401 | Lưu câu trả lời cho phiên thi đã kết thúc trả 409 | Bản ghi chèn hoặc cập nhật trong bảng `session_answers`. |
| `API-070` | POST `/api/v1/sessions/{sessionId}/submit` | STUDENT: 200; Chưa login: 401 | Nộp lại phiên thi đã SUBMITTED trả 409 | Cập nhật status='SUBMITTED', tính toán tổng điểm rawScore và scaledScore (thang 150). |
| `API-071` | GET `/api/v1/sessions/{sessionId}/questions/{questionId}` | STUDENT: 200; Chưa login: 401 | Truy vấn câu hỏi không nằm trong phiên thi này trả 404 | Trả về nội dung câu hỏi và các lựa chọn đáp án. |
| `API-072` | GET `/api/v1/sessions/{sessionId}/answer-keys` | STUDENT: 200; Chưa login: 401 | Xem đáp án khi phiên thi chưa SUBMITTED trả 403 FORBIDDEN | Trả về toàn bộ đáp án đúng và lời giải chi tiết cho từng câu hỏi. |
| `API-073` | POST `/api/v1/tickets` | STUDENT: 201/200; Chưa login: 401 | Gửi ticket thiếu tiêu đề hoặc nội dung phản ánh trả 400 | Bản ghi mới tạo trong bảng `tickets` với status='OPEN' và user_id của thí sinh. |
| `API-074` | GET `/api/v1/subjects` | Toàn quyền PUBLIC: 200 | Không có tham số, luôn trả danh sách môn học hoạt động | Trả về đủ 7 môn V-SAT: Toán học, Tiếng Anh, Vật lí, Hóa học, Sinh học, Lịch sử, Địa lí. |
| `API-075` | GET `/api/v1/subjects/{subjectId}/topics` | Toàn quyền PUBLIC: 200 | ID môn học không tồn tại trả 404 | Khớp danh mục chủ đề theo khung GDPT 2018 trong bảng `topics`. |
| `API-076` | GET `/api/v1/subjects/{subjectId}/topics/{topicId}/subtopics` | Toàn quyền PUBLIC: 200 | ID chủ đề không tồn tại trả 404 | Khớp danh mục chuyên đề chi tiết trong bảng `subtopics`. |

---

### 3.2 Backend Jobs & Integrations (2 Mã Định Danh: JOB-001 & JOB-002)

1. **`JOB-001` - RefreshTokenCleanupService (Scheduled Cron Nightly)**:
   - **Mục tiêu**: Tự động dọn dẹp các refresh token đã hết hạn hoặc đã bị thu hồi quá 7 ngày trong cơ sở dữ liệu.
   - **Cơ chế kích hoạt trực tiếp**: Gọi trực tiếp phương thức kiểm thử hoặc trigger controller nội bộ `RefreshTokenCleanupService.cleanupExpiredTokens()` mà không cần chờ đến mốc thời gian 03:00 sáng.
   - **Xác minh tự động**:
     - Chèn vào bảng `refresh_tokens` 3 bản ghi: Token A (hợp lệ), Token B (hết hạn ngày hôm qua), Token C (thu hồi cách đây 8 ngày).
     - Kích hoạt thực thi hàm dọn dẹp.
     - Chạy truy vấn SQL: Kiểm tra Token B và Token C đã bị xóa hoàn toàn khỏi cơ sở dữ liệu; Token A vẫn còn nguyên vẹn.

2. **`JOB-002` - OpenAiCompatibleLlmClient (External LLM Integration)**:
   - **Mục tiêu**: Tích hợp trợ lý AI gia sư giải thích câu hỏi cho thí sinh qua endpoint `/api/v1/ai/explain`.
   - **Cơ chế kiểm thử độc lập**:
     - Cấu hình biến môi trường `LLM_BASE_URL=http://localhost:18081/v1`.
     - Kịch bản 1 (Thành công 200 OK): Mock server trả về luồng phản hồi chuẩn OpenAI. Assertions kiểm tra JSON đầu ra chứa chuỗi sư phạm hoàn chỉnh.
     - Kịch bản 2 (Xử lý sự cố): Mock server trả về HTTP 500 hoặc cố tình trễ timeout. Assertions kiểm tra client bắt đúng lỗi, không làm sập ứng dụng và trả về thông điệp dự phòng chuẩn mực.

---

### 3.3 Web Admin (10 Mã Định Danh: WEB-001 đến WEB-010)
Kiểm thử tự động hoàn toàn bằng Playwright Headless kết nối trực tiếp frontend Vite 5173 và backend Spring Boot 18080.

1. **`WEB-001` - Dashboard Tổng quan (Dashboard Tab)**:
   - Đăng nhập tài khoản CONTENT_ADMIN.
   - Truy cập giao diện bảng điều khiển.
   - Assertions: Phần tử thống kê hiển thị số liệu người dùng, số lượng đề thi, biểu đồ phân bố điểm số V-SAT được vẽ đầy đủ trên DOM.

2. **`WEB-002` - Quản lý đề thi V-SAT 2026 (ExamList)**:
   - Truy cập danh sách đề thi.
   - Assertions: Bộ lọc môn học, ô tìm kiếm hoạt động; các huy hiệu trạng thái (DRAFT, PUBLISHED, HIDDEN) hiển thị đúng màu; kích hoạt các nút hành động (xuất bản, ẩn, lưu trữ) tự động cập nhật bảng danh sách mà không cần tải lại trang.

3. **`WEB-003` - Tạo đề thi mới (CreateExamWizard 4 bước)**:
   - Bước 1 (Thông tin chung): Điền mã đề, tên đề, chọn môn Toán học, thời gian 90 phút. Bấm Tiếp tục.
   - Bước 2 (Cấu trúc đề): Chọn ma trận đề GDPT 2018 chuẩn hóa. Tự động kiểm tra ràng buộc số lượng câu hỏi.
   - Bước 3 (Xem trước & Sắp xếp): Kiểm tra danh sách câu hỏi được tải, xác minh các công thức KaTeX toán học hiển thị, thực hiện thao tác kéo thả đổi thứ tự câu hỏi và xóa bớt câu.
   - Bước 4 (Hoán vị & Xuất bản): Bấm sinh 4 mã đề hoán vị. Bấm xuất bản.
   - Assertions: Đề thi mới xuất hiện trên danh sách với trạng thái PUBLISHED.

4. **`WEB-004` - Ma trận đề thi V-SAT (BlueprintsPage)**:
   - Kiểm tra hiển thị bảng ma trận GDPT 2018 cho 3 môn: Toán học, Tiếng Anh, Vật lí.
   - Bấm vào chi tiết từng ô ma trận để kiểm tra cấu trúc 4 dạng câu hỏi.
   - Bấm nút Tạo đề từ ma trận này; kiểm tra giao diện tự động chuyển hướng vào Wizard bước 1 với dữ liệu nạp sẵn.

5. **`WEB-005` - Ca thi & Lịch thi (SchedulesPage)**:
   - Tạo mới một ca thi chính thức: nhập tiêu đề ca thi, thời gian bắt đầu, thời gian kết thúc, gắn với đề thi vừa xuất bản.
   - Thay đổi trạng thái ca thi từ UPCOMING sang RUNNING và CLOSED.
   - Assertions: Dữ liệu ca thi được tạo và cập nhật trạng thái chính xác trong cơ sở dữ liệu.

6. **`WEB-006` - Ngân hàng câu hỏi V-SAT 2026 (QuestionBank)**:
   - Mở modal tạo câu hỏi mới `QuestionCreateModal`.
   - Nhập nội dung câu hỏi có chứa công thức LaTeX, tạo các câu lệnh phụ cho dạng Đúng/Sai (Part II) và Ghép nối (Part III).
   - Assertions: Phần xem trước KaTeX hiển thị công thức chuẩn xác; lưu câu hỏi thành công vào ngân hàng.

7. **`WEB-007` - Hàng đợi duyệt câu hỏi V-SAT (ReviewQueue)**:
   - Đăng nhập tài khoản COLLABORATOR tạo một câu hỏi mới và bấm Gửi duyệt.
   - Đăng nhập tài khoản CONTENT_ADMIN mở trang Hàng đợi duyệt.
   - Kịch bản 1: Bấm Yêu cầu chỉnh sửa kèm nội dung nhận xét feedback.
   - Kịch bản 2: Bấm Duyệt chấp thuận câu hỏi vào ngân hàng chính thức.
   - Assertions: Trạng thái câu hỏi thay đổi tức thì trên giao diện và trong cơ sở dữ liệu.

8. **`WEB-008` - Import câu hỏi (ImportQuestions)**:
   - Tải file mẫu từ hệ thống.
   - Dùng thư viện `exceljs` sinh động tệp `.xlsx` hợp lệ trực tiếp trong bộ nhớ với đầy đủ 4 dạng câu hỏi V-SAT.
   - Thực hiện thao tác tải tệp lên qua form multipart.
   - Assertions: Bảng kết quả hiển thị 100% dòng hợp lệ, các câu hỏi mới được lưu thành công vào cơ sở dữ liệu.

9. **`WEB-009` - Cấu hình hệ thống (Settings Tab)**:
   - Kiểm tra giao diện hiển thị quy tắc tính điểm chuẩn V-SAT thang điểm 150.
   - Kiểm tra bảng điểm cộng từng phần cho dạng thức Đúng/Sai (Đúng 1 ý: 0.1đ, Đúng 2 ý: 0.25đ, Đúng 3 ý: 0.5đ, Đúng 4 ý: 1.0đ).
   - Assertions: Các thông số cấu hình hiển thị chính xác theo quy chế thi.

10. **`WEB-010` - MathView KaTeX Renderer (Shared Component)**:
    - Nạp component MathView với chuỗi hỗn hợp gồm công thức inline `$E=mc^2$` và block `$$\int_{0}^{1} x^2 dx$$`.
    - Assertions DOM: Kiểm tra sự tồn tại của các class `.katex-mathml` và `.katex-html`, kiểm tra nội dung text không còn xuất hiện ký tự `$` thô.

---

### 3.4 Android Screens & Flows (35 Mã Định Danh: AND-001 đến AND-035)
Kiểm thử tự động hoàn toàn bằng Espresso, UIAutomator và Espresso-Web trên Headless Emulator `Pixel_10_Pro` kết nối backend qua `adb reverse tcp:18080 tcp:18080`.

1. **`AND-001` - SplashActivity**:
   - Khởi chạy ứng dụng từ màn hình Splash.
   - Kịch bản chưa đăng nhập: Kiểm tra Intent tự động chuyển hướng sang `LoginActivity`.
   - Kịch bản đã lưu token trong EncryptedSharedPreferences: Kiểm tra tự động chuyển hướng vào `MainActivity`.

2. **`AND-002` - LoginActivity**:
   - Nhập email và mật khẩu tài khoản học sinh kiểm thử.
   - Bấm nút Đăng nhập.
   - Assertions: Token được lưu vào bộ nhớ mã hóa và giao diện chuyển tiếp thành công vào `MainActivity`.

3. **`AND-003` - RegisterActivity**:
   - Từ màn hình đăng nhập, chuyển sang màn hình Đăng ký.
   - Nhập thông tin họ tên, email mới, mật khẩu hợp lệ. Bấm Đăng ký.
   - Assertions: Tài khoản được tạo thành công trên backend và tự động đăng nhập.

4. **`AND-004` - MainActivity Container**:
   - Kiểm tra sự hiện diện của thanh điều hướng dưới đáy (BottomNavigationView).
   - Thực hiện chuyển đổi qua lại giữa 4 tab: Trang chủ, Đề thi, Luyện tập, Cá nhân.
   - Đăng nhập với quyền Admin/Cộng tác viên: Kiểm tra nút chuyển đổi chế độ làm việc (Admin/Collaborator Workspace) hiển thị đúng.

5. **`AND-005` - HomeFragment**:
   - Kiểm tra hiển thị chuỗi ngày học tập (daily streak banner).
   - Bấm vào các chip phím tắt môn học (Toán, Anh, Lí); kiểm tra bộ lọc danh sách đề thi phản hồi tương ứng.

6. **`AND-006` - ExamFragment**:
   - Kiểm tra danh sách đề thi được tải từ API.
   - Nhập từ khóa tìm kiếm trên thanh search view.
   - Bấm nút tải gói đề thi ngoại tuyến; kiểm tra huy hiệu trạng thái chuyển sang Đã tải.

7. **`AND-007` - ExamDetailActivity**:
   - Bấm vào một đề thi từ danh sách.
   - Kiểm tra thông tin chi tiết: số lượng câu hỏi, thời gian làm bài, cấu trúc các phần thi.
   - Kiểm tra nút Bắt đầu thi và nút Tải về offline hoạt động.

8. **`AND-008` - DownloadedExamsActivity**:
   - Mở thư viện đề thi đã tải về máy.
   - Kiểm tra hiển thị danh sách các tệp gói đề thi mã hóa `.vsat`.
   - Thực hiện thao tác xóa đề thi ngoại tuyến và kiểm tra cập nhật lại danh sách.

9. **`AND-009` - ExamSessionActivity (Phòng thi trực tuyến & ngoại tuyến)**:
   - Kích hoạt phiên làm bài thi chính thức.
   - Kiểm tra đồng hồ đếm ngược hoạt động chính xác với cảnh báo đổi màu theo thời gian còn lại.
   - Mở hộp thoại lưới câu hỏi (`dialog_question_grid.xml`) để nhảy nhanh tới câu hỏi bất kỳ.
   - Kiểm tra hiển thị và tương tác đủ 4 dạng thức câu hỏi V-SAT:
     - Dạng 1: Trắc nghiệm 4 lựa chọn (Single Choice).
     - Dạng 2: Câu hỏi Đúng/Sai 4 ý nhỏ (True/False Sub-items).
     - Dạng 3: Câu hỏi Ghép nối đối ứng (Matching Items).
     - Dạng 4: Câu hỏi Trả lời ngắn điền số/chuỗi (Short Answer).
   - Kiểm tra công thức toán hiển thị qua KaTeX WebView.
   - Bấm nút Nộp bài; kiểm tra hiển thị hộp thoại cảnh báo số câu chưa hoàn thành trước khi xác nhận nộp.

10. **`AND-010` - ExamResultActivity**:
    - Sau khi nộp bài thành công, giao diện chuyển sang màn hình Kết quả.
    - Assertions: Hiển thị điểm số quy đổi theo thang điểm chuẩn V-SAT 150 điểm, số câu đúng/sai từng phần thi, thời gian làm bài, và nút Xem lại bài thi.

11. **`AND-011` - ExamReviewActivity**:
    - Xem lại toàn bộ lời giải chi tiết của từng câu hỏi với công thức KaTeX.
    - Bấm nút Trợ lý AI giải thích: kiểm tra hộp thoại `showAiExplainDialog` mở ra và hiển thị nội dung giải thích từ backend.
    - Bấm nút Báo lỗi câu hỏi: kiểm tra hộp thoại `showReportDialog` gửi ticket thành công.

12. **`AND-012` - PracticeFragment**:
    - Chọn môn học, chủ đề kiến thức và mức độ khó mong muốn.
    - Bấm nút Bắt đầu luyện tập tức thì.

13. **`AND-013` - PracticeSessionActivity**:
    - Chọn đáp án cho câu hỏi luyện tập.
    - Bấm nút Kiểm tra: hệ thống chấm điểm ngay lập tức, hiển thị đúng/sai kèm giải thích KaTeX.

14. **`AND-014` - ExamHistoryActivity**:
    - Mở lịch sử thi; kiểm tra danh sách các lượt thi đã thực hiện kèm điểm số và ngày giờ thi.
    - Bấm vào một lượt thi cũ để xem lại bài làm.

15. **`AND-015` - FlashcardReviewActivity**:
    - Mở tính năng ôn tập công thức dạng Flashcard (thuật toán lặp lại ngắt quãng Leitner).
    - Thực hiện thao tác lật thẻ xem công thức mặt sau.
    - Bấm các mức độ ghi nhớ (Chưa nhớ, Đã nhớ) và kiểm tra tiến độ cập nhật.

16. **`AND-016` - ProgressActivity**:
    - Mở màn hình Thống kê tiến độ học tập cá nhân.
    - Kiểm tra các chỉ số năng lực theo từng chuyên đề kiến thức và danh sách các chủ đề còn yếu.

17. **`AND-017` - ProfileFragment**:
    - Kiểm tra thông tin cá nhân của học sinh.
    - Đổi ảnh đại diện (Avatar): Dùng stub `ActivityResultRegistry` giả lập chọn tệp ảnh trong bộ nhớ, kiểm tra ảnh đại diện cập nhật mà không cần mở bộ chọn file hệ thống.
    - Đổi mật khẩu tài khoản và đăng xuất.

18. **`AND-018` - SettingsActivity**:
    - Cài đặt giờ thông báo nhắc nhở học tập hàng ngày qua bộ chọn giờ (TimePicker).
    - Bật/tắt thông báo chuỗi ngày học tập.

19. **`AND-019` - LeaderboardActivity**:
    - Xem bảng xếp hạng thí sinh đạt điểm cao theo từng đề thi.
    - Bật/tắt cờ công khai thông tin cá nhân trên bảng xếp hạng (Leaderboard Opt-in).

20. **`AND-020` - AdminActivity Container**:
    - Đăng nhập tài khoản CONTENT_ADMIN trên ứng dụng Android.
    - Chuyển sang giao diện quản trị; kiểm tra nạp đủ các phân hệ quản lý đề thi, câu hỏi và người dùng.

21. **`AND-021` - AdminDashboardFragment**:
    - Kiểm tra hiển thị các số liệu KPI quản trị (số người dùng, số đề thi, tỷ lệ duyệt câu hỏi) trên Android.

22. **`AND-022` - AdminExamListFragment**:
    - Danh sách đề thi phía quản trị với bộ lọc trạng thái.
    - Nút tạo đề thi mới mở `AdminCreateExamActivity`.

23. **`AND-023` - AdminCreateExamActivity**:
    - Nhập tiêu đề, chọn môn học, thời lượng làm bài và tạo đề thi nháp từ Android.

24. **`AND-024` - AdminExamDetailActivity**:
    - Xem cấu hình đề thi, xuất bản đề thi hoặc xóa đề thi nháp.

25. **`AND-025` - AdminQuestionPickerActivity**:
    - Mở danh sách câu hỏi đã duyệt trong ngân hàng và chọn câu hỏi gán vào đề thi.

26. **`AND-026` - AdminQuestionBankFragment**:
    - Duyệt ngân hàng câu hỏi trên Android theo trạng thái (Đã duyệt, Chờ duyệt, Nháp).

27. **`AND-027` - AdminReviewQuestionActivity**:
    - Xem chi tiết câu hỏi chờ duyệt với công thức KaTeX.
    - Thực hiện duyệt chấp thuận hoặc yêu cầu chỉnh sửa kèm lý do.

28. **`AND-028` - AdminUserManagementFragment**:
    - Đăng nhập tài khoản SUPER_ADMIN.
    - Thực hiện thay đổi vai trò người dùng (giữa STUDENT, COLLABORATOR, CONTENT_ADMIN).
    - Thực hiện khóa và mở khóa tài khoản người dùng.

29. **`AND-029` - AdminTicketActivity**:
    - Xem danh sách báo lỗi câu hỏi từ học sinh và cập nhật trạng thái xử lý (Resolved).

30. **`AND-030` - CollaboratorWorkspaceActivity**:
    - Đăng nhập tài khoản COLLABORATOR mở không gian làm việc của cộng tác viên.

31. **`AND-031` - CollaboratorQuestionListFragment**:
    - Xem danh sách câu hỏi do cộng tác viên tự biên soạn (Nháp, Bị từ chối, Đã duyệt).

32. **`AND-032` - CollaboratorCreateQuestionActivity**:
    - Biên soạn câu hỏi mới trên Android, chọn định dạng (Trắc nghiệm, Đúng/Sai, Ghép nối, Trả lời ngắn).
    - Lưu nháp thành công.

33. **`AND-033` - CollaboratorQuestionDetailActivity**:
    - Mở câu hỏi nháp, chỉnh sửa nội dung và bấm Gửi duyệt cho quản trị viên.

34. **`AND-034` - KaTeXHelperFlow**:
    - Kiểm tra cơ chế nhúng WebView nội bộ (`file:///android_asset/katex/`).
    - Thực thi đánh giá JavaScript (`evaluateJavascript`) trong WebView; kiểm tra cây DOM KaTeX kết xuất thành công các ký hiệu toán học đặc biệt.

35. **`AND-035` - OfflineExamBundleDownloadAndSubmitFlow**:
    - Bước 1: Gọi tải gói đề thi qua `GET /api/v1/exams/{id}/download-bundle`.
    - Bước 2: Lưu trữ mã hóa AES-128-GCM vào tệp cục bộ có đuôi mở rộng `.vsat`.
    - Bước 3: Ngắt kết nối mạng giả lập trên Emulator:
      ```bash
      adb shell svc data disable && adb shell svc wifi disable
      ```
    - Bước 4: Mở phòng thi ngoại tuyến, thực hiện làm trọn vẹn đề thi và bấm nộp bài ngoại tuyến (lưu phiên vào cơ sở dữ liệu Room cục bộ).
    - Bước 5: Bật lại kết nối mạng:
      ```bash
      adb shell svc data enable && adb shell svc wifi enable
      ```
    - Bước 6: Kích hoạt đồng bộ hàng loạt qua `POST /api/v1/sessions/{sessionId}/client-submit`; kiểm tra điểm số được chấm chính xác trên máy chủ backend.

---

### 3.5 Android Workers & Notifications (3 Mã Định Danh: WRK-001 đến WRK-003)

1. **`WRK-001` - DailyReminderWorker**:
   - Sử dụng `TestListenableWorkerBuilder` hoặc `WorkManagerTestInitHelper` để kích hoạt thực thi trực tiếp Worker mà không cần chờ lịch định kỳ.
   - Assertions: Worker hoàn thành với kết quả `ListenableWorker.Result.success()`; thông báo nhắc nhở học tập được tạo với kênh `vsat_daily_reminder`.

2. **`WRK-002` - OfflineExamSyncWorker**:
   - Chèn một phiên thi ngoại tuyến đang chờ đồng bộ vào Room Database cục bộ.
   - Thiết lập trạng thái kết nối mạng giả lập `NetworkType.CONNECTED` trong môi trường test WorkManager.
   - Assertions: Worker tự động kích hoạt gửi gói tin lên backend, cập nhật trạng thái phiên thành Đã đồng bộ.

3. **`WRK-003` - NotificationChannel_vsat_daily_reminder**:
   - Khởi chạy `NotificationHelper.ensureChannel(context)`.
   - Assertions: Kiểm tra qua API hệ thống Android `NotificationManager.getNotificationChannel("vsat_daily_reminder")` tồn tại với mức độ ưu tiên `IMPORTANCE_DEFAULT`.

---

## 4. CƠ CHẾ CHẤM ĐIỂM & PHÁN QUYẾT NGHIỆM THU TỰ ĐỘNG (MACHINE VERDICT & COVERAGE GATE)

### 4.1 Cổng phân định tự động (Automated Verdict Gate)
- Kịch bản thực thi: `acceptance-harness/verdict-gate.mjs`.
- Đọc tệp kê khai chuẩn: `docs/testing/feature-inventory.json`.
- Thu thập và đối chiếu các tệp kết quả chạy thực tế:
  - `acceptance-harness/results/backend-report.json`
  - `acceptance-harness/results/web-report.json`
  - `acceptance-harness/results/android-report.xml`

### 4.2 Công thức phán quyết nghiệm thu
- **Tiêu chí ĐẠT (ACCEPTED - Thoát mã `exit 0`)**:
  - Tỷ lệ bao phủ: 100% (đủ 126/126 mã định danh: 76 Backend Endpoints, 2 Jobs, 10 Web Admin, 35 Android Screens/Flows, 3 Android Workers).
  - Tỷ lệ lỗi: Chính xác 0% (không có bất kỳ bài kiểm tra nào bị FAILED hoặc ERROR).
  - Tỷ lệ bỏ qua: 0% (tuyệt đối không cho phép bài kiểm tra nào ở trạng thái SKIPPED).
  - Tỷ lệ flake: Mọi trường hợp cần chạy lại (retry) đều phải vượt qua ở lần thứ 2 và được ghi rõ trong báo cáo.
- **Tiêu chí KHÔNG ĐẠT (REJECTED - Thoát mã `exit 1`)**:
  - Bất kỳ mã định danh nào bị thiếu trong báo cáo kết quả.
  - Có ít nhất 1 bài kiểm tra trả về trạng thái thất bại.
  - Phát hiện bất kỳ dấu hiệu can thiệp từ con người hoặc kết quả giả lập không có đối chiếu cơ sở dữ liệu.

### 4.3 Tệp đầu ra của máy chấm
- `acceptance-harness/results/final-verdict.json`: Chứa bảng tổng hợp kết quả chi tiết từng mã ID, thời gian chạy, số lần retry và phán quyết cuối cùng.
- `acceptance-harness/results/machine-summary.txt`: Chuỗi văn bản tóm tắt dành cho CI/CD pipeline đọc trực tiếp.

---

## 5. LỘ TRÌNH THỰC THI THEO VÒNG LẶP `auto-acceptance-loop` (MULTI-ROUND ROADMAP)

1. **Vòng 1 (Hiện tại - Đã hoàn thành)**:
   - Khảo sát toàn diện hệ thống mã nguồn.
   - Đóng gói tài nguyên kỹ năng `auto-acceptance-loop`.
   - Thiết lập bảng kê khai tính năng `docs/testing/feature-inventory.json` (126 mã định danh).
   - Thiết kế tài liệu kế hoạch thử nghiệm và nghiệm thu tự động phòng sạch `docs/testing/INDEPENDENT_AUTO_ACCEPTANCE_PLAN.md`.

2. **Vòng 2 (Xây dựng bộ công cụ Clean-Room Acceptance Harness)**:
   - Khởi tạo thư mục độc lập `acceptance-harness/` với `package.json` riêng.
   - Viết kịch bản chốt chặn an toàn `safety-preflight.mjs`.
   - Viết kịch bản tự động hóa khởi tạo container PostgreSQL 16 trên cổng 55432, nạp schema và 11 tệp migration.
   - Viết máy chủ giả lập local LLM stub cổng 18081.
   - Viết bộ kịch bản kiểm thử tự động 76 API Backend và 2 Jobs.
   - Viết bộ kịch bản kiểm thử Playwright Headless cho 10 màn hình Web Admin.
   - Viết bộ kiểm thử Android Instrumented Test cho 35 màn hình/luồng và 3 Workers trên Headless Emulator `Pixel_10_Pro`.
   - Xây dựng công cụ chấm điểm `verdict-gate.mjs`.

3. **Vòng 3 (Kích hoạt chạy toàn trình & Xuất phán quyết nghiệm thu)**:
   - Kích hoạt toàn bộ harness chạy tự động từ đầu đến cuối theo quy trình tuần tự.
   - Thu thập toàn bộ tệp nhật ký và báo cáo máy chấm (`results/`).
   - Chạy `verdict-gate.mjs` phân tích đối chiếu.
   - Xác nhận phán quyết cuối cùng `ACCEPTED` (100% 126/126 IDs PASSED).
