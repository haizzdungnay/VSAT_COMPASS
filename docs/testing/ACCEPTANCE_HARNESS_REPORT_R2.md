# BÁO CÁO NGHIỆM THU HẠ TẦNG KIỂM THỬ ĐỘC LẬP VÒNG 2 (ROUND 2 ACCEPTANCE HARNESS REPORT)
**Dự án**: V-SAT Compass (Hệ thống Khảo thí & Luyện thi V-SAT GDPT 2018)  
**Quy trình**: Chu trình nghiệm thu tự động 9 bước (`auto-acceptance-loop`)  
**Người lập**: Subagent `R2-T7-REPORT` (Model: `sonnet`)  
**Cấp thẩm quyền phê duyệt**: Orchestrator Fable (`gemini-4-argon`) / Opus (`claude-3-opus`)  
**Ngày lập**: 2026-11-03  
**Trạng thái**: ĐÃ HOÀN THÀNH KIỂM TRA ĐỘC LẬP - HẠ TẦNG SẴN SÀNG CHUYỂN PHA VÒNG 3  

---

## 1. TÓM TẮT ĐIỀU HÀNH VÒNG 2 (EXECUTIVE SUMMARY)

Vòng 2 của chu trình Nghiệm thu Tự động Toàn diện (`auto-acceptance-loop`) đã hoàn thành việc xây dựng toàn bộ bộ công cụ phòng sạch độc lập (`acceptance-harness/`) bao gồm 19 file mã nguồn. Hạ tầng kiểm thử này hoàn toàn biệt lập với mã nguồn ứng dụng chính và các file test cũ của dự án, tuân thủ nguyên tắc Zero-Human Manual QA.

Toàn bộ 19 file mã nguồn đã vượt qua kiểm tra cú pháp độc lập (`node --check` 16/16 file `.mjs`, `javac` cho JVM trigger), kiểm tra giới hạn độ dài (100% file ≤ 800 dòng, tổng 5.943 dòng), kiểm tra đối chiếu danh mục tính năng (đúng 126/126 Feature IDs từ `feature-inventory.json` qua 5 suites), và vượt qua 11/11 kịch bản kiểm thử đối kháng (Adversarial Gate Tests) của hai chốt chặn an toàn `safety-preflight.mjs` và `verdict-gate.mjs`.

### Bảng theo dõi tiến trình 9 bước trong Vòng 2

| Bước | Định danh bước & Tác vụ | Phân tầng Model | Trạng thái | Bằng chứng / Tạo phẩm nghiệm thu |
|---|---|---|---|---|
| **B1** | Kế hoạch Vòng 2 & Dự thảo phân công hạ tầng | Fable (`gemini-4-argon`) | **Hoàn thành** | Khung phân công 5 nhóm nhiệm vụ xây dựng `acceptance-harness/`: Preflight, DB Bootstrap, Stubs, Suites, Verdict Gate. |
| **B2** | Phê duyệt kế hoạch hạ tầng & chốt chặn | Opus (`claude-3-opus`) | **Hoàn thành** | Phê duyệt kèm điều kiện siết chặt: bắt buộc container hóa PostgreSQL 16 phòng sạch, cấm fallback prod, 100% assertions định lượng. |
| **B3** | Chốt phân công nhiệm vụ Vòng 2 v2 | Fable (`gemini-4-argon`) | **Hoàn thành** | Ban hành lệnh điều phối chính thức kích hoạt các subagent xây dựng harness và subagent kiểm thử độc lập. |
| **B4** | Triển khai xây dựng 19 file Harness | Sonnet (`claude-3-5-sonnet`) | **Hoàn thành** | Hoàn tất 19 file mã nguồn trong `acceptance-harness/` (5.943 dòng mã, 0 file vượt ngưỡng 800 dòng). |
| **B5** | Kiểm tra độc lập phòng sạch (Clean-Room Verify) | Sonnet (`claude-3-5-sonnet`) | **Hoàn thành** | Biên bản `R2-T6-IND-VERIFY`: 16/16 `.mjs` cú pháp chuẩn, đối chiếu `--list` đạt 126/126 IDs, đối kháng 11/11 cases đạt exit code chuẩn. |
| **B6** | Biên soạn Báo cáo Nghiệm thu Hạ tầng Vòng 2 | Sonnet (`claude-3-5-sonnet`) | **Hoàn thành** | Tài liệu `docs/testing/ACCEPTANCE_HARNESS_REPORT_R2.md` (tài liệu này). |
| **B7** | Kiểm toán độc lập Báo cáo (Audit Gate) | Sonnet (`claude-3-5-sonnet`) | *Chờ kích hoạt* | Subagent `R2-T8-AUDIT` đối soát từng cặp `file:line` thực tế trên đĩa trước khi trình nộp. |
| **B8** | Báo cáo Model Điều phối chính | Sonnet (`claude-3-5-sonnet`) | *Chờ kích hoạt* | Trình nộp tóm tắt kết quả kỹ thuật và phán quyết cho Fable. |
| **B9** | Quyết định chuyển pha sang Vòng 3 | Fable (`gemini-4-argon`) | *Chờ phê duyệt* | Fable phê duyệt kích hoạt Vòng 3: Khởi chạy nghiệm thu tự động toàn trình (`run-all.mjs`). |

