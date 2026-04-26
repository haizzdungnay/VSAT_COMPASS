# V-SAT Compass — Task Tracker & Roadmap

> Cập nhật: 2026-04-26 | Phiên bản hiện tại: **v0.8.1**

Tài liệu này tổng hợp lộ trình hoàn thiện app dựa trên:
- `VSAT/ui/tong_hop_du_an_vsat_android_web_v2.txt`
- `VSAT/ui/VSAT_COMPASS_BAO_CAO_VA_HUONG_DAN.txt`
- các mockup trong `VSAT/ui/extracted_screens/`
- trạng thái mã nguồn hiện tại của Android app + backend Spring Boot

Mục tiêu cuối:
- App Android chạy ổn định trên nhiều thiết bị
- Trải nghiệm thi thử nghiêm túc, local-first
- Backend tối thiểu nhưng đủ an toàn để dùng với NeonDB
- Có đủ luồng học viên, quản trị nội dung, cộng tác viên, và vận hành cơ bản

---

## Tóm tắt tiến độ

| Giai đoạn | Trạng thái | Hoàn thành |
|-----------|------------|------------|
| A — Student MVP | ✅ XONG | 100% |
| B — Backend Production | ✅ XONG | 100% (2026-04-25) |
| C — Content Management | ⏳ TIẾP THEO | 0% |
| D — Admin & Operations | 🔜 Tương lai | — |
| E — Chất lượng sản phẩm | 🔜 Tương lai | — |

---

## 1. Nguyên tắc triển khai

- Ưu tiên hoàn thiện `Student MVP` trước mọi module khác.
- Giữ kiến trúc `client-first`: làm bài, timer, điều hướng, chấm điểm chạy trên thiết bị.
- Backend chỉ giữ phần bắt buộc: auth, profile, metadata đề online, đồng bộ kết quả cuối.
- Không mở rộng web/admin nâng cao trước khi Android student flow ổn định.
- Mỗi task chỉ được coi là xong khi có tiêu chí nghiệm thu rõ ràng.

---

## 2. Trạng thái hiện tại

### 2.1 Đã có

- [x] Android app nền tảng: Splash, Login, Register, Home, Exam List, Exam Detail, Exam Session, Exam Result, Profile
- [x] Admin mode cơ bản trong cùng app
- [x] Local-first exam flow cho chế độ `CLIENT_SIDE_EXAM_PROCESSING=true`
- [x] Multi-pack local exams từ `sample_*.json` (5 pack: 2 Toán, 2 Tiếng Anh, 1 Vật lí)
- [x] Đồng bộ kết quả cuối theo kiểu non-blocking nếu backend sẵn sàng
- [x] Auth backend cơ bản bằng Spring Boot + JWT
- [x] Neon PostgreSQL schema và tài liệu nền tảng
- [x] Mockup student + admin/collaborator từ Stitch
- [x] ExamReviewActivity — xem lời giải chi tiết từng câu (đúng/sai/chưa làm, explanation)
- [x] ExamHistoryActivity + ExamHistoryRepository — lịch sử bài làm persist local (cap 200, atomic write, corrupt recovery)
- [x] RelativeTimeHelper, ScoreConstants, debug dev menu (ProfileFragment)
- [x] Backend production-ready trên Render.com — rate limiting (Bucket4j), HSTS, JWT cleanup job
- [x] Session module backend: `POST /sessions/start`, `POST /sessions/{id}/client-submit`
- [x] Error code chuẩn hóa (AppException 10 factory methods)
- [x] Smoke checklist 25 TCs + smoke scripts (auth + sessions)
- [x] 6 unit tests ExamHistoryRepository pass
- [x] B4 bug fixes: 401/403/500 hardening trong production smoke
- [x] B5 DB verification: 14/14 smoke test PASS, DB integrity xác nhận

### 2.2 Chưa hoàn chỉnh

- [x] ~~Backend public production thực sự ổn định~~ — xong v0.8.0
- [x] ~~Student history / analytics thật sự dùng được end-to-end~~ — xong v0.7.0/0.7.1
- [x] ~~Review lời giải chi tiết sau bài thi~~ — xong v0.7.0
- [ ] Quản trị câu hỏi / duyệt nội dung / tạo đề hoàn chỉnh (Phase C)
- [ ] Cộng tác viên nhập và chỉnh sửa câu hỏi hoàn chỉnh (Phase C)
- [ ] Ticket lỗi / phản hồi nội dung (Phase D)
- [ ] User management / role management đủ dùng (Phase D)
- [x] ~~Test plan, regression checklist, release checklist~~ — smoke checklist 25 TCs + unit tests done

