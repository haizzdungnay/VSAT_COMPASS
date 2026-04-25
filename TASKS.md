# V-SAT Compass — Task Tracker

> Cập nhật: 2026-04-25 | Phiên bản hiện tại: v0.8.1

---

## Giai đoạn A — Student MVP (Local-first) ✅ HOÀN THÀNH

| ID | Hạng mục | Trạng thái | Ghi chú |
|----|----------|------------|---------|
| A1 | Android app cơ bản (Login, Register, Home, Profile) | ✅ Done | v0.2.0 |
| A2 | 5 pack đề local (2 Toán, 2 Tiếng Anh, 1 Vật lí) | ✅ Done | v0.7.0 |
| A3 | Luồng làm bài client-side (timer, bookmark, nộp bài) | ✅ Done | v0.5.0 |
| A4 | Màn xem lời giải chi tiết từng câu (ExamReviewActivity) | ✅ Done | v0.7.0 |
| A5 | Lịch sử bài làm persist local + dashboard stats | ✅ Done | v0.7.0 |
| A6 | 6 unit tests ExamHistoryRepository (file I/O, cap 200, corrupt recovery) | ✅ Done | v0.7.1 |
| A7 | Code quality audit: strings.xml, exception handling, ScoreConstants | ✅ Done | v0.7.1 |

---

## Giai đoạn B — Backend Production-Ready ✅ HOÀN THÀNH (2026-04-25)

### B1 — Public Backend Stability

| ID | Hạng mục | Trạng thái | Ghi chú |
|----|----------|------------|---------|
| B1.1 | Backend public stable trên Render.com | ✅ Done (2026-04-25) | https://vsat-compass-api.onrender.com |
| B1.2 | `BASE_URL_CLOUD` khớp production URL | ✅ Done | Verified trên thiết bị Android |
| B1.3 | Health / Auth / CORS / SSL verified | ✅ Done | smoke_auth.sh 9/9 PASS |
| B1.4 | Deploy runbook đầy đủ (pitfalls, incident triage, rollback) | ✅ Done | docs/DEPLOY_RUNBOOK.md v0.8.1 |
| B1.5 | UptimeRobot monitoring 5-phút | ✅ Done | Keep-alive cho Render free tier |

### B2 — Minimum Required APIs

| ID | Hạng mục | Trạng thái | Ghi chú |
|----|----------|------------|---------|
| B2.1 | 5 Auth endpoints verified in production | ✅ Done (2026-04-25) | login, register, refresh, logout, getMe |
| B2.2 | Session endpoints verified in production | ✅ Done (2026-04-25) | POST /sessions/start (201) + client-submit (200) |
| B2.3 | Anti-replay 409 verified (HTTP + DB level) | ✅ Done (2026-04-25) | TC-SESSION-4 PASS, DB status=SUBMITTED |
| B2.4 | Owner check 403 verified | ✅ Done (2026-04-25) | TC-SESSION-5 PASS |
| B2.5 | Smoke scripts: smoke_auth.sh (9 TCs) + smoke_sessions.sh (5 TCs) | ✅ Done | docs/scripts/ |

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

## Giai đoạn C — Content Management MVP ⏳ TIẾP THEO

> Mục tiêu: Collaborator có thể soạn câu hỏi, Content Admin duyệt, tạo đề. Android hiển thị đề thật từ server.

### C1 — Subject & Question Bank

| ID | Hạng mục | Trạng thái |
|----|----------|------------|
| C1.1 | GET /subjects (public) | 📋 TODO |
| C1.2 | POST /collaborator/questions (tạo câu hỏi) | 📋 TODO |
| C1.3 | GET/PUT /collaborator/questions/{id} (sửa, xem câu hỏi) | 📋 TODO |
| C1.4 | POST /collaborator/questions/{id}/reviews (tạo review) | 📋 TODO |
| C1.5 | Content Admin: approve/reject câu hỏi | 📋 TODO |

### C2 — Exam Management

| ID | Hạng mục | Trạng thái |
|----|----------|------------|
| C2.1 | POST /admin/exams (tạo đề) | 📋 TODO |
| C2.2 | PUT /admin/exams/{id}/questions (thêm câu vào đề) | 📋 TODO |
| C2.3 | PUT /admin/exams/{id}/status (publish/draft) | 📋 TODO |
| C2.4 | GET /exams (public list — Android dùng) | 📋 TODO |
| C2.5 | GET /exams/{id} (public detail) | 📋 TODO |

### C3 — Android Integration

| ID | Hạng mục | Trạng thái |
|----|----------|------------|
| C3.1 | Android: load danh sách đề từ GET /exams (thay local fallback) | 📋 TODO |
| C3.2 | Android: load câu hỏi từ GET /sessions/{id}/questions/{qId} | 📋 TODO |
| C3.3 | Android: replace smoke seed exam bằng đề thật từ server | 📋 TODO |
| C3.4 | Xóa smoke_test_seed.sql sau khi Phase C có đề thật | 📋 TODO |

### C4 — Student Stats (tùy chọn Phase C)

| ID | Hạng mục | Trạng thái |
|----|----------|------------|
| C4.1 | GET /my-stats/topics (thống kê theo chủ đề) | 📋 TODO |
| C4.2 | GET /my-stats/weak-topics | 📋 TODO |

---

## Giai đoạn D — Admin & Operations (tương lai)

| ID | Hạng mục | Trạng thái |
|----|----------|------------|
| D1 | Admin dashboard (overview counts) | 🔜 Future |
| D2 | User management (list, role, status) | 🔜 Future |
| D3 | Ticket system (student feedback) | 🔜 Future |
| D4 | Git history cleanup (bfg-repo-cleaner, old JWT_SECRET) | 🔜 Future |
| D5 | Custom domain (nâng Render lên paid tier hoặc VPS) | 🔜 Future |

---

## Tóm tắt tiến độ

| Giai đoạn | Trạng thái | Hoàn thành |
|-----------|------------|------------|
| A — Student MVP | ✅ XONG | 100% |
| B — Backend Production | ✅ XONG | 100% (2026-04-25) |
| C — Content Management | ⏳ TIẾP THEO | 0% |
| D — Admin & Operations | 🔜 Tương lai | — |
