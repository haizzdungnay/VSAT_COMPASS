# Báo cáo Clean-Room Verification (Bước 5b) — Hệ thống V-SAT Compass

- **Ngày kiểm tra**: 2026-11-13
- **Môi trường**: Local clean-room (`127.0.0.1:18080` Spring Boot API mới khởi động từ workspace, `127.0.0.1:55432` PostgreSQL container `vsat-clean-pg`, database `vsat_compass_acceptance`, `127.0.0.1:18081` LLM mock).
- **Tổng kết**: **36 probe** — **33 PASS**, **3 FAIL**, **0 BLOCKED**.

## Lệnh thực thi & Trạng thái tài nguyên

1. Kiểm tra & giải phóng cổng 18080 trước khi chạy:
   - `powershell.exe -NoProfile -Command "... Stop-Process -Id 36780 -Force ... Get-NetTCPConnection -LocalPort 18080 -State Listen"` -> Exit code `1` (cổng 18080 trống, không có tiến trình lắng nghe).
2. Chạy suite clean-room:
   - `cd "/e/TuanDuong/lap trinh kiem com/Java/VSAT_COMPASS/acceptance-harness" && node run-all.mjs --suite cleanroom/cleanroom-probes.mjs --skip-verdict`
   - Exit code: `0`. Kết quả suite (`acceptance-harness/cleanroom/cleanroom-probes.json`): `totalCases: 36`, `passedCount: 33`, `failedCount: 3` (`[RUN-ALL] Step 8: Tearing down acceptance harness resources... - Spawned Backend process terminated.`).
3. Kiểm tra cổng 18080 sau khi chạy:
   - `powershell.exe -NoProfile -Command "Get-NetTCPConnection -LocalPort 18080 -State Listen -ErrorAction SilentlyContinue; docker ps --filter name=vsat-clean-pg --format '{{.Names}} {{.Status}}'"` -> `vsat-clean-pg Up 2 hours (healthy)`, không còn tiến trình nào nghe trên cổng `18080`.

---

## Bảng kết quả 36 Probes (CR-01 .. CR-36)