---

## 3. Roadmap theo giai đoạn

---

## Giai đoạn A — Student MVP ✅ HOÀN THÀNH

Mục tiêu: học viên có thể đăng nhập, vào kho đề, làm bài, nộp bài, xem kết quả, luyện tập tiếp mà không phụ thuộc backend exam engine đầy đủ.

| ID | Hạng mục | Trạng thái | Ghi chú |
|----|----------|------------|---------|
| A1 | Android app cơ bản (Login, Register, Home, Profile) | ✅ Done | v0.2.0 |
| A2 | 5 pack đề local (2 Toán, 2 Tiếng Anh, 1 Vật lí) | ✅ Done | v0.7.0 |
| A3 | Luồng làm bài client-side (timer, bookmark, nộp bài) | ✅ Done | v0.5.0 |
| A4 | Màn xem lời giải chi tiết từng câu (ExamReviewActivity) | ✅ Done | v0.7.0 |
| A5 | Lịch sử bài làm persist local + dashboard stats | ✅ Done | v0.7.0 |
| A6 | 6 unit tests ExamHistoryRepository (file I/O, cap 200, corrupt recovery) | ✅ Done | v0.7.1 |
| A7 | Code quality audit: strings.xml, exception handling, ScoreConstants | ✅ Done | v0.7.1 |

### A1. Ổn định xác thực và hồ sơ ✅
- [x] Xác nhận `login`, `register`, `refresh`, `logout`, `getMe` hoạt động với backend public/dev
- [x] Làm rõ thông báo lỗi mạng, token hết hạn, lỗi tài khoản
- [x] Tự động xử lý refresh token khi access token hết hạn
- [x] Kiểm tra đăng xuất sạch token + user cache

Tiêu chí xong:
- Đăng nhập thành công trên thiết bị thật
- Mất mạng hoặc token lỗi có thông báo rõ ràng
- Không bị vòng lặp login/logout bất thường

### A2. Hoàn thiện kho đề local-first ✅
- [x] Rà lại danh sách đề hiển thị từ nhiều file `sample_*.json`
- [x] Chuẩn hóa metadata đề: môn, số câu, thời gian, mã đề, mô tả
- [x] Thêm ít nhất 1 pack nữa cho môn Toán hoặc Tiếng Anh để test quy mô nhiều đề cùng môn
- [x] Đảm bảo filter và search hoạt động đúng trên dữ liệu local

Tiêu chí xong:
- Có ít nhất 4-5 đề local hiển thị đúng
- Search/filter không bị lỗi dữ liệu trùng

### A3. Hoàn thiện màn thi thử ✅
- [x] Kiểm tra tất cả trạng thái UI từ mockup: điều hướng câu, bookmark, grid câu hỏi, hết giờ, xác nhận nộp bài
- [x] Thêm autosave state trong RAM hoặc local storage khi app bị pause/rotate
- [x] Xử lý resume exam nếu app bị đóng giữa chừng
- [x] Hiển thị rõ trạng thái `Sync: local-only` / `Sync: online enabled`

Tham chiếu mockup:
- `v_sat_compass_giao_di_n_thi_1`
- `v_sat_compass_giao_di_n_thi_2`
- `v_sat_compass_exam_control`
- `v_sat_compass_danh_s_ch_c_u_h_i`
- `v_sat_compass_x_c_nh_n_n_p_b_i`

Tiêu chí xong:
- Làm bài 50 câu không crash
- Rotate/background app không mất state
- Nộp bài luôn tạo được kết quả local

### A4. Hoàn thiện màn kết quả và review ✅
- [x] Làm xong luồng `Xem lời giải chi tiết`
- [x] Hiển thị câu đúng/sai, đáp án đã chọn, giải thích
- [x] Tính điểm theo đúng thang V-SAT dự kiến
- [x] Tạo gợi ý chủ đề yếu từ dữ liệu local