---

## 2. HỒ SƠ 19 TỆP HẠ TẦNG KIỂM THỬ ĐỘC LẬP & TRÍCH DẪN THỰC CHỨNG (`file:line`)

Toàn bộ 19 file trong `acceptance-harness/` đã được kiểm tra trực tiếp trên đĩa, xác nhận 100% file tuân thủ quy tắc dưới 800 dòng mã (max: 710 dòng, min: 18 dòng, tổng: 5.943 dòng).

```
acceptance-harness/
├── package.json (29 dòng)
├── safety-preflight.mjs (186 dòng)
├── docker-compose.clean.yml (18 dòng)
├── db-bootstrap.mjs (432 dòng)
├── verdict-gate.mjs (236 dòng)
├── run-all.mjs (226 dòng)
├── lib/
│   └── test-harness.mjs (329 dòng)
├── stubs/
│   └── llm-mock-server.mjs (166 dòng)
├── jvm-triggers/
│   └── TriggerRefreshTokenCleanup.java (88 dòng)
└── suites/
    ├── backend-admin.mjs (552 dòng)
    ├── backend-public-student-collab.mjs (701 dòng)
    ├── run-web-suite.mjs (443 dòng)
    ├── android-student-suite.mjs (710 dòng)
    ├── android-roles-workers-suite.mjs (593 dòng)
    └── helpers/
        ├── backend-admin-helpers.mjs (181 dòng)
        ├── backend-psc-helpers.mjs (116 dòng)
        ├── web-helpers.mjs (360 dòng)
        ├── android-student-helpers.mjs (313 dòng)
        └── android-roles-helpers.mjs (264 dòng)
```

### 2.1 Cấu hình & Chốt chặn An toàn Môi trường

1. **`acceptance-harness/package.json`** (29 dòng):
   - `package.json:1-6`: Khai báo gói độc lập `acceptance-harness` phiên bản `1.0.0`, cấu hình ESM module (`"type": "module"`), chế độ `"private": true`.
   - `package.json:7-19`: Danh mục 11 kịch bản npm thực thi riêng rẽ (`preflight`, `db:up`, `db:bootstrap`, `mock:llm`, `test:backend:admin`, `test:backend:psc`, `test:web`, `test:android:student`, `test:android:roles`, `verdict`, `run:all`).
   - `package.json:20-28`: Khai báo ràng buộc Node.js `engines: >=20.0.0` và siêu dữ liệu phòng sạch.

