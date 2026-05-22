# V-SAT Compass — Smoke Test Checklist (Student MVP)

> Phiên bản: v0.9.2 | Cập nhật: 2026-05-05
> Chạy checklist này trước mỗi release hoặc sau mỗi thay đổi lớn ảnh hưởng đến student flow.
> Thiết bị test tối thiểu: 1 emulator (API 28+) + 1 thiết bị vật lý.

---

## Môi trường

| Hạng mục | Giá trị |
|----------|---------|
| minSdk | 28 |
| targetSdk | 36 |
| Backend bắt buộc | Auth module (login/register) |
| Backend tùy chọn | Exam/Session (fallback về local) |
| Test account | `student@vsat.com` / `Student@123` |

---

## TC-001: Cài app mới + Đăng nhập thành công

**Tiền điều kiện:** App mới cài, chưa có dữ liệu  
**Bước:**
1. Mở app → SplashActivity tự redirect sang LoginActivity
2. Nhập `student@vsat.com` / `Student@123` → nhấn Đăng nhập

**Kỳ vọng:**
- Chuyển sang HomeFragment, hiển thị lời chào theo buổi
- 3 stat cards hiển thị "0", "--", "0ph" (chưa có lịch sử)

**Pass/Fail:** [ ]

---

## TC-002: Danh sách 5 đề local hiển thị đúng

**Tiền điều kiện:** Đã đăng nhập  
**Bước:**
1. Mở tab "Kho đề" → ExamFragment

**Kỳ vọng:**
- Hiển thị đúng 5 đề: Toán đề 1, Toán đề 2, Anh đề 1, Anh đề 2, Vật lí đề 1
- Không crash, không loading spinner vô hạn

**Pass/Fail:** [ ]

---

## TC-003: Filter chip theo môn

**Tiền điều kiện:** Đang ở ExamFragment  
**Bước:**
1. Nhấn chip "Toán" → đếm số đề
2. Nhấn chip "Tiếng Anh" → đếm số đề
3. Nhấn chip "Vật lí" → đếm số đề
4. Nhấn chip "Tất cả" → đếm số đề

**Kỳ vọng:**
- "Toán" → 2 đề
- "Tiếng Anh" → 2 đề
- "Vật lí" → 1 đề
- "Tất cả" → 5 đề

**Pass/Fail:** [ ]

---

## TC-004: Làm bài thi + nộp bài

**Tiền điều kiện:** Đang ở ExamFragment  
**Bước:**
1. Chọn "Đề Toán nâng cao số 2" → ExamDetailActivity → "Bắt đầu thi ngay"
2. Trả lời 5 câu đúng, 3 câu sai, bỏ trống 2 câu
3. Nhấn "Nộp bài" từ nút Next hoặc grid → xác nhận

**Kỳ vọng:**
- ExamResultActivity hiện điểm V-SAT (thang 1200)
- Số câu đúng khớp (5)
- Thời gian làm bài > 0
- Nút "Xem lời giải chi tiết" có thể nhấn

**Pass/Fail:** [ ]

---

## TC-005: Xem lời giải chi tiết

**Tiền điều kiện:** Đang ở ExamResultActivity sau khi nộp bài TC-004  
**Bước:**
1. Nhấn "Xem lời giải chi tiết" → ExamReviewActivity
2. Kiểm tra màu option từng câu đã trả lời đúng/sai
3. Kiểm tra lời giải hiển thị
4. Điều hướng ← Câu trước / Câu tiếp →
5. Nhấn "Tất cả câu" → grid → nhấn câu thứ 5

**Kỳ vọng:**
- Option đúng + chọn đúng → xanh #E8F5E9, icon ✓
- Option sai + đã chọn → đỏ nhạt, icon ✗ + "Của bạn"
- Câu bỏ trống → badge cam "Bạn chưa trả lời câu này"
- Lời giải có nội dung (không blank)
- Nhảy đúng câu khi tap grid

**Pass/Fail:** [ ]

---

## TC-006: Lịch sử bài làm persist sau kill app

**Tiền điều kiện:** Vừa hoàn thành TC-004 (đã nộp ít nhất 1 bài)  
**Bước:**
1. Force-kill app từ task manager
2. Mở lại app → đăng nhập
3. Vào HomeFragment → xem stat cards
4. Nhấn "Xem tất cả" → ExamHistoryActivity

