# V-SAT Compass — Phase F: Offline Download + Competitive Features

> Tạo: 2026-06-18 | Base HEAD: `40cec10` (main/origin/main) | Tag prod gần nhất: `v0.10.2`
>
> Kế hoạch này **độc lập** với `task.md` (roadmap A–E). Phase F là lớp tính năng cạnh tranh thêm, soạn sau khi đối chiếu app với Tuyensinh247 / MapStudy / Azota (VN) và Khan Academy / LearnQ / Aniko / Bluebook (SAT quốc tế).
>
> ⚠️ **Bối cảnh:** Neon DB (cloud) đang treo quota → đang demo bằng local backend (Docker `vsat-pg`) + APK debug LAN. Mọi mục Phase F **làm + verify local trước**, prod smoke + deploy để sau khi Neon sống lại (theo đúng nguyên tắc đang áp dụng cho batch real-data).

---

## Quyết định đã chốt (onii-chan, 2026-06-18)

- **Tải đề offline chỉ áp dụng cho đề FREE** (đề hiện tại đều FREE + price=0). Đề tính tiền: **defer** tới khi làm luồng kiếm tiền/commerce sau demo.
- **AI chấm thi: KHÔNG làm** (khó, defer vô thời hạn). Phase F chỉ làm **AI giải thích lời giải** (khác AI chấm điểm).
- **F4 (AI giải thích):** backend proxy OpenAI-compatible (`{base}/chat/completions`), model `gemini/gemini-2.5-flash-lite`. Config cụ thể: base `http://localhost:20128/v1` (tunnel `https://ri2m8vp.abc-tunnel.us/v1`), key `sk-...` (router của onii-chan). Sản phẩm thử nghiệm — ưu tiên chạy được. Giá trị thật để trong file config LOCAL **đã .gitignore** (không commit lên GitHub); rotate key sau demo nếu cần.
- **F8 (nhắc học):** giai đoạn này CHỈ local notification (WorkManager), **KHÔNG FCM**; push "đề mới" từ server defer tới khi online.
- **F9 (leaderboard):** opt-in + tên hiển thị; mặc định tắt; ưu tiên xếp hạng theo điểm `exam_sessions` sẵn có thay vì đẩy XP local lên server.
- **Schema debt:** `ddl-auto=validate` ở cả dev/prod, không Flyway → mọi bảng/cột mới phải kèm DDL tường minh trong `docs/sql/` và áp lên prod trước khi deploy (xem `0001_create_tickets.sql`). Cân nhắc Flyway khi lên online.
- Format kế hoạch theo `task.md`; mỗi task chỉ "xong" khi có tiêu chí nghiệm thu rõ ràng.

- **Mã hoá gói đề tải về: CÓ — mã hoá nhẹ AES bằng key của app** (chốt 2026-06-18). Mục tiêu là chống tò mò mở file xem đáp án trước khi làm, KHÔNG phải bảo mật tuyệt đối.

---

## Nguyên tắc Phase F

- Giữ kiến trúc **local-first**: tính năng mới không được làm vỡ luồng offline đang có.
- Tái dùng hạ tầng sẵn có (`SessionContentRepository`, `ExamHistoryRepository`, `OfflineDemoDataHelper`, `NetworkUtils`, `my-stats`, `Difficulty` enum, `session_answers`) thay vì dựng mới.
- Backend đổi tối thiểu; ưu tiên endpoint mới thay vì sửa contract cũ (tránh regression đề/đáp án đã strip).
- Làm + test local → prod smoke sau (khi Neon sống).

## Bảng tổng quan Phase F