2. **`acceptance-harness/safety-preflight.mjs`** (186 dòng):
   - `safety-preflight.mjs:4-5`: Danh sách whitelist máy chủ `ALLOWED_HOSTS` (`127.0.0.1`, `localhost`) và danh sách chuỗi cấm `FORBIDDEN_SUBSTRINGS` (`onrender.com`, `supabase`, `neon.tech`, `host.docker.internal`).
   - `safety-preflight.mjs:7-27`: Hàm `parseSafeUrl()` và `assertSafeHost()` chuẩn hóa URL, bóc tách tiền tố `jdbc:`, kích hoạt lỗi `[SECURITY]` khi phát hiện truy cập bên ngoài localhost.
   - `safety-preflight.mjs:62-108`: Quét biến môi trường ứng dụng (`DATABASE_URL`, `SPRING_DATASOURCE_URL`, `API_BASE_URL`, `VITE_API_BASE_URL`, `LLM_BASE_URL`), cấm kết nối database sản xuất `vsat_compass` tại cổng mặc định 5432.
   - `safety-preflight.mjs:110-141`: Kiểm tra tính khả dụng của các cổng phòng sạch (`55432` cho PostgreSQL, `18080` cho Backend Spring Boot, `18081` cho LLM Mock, `15173` cho Web Vite).
   - `safety-preflight.mjs:143-186`: Hàm điều phối `runSafetyPreflight()`, kiểm tra sự tồn tại của file APK và kích hoạt `process.exit(1)` khi vi phạm quy tắc an toàn.

### 2.2 Hạ tầng Dữ liệu Phòng sạch & Giả lập Dịch vụ

3. **`acceptance-harness/docker-compose.clean.yml`** (18 dòng):
   - `docker-compose.clean.yml:2-10`: Định nghĩa dịch vụ `vsat-clean-pg` sử dụng image `postgres:16-alpine`, ánh xạ cổng cô lập `127.0.0.1:55432:5432`, cơ sở dữ liệu `vsat_compass_acceptance`, tài khoản `vsat_clean`.
   - `docker-compose.clean.yml:11-18`: Cấu hình ổ đĩa nhớ tạm thời `tmpfs` dung lượng 512MB trên RAM (`/var/lib/postgresql/data:rw,noexec,nosuid,size=512m`), cấu hình kiểm tra sức khỏe `pg_isready` chu kỳ 2 giây, hủy bỏ tự khởi động lại (`restart: "no"`).

4. **`acceptance-harness/db-bootstrap.mjs`** (432 dòng):
   - `db-bootstrap.mjs:1-40`: Khởi tạo tham số kết nối vùng đệm và hàm `runSqlInContainer()` thực thi trực tiếp qua lệnh `docker exec -i vsat-clean-pg psql`.
   - `db-bootstrap.mjs:42-150`: Định nghĩa dữ liệu mẫu cố định phòng sạch `CLEAN_FIXTURES_SQL` bao gồm 4 tài khoản tiêu chuẩn với mật khẩu đã mã hóa BCrypt (`student@vsat.com`, `collab@vsat.com`, `contentadmin@vsat.com`, `superadmin@vsat.com`), môn học, ma trận đề, câu hỏi và đề thi mẫu.
   - `db-bootstrap.mjs:260-310`: Nạp tệp cơ sở dữ liệu nền tảng gốc `VSAT/vsat-compass-api/docs/sql/vsat_database_schema.sql` (29 bảng dữ liệu cơ bản).
   - `db-bootstrap.mjs:312-405`: Thực thi tuần tự 11 tệp migration SQL (`0001_create_refresh_tokens.sql` đến `0011_add_exam_window_title.sql`), hoàn thiện cấu trúc 40 bảng dữ liệu sẵn sàng cho Hibernate `ddl-auto=validate`.
   - `db-bootstrap.mjs:407-432`: Hàm `bootstrapDatabase()` nạp schema, migration, fixtures và xác nhận hoàn tất quy trình khởi tạo dữ liệu phòng sạch.

5. **`acceptance-harness/stubs/llm-mock-server.mjs`** (166 dòng):
   - `llm-mock-server.mjs:1-35`: Khởi tạo máy chủ HTTP thuần cổng `18081` gắn với giao diện `127.0.0.1`, hỗ trợ quản lý trạng thái (`normal`, `error`, `timeout`) và ghi nhật ký yêu cầu.
   - `llm-mock-server.mjs:36-110`: Triển khai endpoint chuẩn OpenAI `/v1/chat/completions` và `/v1/models`, bóc tách nội dung câu hỏi toán/khoa học từ prompt.
   - `llm-mock-server.mjs:111-166`: Bộ sinh phản hồi sư phạm tự động có định dạng Markdown LaTeX chứa công thức toán học (`$$` và `\frac`), xuất hàm `startLlmMock()` phục vụ tích hợp điều phối tự động.

### 2.3 Thư viện Kiểm thử Cốt lõi & Cổng Phán quyết