Tham chiếu mockup:
- `v_sat_compass_k_t_qu_thi`
- `v_sat_compass_xem_l_i_gi_i_chi_ti_t`
- `v_sat_compass_gi_i_th_ch_b_ng_ai`
- `v_sat_compass_luy_n_t_p_theo_ch`

Tiêu chí xong:
- Sau nộp bài có thể xem summary và review từng câu
- Có danh sách chủ đề cần luyện tập tiếp

### A5. History và stats cơ bản ✅
- [x] Lưu lịch sử bài làm local
- [x] Hiển thị số lần thi, điểm gần nhất, điểm cao nhất theo đề
- [x] Tạo dashboard học viên dựa trên local history
- [x] Nếu có backend online, đồng bộ kết quả cuối và lịch sử

Tham chiếu mockup:
- `v_sat_compass_trang_ch`
- `v_sat_compass_thi_c_hi_u`
- `v_sat_compass_b_ng_x_p_h_ng` (nếu dùng sau)

Tiêu chí xong:
- Người dùng làm 2-3 đề liên tiếp vẫn xem được lịch sử và thống kê

---

## Giai đoạn B — Backend Production-Ready ✅ HOÀN THÀNH (2026-04-25)

Mục tiêu: backend đủ nhỏ để chi phí thấp nhưng đủ an toàn và vận hành được với NeonDB.

### B1 — Public Backend Stability

| ID | Hạng mục | Trạng thái | Ghi chú |
|----|----------|------------|---------|
| B1.1 | Backend public stable trên Render.com | ✅ Done (2026-04-25) | https://vsat-compass-api.onrender.com |
| B1.2 | `BASE_URL_CLOUD` khớp production URL | ✅ Done | Verified trên thiết bị Android |
| B1.3 | Health / Auth / CORS / SSL verified | ✅ Done | smoke_auth.sh 9/9 PASS |
| B1.4 | Deploy runbook đầy đủ (pitfalls, incident triage, rollback) | ✅ Done | docs/DEPLOY_RUNBOOK.md v0.8.1 |
| B1.5 | UptimeRobot monitoring 5-phút | ✅ Done | Keep-alive cho Render free tier |

- [x] Xác định domain public thật sự đang chạy
- [x] Sửa `BASE_URL_CLOUD` sang domain đúng (`https://vsat-compass-api.onrender.com/api/v1/`)
- [x] Kiểm tra health endpoint, auth endpoint, CORS, SSL
- [x] Tài liệu hóa cách deploy và rollback (`docs/DEPLOY_RUNBOOK.md`)

Tiêu chí xong:
- App Android gọi được backend public thật
- Không còn 404/no-server ở domain cấu hình

### B2 — Minimum Required APIs

| ID | Hạng mục | Trạng thái | Ghi chú |
|----|----------|------------|---------|
| B2.1 | 5 Auth endpoints verified in production | ✅ Done (2026-04-25) | login, register, refresh, logout, getMe |
| B2.2 | Session endpoints verified in production | ✅ Done (2026-04-25) | POST /sessions/start (201) + client-submit (200) |
| B2.3 | Anti-replay 409 verified (HTTP + DB level) | ✅ Done (2026-04-25) | TC-SESSION-4 PASS, DB status=SUBMITTED |
| B2.4 | Owner check 403 verified | ✅ Done (2026-04-25) | TC-SESSION-5 PASS |
| B2.5 | Smoke scripts: smoke_auth.sh (9 TCs) + smoke_sessions.sh (5 TCs) | ✅ Done | docs/scripts/ |

- [x] `POST /auth/login`, `POST /auth/register`, `POST /auth/refresh`, `POST /auth/logout`, `GET /auth/me`
- [x] `POST /sessions/start` (bootstrap session nhẹ)
- [x] `POST /sessions/{sessionId}/client-submit`
- [ ] Nếu cần đề online: `GET /exams`, `GET /exams/{id}` ← Phase C

Tiêu chí xong:
- Student flow vẫn chạy khi backend có hoặc không có
- Khi backend có, auth và sync kết quả hoạt động đúng

### B3 — Security & Stability Hardening