**Kỳ vọng:**
- Stat "Bài thi" = 1 (ít nhất)
- ExamHistoryActivity hiển thị entry bài vừa làm
- Ngày giờ hiển thị tương đối ("vừa xong" hoặc "X phút trước")

**Pass/Fail:** [ ]

---

## TC-007: Xem lại bài cũ từ lịch sử

**Tiền điều kiện:** ExamHistoryActivity có ít nhất 1 entry  
**Bước:**
1. Mở ExamHistoryActivity
2. Nhấn nút "Xem lại" trên 1 entry
3. Điều hướng qua vài câu

**Kỳ vọng:**
- ExamReviewActivity mở đúng đề của entry đó
- Đáp án đã chọn còn hiển thị đúng màu (xanh/đỏ)
- Không crash

**Pass/Fail:** [ ]

---

## TC-008: Filter lịch sử theo môn

**Tiền điều kiện:** ExamHistoryActivity có entries từ nhiều môn khác nhau  
**Bước:**
1. Nhấn chip "Toán"
2. Nhấn chip "Tiếng Anh"
3. Nhấn chip "Hóa học" (nếu không có bài Hóa)
4. Nhấn "Tất cả"

**Kỳ vọng:**
- Chip "Toán" → chỉ hiện entry môn Toán
- Chip "Tiếng Anh" → chỉ hiện entry môn Tiếng Anh
- Chip không có dữ liệu → empty state "Không có bài thi nào ở môn này"
- "Tất cả" → hiện lại toàn bộ

**Pass/Fail:** [ ]

---

## TC-009: Rotate device giữ nguyên câu đang xem

**Tiền điều kiện:** Đang ở ExamReviewActivity, đang xem câu 5  
**Bước:**
1. Rotate device sang landscape
2. Rotate ngược lại về portrait

**Kỳ vọng:**
- Sau mỗi lần rotate vẫn ở câu 5
- Counter "5/30" không bị reset

**Pass/Fail:** [ ]

---

## TC-010: Rotate device giữ filter History

**Tiền điều kiện:** ExamHistoryActivity đang filter "Toán"  
**Bước:**
1. Rotate device sang landscape

**Kỳ vọng:**
- Chip "Toán" vẫn được chọn sau rotate
- List vẫn hiện đề môn Toán

**Pass/Fail:** [ ]

---

## TC-011: Empty state khi chưa có lịch sử

**Tiền điều kiện:** Xóa lịch sử qua dev menu (long-press tên trong Profile → Xóa toàn bộ lịch sử)  
**Bước:**
1. Vào HomeFragment → xem stat cards
2. Nhấn "Xem tất cả" → ExamHistoryActivity

**Kỳ vọng:**
- HomeFragment: stat "Bài thi" = 0, Điểm TB = "--", Thời gian = "0ph"
- ExamHistoryActivity: hiện empty state "Chưa có bài thi nào. Hãy thử đề đầu tiên nhé!"
- Không crash, không hiện RecyclerView rỗng

**Pass/Fail:** [ ]

---

## TC-012: Dashboard cập nhật sau khi làm thêm bài

**Tiền điều kiện:** Đã có 1 entry trong lịch sử  
**Bước:**
1. Làm thêm 1 bài thi khác → nộp
2. Quay về HomeFragment

**Kỳ vọng:**
- Stat "Bài thi" tăng lên 2
- Điểm TB cập nhật theo trung bình 2 bài
- Section "Tiếp tục luyện tập" hiện tên đề vừa làm

**Pass/Fail:** [ ]

---

## TC-013: Dev menu chỉ tồn tại ở debug build

**Tiền điều kiện:** Debug APK đã cài  
**Bước:**
1. Vào ProfileFragment → long-press vào tên hiển thị
2. Chọn "Inject 50 lịch sử mock"
3. Vào ExamHistoryActivity

**Kỳ vọng:**
- Dialog dev menu xuất hiện
- Sau inject: History có 50+ entries, scroll mượt không lag
- Release APK: long-press không làm gì cả

**Pass/Fail (debug):** [ ]  
**Pass/Fail (release build — long-press no-op):** [ ]

---

## TC-014: Sync status khi làm bài

**Tiền điều kiện:** Đang làm bài trong ExamSessionActivity  
**Bước:**
1. Tắt Wi-Fi/4G → bắt đầu bài thi
2. Kiểm tra nhãn sync ở top bar

**Kỳ vọng:**
- Nhãn hiển thị "Sync: local-only" khi không có backend
- App vẫn cho làm bài bình thường, không block

**Pass/Fail:** [ ]

---