6. **`acceptance-harness/lib/test-harness.mjs`** (329 dòng):
   - `test-harness.mjs:10-25`: Khai báo 4 tài khoản người dùng định danh chuẩn xác cho 4 vai trò hệ thống (`STUDENT`, `COLLABORATOR`, `CONTENT_ADMIN`, `SUPER_ADMIN`).
   - `test-harness.mjs:27-75`: Hệ thống hàm kiểm tra định lượng độc lập `assert()`, `assertEqual()`, `assertTruthy()`, `assertMatches()`, ngăn chặn hoàn toàn việc ghi nhận ca kiểm thử rỗng.
   - `test-harness.mjs:76-180`: Cơ chế xác thực tự động và lưu đệm token JWT (`loginAndGetToken()`), cơ chế thực hiện HTTP request thuần qua fetch API có kèm timeout.
   - `test-harness.mjs:182-260`: Hàm đăng ký ca kiểm thử `registerCase()` và động cơ thực thi `runRegisteredCases()` hỗ trợ chạy lại tối đa 1 lần (max 1 retry) khi gặp lỗi kết nối tạm thời.
   - `test-harness.mjs:262-329`: Cơ chế kết xuất báo cáo chuẩn JSON chứa đầy đủ mảng assertions, thời gian thực thi, số lần thử và thông tin lỗi.

7. **`acceptance-harness/verdict-gate.mjs`** (236 dòng):
   - `verdict-gate.mjs:7-35`: Đọc và bóc tách danh mục 126 mã định danh bắt buộc từ `docs/testing/feature-inventory.json` qua 5 phân vùng tính năng hệ thống.
   - `verdict-gate.mjs:37-120`: Thu thập toàn bộ các file báo cáo trong thư mục `acceptance-harness/results/`, đối soát từng mã ID, kiểm tra tính hợp lệ của mảng `assertions` (bắt buộc > 0), phát hiện và từ chối các ca kiểm thử trạng thái `SKIPPED`.
   - `verdict-gate.mjs:122-160`: Kiểm tra ngưỡng chạy lại, từ chối nghiệm thu (`exit 1`) nếu có bất kỳ ca kiểm thử nào có số lần chạy vượt quá 2 (`attempts > 2`).
   - `verdict-gate.mjs:161-236`: Hàm phân định cuối cùng `evaluateVerdict()`, xuất file phán quyết máy đọc `final-verdict.json` và tệp tóm tắt `machine-summary.txt`, trả về `exit 0` khi đạt tuyệt đối 126/126 IDs hoặc `exit 1` khi có vi phạm.

8. **`acceptance-harness/run-all.mjs`** (226 dòng):
   - `run-all.mjs:1-40`: Tích hợp các module điều phối chính (`safety-preflight.mjs`, `db-bootstrap.mjs`, `llm-mock-server.mjs`, `verdict-gate.mjs`).
   - `run-all.mjs:42-105`: Tự động hóa vòng đời khởi động hệ thống: kiểm tra an toàn preflight, khởi động container PostgreSQL sạch cổng 55432, nạp schema và fixtures, khởi động LLM Mock Server cổng 18081, biên dịch và chạy Backend Spring Boot cổng 18080 với biến môi trường clean-room.
   - `run-all.mjs:107-165`: Thực thi tuần tự 5 bộ kiểm thử (`backend-admin`, `backend-psc`, `run-web-suite`, `android-student`, `android-roles-workers`), lưu trữ toàn bộ kết quả vào thư mục `results/`.
   - `run-all.mjs:167-226`: Kích hoạt cổng phán quyết `verdict-gate.mjs`, dọn dẹp các tiến trình con (teardown Spring Boot, tắt LLM mock, dừng container PostgreSQL sạch) và thoát với exit code phân định.

