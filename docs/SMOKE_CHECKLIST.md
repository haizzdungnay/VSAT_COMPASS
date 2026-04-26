# V-SAT Compass — Smoke Test Checklist (Student MVP)

> Phiên bản: v0.8.1 | Cập nhật: 2026-04-26
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

## Tổng kết

| Tổng TC | Pass | Fail | Bỏ qua |
|---------|------|------|--------|
| 25      | 10 backend TCs (TC-016→TC-025) verified 2026-04-25 | 0 | 0 |

**Ghi chú lần chạy (Phase B — Backend TCs):**
- Ngày: 2026-04-25
- Môi trường: Production (`https://vsat-compass-api.onrender.com/api/v1/`)
- Build version: v0.8.1 (commit `727f9a4`)
- Tester: smoke_auth.sh (9/9) + smoke_sessions.sh (5/5) tự động
- Ghi chú: TC-024 (rate limit) và TC-025 (timestamp) được verify thủ công qua curl
