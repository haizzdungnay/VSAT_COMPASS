# Prompt cho Codex — Chạy một vòng E2E trên máy/emulator (chỉ TEST + BÁO CÁO)

> Agent tự dựng môi trường, chạy kịch bản, thu log/screenshot, xuất báo cáo. **KHÔNG fix, KHÔNG commit.**

---

## MỤC TIÊU

Chạy một vòng kiểm thử end-to-end cho **V-SAT Compass** (Android + backend Spring Boot) ở chế độ **local demo**, phủ luồng lõi + toàn bộ tính năng Phase F (F1–F9, trừ AI chấm thi không có). Phát hiện lỗi/crash/regressions và **xuất báo cáo pass/fail**. Đây là vòng kiểm thử trước khi commit — **không sửa code, không commit/tag/deploy**.

## NGỮ CẢNH DỰ ÁN

- Repo gốc: `E:\TuanDuong\lap trinh kiem com\Java\VSAT_COMPASS`
- Backend: `VSAT/vsat-compass-api/` (Spring Boot, JDK qua JBR: `JAVA_HOME=C:\Program Files\Android\Android Studio\jbr`). DB Postgres local Docker container `vsat-pg` / db `vsat_compass` / user-pass `postgres/postgres`. `ddl-auto=validate` → phải áp DDL tay.
- Android: module `app/` (debug build dùng backend local qua flag `USE_LOCAL_BACKEND` + `LOCAL_LAN_HOST`).
- LLM cho F4: config trong `VSAT/vsat-compass-api/.env.local` (đã .gitignore) — base `http://localhost:20128/v1`, model `gemini/gemini-2.5-flash-lite`. **KHÔNG in key, KHÔNG đổi `.env.local`.**
- Tài khoản seed (local): student `student@vsat.com`, content admin `content@vsat.com`, super admin (xem seed). Nếu thiếu, tạo thêm qua `/auth/register` + cập nhật role bằng SQL.

## BƯỚC 0 — Dựng môi trường

1. Bật Docker `vsat-pg`; áp DDL **đúng thứ tự**:
   ```
   psql -h localhost -U postgres -d vsat_compass -f VSAT/vsat-compass-api/docs/sql/0001_create_tickets.sql
   psql -h localhost -U postgres -d vsat_compass -f VSAT/vsat-compass-api/docs/sql/0002_add_leaderboard_opt_in.sql
   ```
2. Boot backend dev (nạp `.env.local`), bind LAN cho thiết bị thật: `--server.address=0.0.0.0`. Xác nhận `GET /api/v1/actuator/health` = UP, không lỗi schema validate.
3. Phát hiện target Android: `adb devices`. Nếu chưa có, khởi động 1 emulator AVD. Ghi rõ dùng emulator hay thiết bị thật.
   - Emulator trỏ host backend qua `10.0.2.2`; thiết bị thật trỏ IP LAN của máy. Cấu hình `LOCAL_LAN_HOST` phù hợp (chỉ ở bản debug, KHÔNG commit).
4. Build + cài: `gradlew.bat :app:assembleDebug` rồi `adb install -r` APK debug. Bật logcat ghi ra `temp/e2e-logcat.txt`.

## BƯỚC 1 — Backend API E2E (scriptable, dùng curl + token)

Lấy token qua `/auth/login`. Với mỗi case ghi status + tóm tắt body. **Che key/token trong báo cáo.**