9. **`acceptance-harness/jvm-triggers/TriggerRefreshTokenCleanup.java`** (88 dòng):
   - `TriggerRefreshTokenCleanup.java:14-30`: Kiểm tra sự hiện diện của lớp nghiệp vụ `com.vsatcompass.api.service.RefreshTokenCleanupService` và phương thức `cleanupExpiredTokens()` qua Java Reflection trong classpath.
   - `TriggerRefreshTokenCleanup.java:31-50`: Thiết lập kết nối JDBC trực tiếp tới cơ sở dữ liệu phòng sạch `jdbc:postgresql://127.0.0.1:55432/vsat_compass_acceptance`.
   - `TriggerRefreshTokenCleanup.java:52-88`: Thực thi trực tiếp câu truy vấn dọn dẹp token hết hạn tương đương với logic Repository, hỗ trợ cơ chế thực thi dự phòng qua containerized psql, phục vụ nghiệm thu tự động cho mã `JOB-001`.

### 2.4 Năm Bộ Kịch bản Kiểm thử Nghiệm thu & Bộ Trợ thủ (5 Suites & Helpers)

10. **`acceptance-harness/suites/backend-admin.mjs`** (552 dòng) & **`acceptance-harness/suites/helpers/backend-admin-helpers.mjs`** (181 dòng):
    - Đăng ký và thực thi 42 Feature IDs (`API-001` đến `API-042`).
    - Bao phủ toàn bộ các API quản trị: Admin Blueprint (`API-001`..`API-002`), Dashboard Thống kê (`API-003`), Quản lý Đề thi và Vòng đời Xuất bản (`API-004`..`API-022`), Khung giờ thi (`API-023`..`API-028`), Ngân hàng Câu hỏi và Quy trình Duyệt (`API-029`..`API-036`), Xử lý Khiếu nại Ticket (`API-037`..`API-038`), Quản lý Người dùng và Phân quyền (`API-039`..`API-042`).
    - File helper cung cấp các hàm tạo payload đề thi GDPT 2018, ma trận kiến thức, và xử lý phân trang API.

11. **`acceptance-harness/suites/backend-public-student-collab.mjs`** (701 dòng) & **`acceptance-harness/suites/helpers/backend-psc-helpers.mjs`** (116 dòng):
    - Đăng ký và thực thi 36 Feature IDs (`API-043` đến `API-076`, `JOB-001`, `JOB-002`).
    - Bao phủ toàn bộ các API công khai và học sinh: Trạng thái hệ thống (`API-043`), Đăng ký/Đăng nhập/Đổi mật khẩu JWT (`API-044`..`API-052`), Không gian Cộng tác viên soạn thảo câu hỏi (`API-053`..`API-057`), Danh mục đề thi và Giải thích AI (`API-058`..`API-060`), Tải gói đề thi offline (`API-061`), Bảng xếp hạng và Thống kê năng lực (`API-062`..`API-064`), Luyện tập thích ứng (`API-065`..`API-066`), Phiên thi trực tiếp và Nộp bài ngoại tuyến (`API-067`..`API-072`), Gửi phản hồi khiếu nại (`API-073`), Danh mục môn học chương trình mới (`API-074`..`API-076`).
    - Kiểm thử tích hợp tiến trình nền: Kích hoạt JVM dọn dẹp Refresh Token hết hạn (`JOB-001`), Kiểm thử máy khách AI Tutor xử lý lỗi kết nối và định dạng KaTeX (`JOB-002`).

12. **`acceptance-harness/suites/run-web-suite.mjs`** (443 dòng) & **`acceptance-harness/suites/helpers/web-helpers.mjs`** (360 dòng):
    - Đăng ký và thực thi 10 Feature IDs (`WEB-001` đến `WEB-010`).
    - Tự động hóa kiểm thử giao diện Quản trị Web Admin qua Playwright Headless: Đăng nhập và Dashboard KPI (`WEB-001`), Danh sách và Bộ lọc Đề thi (`WEB-002`), Wizard tạo đề thi 4 bước (`WEB-003`), Ma trận Blueprint GDPT 2018 (`WEB-004`), Lịch thi Exam Windows (`WEB-005`), Ngân hàng câu hỏi (`WEB-006`), Hàng đợi duyệt câu hỏi cộng tác viên (`WEB-007`), Nhập câu hỏi Excel tự động (`WEB-008`), Cấu hình thang điểm 150 V-SAT (`WEB-009`), Bộ kết xuất công thức toán học MathView KaTeX (`WEB-010`).
    - File helper quản lý khởi tạo trình duyệt Chromium headless, chụp ảnh màn hình xác thực, và mô phỏng tải tệp Excel nhị phân trên bộ nhớ.