## TC-015: Thoát giữa chừng bài thi

**Tiền điều kiện:** Đang làm bài thi ở câu 5/30  
**Bước:**
1. Nhấn nút Back hoặc nút ← trên toolbar
2. Xem dialog → nhấn "Tiếp tục thi"
3. Nhấn Back lại → nhấn "Thoát không nộp"

**Kỳ vọng:**
- Dialog hỏi xác nhận thoát xuất hiện
- "Tiếp tục thi" → đóng dialog, tiếp tục ở câu 5
- "Thoát không nộp" → finish activity, không lưu lịch sử
- "Nộp và thoát" → submit, chuyển sang ExamResultActivity

**Pass/Fail:** [ ]

---

---

# Backend Smoke Tests (v0.8.0)

> Bổ sung từ Phase B. Chạy trực tiếp bằng `docs/scripts/smoke_auth.sh` và `smoke_sessions.sh`,
> hoặc kiểm tra thủ công từng TC dưới đây.
>
> **Production-verified: 2026-04-25 ✅** — TC-016 đến TC-025 đã pass 14/14 trên production
> (`https://vsat-compass-api.onrender.com/api/v1/`). Xem kết quả đầy đủ trong `CHANGELOG.md [0.8.1]`.

---

## TC-016: Health endpoint phản hồi

**Bước:**
1. `curl https://vsat-compass-api.onrender.com/api/v1/actuator/health`

**Kỳ vọng:** HTTP 200, body chứa `{"status":"UP"}`

**Pass/Fail:** [ ]

---

## TC-017: Login trả token hợp lệ

**Bước:**
1. POST `/auth/login` với `student@vsat.com` / `Student@123`

**Kỳ vọng:** HTTP 200, body có `accessToken`, `refreshToken`, `user.email`

**Pass/Fail:** [ ]

---

## TC-018: Login sai mật khẩu trả 401 + error code

**Bước:**
1. POST `/auth/login` với email đúng, password sai

**Kỳ vọng:** HTTP 401, `error.code` = `AUTH_INVALID_CREDENTIALS`

**Pass/Fail:** [ ]

---

## TC-019: Register email trùng trả 409

**Bước:**
1. POST `/auth/register` với email đã tồn tại

**Kỳ vọng:** HTTP 409, `error.code` = `AUTH_EMAIL_TAKEN`

**Pass/Fail:** [ ]

---

## TC-020: Refresh token hợp lệ

**Bước:**
1. Login → lấy refreshToken
2. POST `/auth/refresh` với refreshToken

**Kỳ vọng:** HTTP 200, body có `accessToken` mới

**Pass/Fail:** [ ]

---

## TC-021: GET /auth/me với Bearer hợp lệ

**Bước:**
1. Login → lấy accessToken
2. GET `/auth/me` với `Authorization: Bearer <token>`

**Kỳ vọng:** HTTP 200, body có `user.email`, `user.role`

**Pass/Fail:** [ ]

---

## TC-022: Session start + client-submit

**Bước:**
1. Login → lấy accessToken
2. POST `/sessions/start` → lấy sessionId
3. POST `/sessions/{sessionId}/client-submit` với score, correctCount, totalQuestions, timeSpentSeconds

**Kỳ vọng:**
- Start: HTTP 201, `status` = `IN_PROGRESS`
- Submit: HTTP 200, `status` = `SUBMITTED`

**Pass/Fail:** [ ]

---

## TC-023: Anti-replay: submit lần 2 trả 409

**Bước:**
1. Sau TC-022, gọi lại `/sessions/{sessionId}/client-submit` cùng body

**Kỳ vọng:** HTTP 409, `error.code` = `SESSION_ALREADY_SUBMITTED`

**Pass/Fail:** [ ]

---

## TC-024: Rate limiting trên /auth/login

**Bước:**
1. Gửi 11 request login liên tiếp trong 1 phút

**Kỳ vọng:** Request thứ 11 trả HTTP 429, `error.code` = `RATE_LIMIT_EXCEEDED`

**Pass/Fail:** [ ]

---

## TC-025: Response có timestamp

**Bước:**
1. Gọi bất kỳ endpoint nào (ví dụ: login thành công)

**Kỳ vọng:** Response body có field `timestamp` dạng ISO-8601

**Pass/Fail:** [ ]

---

## TC-026: Client-submit với sessionId không tồn tại trả 404

**Bước:**
1. Login → lấy accessToken
2. POST `/sessions/999999999/client-submit` với body hợp lệ

