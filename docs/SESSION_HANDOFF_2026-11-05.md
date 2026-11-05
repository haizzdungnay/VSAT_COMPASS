# VSAT_COMPASS - Biên Bản Bàn Giao Kỹ Thuật (Session Handoff)

- **Ngày tạo**: 2026-11-05
- **Mục đích tạo file**: Ghi nhận toàn bộ hiện trạng kỹ thuật, kết quả nghiệm thu tự động (Clean-Room Acceptance), các chỉnh sửa đã đưa vào harness/CI, và chi tiết các đầu việc cần làm tiếp theo để đưa GitHub Actions CI đạt 126/126 test cases PASS mà không cần chạy lại các tiến trình tốn thời gian.

---

## 1. Trạng Thái Hiện Tại Đã Đạt Được

| Hạng mục | Trạng thái | Chi tiết |
|---|---|---|
| **Clean-Room Local Acceptance** | **126/126 PASS (100%)** | Chạy độc lập hoàn toàn với DB riêng biệt `vsat_compass_acceptance` trên port 55432. |
| **Android Release APK** | **Đã build & ký xong** | File: `app/build/outputs/apk/release/app-release.apk`<br>Dung lượng: `4,335,909 bytes`<br>SHA-256: `75198384c4affc1ff1d97ef03d5391175782edf883dc5701a9439cc64c8fdc67`. |
| **GitHub Actions CI (Ubuntu)** | **123/126 PASS** | Run ID: `41616028237`. Toàn bộ Web (10/10), Android mock (35/35), Worker (3/3) và 74/76 API đều PASS. |
| **Cấu hình chuyển tiếp JVM/CI** | **Đã cập nhật** | Thêm chuyển tiếp `systemProperties = System.properties` vào `bootRun` của `VSAT/vsat-compass-api/build.gradle`.<br>Bổ sung `LLM_API_KEY`, `LLM_MODEL`, `JWT_SECRET` vào `.github/workflows/auto-acceptance.yml` và `acceptance-harness/run-all.mjs`. |

---

## 2. 3 Feature IDs Cần Xử Lý Để Đạt 126/126 Trên CI

Chỉ còn 3/126 test cases bị lỗi trên môi trường Ubuntu CI runner:

1. **`API-072` (Login STUDENT)**:
   - **Hiện tượng**: Trả về HTTP 500 `INTERNAL_ERROR`.
   - **Vị trí**: `acceptance-harness/suites/backend-public-student-collab.mjs` (khoảng dòng 550–590).
   - **Nguyên nhân tiềm năng**: Tài khoản sinh viên test (`student@vsat.local`) trong DB clean-room chưa có bản ghi profile sinh viên tương ứng hoặc hash mật khẩu bcrypt không khớp khi authenticate.

2. **`API-073` (Login COLLABORATOR)**:
   - **Hiện tượng**: Trả về HTTP 500 `INTERNAL_ERROR`.
   - **Vị trí**: `acceptance-harness/suites/backend-public-student-collab.mjs` (khoảng dòng 590–630).
   - **Nguyên nhân tiềm năng**: Tương tự `API-072`, thiếu bản ghi profile cộng tác viên hoặc thiếu role trong claim JWT.

3. **`JOB-002` (Background Job với Mock LLM)**:
   - **Hiện tượng**: Báo lỗi `fetch failed` khi gọi tới mock LLM server.
   - **Vị trí**: `acceptance-harness/suites/backend-public-student-collab.mjs` (khoảng dòng 650–702) và `acceptance-harness/suites/helpers/backend-psc-helpers.mjs`.
   - **Nguyên nhân tiềm năng**: Mock LLM server lắng nghe trên `127.0.0.1:18081`, nhưng background job trong Spring Boot kết nối qua URL hoặc hostname không tương thích trên Linux (`localhost` vs `127.0.0.1`), hoặc mock server tắt trước khi job xử lý xong.

---

## 3. Các Đầu Việc Cần Làm Tiếp Theo

1. **Sửa dứt điểm 3 test cases**:
   - Mở `acceptance-harness/suites/backend-public-student-collab.mjs`.
   - Kiểm tra bước seed / tạo user sinh viên và CTV trong `acceptance-harness/suites/helpers/backend-psc-helpers.mjs` (đảm bảo tạo đầy đủ user + profile trước khi test gọi login).
   - Đồng bộ URL kết nối Mock LLM giữa Spring Boot backend và mock server về cùng `http://127.0.0.1:18081`.

2. **Kiểm tra cú pháp nhanh**:
   - Chạy `node --check` trên các file test suites đã sửa (tuyệt đối không chạy full suite nặng nếu không cần thiết).

3. **Push và kiểm tra kết quả CI**:
   - Push commit lên `origin/main`.
   - Kiểm tra GitHub Actions workflow `Clean-Room Acceptance` chuyển xanh (126/126 PASS).

---

## 4. Quy Tắc Vận Hành Bắt Buộc (Strict Constraints)

- **Không gắn AI co-author**: Tuyệt đối không thêm `Co-Authored-By: Claude...` hoặc bất kỳ AI attribution trailer nào vào commit message. Tác giả duy nhất là người dùng (`haizzdungnay`).
- **Tốc độ & Thực dụng**: Ưu tiên đọc và sửa trực tiếp code tại chỗ, không chạy các lệnh ngâm nhiều giờ gây tắc nghẽn.