13. **`acceptance-harness/suites/android-student-suite.mjs`** (710 dòng) & **`acceptance-harness/suites/helpers/android-student-helpers.mjs`** (313 dòng):
    - Đăng ký và thực thi 20 Feature IDs (`AND-001` đến `AND-020`).
    - Kiểm thử giao diện và luồng nghiệp vụ Học sinh trên ứng dụng Android: Khởi động Splash và định tuyến vai trò (`AND-001`), Xác thực và lưu trữ bảo mật Token (`AND-002`..`AND-003`), Điều hướng MainActivity và Dashboard học tập (`AND-004`..`AND-005`), Danh mục đề và chi tiết đề thi (`AND-006`..`AND-007`), Quản lý đề thi ngoại tuyến đã tải về (`AND-008`), Phòng thi trực tuyến và đồng hồ đếm ngược (`AND-009`), Bảng điểm chuẩn 150 điểm và xem lại bài giải (`AND-010`..`AND-011`), Luyện tập theo chuyên đề thích ứng (`AND-012`..`AND-013`), Lịch sử làm bài thi (`AND-014`), Ôn tập công thức Flashcard lặp lại ngắt quãng Leitner (`AND-015`), Biểu đồ tiến độ phân tích điểm yếu (`AND-016`), Hồ sơ cá nhân và Cài đặt nhắc học tập (`AND-017`..`AND-018`), Bảng xếp hạng thi đua (`AND-019`), Cổng quản trị di động (`AND-020`).
    - File helper cung cấp cơ chế tương tác ADB headless, phân tích XML phân cấp giao diện (view hierarchy dump), và xác thực phản hồi của thành phần UI.

14. **`acceptance-harness/suites/android-roles-workers-suite.mjs`** (593 dòng) & **`acceptance-harness/suites/helpers/android-roles-helpers.mjs`** (264 dòng):
    - Đăng ký và thực thi 18 Feature IDs (`AND-021` đến `AND-035`, `WRK-001` đến `WRK-003`).
    - Bao phủ các tính năng nâng cao và tác vụ nền: Thống kê quản trị viên trên di động (`AND-021`), Danh sách và form tạo đề thi Android (`AND-022`..`AND-025`), Ngân hàng và giao diện duyệt câu hỏi KaTeX (`AND-026`..`AND-027`), Phân quyền tài khoản người dùng (`AND-028`), Xử lý khiếu nại câu hỏi (`AND-029`), Không gian làm việc của Cộng tác viên (`AND-030`..`AND-033`), Luồng hiển thị KaTeX trong WebView di động (`AND-034`), Luồng tải trọn gói đề thi ngoại tuyến kèm tài nguyên (`AND-035`).
    - Kiểm thử tự động WorkManager và Notification: Kích hoạt Worker gửi thông báo nhắc học duy trì chuỗi streak (`WRK-001`), Worker tự động đồng bộ bài thi offline khi có mạng trở lại (`WRK-002`), Khởi tạo kênh thông báo hệ thống NotificationChannel (`WRK-003`).
    - File helper quản lý lệnh shell ADB gửi Intent kích hoạt BroadcastReceiver và kiểm tra bảng SQLite nội bộ của ứng dụng di động.

---

## 3. BẢNG ĐỐI CHIẾU ĐỘ PHỦ 126/126 IDS QUA 5 PHÂN HỆ (`--list`)

Tất cả 5 bộ kịch bản kiểm thử trong `acceptance-harness/suites/` đã được thực thi với cờ `--list`. Kết quả trả về đúng 126/126 mã định danh duy nhất, hoàn toàn trùng khớp với `docs/testing/feature-inventory.json` (thiếu 0, thừa 0, trùng lặp 0).