| ID | Tính năng | Ưu tiên | Trạng thái | Phụ thuộc |
|----|-----------|---------|------------|-----------|
| F1 | Tải đề FREE về làm offline + sync kết quả | **P0** | 📋 TODO | quyết định mã hoá |
| F2 | Adaptive practice (luyện theo chủ đề yếu + độ khó động) | **P1** | 📋 TODO | F-data (Difficulty, weak-topics) |
| F3 | Gamification: streak ngày + XP/level + daily challenge | **P1** | 📋 TODO | — |
| F4 | AI giải thích lời giải từng câu (review) | **P1** | 📋 TODO | chọn LLM provider |
| F5 | Học viên báo lỗi câu hỏi + ticket admin (= D3 cũ) | **P1** | 📋 TODO | — |
| F6 | Flashcard / spaced-repetition (từ vựng + công thức) | P2 | 📋 TODO | — |
| F7 | Analytics tiến bộ theo thời gian + thời gian/câu | P2 | 📋 TODO | session_answers (đã có) |
| F8 | Nhắc học theo lịch (local notification) | P2 | ✅ Done (local) | — |
| F9 | Leaderboard opt-in (XP/điểm bài thi) | P2 (tùy chọn) | ✅ Done | — |

---

## F1 — Tải đề FREE về làm offline (P0)

**Mục tiêu:** Mỗi item đề FREE có ô "Tải xuống". Đề đã tải → làm bài hoàn toàn offline (bỏ qua `startSession` mạng), chấm tại máy, kết quả vào hàng đợi và sync khi có mạng. Đây cũng là cơ hội **vá luôn lỗi luồng "Làm bài" chưa offline thuần / màn đen** đang treo trong `task.md`.

### F1-A — Backend: download bundle endpoint

| ID | Hạng mục | Tiêu chí |
|----|----------|----------|
| F1-A.1 | `GET /exams/{id}/download-bundle` — trả full nội dung đề (metadata + tất cả câu hỏi + đáp án đúng + lời giải) | **CHỈ** cho đề `status=PUBLISHED` **và** FREE+price=0; đề khác → `403`/`404` chống enumeration |
| F1-A.2 | DTO `ExamDownloadBundleResponse` riêng, **KHÔNG** đụng `PublicQuestionForStudentResponse` đang strip đáp án | endpoint thường trong session vẫn strip như cũ; chỉ bundle mới có đáp án |
| F1-A.3 | Versioning bundle (field `examVersion`/`updatedAt`) để client biết khi nào cần tải lại | client so version để cảnh báo đề đã cập nhật |
| F1-A.4 | Mockito test cho service bundle (FREE pass / non-FREE reject / non-PUBLISHED reject) | test PASS |

Tham chiếu code: `VSAT/vsat-compass-api/src/main/java/com/vsatcompass/api/controller/student/`, entity `Exam`/`Question`/`QuestionOption`, `MyStatsController` làm mẫu controller student.

### F1-B — Android: download manager + lưu local

| ID | Hạng mục | Tiêu chí |
|----|----------|----------|
| F1-B.1 | Ô/nút "Tải xuống" trên item đề (exam list), chỉ hiện với đề FREE | có 3 trạng thái: chưa tải / đang tải / đã tải |
| F1-B.2 | `OfflineExamRepository` + lưu bundle vào internal storage (file riêng từng đề) | atomic write, có thể xoá đề đã tải |
| F1-B.3 | Mã hoá gói tải bằng AES (key của app) khi ghi xuống, giải mã khi đọc | file trên máy không đọc được đáp án bằng cách mở trực tiếp |
| F1-B.4 | Quản lý dung lượng: liệt kê đề đã tải + tổng dung lượng + nút xoá | màn "Đề đã tải" |

Tham chiếu: `app/.../data/repository/ExamRepository.java`, `util/LocalExamDataSource.java`, `ExamHistoryRepository.java` (mẫu atomic write + cap).

### F1-C — Android: luồng làm bài offline + sync

| ID | Hạng mục | Tiêu chí |
|----|----------|----------|
| F1-C.1 | `ExamSessionActivity` rẽ nhánh: nếu đề đã tải → đọc nội dung local, **bỏ qua `startSession` mạng** | mất mạng vẫn vào làm bài được (không còn `finish()`) |
| F1-C.2 | Chấm điểm tại máy từ đáp án trong bundle; dựng review từ lời giải local | kết quả + review hiển thị offline đúng |
| F1-C.3 | Hàng đợi sync: kết quả làm offline lưu `ExamHistoryEntry` local + queue → khi online gọi `client-submit` | có mạng tự đẩy; trùng thì idempotent (anti-replay 409 đã có) |
| F1-C.4 | Fix lỗi màn đen "Làm bài" trong nhánh offline | smoke 50 câu offline không crash, rotate/background giữ state |