| ID | Mô tả | Trạng thái | Thời lượng | Ghi chú thực tế (HTTP + DB) |
|---|---|---|---|---|
| CR-01 | Publish đề chuẩn 25 câu (9/6/5/5, 90 phút MATH) | **FAIL** | 9650ms | HTTP `200`, `exams.status='PUBLISHED'`, `SUM(max_raw_score)=150.00`, nhưng `exam_sections` trả rỗng `""` (thiếu 4 section). |
| CR-02 | Publish đề thiếu câu (< 25 câu) bị chặn | PASS | 2814ms | HTTP `400` (`VF_COUNT`), DB giữ `PENDING_REVIEW`. |
| CR-03 | Publish đề 25 câu sai cơ cấu (25 SC) bị chặn | PASS | 3886ms | HTTP `400` (`VF_MIX`), DB giữ `PENDING_REVIEW`. |
| CR-04 | Publish đề 9/6/5/5 sai thứ tự phần bị chặn | PASS | 1744ms | HTTP `400` (`VF_ORDER`), DB giữ `PENDING_REVIEW`. |
| CR-05 | Tạo/publish đề Toán 60 phút sai chuẩn bị chặn | PASS | 593ms | `POST /admin/exams` trả `400` (`subject MATH requires 90 minutes, got 60`), DB 0 row. |
| CR-06 | Đề Toán gắn câu ENG bị chặn | PASS | 2228ms | Tạo đề trả `201`; thêm câu ENG trả `400` (`Question subject (2) does not match exam subject (1)`). |
| CR-07 | Publish đề có câu Đúng/Sai chỉ 3 mệnh đề bị chặn | PASS | 2506ms | HTTP `400` (`VF_TF4`), DB giữ `PENDING_REVIEW`. |
| CR-08 | `CONTENT_ADMIN` không được publish đề | PASS | 1396ms | HTTP `403`, DB giữ `PENDING_REVIEW` & `publish_date IS NULL`. |
| CR-09 | `COLLABORATOR` không được publish đề | PASS | 1464ms | HTTP `403`, DB giữ `PENDING_REVIEW`. |
| CR-10 | `STUDENT` & `ANONYMOUS` không được publish đề | PASS | 2253ms | `STUDENT` `403`, `ANONYMOUS` `401`, DB giữ `PENDING_REVIEW`. |
| CR-11 | Sinh đề theo blueprint MATH đủ 25 câu 9/6/5/5 đúng thứ tự | PASS | 2607ms | HTTP `200`, `selectedQuestions=25`, `rawScore=150`, DB đúng thứ tự phần. |
| CR-12 | Sinh đề vào đề `PUBLISHED` trả 409, không ghi đè | PASS | 2174ms | HTTP `409` (`INVALID_STATE`), `exam_questions` giữ nguyên. |
| CR-13 | Sinh đề khi thiếu câu trả cảnh báo hoặc từ chối | PASS | 2425ms | HTTP `200` kèm `generationWarnings` (3 phần thiếu), `selectedQuestions=6`. |
| CR-14 | `rawScore` khi sinh đề thiếu câu bằng tổng thực tế (< 150) | PASS | 3973ms | Tạo đề `201`; sinh trả `200` với `rawScore=36` khớp DB `SUM(max_raw_score)=36`. |
| CR-15 | Blueprint có `totalQuestions=10` bị từ chối | PASS | 1286ms | HTTP `400`, DB 0 row. |
| CR-16 | Blueprint 25 câu sai cơ cấu (25 SC) bị từ chối | PASS | 1482ms | HTTP `400`, DB 0 row. |
| CR-17 | Blueprint có cell `questionCount=-1` bị từ chối | PASS | 1085ms | HTTP `400`, DB 0 row. |
| CR-18 | Blueprint trỏ `scoringProfileId=999999` bị từ chối | PASS | 693ms | HTTP `400`, DB 0 row. |
| CR-19 | Blueprint seed mặc định đủ 25 câu / 150 điểm & profile hợp lệ | PASS | 527ms | HTTP `200`, DB không có `scoring_profile_id IS NULL` hay profile inactive. |
| CR-20 | Import `dryRun=true` không ghi DB | PASS | 1692ms | HTTP `200`, `validCount=2`, `errorCount=0`, `importedQuestions=0`, DB 0 row. |
| CR-21 | Import `dryRun=false` tạo câu trạng thái `PENDING_REVIEW` | PASS | 1929ms | HTTP `200`, `importedQuestions=1`, DB `status='PENDING_REVIEW'`, 0 `APPROVED`. |
| CR-22 | Import `questionType=ABC` bị báo lỗi, không ép `SINGLE_CHOICE` | PASS | 1388ms | HTTP `400` (`còn 1 dòng lỗi`), DB 0 row. |
| CR-23 | Import `SINGLE_CHOICE` đáp án `"A,B"` bị báo lỗi, không lưu DB | PASS | 1502ms | HTTP `400` (`còn 1 dòng lỗi`), DB 0 row. |
| CR-24 | Import câu `MATCHING` lưu 4 tiểu mục vào `question_matching_items` | **FAIL** | 3928ms | HTTP `200` (`importedQuestions=1`), nhưng DB `question_matching_items` khớp `question_matching_choices` = `0`. |
| CR-25 | Import câu `SHORT_ANSWER` số `12.5` lưu khóa `NUMERIC` | **FAIL** | 2197ms | HTTP `200` (`importedQuestions=1`), nhưng DB `question_short_answer_keys` (`NUMERIC`, `12.5`) = `0`. |
| CR-26 | Tạo `TRUE_FALSE` 3 mệnh đề trả 400 | PASS | 1221ms | HTTP `400`, DB 0 row. |
| CR-27 | Tạo `SINGLE_CHOICE` 2 phương án hoặc 2 đáp án đúng trả 400 | PASS | 1267ms | Cả 2 request trả `400`, DB 0 row. |
| CR-28 | Tạo `MATCHING` 2 cặp trả 400 | PASS | 701ms | HTTP `400`, DB 0 row. |
| CR-29 | Tạo `SHORT_ANSWER` không có đáp án đúng trả 400 | PASS | 1321ms | HTTP `400`, DB 0 row. |
| CR-30 | Bundle học sinh không lộ đáp án hay lời giải | PASS | 3958ms | HTTP `200`, quét đệ quy 0 key cấm, không có `"⇄"`, không có đáp án SA. |
| CR-31 | Bundle học sinh có `maxRawScore`, tổng 150, đúng thứ tự 9/6/5/5 | PASS | 11ms | HTTP `200`, 25 câu, từng câu `maxRawScore=6`, tổng `150`, đúng thứ tự. |
| CR-32 | Nộp đúng 25 câu qua server-side đạt `rawScore=150` | PASS | 1194ms | `start` `201`, lưu 25 câu `200`, `submit` `200`, `rawScore=150`, `correctCount=25`. |
| CR-33 | `client-submit` giả `score=100, correctCount=25` bị server tự chấm lại | PASS | 415ms | `start` `201`, `client-submit` `200` trả `rawScore=0`, `correctCount=0`, `score=0`, DB `0.00|0`. |
| CR-34 | Đề `PUBLISHED` khóa sửa metadata & thêm/xóa/đổi thứ tự câu | PASS | 2824ms | Cả 4 request trả `409` (`INVALID_STATE`), title & `exam_questions` giữ nguyên. |
| CR-35 | Đề `HIDDEN` khóa thêm/xóa/đổi thứ tự câu | PASS | 1125ms | Cả 3 request trả `409` (`INVALID_STATE`), `exam_questions` giữ nguyên. |
| CR-36 | Đề `HIDDEN` đổi sang môn khác khi còn câu môn cũ không publish lại được | PASS | 889ms | `PUT` đổi sang ENG (60p) trả `200`; `publish` trả `400` (`VF_SUBJECT`), không có đề `PUBLISHED` lệch môn. |