| Phân hệ / Tệp Kịch bản Kiểm thử | Loại đối tượng kiểm thử | Số lượng ID | Danh sách mã định danh được phân bổ |
|---|---|:---:|---|
| `suites/backend-admin.mjs` | Backend Spring Boot (Admin API) | **42** | `API-001` đến `API-042` |
| `suites/backend-public-student-collab.mjs` | Backend API (Public, Student, Collab, Jobs) | **36** | `API-043` đến `API-076`, `JOB-001`, `JOB-002` |
| `suites/run-web-suite.mjs` | Web Admin SPA (Vite + React + Tailwind) | **10** | `WEB-001` đến `WEB-010` |
| `suites/android-student-suite.mjs` | Android App (Student Core Features) | **20** | `AND-001` đến `AND-020` |
| `suites/android-roles-workers-suite.mjs` | Android App (Admin/Collab/Workers) | **18** | `AND-021` đến `AND-035`, `WRK-001` đến `WRK-003` |
| **TỔNG CỘNG HỆ THỐNG** | **Toàn bộ bề mặt V-SAT Compass** | **126** | **126/126 IDs (Đạt tỷ lệ bao phủ 100%)** |

---

## 4. KẾT QUẢ KIỂM THỬ ĐỐI KHÁNG (ADVERSARIAL GATE VERIFICATION)

Để đảm bảo tính nghiêm ngặt và không thỏa hiệp của hệ thống cổng nghiệm thu tự động, hai chốt chặn an toàn `safety-preflight.mjs` và `verdict-gate.mjs` đã trải qua 11 kịch bản kiểm thử đối kháng giả lập các tình huống vi phạm quy chuẩn an toàn và gian lận kết quả test.

### 4.1 Kết quả Kiểm thử Đối kháng `safety-preflight.mjs` (6/6 Ca Đạt)

Chốt chặn an toàn môi trường bắt buộc từ chối mọi cấu hình hướng ra môi trường ngoài hoặc trỏ vào cơ sở dữ liệu thật của dự án:

| STT | Kịch bản Kiểm thử Đối kháng | Cấu hình Đầu vào Giả lập | Kết quả Mong đợi | Kết quả Thực tế | Đánh giá |
|:---:|---|---|:---:|:---:|:---:|
| 1 | Chặn tên miền đám mây sản xuất | `DATABASE_URL=postgres://user:pass@ep-cool.onrender.com/db` | Exit Code `1` | Exit Code `1` (Bị chặn bởi bộ lọc host) | **ĐẠT** |
| 2 | Chặn kỹ thuật tấn công Subdomain Fake | `API_BASE_URL=http://localhost.evil.com:18080` | Exit Code `1` | Exit Code `1` (Hostname không khớp whitelist) | **ĐẠT** |
| 3 | Chặn mạng nội bộ Docker Host | `SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/db` | Exit Code `1` | Exit Code `1` (Chứa chuỗi cấm) | **ĐẠT** |
| 4 | Chặn truy cập Database Sản xuất tại cổng 5432 | `DATABASE_URL=postgresql://localhost:5432/vsat_compass` | Exit Code `1` | Exit Code `1` (Cấm sử dụng cổng 5432 / DB thật) | **ĐẠT** |
| 5 | Chặn nạp file APK Release ký thương mại | `APK_PATH=app/release/app-release.apk` | Exit Code `1` | Exit Code `1` (Chỉ chấp nhận APK Debug kiểm thử) | **ĐẠT** |
| 6 | Cho phép môi trường Phòng sạch hợp lệ | Cổng 55432, 18080, 18081, 15173 trên `127.0.0.1` | Exit Code `0` | Exit Code `0` (Vượt qua toàn bộ chốt kiểm tra) | **ĐẠT** |

### 4.2 Kết quả Kiểm thử Đối kháng `verdict-gate.mjs` (5/5 Ca Đạt)

Cổng phán quyết phân định tự động bắt buộc từ chối mọi trường hợp thiếu hụt dữ liệu hoặc bỏ qua kiểm thử:

| STT | Kịch bản Kiểm thử Đối kháng | Dữ liệu Báo cáo Giả lập | Kết quả Mong đợi | Kết quả Thực tế | Đánh giá |
|:---:|---|---|:---:|:---:|:---:|
| 1 | Chặn thiếu hụt mã định danh tính năng | Báo cáo chỉ chứa 125/126 IDs (cố tình xóa bỏ `API-001`) | Exit Code `1` | Exit Code `1` (Báo lỗi thiếu `API-001`) | **ĐẠT** |
| 2 | Chặn ca kiểm thử rỗng không có xác minh | Ca kiểm thử `API-005` có mảng `assertions: []` | Exit Code `1` | Exit Code `1` (Báo lỗi assertion rỗng) | **ĐẠT** |
| 3 | Chặn ca kiểm thử bị bỏ qua (Zero-Skip) | Ca kiểm thử `WEB-003` có trạng thái `SKIPPED` | Exit Code `1` | Exit Code `1` (Từ chối trạng thái bỏ qua kiểm thử) | **ĐẠT** |
| 4 | Chặn hành vi Flaky chạy lại quá ngưỡng | Ca kiểm thử `AND-009` có số lần chạy lại `attempts: 3` | Exit Code `1` | Exit Code `1` (Vượt ngưỡng tối đa 1 lần retry) | **ĐẠT** |
| 5 | Phê duyệt bộ kết quả phòng sạch hoàn hảo | Đủ 126/126 IDs hợp lệ, 100% assertions định lượng | Exit Code `0` | Exit Code `0` (Xuất `final-verdict.json` ACCEPTED) | **ĐẠT** |

---

## 5. ĐỀ XUẤT KẾ HOẠCH THỰC THI VÒNG 3 (TRÌNH FABLE PHÊ DUYỆT)

Subagent `R2-T7-REPORT` kính trình Orchestrator Fable và Opus xem xét phê duyệt chuyển giao sang Vòng 3 (`Full Autonomous Run & Final Verdict`).

### 5.1 Quy trình Thực thi Toàn trình Tự động Vòng 3

Vòng 3 sẽ thực thi một lệnh điều phối duy nhất:
```bash
node acceptance-harness/run-all.mjs
```

Quy trình vận hành khép kín diễn ra theo 7 giai đoạn tự động hóa 100%:

```
[Giai đoạn 1: Safety Preflight]
   └── Quét 5 biến môi trường + 4 cổng phòng sạch (55432, 18080, 18081, 15173).
[Giai đoạn 2: Disposable Database Bootstrap]
   └── Khởi động container vsat-clean-pg (PostgreSQL 16 Alpine trên RAM tmpfs).
   └── Nạp baseline 29 bảng + 11 migrations (tổng 40 bảng) + nạp dữ liệu mẫu cố định.
[Giai đoạn 3: Local LLM Mock Server]
   └── Khởi động máy chủ giả lập OpenAI tại 127.0.0.1:18081 hỗ trợ KaTeX LaTeX tutor.
[Giai đoạn 4: Spring Boot Backend Startup]
   └── Khởi chạy ứng dụng Spring Boot cổng 18080, Hibernate ddl-auto=validate trên 40 bảng.
[Giai đoạn 5: Parallel Test Suites Execution]
   ├── Thực thi suites/backend-admin.mjs (42 IDs)
   ├── Thực thi suites/backend-public-student-collab.mjs (36 IDs)
   ├── Thực thi suites/run-web-suite.mjs (10 IDs)
   ├── Thực thi suites/android-student-suite.mjs (20 IDs)
   └── Thực thi suites/android-roles-workers-suite.mjs (18 IDs)
[Giai đoạn 6: Final Verdict Gate]
   └── Đọc 5 tệp kết quả JSON, đối soát 126/126 IDs, kiểm tra 0 skip, 0 empty assertion.
   └── Xuất tạo phẩm final-verdict.json và machine-summary.txt.
[Giai đoạn 7: Teardown & Exit]
   └── Hủy tiến trình Spring Boot, dừng Mock Server, hủy container vsat-clean-pg.
   └── Trả về exit code 0 khi toàn bộ 126/126 IDs được phân định ACCEPTED.
```

### 5.2 Tiêu chí Hoàn thành Vòng 3 (Definition of Done)
1. Cổng phán quyết `verdict-gate.mjs` trả về mã trạng thái thoát `exit 0`.
2. Tệp `acceptance-harness/results/final-verdict.json` xác nhận trạng thái `"verdict": "ACCEPTED"` cho 126/126 mã tính năng.
3. Không có bất kỳ cảnh báo an toàn nào bị vi phạm trong suốt quá trình chạy.
4. Sẵn sàng tạo báo cáo tổng kết nghiệm thu cuối cùng trình nộp người dùng.

---
*Báo cáo được lập tự động bởi Subagent R2-T7-REPORT, sẵn sàng chuyển tiếp sang bước kiểm toán R2-T8-AUDIT.*