Tham chiếu: `app/.../ui/exam/session/ExamSessionActivity.java`, `ExamResultActivity.java`, `SessionContentRepository.java`, `NetworkUtils.java`, flag `USE_BACKEND_EXAM_CONTENT`.

**Nghiệm thu F1:** Tắt mạng hoàn toàn → tải trước 1 đề FREE (khi còn mạng) → làm trọn bài offline → xem kết quả + review offline → bật mạng → kết quả tự sync lên backend, không tạo bản trùng.

---

## F2 — Adaptive practice (P1)

**Mục tiêu:** Chế độ "Luyện thông minh": chọn câu theo chủ đề yếu (`/my-stats/weak-topics` đã có) và nâng/giảm `Difficulty` theo chuỗi đúng/sai trong phiên.

| ID | Hạng mục | Tiêu chí |
|----|----------|----------|
| F2.1 | Backend: endpoint cấp câu luyện theo `subtopicId` + `difficulty` (lọc từ question bank) | trả câu đã strip đáp án như session thường |
| F2.2 | Thuật toán adaptive client: đúng N câu liên tiếp → tăng độ khó, sai → giảm | logic test bằng JVM test |
| F2.3 | UI luyện tập: bám `PracticeFragment` + weak-topics hiện có | vào từ màn Practice |

Tham chiếu: `app/.../ui/practice/PracticeFragment.java`, `StudentStatsRepository.java`, `Difficulty` enum backend.

**Nghiệm thu:** Luyện 1 chủ đề yếu, độ khó câu tự điều chỉnh theo kết quả; thống kê chủ đề cập nhật sau phiên.

---

## F3 — Gamification (P1)

**Mục tiêu:** Tạo động lực quay lại hằng ngày (nhóm app SAT ghi nhận giữ streak gắn với tăng điểm rõ rệt).

| ID | Hạng mục | Tiêu chí |
|----|----------|----------|
| F3.1 | Streak ngày: đếm ngày liên tiếp có hoạt động (làm bài/luyện tập) | widget streak trên Home |
| F3.2 | XP + level: mỗi câu đúng / bài hoàn thành cộng XP, lên level | hiển thị Home/Profile |
| F3.3 | Daily challenge: 1 nhiệm vụ/ngày (vd "làm 10 câu Toán") | reset theo ngày |
| F3.4 | Lưu trữ: bảng `daily_activity` (backend) hoặc local trước, sync sau | offline vẫn cộng streak/XP, sync khi online |

**Nghiệm thu:** Hoạt động 2 ngày liên tiếp → streak = 2; làm bài → XP tăng; daily challenge reset mỗi ngày.

---

## F4 — AI giải thích lời giải (P1)

**Mục tiêu:** Trong màn review câu sai, nút "Giải thích thêm" gọi LLM diễn giải bước giải / vì sao đáp án đúng. **KHÔNG** dùng AI để chấm điểm.

| ID | Hạng mục | Tiêu chí |
|----|----------|----------|
| F4.1 | Backend proxy endpoint gọi LLM (giữ API key server-side, rate-limit) | không lộ key xuống client |
| F4.2 | Nút "Giải thích thêm" trong màn review, hiển thị giải thích AI | có loading/empty/error state |
| F4.3 | Cache giải thích theo câu hỏi để đỡ gọi lại tốn phí | lần 2 lấy từ cache |

> Cần chốt LLM provider + ngân sách. Mockup `giải_thích_bằng_ai` đã có sẵn trong thiết kế.

---

## F5 — Báo lỗi câu hỏi + Ticket (P1) — chính là D3 cũ

**Mục tiêu:** Vòng khép kín: học viên báo sai đáp án/nội dung → admin xử lý → đóng ticket. Tối thiểu để giữ chất lượng ngân hàng đề.