| ID | Hạng mục | Trạng thái | Ghi chú |
|----|----------|------------|---------|
| B3.1 | Không có direct DB access từ Android | ✅ Verified | Audit CLEAN — không có PostgreSQL/JDBC trong app/ |
| B3.2 | Request validation chuẩn hóa (password pattern, email max, fullName) | ✅ Done | AuthRequest.java |
| B3.3 | Error JSON chuẩn hóa (10 error codes + ApiResponse envelope) | ✅ Done | AppException + GlobalExceptionHandler |
| B3.4 | Logging levels phù hợp (WARN cho Hibernate/Security trên prod) | ✅ Done | application.yml prod profile |
| B3.5 | Không có secret nào trong repo hiện tại | ✅ Verified | JWT_SECRET cũ trong git history đã được rotate |
| B3.6 | Rate limiting (login 10/phút, register 5/giờ, refresh 30/phút) | ✅ Done | RateLimitFilter + Bucket4j |
| B3.7 | HSTS header (max-age 1 year, includeSubDomains) | ✅ Done | SecurityConfig |

- [x] Kiểm tra không để app truy cập trực tiếp NeonDB
- [x] Chuẩn hóa validation request/response
- [x] Chuẩn hóa error JSON cho Android parse ổn định
- [x] Bật log vừa đủ cho prod và dev
- [x] Kiểm tra secret/env/deploy config không lộ ra repo
- [x] Rate limiting Bucket4j + HSTS header + JWT cleanup job

Tiêu chí xong:
- Có checklist bảo mật backend tối thiểu
- Có thể redeploy mà không sửa code Android

### B4 — Bug Fixes (phát hiện trong quá trình smoke test)

| ID | Bug | Trạng thái | Commit |
|----|-----|------------|--------|
| B4.1 | `/auth/**` permitAll → GET /auth/me không có token trả 500 thay vì 401 | ✅ Fixed | `24b73af` |
| B4.2 | Spring Security default → 403 thay vì 401 cho missing Bearer | ✅ Fixed | `bd0c26b` |
| B4.3 | POST /sessions/start → 500 khi exam_id FK violation (exams table rỗng) | ✅ Fixed | `bd0c26b` |

### B5 — DB Verification (2026-04-25)

| Hạng mục | Kết quả |
|----------|---------|
| users table | 4 tài khoản seed xác nhận (STUDENT, COLLABORATOR, CONTENT_ADMIN, SUPER_ADMIN) |
| refresh_tokens | Tạo đúng khi login, revoke đúng khi logout |
| exam_sessions | 2 rows từ smoke test, score/correct_count đúng, 0 orphan sessions |
| Tổng smoke test | **14/14 PASS** (9 auth + 5 session) |

---

## Giai đoạn C — Quản trị nội dung MVP ⏳ TIẾP THEO

Mục tiêu: Collaborator có thể soạn câu hỏi, Content Admin duyệt, tạo đề. Android hiển thị đề thật từ server.

### C1 — Subject & Question Bank

| ID | Hạng mục | Trạng thái |
|----|----------|------------|
| C1.1 | GET /subjects (public) | 📋 TODO |
| C1.2 | POST /collaborator/questions (tạo câu hỏi) | 📋 TODO |
| C1.3 | GET/PUT /collaborator/questions/{id} (sửa, xem câu hỏi) | 📋 TODO |
| C1.4 | POST /collaborator/questions/{id}/reviews (tạo review) | 📋 TODO |
| C1.5 | Content Admin: approve/reject câu hỏi | 📋 TODO |

- [ ] Danh sách câu hỏi
- [ ] Xem chi tiết câu hỏi
- [ ] Tạo mới câu hỏi
- [ ] Chỉnh sửa câu hỏi
- [ ] Gắn môn / chủ đề / mức độ / đáp án / lời giải
- [ ] Filter nâng cao

Tham chiếu mockup:
- `qu_n_tr_danh_s_ch_c_u_h_i`
- `qu_n_tr_xem_tr_c_c_u_h_i`
- `qu_n_tr_chi_ti_t_c_u_h_i`
- `qu_n_tr_b_l_c_n_ng_cao`
- `qu_n_tr_bi_n_so_n_c_u_h_i`
- `v_sat_qu_n_tr_ng_n_h_ng_c_u_h_i`

Tiêu chí xong:
- Admin/collaborator tạo và sửa câu hỏi được end-to-end

### C2 — Review Workflow