---

## Chi tiết các Probe FAIL (Trích đoạn & Vị trí nghi ngờ)

### 1. CR-01 — Thiếu bản ghi `exam_sections` khi publish đề
- **Kỳ vọng**: Sau `POST /admin/exams/{examId}/publish`, bảng `exam_sections` có đúng 4 phần (`TRUE_FALSE:9,SINGLE_CHOICE:6,MATCHING:5,SHORT_ANSWER:5`).
- **Thực tế**: HTTP `200`, `exams.status = 'PUBLISHED'`, `SUM(exam_questions.max_raw_score) = 150.00`, nhưng truy vấn `exam_sections` trả rỗng `""`.
- **Trích đoạn response**:
  ```json
  {"data":{"id":3,"examCode":"CR01_EXAM_635301_72285","subjectId":1,"subjectCode":"MATH","questionCount":25,"durationMinutes":90,"status":"PUBLISHED"},"message":"Exam published","success":true}
  ```
- **Vị trí nghi ngờ**: `VSAT/vsat-compass-api/src/main/java/com/vsatcompass/api/service/impl/AdminExamServiceImpl.java:323-339` (`publish` chỉ validate và lưu `Exam`, không sinh/lưu các bản ghi `exam_sections`).

### 2. CR-24 — Import `MATCHING` không tạo dòng trong `question_matching_items`
- **Kỳ vọng**: `POST /admin/questions/import?dryRun=false` với câu `MATCHING` hợp lệ phải tạo 4 dòng trong `question_matching_items` khớp `question_matching_choices`.
- **Thực tế**: HTTP `200`, `importedQuestions: 1`, `errorCount: 0`, nhưng truy vấn `JOIN question_matching_items` và `question_matching_choices` trả `0`.
- **Trích đoạn response**:
  ```json
  {"data":{"totalRows":1,"validCount":1,"errorCount":0,"importedQuestions":1,"dryRun":false,"items":[{"questionType":"MATCHING","questionText":"CR24_MATCH_635301_24254 Ghép nối 4 cặp","options":["Đáp A","Đáp B","Đáp C","Đáp D"],"correctAnswer":"1-C,2-A,3-D,4-B","valid":true}],"errors":[]},"message":"Import câu hỏi thành công (1 câu)","success":true}
  ```
- **Vị trí nghi ngờ**: `VSAT/vsat-compass-api/src/main/java/com/vsatcompass/api/service/impl/QuestionImportServiceImpl.java:910-933` (`saveQuestionAndOptions` nhánh `MATCHING` chỉ lưu `question_options` và `question_matching_choices`, không lưu `question_matching_items`).