| ID | Hạng mục | Tiêu chí |
|----|----------|----------|
| F5.1 | Backend: entity `Ticket` + `POST /tickets` (student) + `GET/PATCH /admin/tickets` | role-based; trạng thái OPEN/IN_PROGRESS/CLOSED |
| F5.2 | Android student: nút "Báo lỗi câu hỏi" trong review/làm bài | gửi kèm questionId + mô tả |
| F5.3 | Android admin: danh sách ticket + cập nhật trạng thái/phản hồi | bám `AdminUserManagementFragment` làm mẫu |

Tham chiếu mockup: `v_sat_qu_n_tr_ticket_l_i_n_i_dung`, `v_sat_qu_n_tr_y_u_c_u_s_a_l_i`.

---

## F6 — Flashcard / spaced-repetition (P2)

Bộ thẻ từ vựng Tiếng Anh + công thức Toán/Lý, lặp lại ngắt quãng (SM-2 đơn giản). Bổ trợ luyện ngoài giờ làm đề. Local-first, sync tiến độ sau.

## F7 — Analytics tiến bộ (P2)

Biểu đồ xu hướng điểm qua các lần thi + phân tích thời gian/câu. **Dữ liệu đã có** trong `session_answers`, chỉ thiếu phần dựng API tổng hợp + màn hình biểu đồ.

## F8 — Nhắc học theo lịch (P2)

**Trạng thái (2026-06-18):** xong **local-only** (WorkManager). Push "đề mới" từ server qua FCM **defer** tới khi online thật.

- `app/.../notification/NotificationHelper.java` — tạo channel + post notification, xin quyền `POST_NOTIFICATIONS` (Android 13+).
- `app/.../notification/DailyReminderWorker.java` — Worker đọc streak từ `GamificationRepository` (F3) để dựng nội dung nhắc.
- `app/.../notification/ReminderScheduler.java` — `WorkManager.enqueueUniquePeriodicWork(1 ngày)`, initialDelay trỏ vào giờ user chọn.
- `app/.../util/NotificationPreferences.java` — SharedPreferences (enabled / hour / minute / streakReminder).
- `app/.../ui/settings/SettingsActivity.java` — màn cài đặt thông báo (toggle + time picker + streak copy + permission flow).
- `app/.../VsatApp.java` — `onCreate` gọi `ensureChannel` + `ReminderScheduler.apply`.
- `gradle/libs.versions.toml` + `app/build.gradle.kts` — thêm `androidx.work:work-runtime-ktx:2.9.1`.
- `AndroidManifest.xml` — thêm `POST_NOTIFICATIONS` permission + khai báo `SettingsActivity`.

### Nghiệm thu (local): bật nhắc giờ gần hiện tại → nhận notification dù app đóng; tắt → không nhận; từ chối quyền → không crash. `./gradlew :app:assembleDebug` PASS.

## F9 — Leaderboard / social (P2, tùy chọn)

**Trạng thái (2026-06-18):** xong opt-in + tên hiển thị. Mặc định ẩn.

- **Nguồn điểm:** tổng `exam_sessions.score` của các bài `SUBMITTED` — tận dụng dữ liệu backend sẵn có, không cần đẩy XP local lên server (XP của F3 vẫn ở local; khi nào cần thiết sẽ sync).
- **DDL mới:** `VSAT/vsat-compass-api/docs/sql/0002_add_leaderboard_opt_in.sql` — thêm cột `users.leaderboard_opt_in BOOLEAN NOT NULL DEFAULT FALSE`. Đã đánh dấu schema debt (xem phần cuối).
- **Backend:**
  - `entity/User.java` — thêm `leaderboardOptIn` (mặc định false, `@PrePersist` set null-safe).
  - `repository/UserScoreAggregate.java` (projection) + `ExamSessionRepository.aggregateScoresByUser()`.
  - `service/LeaderboardService` + `impl/LeaderboardServiceImpl` — `getTop` (filter opt-in, sort total_score desc, tie-breaker exam_count desc, rồi tên), `getMe`, `setOptIn`.
  - `controller/student/LeaderboardController.java` — `GET /leaderboard?limit=20` (role STUDENT).
  - `controller/auth/AuthController.java` — `GET /auth/me/leaderboard`, `PATCH /auth/me/leaderboard` (body `{optIn}`).
  - `dto/response/LeaderboardEntryResponse`, `LeaderboardMeResponse`; `dto/request/LeaderboardOptInRequest`.
  - `dto/response/AuthResponse.UserInfo` + `mapper/UserMapper` — expose `leaderboardOptIn`.
  - `config/SecurityConfig.java` — `.requestMatchers("/leaderboard/**").hasRole("STUDENT")` (rule cụ thể trước rule chung).
  - Test: `src/test/.../service/LeaderboardServiceTest.java` — 9 case (chỉ opt-in, sort theo điểm, tie-breaker, list rỗng, getMe opt-in true/false, setOptIn, null userId, user không tồn tại). PASS.