| ID | Hạng mục | Trạng thái |
|----|----------|------------|
| C2.1 | POST /admin/exams (tạo đề) | 📋 TODO |
| C2.2 | PUT /admin/exams/{id}/questions (thêm câu vào đề) | 📋 TODO |
| C2.3 | PUT /admin/exams/{id}/status (publish/draft) | 📋 TODO |
| C2.4 | GET /exams (public list — Android dùng) | 📋 TODO |
| C2.5 | GET /exams/{id} (public detail) | 📋 TODO |

- [ ] Gửi duyệt câu hỏi
- [ ] Duyệt / từ chối / yêu cầu chỉnh sửa
- [ ] Xem lịch sử phiên bản nội dung
- [ ] Ghi comment phản hồi

Tham chiếu mockup:
- `qu_n_tr_ph_duy_t_n_i_dung`
- `v_sat_qu_n_tr_duy_t_c_u_h_i`
- `qu_n_tr_l_ch_s_phi_n_b_n`
- `qu_n_tr_y_u_c_u_ch_nh_s_a`

Tiêu chí xong:
- Có workflow ít nhất: draft → pending → approved/rejected

### C3 — Exam Management & Android Integration

| ID | Hạng mục | Trạng thái |
|----|----------|------------|
| C3.1 | Android: load danh sách đề từ GET /exams (thay local fallback) | 📋 TODO |
| C3.2 | Android: load câu hỏi từ GET /sessions/{id}/questions/{qId} | 📋 TODO |
| C3.3 | Android: replace smoke seed exam bằng đề thật từ server | 📋 TODO |
| C3.4 | Xóa smoke_test_seed.sql sau khi Phase C có đề thật | 📋 TODO |

- [ ] Tạo đề thi từ question bank
- [ ] Gán câu hỏi vào đề
- [ ] Thiết lập thời gian, số câu, trạng thái publish
- [ ] Chọn đề miễn phí / trả phí / premium metadata

Tham chiếu mockup:
- `v_sat_qu_n_tr_t_o_thi`
- `v_sat_compass_kho_thi`
- `v_sat_compass_exam_minimal`
- `v_sat_compass_exam_premium`

Tiêu chí xong:
- Admin tạo được đề mới và student thấy đề khi publish

### C4 — Student Stats (tùy chọn Phase C)

| ID | Hạng mục | Trạng thái |
|----|----------|------------|
| C4.1 | GET /my-stats/topics (thống kê theo chủ đề) | 📋 TODO |
| C4.2 | GET /my-stats/weak-topics | 📋 TODO |

---

## Giai đoạn D — Admin & Operations 🔜 Tương lai

Mục tiêu: có đủ công cụ để app vận hành như một sản phẩm thật ở quy mô nhỏ.

| ID | Hạng mục | Trạng thái |
|----|----------|------------|
| D1 | Admin dashboard (overview counts) | 🔜 Future |
| D2 | User management (list, role, status) | 🔜 Future |
| D3 | Ticket system (student feedback) | 🔜 Future |
| D4 | Git history cleanup (bfg-repo-cleaner) | 🔜 Future |
| D5 | Custom domain (nâng Render lên paid tier hoặc VPS) | 🔜 Future |

### D1. Ticket / báo lỗi
- [ ] Học viên gửi báo lỗi câu hỏi / nội dung
- [ ] Admin xem danh sách ticket
- [ ] Gán xử lý, cập nhật trạng thái, phản hồi

Tham chiếu mockup:
- `v_sat_qu_n_tr_ticket_l_i_n_i_dung`
- `v_sat_qu_n_tr_y_u_c_u_s_a_l_i`

Tiêu chí xong:
- Có vòng khép kín student report → admin xử lý → đóng ticket

### D2. User & role management
- [ ] Danh sách user
- [ ] Xem role/status
- [ ] Khóa/mở user
- [ ] Đổi role
- [ ] Chuyển mode student/admin trong app đúng quyền

Tham chiếu mockup:
- `v_sat_qu_n_tr_ng_i_d_ng_quy_n`
- `v_sat_compass_t_i_kho_n_admin_1`
- `v_sat_compass_t_i_kho_n_admin_2`
- `v_sat_compass_x_c_nh_n_chuy_n_ch`

Tiêu chí xong:
- Role thay đổi có hiệu lực đúng ở UI và backend

