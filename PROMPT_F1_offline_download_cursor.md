# Prompt cho Cursor — Triển khai F1: Tải đề FREE làm offline + sync

> Copy toàn bộ phần dưới dán vào Cursor.

---

## NGỮ CẢNH DỰ ÁN

**V-SAT Compass** — app luyện thi V-SAT, 1 repo gồm 2 phần:

- **Android (Java):** `app/src/main/java/com/example/v_sat_compass/`
  - `data/repository/`: `ExamRepository`, `StudentStatsRepository`, `SessionContentRepository`, `ExamHistoryRepository`, `AdminUserRepository`
  - `util/`: `LocalExamDataSource` (sample_*.json offline fallback), `OfflineDemoDataHelper`, `NetworkUtils`
  - `ui/exam/session/`: `ExamSessionActivity`, `ExamResultActivity`
  - `data/api/`: `ApiClient` (Retrofit), các `*Api`
  - Flag liên quan: `USE_BACKEND_EXAM_CONTENT` (đang `true` → làm bài bắt buộc gọi backend)
- **Backend (Spring Boot, Java):** `VSAT/vsat-compass-api/src/main/java/com/vsatcompass/api/`
  - `controller/student/`: `MyStatsController`, `SessionController` (mẫu controller student)
  - `dto/response/`: có `PublicQuestionForStudentResponse` — **cố ý strip** `correctAnswer`, `isCorrect`, `explanation` để chống lộ đáp án; câu hỏi trả theo từng session.
  - entity: `Exam`, `Question`, `QuestionOption` (đáp án đúng + lời giải), `ExamStatus` (có `PUBLISHED`), exam FREE + `price=0`.
  - `exam_sessions`, `session_answers` tables; anti-replay 409 đã có ở `client-submit`.

**Hiện trạng quan trọng:** App quảng cáo "local-first" nhưng luồng LÀM BÀI lại bắt buộc backend: `USE_BACKEND_EXAM_CONTENT=true` → phải gọi `startSession`; mất mạng là `finish()`. Có lỗi màn đen khi bấm "Làm bài" trong một số trường hợp. Neon DB cloud đang treo → đang chạy local Docker để demo (mọi thứ verify LOCAL trước, prod smoke để sau).

## QUYẾT ĐỊNH ĐÃ CHỐT (tuân thủ tuyệt đối)

- Tải đề offline **CHỈ cho đề FREE + price=0 + status=PUBLISHED**. Đề tính tiền: KHÔNG đụng tới (defer).
- Gói đề tải về **mã hoá nhẹ AES bằng key của app** (chống tò mò mở file xem đáp án; không cần bảo mật tuyệt đối).
- **KHÔNG** dùng AI chấm thi.
- Endpoint thường trong session **vẫn giữ nguyên** việc strip đáp án (`PublicQuestionForStudentResponse`); chỉ bundle tải-về mới chứa đáp án.

## MỤC TIÊU F1

Mỗi item đề FREE có ô "Tải xuống". Đề đã tải → làm bài **hoàn toàn offline** (bỏ qua `startSession` mạng), chấm tại máy, xem review offline; kết quả vào hàng đợi và **tự sync khi có mạng**. Tiện thể **vá lỗi màn đen / luồng làm bài chưa offline thuần**.

## HẠNG MỤC TRIỂN KHAI

### F1-A — Backend: download bundle endpoint
1. Thêm `GET /exams/{id}/download-bundle` ở controller student.
   - Chỉ trả khi đề `status=PUBLISHED` **và** FREE (`price=0`). Ngược lại: `404 RESOURCE_NOT_FOUND` (chống enumeration, đồng bộ convention hiện có).
   - Yêu cầu role `STUDENT` như các endpoint student khác.
2. DTO mới `ExamDownloadBundleResponse` (KHÔNG sửa `PublicQuestionForStudentResponse`): gồm metadata đề + danh sách câu hỏi đầy đủ **có** `correctAnswer` + `explanation` + options, và field version (`examVersion` hoặc `updatedAt`).
3. Service + Mockito test: FREE+PUBLISHED → pass; non-FREE → reject; non-PUBLISHED → reject.

### F1-B — Android: download manager + lưu local (mã hoá)
1. Ô/nút "Tải xuống" trên item đề trong exam list — **chỉ hiện với đề FREE**. 3 trạng thái: chưa tải / đang tải / đã tải.
2. `OfflineExamRepository`: tải bundle, ghi xuống internal storage (file riêng từng đề), **mã hoá AES bằng key app** khi ghi, giải mã khi đọc. Atomic write (tham khảo pattern trong `ExamHistoryRepository`). Hỗ trợ xoá đề đã tải.
3. Màn "Đề đã tải": liệt kê đề đã tải + tổng dung lượng + nút xoá.

### F1-C — Android: luồng làm bài offline + sync
1. `ExamSessionActivity` rẽ nhánh: nếu đề đã tải → đọc nội dung từ `OfflineExamRepository`, **bỏ qua `startSession` mạng**. Mất mạng vẫn vào làm bài được (không `finish()`).
2. Chấm điểm tại máy từ đáp án trong bundle; dựng review (đúng/sai/đáp án/lời giải) từ dữ liệu local.
3. Hàng đợi sync: kết quả offline lưu `ExamHistoryEntry` local + queue; khi `NetworkUtils.isOnline` → gọi `client-submit` đẩy lên; idempotent (đã có anti-replay 409 nên trùng không tạo bản mới).
4. Vá lỗi màn đen "Làm bài" trong nhánh offline.

## RÀNG BUỘC

- **KHÔNG** sửa `PublicQuestionForStudentResponse` hay làm rò đáp án ở endpoint session thường.
- **KHÔNG** cho tải đề non-FREE.
- **KHÔNG** commit (chỉ code + để onii-chan review). Nếu có commit về sau: docs/commit message **không co-author**, không kéo theo config demo dirty (`application.yml`, `app/build.gradle.kts`, `ApiClient.java`, `gradle.properties`).
- **KHÔNG** đụng git tag, không deploy, không prod smoke (Neon đang treo).
- Tái dùng hạ tầng sẵn có (`NetworkUtils`, `ExamHistoryRepository`, `SessionContentRepository`, `client-submit`) thay vì dựng song song.

## NGHIỆM THU

- Backend: `./gradlew test` cho test mới PASS (FREE pass / non-FREE + non-PUBLISHED reject).
- Android: `./gradlew :app:assembleDebug` PASS.
- Luồng end-to-end (LOCAL): còn mạng → tải 1 đề FREE → **bật airplane mode** → vào làm trọn bài (50 câu không crash, rotate/background giữ state) → xem kết quả + review offline → tắt airplane mode → kết quả tự sync lên backend, **không tạo session trùng**.
- File đề đã tải trên máy mở trực tiếp KHÔNG đọc được đáp án (đã mã hoá).
- Báo lại: file đã thêm/sửa, cách verify, và bất kỳ chỗ nào phải điều chỉnh ngoài phạm vi trên.
