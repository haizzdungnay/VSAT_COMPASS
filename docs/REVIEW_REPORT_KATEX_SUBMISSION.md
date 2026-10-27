# BÁO CÁO REVIEW: BACKEND SUBMISSION PAYLOAD & ANDROID KATEX SUPPORT

**Dự án**: VSAT_COMPASS (Full-stack & Mobile)  
**Thời điểm**: 27/10/2026  
**Trạng thái**: Hoàn thành 100%, tất cả test suite passed, CI/CD pipeline xanh.

---

## 1. Mục tiêu công việc

1. **Nâng cấp Backend Submission Payload**:
   - Mở rộng endpoint `POST /sessions/{id}/answers` nhận JSONB payload (`answerPayload`) cho các tiểu mục câu hỏi phức hợp theo chuẩn V-SAT 2026:
     - Đúng/Sai (`TRUE_FALSE`): `{"a": true, "b": false, "c": true, "d": false}`
     - Ghép hợp / Nối cặp (`MATCHING`): `{"1": "A", "2": "C", "3": "B", "4": "D"}`
     - Điền số / Trả lời ngắn (`SHORT_ANSWER`): `{"value": "37"}`
     - Trắc nghiệm đơn (`SINGLE_CHOICE`): Tương thích ngược qua `selectedOptionId`.
   - Cập nhật chấm điểm server-side trong `SessionServiceImpl`:
     - Phân tầng điểm Đúng/Sai (1 ý = 1đ, 2 ý = 2đ, 3 ý = 3đ, 4 ý = 6đ).
     - Phân bổ điểm Nối cặp (1.5đ / cặp ghép đúng).
     - Chấm chuỗi số/văn bản Điền số (dung sai số thực hoặc so khớp chuỗi).
     - Tổng hợp `raw_score` (tối đa 150) và `max_raw_score` theo ma trận đề V-SAT.

2. **Android KaTeX Support**:
   - Tích hợp KaTeX vào Android Client để hiển thị công thức toán học (`$...$`, `$$...$$`, `\[...\]`, `\(...\)`).
   - Nhúng trực tiếp tài nguyên KaTeX offline vào `app/src/main/assets/katex/` (js, css, auto-render, fonts) để chạy mượt mà ngay cả khi thiết bị mất mạng.
   - Thêm `KaTeXHelper` và view WebView `wvQuestionMath` tự động ẩn/hiện thay thế `TextView` khi câu hỏi chứa công thức toán.

---

## 2. Chi tiết các tệp thay đổi

| Module | Tệp | Nội dung thay đổi |
|---|---|---|
| **Backend** | `SessionRequest.java` | Thêm trường `answerPayload` (`Map<String, Object>`) vào `SubmitAnswer` và `ClientSubmitAnswer`; bỏ bắt buộc `selectedOptionId`. |
| **Backend** | `SessionAnswer.java` | Map các cột DB: `answer_payload` (jsonb), `correct_subitems`, `total_subitems`, `raw_points_awarded`. |
| **Backend** | `ExamSession.java` | Map các cột DB: `raw_score`, `max_raw_score`. |
| **Backend** | `SessionResponse.java` | Bổ sung `rawScore` và `maxRawScore` vào DTO trả về. |
| **Backend** | `SessionServiceImpl.java` | Cập nhật `submitAnswer` xử lý 4 dạng thức câu hỏi; cập nhật `serverSubmit` tính điểm chuẩn 150; cập nhật `clientSubmit` lưu payload. |
| **Backend** | `SessionServiceTest.java` | Bổ sung 3 unit tests: `TRUE_FALSE` tiered scoring, `MATCHING` scoring, `SHORT_ANSWER` numeric evaluation. |
| **Android** | `ExamHistoryRepository.java` | Fix atomic rename dùng `Files.move(..., REPLACE_EXISTING)` đảm bảo tương thích file system Windows JVM khi test. |
| **Android** | `KaTeXHelper.java` | Tiện ích kiểm tra math delimiters, escape HTML và render KaTeX qua WebView local assets. |
| **Android** | `KaTeXHelperTest.java` | Unit tests cho `KaTeXHelper` (detection, escaping, HTML template). |
| **Android** | `activity_exam_session.xml` | Thêm `wvQuestionMath` vào layout câu hỏi. |
| **Android** | `ExamSessionActivity.java` | Gọi KaTeX render khi câu hỏi có công thức; truyền `answerPayload` khi tương tác Đúng/Sai, Ghép cặp, Điền số. |
| **Android Assets** | `app/src/main/assets/katex/` | Đóng gói `katex.min.js`, `katex.min.css`, `auto-render.min.js` và 20 font KaTeX woff2/ttf. |

---

## 3. Kết quả kiểm thử

- **Backend Unit Tests**: `SessionServiceTest` (42/42 tests pass). Toàn bộ test suite backend hoàn thành trong 8s không lỗi.
- **Android Unit Tests**: 222/222 tests pass (bao gồm 3 test mới của `KaTeXHelperTest`).
- **Android Build**: `./gradlew assembleDebug` tạo file APK thành công.
- **Web Admin**: Oxlint check sạch 100%, Vite build hoàn tất trong 1.58s.
- **Đánh giá Claude CLI**: Xác nhận cả hai suite đều pass 100% (0 fail / 0 error / 0 skip).

---

## 4. Kết luận & Hướng phát triển

- Hệ thống đã sẵn sàng hỗ trợ trọn vẹn luồng làm bài và nộp bài với 4 dạng thức câu hỏi chuẩn V-SAT 2026 từ ứng dụng di động đến server.
- Công thức toán học KaTeX được hiển thị tối ưu và hoạt động cả khi offline.
