# Prompt cho Cursor/Codex — Fix 2 blocker từ E2E (F9 500 + F1 ANR)

> Copy toàn bộ phần dưới dán vào agent. Sau fix phải **chạy lại** đúng 2 case này + smoke liên quan.

---

## NGỮ CẢNH

Repo **V-SAT Compass** (Android `app/` + backend `VSAT/vsat-compass-api/`). Vòng E2E (`temp/E2E_REPORT.md`) phát hiện 2 blocker (+1 cosmetic). Cần fix để E2E xanh trước khi commit. Môi trường local demo: Postgres Docker `vsat-pg`, backend dev `.env.local`, JBR `JAVA_HOME=C:\Program Files\Android\Android Studio\jbr`, `ddl-auto=validate`.

**Ràng buộc chung:** KHÔNG commit/tag/deploy; KHÔNG in key/token; KHÔNG đổi `.env.local`; KHÔNG revert/commit file dirty cũ; nếu commit sau này **không co-author**; không đổi `ddl-auto`. Sửa đúng gốc, không che lỗi.

---

## BLOCKER 1 — F9 leaderboard trả 500 (đã chốt nguyên nhân)

**Triệu chứng:** `GET /leaderboard` + `PATCH /auth/me/leaderboard` → 500. Log:
```
where es1_0.status='SUBMITTED'::SessionStatus and es1_0.score is not null
ERROR: type "sessionstatus" does not exist
```
Tại `ExamSessionRepository.aggregateScoresByUser()` (gọi từ `LeaderboardServiceImpl`).

**Nguyên nhân thật:** entity `ExamSession.status` khai báo:
```java
@Enumerated(EnumType.STRING)
@Column(nullable = false, columnDefinition = "session_status")
private SessionStatus status;
```
`columnDefinition = "session_status"` khiến Hibernate coi cột là kiểu enum Postgres và sinh cast `::SessionStatus` khi so sánh `status = :enumParam` trong JPQL. Nhưng DB **không có** kiểu enum đó — cột thực chất là varchar (lưu STRING). Đây là JPQL đầu tiên lọc `status` qua SQL nên mới lộ.

**Yêu cầu:**
1. Xác nhận kiểu cột thật: `psql ... -c "\d exam_sessions"` (kỳ vọng `status` là `varchar/text`, không phải enum type). Dán kết quả.
2. Fix `aggregateScoresByUser` để **không sinh cast enum**. Hướng ưu tiên (chọn cái sạch nhất, không đổi hành vi khác):
   - Chuyển sang **native query** so sánh chuỗi literal: `WHERE status = 'SUBMITTED' AND score IS NOT NULL` (cột varchar → không cast), `nativeQuery = true`, map về projection `UserScoreAggregate` (đảm bảo tên cột khớp).
   - Hoặc JPQL so sánh theo string: `WHERE CAST(es.status AS string) = :statusName` với param String `"SUBMITTED"`.
3. Cân nhắc gốc rễ: nếu `columnDefinition = "session_status"` là thừa/sai (cột vốn varchar), việc bỏ nó là sạch hơn — **nhưng** với `ddl-auto=validate` phải đảm bảo không vỡ validate; nếu rủi ro thì giữ entity, chỉ sửa query (an toàn hơn cho demo). Nêu rõ lựa chọn + lý do.
4. Đảm bảo `LeaderboardServiceTest` vẫn xanh; thêm/điều chỉnh test nếu cần.

**Verify lại:** với ≥2 student opt-in + 1 tắt → `GET /leaderboard` trả 200, chỉ gồm opt-in, sort tổng `exam_sessions.score` giảm dần; `PATCH /auth/me/leaderboard {optIn:false}` → biến mất. `./gradlew test` PASS.

---

## BLOCKER 2 — F1 offline download gây ANR (cần bắt trace để chốt)

**Triệu chứng (E2E):** đăng nhập student → Luyện tập/kho đề → bấm **"Tải xuống"** đề FREE (sau khi làm/review 1 bài) → dialog `V-SAT_COMPASS isn't responding`. ANR component: `ui.exam.session.ExamResultActivity`, "Input dispatching timed out, Waited 5001ms".

**Đã loại trừ khi đọc tĩnh:** `OfflineExamRepository.downloadExam` chạy trên background executor và post kết quả qua `mainHandler`; `ExamResultActivity.onCreate` dùng `loadWeakTopics()` async + vòng lặp nhỏ — không thấy block hiển nhiên. ⇒ **phải lấy stack trace main thread lúc ANR** mới chốt đúng.

**Yêu cầu:**
1. **Bắt ANR trace** (đây là bước quyết định, làm trước khi sửa):
   - Tái hiện trên emulator, rồi `adb pull /data/anr/ temp/anr/` (hoặc `adb bugreport temp/bugreport.zip`), mở trace tìm **stack của main thread ("main" tid=1)** tại thời điểm ANR. Dán đoạn stack chính vào report.
2. Từ stack, xác định **frame chặn main thread** và sửa: chuyển công việc nặng/đồng bộ sang background (Executor) rồi post kết quả về UI. Các nghi vấn để kiểm:
   - Lời gọi mạng **đồng bộ `.execute()` trên main thread** ở đâu đó trong luồng download/offline-result/sync (vd `OfflineExamSyncRepository` flush sync, `StudentStatsRepository.loadTopicStatsSync()`, hay một repo gọi thẳng trên UI).
   - Đọc/giải mã bundle (AES) hoặc I/O file chạy nhầm trên main thread.
   - Vòng lặp/`wait`/khóa đồng bộ trên main thread.
3. Sửa để thao tác đó chạy nền + cập nhật UI qua main handler/`runOnUiThread`; giữ nguyên hành vi chức năng.

**Verify lại (LOCAL, bắt buộc trên máy/emulator):** tải 1 đề FREE (không ANR) → **airplane mode** → làm trọn bài, rotate + background/resume giữ state → kết quả + review offline → tắt airplane → kết quả tự sync, **không tạo session trùng** (đối chiếu `exam_sessions`). Logcat không còn ANR.

---

## BLOCKER 3 (cosmetic, tùy chọn) — thiếu dấu tiếng Việt
Vài chuỗi Phase F thiếu dấu: `Dong luc hom nay`, `Giai thich them`, `Tien trinh hoc tap`... Nếu nhanh thì sửa trong `strings.xml` cho đúng tiếng Việt có dấu; không phải blocker.

## BÁO CÁO KHI XONG
- Blocker 1: kết quả `\d exam_sessions`, cách fix query, verify leaderboard 200 + test PASS.
- Blocker 2: stack main-thread ANR nguyên văn → frame chặn → cách fix → verify offline airplane-mode không ANR + sync không trùng.
- File đã sửa; xác nhận không đụng file dirty cũ / không commit.
