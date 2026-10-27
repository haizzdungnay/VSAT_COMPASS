# Prompt cho Cursor — F2 (Adaptive Practice) + F4 (AI giải thích lời giải)

> Copy toàn bộ phần dưới dán vào Cursor.

---

## NGỮ CẢNH DỰ ÁN

**V-SAT Compass** — app luyện thi V-SAT, 1 repo:

- **Android (Java):** `app/src/main/java/com/example/v_sat_compass/`
  - `ui/practice/PracticeFragment.java` (+ `PracticeTopicAdapter`) — màn luyện tập theo chủ đề, đã dùng weak-topics
  - `ui/exam/session/ExamReviewActivity.java` — xem lời giải từng câu (đúng/sai/đáp án/explanation)
  - `data/repository/StudentStatsRepository.java` — gọi `/my-stats/topics`, `/my-stats/weak-topics`
  - `data/api/` Retrofit + envelope `ApiResponse<T>`; `util/NetworkUtils.java`
- **Backend (Spring Boot, Java):** `VSAT/vsat-compass-api/src/main/java/com/vsatcompass/api/`
  - `controller/student/MyStatsController.java`, `SessionController.java` (mẫu controller student)
  - entity `Question`/`Subtopic`/`Topic`/`Subject`, enum `Difficulty` (EASY/MEDIUM/HARD hoặc tương tự — kiểm tra file thật), `QuestionStatus` (chỉ lấy câu APPROVED/đã publish)
  - DTO session strip đáp án: `SessionQuestionContentResponse`; **không** rò đáp án ngoài luồng review
  - `config/SecurityConfig.java` (rule cụ thể đặt TRƯỚC rule chung), `exception/AppException`, `ApiResponse`

**Bối cảnh:** Neon cloud treo → demo bằng Postgres local Docker (`vsat-pg`/`vsat_compass`). `ddl-auto: validate` ở cả dev/prod, không Flyway → **nếu thêm bảng mới phải kèm DDL tường minh** (xem `VSAT/vsat-compass-api/docs/sql/0001_create_tickets.sql` làm mẫu) và áp vào DB local. Làm + verify LOCAL trước, prod để sau.

## RÀNG BUỘC CHUNG (tuân thủ tuyệt đối)

- KHÔNG commit / tag / deploy / prod smoke. Chỉ code + để onii-chan review.
- KHÔNG revert/commit file dirty cũ (`application.yml`, `app/build.gradle.kts`, `ApiClient.java`, `gradle.properties`, `SessionController.java`, docs/docker/logs).
- Nếu sau này commit: message **không co-author**.
- Giữ local-first; tái dùng hạ tầng có sẵn; không làm rò đáp án ở luồng session thường.
- Auth: endpoint luyện tập/AI yêu cầu `ROLE_STUDENT`; rule SecurityConfig cụ thể đặt trước rule chung.
- Nếu thêm bảng DB: kèm file DDL tường minh trong `docs/sql/` + áp vào DB local (KHÔNG đổi `ddl-auto`).

---

## F2 — Adaptive Practice (P1)

**Mục tiêu:** Chế độ "Luyện thông minh": chọn câu theo chủ đề yếu (`/my-stats/weak-topics` đã có) và **nâng/giảm độ khó theo chuỗi đúng/sai** trong phiên, mô phỏng cách thi adaptive.

### F2-A — Backend
| ID | Hạng mục | Tiêu chí |
|----|----------|----------|
| F2-A.1 | `GET /practice/questions?subtopicId=&difficulty=&limit=` (role STUDENT): trả câu đã **strip đáp án** (tái dùng/đồng dạng `SessionQuestionContentResponse`), chỉ lấy câu APPROVED/publish | không rò `correctAnswer`/`explanation` |
| F2-A.2 | `POST /practice/grade` (role STUDENT): nhận `questionId` + đáp án chọn, trả đúng/sai (+ explanation cho riêng câu vừa làm) | chấm server-side để không lộ đáp án trước |
| F2-A.3 | Mockito test: lọc theo subtopic+difficulty, chấm đúng/sai | test PASS |

> Nếu thiết kế cho phép chấm tại client an toàn hơn thì vẫn phải đảm bảo không trả cả đáp án của các câu **chưa làm**. Ưu tiên chấm từng-câu server-side.

### F2-B — Android
| ID | Hạng mục | Tiêu chí |
|----|----------|----------|
| F2-B.1 | Vào "Luyện thông minh" từ `PracticeFragment` (chọn 1 chủ đề yếu) | dùng dữ liệu weak-topics hiện có |
| F2-B.2 | Thuật toán adaptive client: đúng N câu liên tiếp (vd 3) → tăng difficulty; sai → giảm; bắt đầu ở MEDIUM | có JVM unit test cho logic chọn độ khó |
| F2-B.3 | UI luyện: hiển thị câu, chấm từng câu, hiện đúng/sai + giải thích ngay; cập nhật thống kê sau phiên | empty/error state |

