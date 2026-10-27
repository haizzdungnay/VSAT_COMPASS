# Prompt cho Cursor — Triển khai F3 (Gamification) + F5 (Ticket báo lỗi)

> Copy toàn bộ phần dưới dán vào Cursor.

---

## NGỮ CẢNH DỰ ÁN

**V-SAT Compass** — app luyện thi V-SAT, 1 repo gồm 2 phần:

- **Android (Java):** `app/src/main/java/com/example/v_sat_compass/`
  - `ui/home/HomeFragment.java` (màn chính, có quick stats), `ui/profile/ProfileFragment.java`
  - `ui/exam/session/ExamReviewActivity.java` (xem lời giải từng câu), `ExamSessionActivity.java`
  - `ui/admin/users/AdminUserManagementFragment.java`, `ui/admin/dashboard/AdminDashboardFragment.java` (mẫu màn admin có list + live data)
  - `data/repository/ExamHistoryRepository.java` (mẫu lưu local JSON: atomic write, cap, corrupt-recovery), `StudentStatsRepository.java`
  - `data/api/`: `ApiClient` (Retrofit) + các `*Api`; envelope `ApiResponse<T>`
  - `util/NetworkUtils.java` (`isOnline`)
- **Backend (Spring Boot, Java):** `VSAT/vsat-compass-api/src/main/java/com/vsatcompass/api/`
  - `controller/admin/`, `controller/student/`; `entity/`; `repository/`; `service/` + `service/impl/`
  - `config/SecurityConfig.java` (rule theo path + role; rule cụ thể đặt TRƯỚC rule chung)
  - `dto/request|response/`; `exception/AppException` (factory `notFound`, `badRequest`...) + `GlobalExceptionHandler`; envelope `ApiResponse`
  - role: `STUDENT`, `COLLABORATOR`, `CONTENT_ADMIN`, `SUPER_ADMIN`; auth `@PreAuthorize` + SecurityConfig
  - test: `src/test/java/com/vsatcompass/api/service/*Test.java` (Mockito)

**Bối cảnh vận hành:** Neon DB cloud đang treo → đang demo bằng local backend (Docker) + APK debug. **Làm + verify LOCAL trước**, prod smoke + deploy để sau.

## RÀNG BUỘC CHUNG (tuân thủ tuyệt đối)

- **KHÔNG** commit, **KHÔNG** tag, **KHÔNG** deploy, **KHÔNG** prod smoke. Chỉ code + để onii-chan review.
- **KHÔNG** revert/commit các file dirty cũ đã có sẵn (`application.yml`, `app/build.gradle.kts`, `ApiClient.java`, `gradle.properties`, `SessionController.java`, docs/docker/log files). Để nguyên.
- Nếu về sau có commit: message **không co-author**, không kéo theo config demo dirty.
- Giữ **local-first**: tính năng mới không làm vỡ luồng offline đang có.
- Tái dùng pattern sẵn có (`ExamHistoryRepository` cho lưu local, `AdminUserManagementFragment` cho màn admin list, `AppException`/`ApiResponse` cho backend).
- Auth: endpoint student yêu cầu `ROLE_STUDENT`; endpoint admin yêu cầu `CONTENT_ADMIN`/`SUPER_ADMIN`. Rule SecurityConfig cụ thể đặt trước rule chung.

---

## F3 — Gamification (streak + XP/level + daily challenge)

**Mục tiêu:** Tạo động lực quay lại hằng ngày. **Local-first**: lưu + tính toán trên máy trước (offline vẫn cộng được); phần sync backend để sau (Neon đang treo) — chỉ chừa chỗ mở rộng, KHÔNG bắt buộc backend lúc này.

| ID | Hạng mục | Tiêu chí |
|----|----------|----------|
| F3.1 | `GamificationRepository` lưu local (JSON, atomic write theo mẫu `ExamHistoryRepository`): ngày hoạt động gần nhất, streak hiện tại, tổng XP, level | corrupt-recovery + cap như mẫu |
| F3.2 | Streak ngày: mỗi ngày có hoạt động (hoàn thành 1 bài/1 phiên luyện) → streak +1; bỏ 1 ngày → reset về 1 | tính theo ngày local (không phụ thuộc backend) |
| F3.3 | XP + level: cộng XP khi hoàn thành bài (vd mỗi câu đúng +X, hoàn thành bài +Y); công thức level từ tổng XP | hiển thị nhất quán |
| F3.4 | Daily challenge: 1 nhiệm vụ/ngày (vd "làm 10 câu"); hoàn thành → đánh dấu xong; reset theo ngày | reset đúng khi sang ngày mới |
| F3.5 | UI: widget streak + XP/level trên `HomeFragment`; chi tiết XP/level trên `ProfileFragment`; daily challenge hiện ở Home | có empty/zero state |