**Kỳ vọng:** HTTP 404, `error.code` = `RESOURCE_NOT_FOUND`

**Pass/Fail:** [ ]

---

## TC-027: Client-submit payload không hợp lệ (correctCount > totalQuestions) trả 400

**Bước:**
1. Login → lấy accessToken
2. POST `/sessions/start` → lấy sessionId (trạng thái IN_PROGRESS)
3. POST `/sessions/{sessionId}/client-submit` với `correctCount: 50`, `totalQuestions: 10` (và các field hợp lệ)

**Kỳ vọng:** HTTP 400, `error.code` = `VALIDATION_FAILED`

**Pass/Fail:** [ ]

---

## Tổng kết

| Tổng TC | Pass | Fail | Bỏ qua |
|---------|------|------|--------|
| 33      | 10 backend TCs (TC-016→TC-025) verified 2026-04-25; TC-026 và TC-027 added C1.2d-1b; TC-028→TC-032 added C1.6-A; TC-033 added C1.6-A.5 (not yet production-verified) | 0 | 0 |

**Ghi chú lần chạy (Phase B — Backend TCs):**
- Ngày: 2026-04-25
- Môi trường: Production (`https://vsat-compass-api.onrender.com/api/v1/`)
- Build version: v0.8.1 (commit `727f9a4`)
- Tester: smoke_auth.sh (9/9) + smoke_sessions.sh (5/5) tự động
- Ghi chú: TC-024 (rate limit) và TC-025 (timestamp) được verify thủ công qua curl

---

## TC-028: TC-SESSION-8 — In-session question endpoint strips answer keys

**Bước:**
1. Login → lấy accessToken
2. POST `/sessions/start` → lấy sessionId trạng thái IN_PROGRESS
3. GET `/sessions/{sessionId}/questions/{questionId}`

**Kỳ vọng:**
- HTTP 200
- Response có `options[].content`
- Response không có `isCorrect`, `correctAnswer`, hoặc `explanation`

**Pass/Fail:** [ ]

---

## TC-029: TC-SESSION-9 — In-session question endpoint rejects non-owner

**Bước:**
1. Tạo session bằng student account
2. GET `/sessions/{sessionId}/questions/{questionId}` bằng Bearer của user khác

**Kỳ vọng:** HTTP 403, không trả nội dung câu hỏi

**Pass/Fail:** [ ]

---

## TC-030: TC-SESSION-10 — Answer keys endpoint works after SUBMITTED

**Bước:**
1. POST `/sessions/start`
2. POST `/sessions/{sessionId}/client-submit`
3. GET `/sessions/{sessionId}/answer-keys`

**Kỳ vọng:**
- HTTP 200
- Response có `questions[].correctOptionIds`
- Response có `questions[].explanation` nếu câu hỏi có lời giải

**Pass/Fail:** [ ]

---

## TC-031: TC-SESSION-11 — Answer keys endpoint rejects IN_PROGRESS

**Bước:**
1. POST `/sessions/start`
2. GET `/sessions/{sessionId}/answer-keys` trước khi client-submit

**Kỳ vọng:** HTTP 400, `error.code` = `BAD_REQUEST`

**Pass/Fail:** [ ]

---

## TC-032: TC-SESSION-12 — Answer keys endpoint rejects non-owner

**Bước:**
1. Tạo và submit session bằng student account
2. GET `/sessions/{sessionId}/answer-keys` bằng Bearer của user khác

**Kỳ vọng:** HTTP 403, không trả correctOptionIds

**Pass/Fail:** [ ]

---

## TC-033: TC-SESSION-13 — Session start returns orderedQuestionIds for composed exam

**Bước:**
1. Login → lấy accessToken
2. POST `/sessions/start` với `EXAM_ID=6`
3. Kiểm tra `data.orderedQuestionIds`

**Kỳ vọng:**
- HTTP 201
- `data.orderedQuestionIds` không null
- `data.orderedQuestionIds` không rỗng
- Ghi chú: `EXAM_ID=1` / SMOKE_001 có thể trả `orderedQuestionIds` rỗng vì exam metadata có `questionCount` nhưng hiện có zero `exam_question` rows.

**Pass/Fail:** [ ]

---

# Backend Smoke Scripts (v0.9.4)

Run these production smoke scripts before release tagging:

- `SMOKE_AUTH_SKIP_REGISTER=1 bash docs/scripts/smoke_auth.sh` — expected 7 pass / 0 fail / 2 skipped.
- `bash docs/scripts/smoke_subjects.sh` — expected 4/4 PASS.
- `bash docs/scripts/smoke_sessions.sh` — expected 13/13 PASS. _(From C1.6-A: script expanded from 7 to 12 cases; C1.6-A.5 adds TC-SESSION-13 for `orderedQuestionIds`. Historical production-verified: 5/5 PASS for v0.9.4 on 2026-05-06. TC-SESSION-6 through TC-SESSION-13 not yet production-verified.)_
- `SMOKE_ADMIN_PASSWORD=... bash docs/scripts/smoke_admin_exams.sh` — expected 12/12 PASS for admin exam metadata CRUD plus DRAFT discard.
- `SMOKE_COLLAB1_PASSWORD=... SMOKE_COLLAB2_PASSWORD=... SMOKE_ADMIN_PASSWORD=... bash VSAT/vsat-compass-api/docs/scripts/smoke_questions.sh` — expected 32/32 PASS.
- `EXAM_ID=2 bash VSAT/vsat-compass-api/docs/scripts/smoke_exams.sh` — expected 10/10 PASS.
- `bash docs/scripts/smoke_admin_exam_composition.sh` — expected 17 PASS / 0 FAIL / 0 SKIP / 0 BLOCKED when production has at least three APPROVED/PUBLISHED question fixtures.

`smoke_admin_exams.sh` DRAFT discard safety contract:
- The script creates its own DRAFT exam for the discard check.
- It calls `DELETE /admin/exams/{id}` only for that smoke-created DRAFT exam.
- It verifies the follow-up `GET /admin/exams/{id}` returns `404 RESOURCE_NOT_FOUND`.
- It must not delete any pre-existing production exam.

Production-verified for v0.9.4 on 2026-05-06:

| Script | Result |
|--------|--------|
| `smoke_auth.sh` no-register | 7 pass / 0 fail / 2 skipped |
| `smoke_subjects.sh` | 4/4 PASS |
| `smoke_sessions.sh` | 5/5 PASS |
| `smoke_questions.sh` | 32/32 PASS |
| `smoke_exams.sh` | 10/10 PASS |
| `smoke_admin_exams.sh` | 12/12 PASS, including DRAFT discard |
| `smoke_admin_exam_composition.sh` | 17 pass / 0 fail / 0 skip / 0 blocked |

v0.9.4 deploy verification also passed warm-up and stability probes:

- `/actuator/health`: 200.
- `/exams`: 200.
- authenticated `/admin/exams`: 200.
- Stability watch: 5/5 rounds all 200.

Additional C1.2b-2 regression script:

- `SMOKE_QUESTION_IDS="1,2,3" bash docs/scripts/smoke_admin_exam_composition.sh` - covers admin exam composition, workflow, public visibility, republish audit overwrite, and ARCHIVED rejection. The script attempts to auto-discover APPROVED/PUBLISHED question IDs and exits BLOCKED instead of false FAIL when fixtures are missing. Production PASS for v0.9.4: 17 pass / 0 fail / 0 skip / 0 blocked.

### Runner prerequisites (Phase C1.2b-3)

- **Line endings:** all scripts under `docs/scripts/*.sh` are pinned to LF via `.gitattributes` (`*.sh text eol=lf`). Fresh clones on Windows check out LF without manual conversion.
- **`jq` (recommended):** all exam-family scripts auto-detect `jq` and fall back to `grep`/`sed` JSON parsing when missing. The public exam runner validates that `jq` is runnable, not only present on `PATH`. The runner header reports `JSON parser: jq` vs. `JSON parser: grep/sed fallback`.
- **No-`jq` fallback contract** (see `docs/DEPLOY_RUNBOOK.md` → "Smoke Script jq Fallback" for full detail):
  - `smoke_admin_exams.sh` — self-discovers subject id; no env override required.
  - `smoke_admin_exam_composition.sh` — set `SMOKE_QUESTION_IDS="<id1>,<id2>,<id3>"` if three APPROVED/PUBLISHED fixtures cannot be auto-resolved.
  - `VSAT/vsat-compass-api/docs/scripts/smoke_exams.sh` — set `EXAM_ID=<seeded-public-exam-id>` explicitly (the grep/sed fallback cannot reliably pick the smoke exam from a paginated `/exams` response). The script logs `EXAM_ID` override use and exits with an actionable message if `jq` is unavailable and `EXAM_ID` is missing. Production v0.9.3 used `EXAM_ID=2`.