**Nghiệm thu F2 (LOCAL):** luyện 1 chủ đề yếu → độ khó câu tự điều chỉnh theo kết quả; thống kê chủ đề cập nhật sau phiên; `./gradlew :app:assembleDebug` PASS; backend test mới PASS.

---

## F4 — AI giải thích lời giải (P1) — KHÔNG phải AI chấm thi

**Mục tiêu:** Trong màn review câu (đặc biệt câu sai), nút "Giải thích thêm" gọi LLM diễn giải bước giải / vì sao đáp án đúng. **Tuyệt đối không** dùng AI để chấm điểm hay quyết định đúng/sai.

### Quyết định kỹ thuật (đã chốt)
- Backend làm **proxy** gọi LLM theo chuẩn **OpenAI-compatible** (endpoint `POST {LLM_BASE_URL}/chat/completions`). Đây là **sản phẩm thử nghiệm — ưu tiên chạy được ngay**, KHÔNG cần bảo mật.
- **Giá trị config cụ thể (dùng luôn để chạy):**
  - `LLM_BASE_URL` = `http://localhost:20128/v1` (router chạy local cùng máy backend; nếu backend không cùng máy thì dùng tunnel `https://ri2m8vp.abc-tunnel.us/v1`)
  - `LLM_API_KEY` = `sk-2da3930f0a6faf36-5tdbtv-3dbc4d73`
  - `LLM_MODEL` = `gemini/gemini-2.5-flash-lite`
- **Cách nạp:** đọc qua env/`@Value` với 3 default trên để backend chạy được ngay không cần set env. Nhưng **đặt giá trị thật vào một file config LOCAL được .gitignore** (vd `VSAT/vsat-compass-api/.env.local` hoặc `application-local.yml` đã ignore) — KHÔNG ghi key vào source được commit / `application.yml` / file nào sẽ lên git, để key không lọt vào lịch sử khi push GitHub. Thiếu config → endpoint trả lỗi rõ ràng (`503`/`badRequest` "LLM chưa cấu hình"), không crash.
- Đảm bảo file chứa key nằm trong `.gitignore` (thêm dòng ignore nếu chưa có).

### F4-A — Backend
| ID | Hạng mục | Tiêu chí |
|----|----------|----------|
| F4-A.1 | `POST /ai/explain` (role STUDENT): nhận `questionId` (+ optional đáp án người dùng đã chọn), backend dựng prompt từ nội dung câu + đáp án đúng + lời giải gốc, gọi LLM, trả giải thích | key giữ server-side; rate-limit cơ bản |
| F4-A.2 | Cấu hình LLM qua env (`LLM_BASE_URL`/`LLM_API_KEY`/`LLM_MODEL`, mặc định model `gemini/gemini-2.5-flash-lite`); thiếu env → lỗi rõ ràng | không lộ key xuống client; key chỉ từ env |
| F4-A.3 | Cache giải thích theo `questionId` (bảng hoặc cache đơn giản) để đỡ gọi lại tốn phí | lần 2 lấy từ cache. **Nếu dùng bảng DB mới → kèm DDL tường minh trong `docs/sql/` + áp local** |
| F4-A.4 | Mockito test với LLM client mock (không gọi mạng thật trong test) | test PASS |

### F4-B — Android
| ID | Hạng mục | Tiêu chí |
|----|----------|----------|
| F4-B.1 | Nút "Giải thích thêm" trong `ExamReviewActivity` (+ layout) | có loading/empty/error state |
| F4-B.2 | `AiApi` + repository gọi `/ai/explain`, render giải thích | offline → báo cần mạng |

**Nghiệm thu F4 (LOCAL):**
- Với env LLM hợp lệ: bấm "Giải thích thêm" ở 1 câu → nhận giải thích; gọi lại lần 2 lấy từ cache.
- Thiếu env LLM: endpoint trả lỗi rõ ràng, app hiện thông báo, **không crash**.
- Test mock PASS; `./gradlew :app:assembleDebug` PASS; nếu thêm bảng cache thì DDL đã áp local + backend boot OK.

---

## BÁO CÁO KHI XONG
Liệt kê file thêm/sửa (tách F2 / F4), DDL mới nếu có, cách verify (kể cả trường hợp thiếu env LLM), và chỗ nào phải điều chỉnh ngoài phạm vi.