**Điểm móc nối:** gọi cập nhật gamification ở chỗ kết thúc bài (sau submit/chấm điểm) trong `ExamSessionActivity` và luồng luyện tập — tái dùng nơi đang ghi `ExamHistoryEntry`.

**Nghiệm thu F3:**
- Hoàn thành bài hôm nay → streak = 1, XP tăng, daily challenge tiến triển.
- Giả lập sang ngày mới có hoạt động → streak = 2; bỏ 1 ngày → streak reset.
- Hoạt động khi offline vẫn cộng streak/XP (không cần mạng).
- `./gradlew :app:assembleDebug` PASS.

---

## F5 — Báo lỗi câu hỏi + Ticket admin

**Mục tiêu:** Vòng khép kín: học viên báo sai đáp án/nội dung câu hỏi → admin xem & xử lý → đóng ticket. Mockup tham chiếu: `v_sat_qu_n_tr_ticket_l_i_n_i_dung`, `v_sat_qu_n_tr_y_u_c_u_s_a_l_i`.

### F5-A — Backend

| ID | Hạng mục | Tiêu chí |
|----|----------|----------|
| F5-A.1 | Entity `Ticket` (id, questionId nullable, reporterUserId, message, status, createdAt, updatedAt, resolverNote nullable) + enum `TicketStatus` (OPEN / IN_PROGRESS / CLOSED) + repository | tạo bảng tương ứng |
| F5-A.2 | `POST /tickets` (role `STUDENT`): tạo ticket với `questionId` + `message`; trạng thái khởi tạo OPEN | validate message non-blank (`AppException.badRequest`) |
| F5-A.3 | `GET /admin/tickets` (role `CONTENT_ADMIN`/`SUPER_ADMIN`): list có filter `status` (paged) | trả `ApiResponse` envelope như các admin endpoint khác |
| F5-A.4 | `PATCH /admin/tickets/{id}` (admin): cập nhật `status` + `resolverNote`; state machine hợp lệ | chuyển trạng thái sai → `409`/`badRequest` theo convention |
| F5-A.5 | SecurityConfig rules + Mockito test cho service (tạo / list filter / cập nhật trạng thái / chặn role sai) | test PASS |

Tham chiếu: các controller/service/dto hiện có làm mẫu (vd user management); `AppException` + `GlobalExceptionHandler` cho error code.

### F5-B — Android

| ID | Hạng mục | Tiêu chí |
|----|----------|----------|
| F5-B.1 | `TicketApi` + `TicketRepository` (callback wrapper như các repo hiện có) | — |
| F5-B.2 | Student: nút "Báo lỗi câu hỏi" trong `ExamReviewActivity` (và/hoặc trong khi làm bài) → dialog nhập mô tả → `POST /tickets` kèm `questionId` | có loading/success/error state |
| F5-B.3 | Admin: màn danh sách ticket (bám `AdminUserManagementFragment`): list + filter trạng thái + cập nhật trạng thái/ghi chú | đổi trạng thái phản ánh ngay trên list |

**Nghiệm thu F5 (LOCAL):**
- Student báo lỗi 1 câu → ticket xuất hiện ở list admin với trạng thái OPEN.
- Admin đổi OPEN → IN_PROGRESS → CLOSED, kèm ghi chú; list cập nhật đúng.
- Role student gọi `/admin/tickets` → bị chặn; role admin gọi `POST /tickets` không phải luồng dự kiến (theo convention).
- Backend `./gradlew test` (test mới) PASS; Android `./gradlew :app:assembleDebug` PASS.

---

## BÁO CÁO KHI XONG

Liệt kê file đã thêm/sửa (tách F3 / F5), cách verify đã chạy, và bất kỳ chỗ nào phải điều chỉnh ngoài phạm vi trên (đặc biệt nếu phải đụng file đang dirty — nếu có thì nêu rõ, đừng tự revert).