- Auth: login student/admin OK; `/auth/me` có `leaderboardOptIn`.
- Exams: `GET /exams` list PUBLISHED; `GET /exams/{id}` detail.
- **F1** `GET /exams/{id}/download-bundle`: đề FREE+PUBLISHED (token STUDENT) → 200 có câu hỏi + đáp án; đề non-FREE/không tồn tại → 404; gọi không token → 401/403.
- Sessions: `POST /sessions/start` → 201; `POST /sessions/{id}/submit` (hoặc client-submit) → 200; nộp lại → anti-replay 409.
- Stats: `/my-stats/topics`, `/my-stats/weak-topics` → 200.
- **F2** `GET /practice/questions?...` → câu **không có** đáp án/explanation; `POST /practice/grade` → đúng/sai + explanation câu vừa làm.
- **F4** `POST /ai/explain` (STUDENT): router sống → 200 có explanation; mô phỏng router chết (`--LLM_BASE_URL=http://localhost:20129/v1`) → **503 message rõ**, không 500 trống.
- **F5** Ticket: student `POST /tickets` → OPEN; admin `GET /admin/tickets?status=OPEN` thấy; `PATCH /admin/tickets/{id}` OPEN→IN_PROGRESS→CLOSED; student gọi `/admin/tickets` bị chặn; sai role bị chặn.
- Admin: `GET /admin/stats`, `GET /admin/users` (role đúng).
- **F9** Leaderboard: ≥2 student opt-in + 1 tắt → `GET /leaderboard` chỉ gồm opt-in, sort tổng `exam_sessions.score` giảm dần; `PATCH /auth/me/leaderboard {optIn:false}` → biến mất khỏi bảng.

## BƯỚC 2 — Android UI E2E (trên emulator/thiết bị)

Drive qua adb; chụp `adb exec-out screencap` vào `temp/e2e-shots/` ở các bước chính; bắt mọi crash/ANR trong logcat.

- Đăng nhập student → Home (streak/XP/daily challenge **F3** hiển thị).
- Kho đề → mở 1 đề → **làm bài online**: start session, trả lời, nộp, xem kết quả + review. **Kiểm tra lỗi màn đen "Làm bài" có còn không.**
- Review câu → **F4** "Giải thích thêm": loading → có giải thích (router sống) / báo lỗi gọn (router chết). Không crash.
- **F1 OFFLINE (quan trọng — chưa từng test máy):** tải 1 đề FREE khi còn mạng → **bật airplane mode** → vào làm trọn bài (≥ vài câu, lý tưởng ~50 câu), **rotate màn hình + chuyển nền/quay lại** giữ state → xem kết quả + review offline → **tắt airplane mode** → kết quả tự sync, không tạo session trùng (đối chiếu DB `exam_sessions`).
- **F2** Luyện thông minh từ Practice: độ khó câu thay đổi theo đúng/sai.
- **F6** Flashcard: ôn vài thẻ (Again/Good), thoát/mở lại app giữ tiến độ.
- **F7** Tiến bộ: sau vài bài, màn Progress hiển thị biểu đồ; chưa dữ liệu → empty state.
- **F8** Profile → Thông báo: bật nhắc + chọn giờ gần hiện tại → nhận notification kể cả khi kill app; từ chối quyền POST_NOTIFICATIONS → không crash.
- **F9** Profile → Bảng xếp hạng: render top + toggle opt-in (tắt → mình ẩn).
- Phân quyền: tài khoản SUPER_ADMIN thấy admin center (dashboard, user mgmt, ticket); student không thấy.

## BÁO CÁO (xuất ra `temp/E2E_REPORT.md`)

- Môi trường: emulator/thiết bị, backend host, kết quả health, DDL đã áp.
- Bảng pass/fail từng case (Bước 1 + Bước 2) kèm status/ghi chú.
- Danh sách **bug/crash phát hiện** (kèm trích logcat + đường dẫn screenshot) — mô tả tái hiện, **KHÔNG tự fix**.
- Kết luận: sẵn sàng commit hay còn blocker.

## RÀNG BUỘC
- **KHÔNG sửa code, KHÔNG commit/tag/deploy.** Chỉ test + báo cáo. (Cấu hình debug `LOCAL_LAN_HOST` nếu phải chỉnh thì để ở working tree, không commit.)
- **KHÔNG in API key/token** ra báo cáo/log; che bằng `***`. KHÔNG đổi `.env.local`.
- KHÔNG revert/commit file dirty cũ. Áp DDL chỉ trên DB **local**.
- Nếu không khởi động được emulator/thiết bị: vẫn chạy trọn Bước 1 (API E2E) + build/install check, và ghi rõ phần UI không chạy được + lý do.