- **Android:**
  - `data/api/LeaderboardApi.java` — `getTop`, `getMe`, `setOptIn`.
  - `data/model/LeaderboardEntry`, `LeaderboardMe`, `LeaderboardOptInRequest`.
  - `data/repository/LeaderboardRepository.java`.
  - `ui/leaderboard/LeaderboardActivity.java` — top list + opt-in toggle (programmatic UI đồng nhất AdminTicketActivity), empty state, banner "Bạn đang ẩn khỏi bảng", highlight dòng của mình.
  - `res/layout/fragment_profile.xml` — thêm mục "Bảng xếp hạng" trong card cài đặt.
  - `AndroidManifest.xml` — khai báo `LeaderboardActivity`.

### Nghiệm thu (local): 2 user opt-in → cùng lên bảng theo thứ hạng; 1 user tắt → biến mất khỏi bảng; student gọi được, role khác theo convention. Backend test PASS. `./gradlew :app:assembleDebug` PASS.

---

## Thứ tự thực hiện đề xuất

1. **F1 (P0)** — làm trước, vì đúng yêu cầu trước mắt + vá luôn lỗi offline/màn đen.
2. **F3 + F5 (P1)** — gamification & ticket: nhỏ, tăng giá trị demo nhanh.
3. **F2 + F4 (P1)** — adaptive & AI giải thích: tạo khác biệt cạnh tranh.
4. **F6–F9 (P2)** — sau demo.

## Bước verify chung (mọi feature)

- Build: `./gradlew :app:assembleDebug` PASS; backend `./gradlew test` (test mới) PASS.
- Smoke local đúng luồng người dùng (đặc biệt F1: bay-mode/tắt mạng).
- Prod smoke + deploy: **defer tới khi Neon DB sống lại** (cùng batch với real-data closeout).

## Lưu ý đồng bộ tài liệu

- Khi một mục Phase F xong: tick trạng thái ở bảng tổng quan + thêm ngày; nếu thay đổi lớn ghi `CHANGELOG.md`.
- Đây là file kế hoạch; khi thực thi vẫn theo quy trình commit docs-only của `task.md` (không kéo config demo dirty, không co-author).

## Schema debt: F5 tickets (2026-06-18)

- Backend uses `spring.jpa.hibernate.ddl-auto=validate`, so new F5 schema is not auto-created.
- Local DB `vsat-pg` has been patched with `VSAT/vsat-compass-api/docs/sql/0001_create_tickets.sql`.
- Before deploying F5 to prod/Neon, run `0001_create_tickets.sql` on prod first; otherwise Hibernate validate will fail at boot because `tickets` is missing or incompatible.
- Consider adopting Flyway/Liquibase before the next schema-bearing feature so schema changes are versioned and applied consistently.

## Schema debt: F9 leaderboard (2026-06-18)

- New column `users.leaderboard_opt_in BOOLEAN NOT NULL DEFAULT FALSE`.
- Migration file: `VSAT/vsat-compass-api/docs/sql/0002_add_leaderboard_opt_in.sql`.
- Local DB `vsat-pg` must be patched with this file before booting the backend; otherwise Hibernate `ddl-auto=validate` will fail with "column leaderboard_opt_in not found".
- Order to apply on prod: `0001_create_tickets.sql` first, then `0002_add_leaderboard_opt_in.sql`.