### D3. Dashboard & audit cơ bản
- [ ] Dashboard tổng quan admin
- [ ] Nhật ký hoạt động cơ bản
- [ ] Số lượng user, đề thi, câu hỏi, ticket

Tham chiếu mockup:
- `v_sat_qu_n_tr_t_ng_quan_1`
- `v_sat_qu_n_tr_t_ng_quan_2`
- `v_sat_qu_n_tr_nh_t_k_h_th_ng`

Tiêu chí xong:
- Admin có màn tổng quan đủ để quản lý hệ thống

---

## Giai đoạn E — Hoàn thiện chất lượng sản phẩm 🔜 Tương lai

Mục tiêu: từ bản chạy được sang bản có thể demo, bàn giao, hoặc phát hành thử.

### E1. Chất lượng dữ liệu
- [ ] Chuẩn hóa ngân hàng câu hỏi Toán và Tiếng Anh
- [ ] Đảm bảo mỗi môn có đủ dữ liệu demo hợp lý
- [ ] Soát lỗi chính tả, lời giải, đáp án
- [ ] Tạo file import chuẩn nếu dùng Excel

### E2. Test & QA
- [ ] Viết checklist smoke test Android
- [ ] Test login / offline / online / hết giờ / submit / rotate / background
- [ ] Test role student / collaborator / content admin / super admin
- [ ] Test migration dữ liệu local packs

### E3. Hiệu năng & UX
- [ ] Kiểm tra thời gian mở danh sách đề
- [ ] Kiểm tra bộ nhớ khi làm bài dài
- [ ] Tối ưu text overflow, spacing, dark edge cases
- [ ] Tinh chỉnh thông báo lỗi và trạng thái loading/empty/error

### E4. Release readiness
- [ ] Checklist cấu hình release build
- [ ] Chuẩn hóa icon, splash, app name, versioning
- [ ] Kiểm tra proguard/r8 tối thiểu
- [ ] Viết hướng dẫn vận hành ngắn cho dev/admin

---

## 4. Danh sách ưu tiên thực hiện ngay

### Ưu tiên P0 — ✅ Hoàn thành toàn bộ
- [x] Làm cho backend public chạy thật sự ổn định
- [x] Hoàn thiện `Xem lời giải chi tiết`
- [x] Lưu lịch sử bài làm local
- [x] Bổ sung thêm pack đề local cho Toán / Tiếng Anh
- [x] Hoàn thiện bootstrap + sync kết quả cuối

### Ưu tiên P1
- [ ] Question bank quản trị
- [ ] Review workflow câu hỏi
- [ ] Tạo đề thi quản trị
- [ ] Ticket nội dung
- [ ] User / role management

### Ưu tiên P2
- [ ] Dashboard admin nâng cao
- [ ] Commerce / đề premium / credit
- [ ] Web dashboard / web exam parity
- [ ] Leaderboard / social features

---

## 5. Định nghĩa "app hoàn chỉnh" cho giai đoạn hiện tại

App được coi là đủ hoàn chỉnh cho bản bàn giao/MVP nghiêm túc khi thỏa đồng thời:

- [x] Học viên đăng nhập, chọn đề, làm bài, nộp bài, xem kết quả, xem lời giải, xem lịch sử
- [x] App chạy ổn khi backend exam engine chưa đầy đủ
- [x] Auth và sync kết quả cuối hoạt động với backend public
- [x] Có ít nhất 2 môn demo tốt: Toán, Tiếng Anh
- [ ] Admin quản lý được câu hỏi và đề ở mức MVP (Phase C)
- [ ] Có phân quyền rõ giữa Student / Collaborator / Content Admin / Super Admin (Phase C/D)
- [x] Có checklist test và changelog để quản lý thay đổi

---

## 6. Cách dùng file này

Mỗi khi hoàn thành một task:
- tick `[x]` trong checklist tương ứng
- cập nhật trạng thái trong bảng ID (📋 TODO → ✅ Done)
- thêm ngày hoàn thành vào cột Ghi chú nếu cần
- nếu thay đổi lớn, cập nhật thêm vào `CHANGELOG.md`
- nếu phát sinh scope mới, chỉ thêm vào đúng giai đoạn tương ứng, không ghi rải rác
- cập nhật bảng "Tóm tắt tiến độ" ở đầu file khi một phase kết thúc