### 3. CR-25 — Import `SHORT_ANSWER` không tạo khóa chấm trong `question_short_answer_keys`
- **Kỳ vọng**: `POST /admin/questions/import?dryRun=false` với câu `SHORT_ANSWER` (`12.5`) phải lưu 1 dòng `value_type = 'NUMERIC'`, `numeric_value = 12.5` vào `question_short_answer_keys`.
- **Thực tế**: HTTP `200`, `importedQuestions: 1`, `errorCount: 0`, nhưng truy vấn `question_short_answer_keys` trả `0`.
- **Trích đoạn response**:
  ```json
  {"data":{"totalRows":1,"validCount":1,"errorCount":0,"importedQuestions":1,"dryRun":false,"items":[{"questionType":"SHORT_ANSWER","questionText":"CR25_SA_635301_2298 Tính giá trị","options":["12.5","","",""],"correctAnswer":"12.5","valid":true}],"errors":[]},"message":"Import câu hỏi thành công (1 câu)","success":true}
  ```
- **Vị trí nghi ngờ**: `VSAT/vsat-compass-api/src/main/java/com/vsatcompass/api/service/impl/QuestionImportServiceImpl.java:900-909` (`saveQuestionAndOptions` nhánh `SHORT_ANSWER` chỉ lưu `question_options`, không lưu `question_short_answer_keys`).

---

## Ghi chú điều chỉnh probe so với Inventory (không sửa code sản phẩm)

1. `POST /admin/exams` (CR-06, CR-14) và `POST /sessions/start` (CR-32, CR-33) trả HTTP `201` (Created) thay vì `200`; probe đã cập nhật kỳ vọng `201`.
2. Template Excel của `/admin/questions/import` (CR-20..CR-25) dùng tên cột có khoảng trắng (`Question Text`, `Option A`..`D`) và `QuestionImportServiceImpl` yêu cầu cặp ghép dạng `1-C,2-A,3-D,4-B` (thay vì `1->C`).
3. Probe CR-36 gửi `{"subjectId": <ENG_ID>, "durationMinutes": 60}` trong bước `PUT /admin/exams/{examId}` (vì gửi thiếu `durationMinutes` bị từ chối `400` ngay do lệ thuộc `90` phút của môn MATH cũ) để kiểm chứng đúng chốt chặn `VF_SUBJECT` ở bước `publish`.

## Re-run sau T7/T8 (2026-11-16)

- **Lệnh**: `cd "/e/TuanDuong/lap trinh kiem com/Java/VSAT_COMPASS/acceptance-harness" && node run-all.mjs --suite cleanroom/cleanroom-probes.mjs --skip-verdict`
- **Exit code**: `0`.
- **Kết quả**: `36` probes — `33 PASS`, `3 FAIL`, `0 BLOCKED`.
- **FAIL**:
  - `CR-01`: `Assertion "assertDbRow" FAILED: expected "custom validation passed", got "" (DB exam_sections must have 4 sections 9/6/5/5 in exact order)`.
  - `CR-24`: `Assertion "assertDbRow" FAILED: expected "custom validation passed", got "0" (DB question_matching_items must have 4 rows matching question_matching_choices)`.
  - `CR-25`: `Assertion "assertDbRow" FAILED: expected "custom validation passed", got "0" (DB question_short_answer_keys must have NUMERIC key = 12.5)`.
- **So với lần trước**: Không đổi trạng thái; CR-01, CR-24, CR-25 vẫn FAIL, 33 ca còn lại PASS.
- **Kết quả JSON**: `acceptance-harness/cleanroom/cleanroom-probes.json`.
- **Ghi chú schema**: Grep xác nhận `exam_sections` chỉ được tạo trong `V6__create_vsat_scoring_and_sections.sql:28-40`; `question_matching_items` và `question_short_answer_keys` chỉ được tạo trong `V5__create_vsat_question_tables.sql:36-48,51-62`. Runtime code sản phẩm không ghi/đọc các bảng này; chỉ `acceptance-harness/db-bootstrap.mjs` chạm tới chúng. Đây là schema chết, không phải lỗi probe/gate.
- **Cổng sau chạy**: Suite để lại backend trên `18080`; đã dừng riêng tiến trình suite (PID `20820`, `13144`), kiểm tra lại cổng không còn LISTENING.
- **Nguồn lỗi JSON**: Các chuỗi lỗi trên trích từ trường `results[].error` trong file JSON.
- **DB mục tiêu**: `vsat-clean-pg`, `127.0.0.1:55432/vsat_compass_acceptance`; đã xác nhận trước chạy.
- **Cổng trước chạy**: `18080` không LISTENING.

