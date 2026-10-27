# Prompt cho Cursor — F6 (Flashcard / spaced-repetition) + F7 (Analytics tiến bộ)

> Copy toàn bộ phần dưới dán vào Cursor.

---

## NGỮ CẢNH DỰ ÁN

**V-SAT Compass** — app luyện thi V-SAT, 1 repo:

- **Android (Java):** `app/src/main/java/com/example/v_sat_compass/`
  - `data/repository/ExamHistoryRepository.java` — mẫu lưu local JSON: atomic write (`.tmp`+`renameTo`), cap, corrupt-recovery
  - `data/repository/GamificationRepository.java` — mẫu local state JSON (F3, vừa làm)
  - `data/repository/StudentStatsRepository.java`, `ExamHistoryEntry` (lịch sử bài làm)
  - `ui/home/HomeFragment.java`, `ui/practice/PracticeFragment.java`, `ui/profile/ProfileFragment.java`
  - `ui/exam/session/ExamResultActivity.java` (đang render weak-topics)
- **Backend (Spring Boot):** `VSAT/vsat-compass-api/...` — có `session_answers` (lưu từng câu), `/my-stats/topics`, `/my-stats/weak-topics`

**Bối cảnh:** Neon treo → demo Postgres local Docker. `ddl-auto: validate`, không Flyway → thêm bảng phải kèm DDL tường minh (`docs/sql/`, mẫu `0001_create_tickets.sql`) + áp local. Làm + verify LOCAL trước.

## RÀNG BUỘC CHUNG
- KHÔNG commit / tag / deploy / prod smoke. Chỉ code + review.
- KHÔNG revert/commit file dirty cũ; nếu commit sau này: **không co-author**.
- Giữ local-first; tái dùng pattern repo local có sẵn; auth student cho endpoint student.
- Thêm bảng DB → kèm DDL tường minh + áp local; KHÔNG đổi `ddl-auto`.

---

## F6 — Flashcard / spaced-repetition (P2)

**Mục tiêu:** Bộ thẻ ôn (từ vựng Tiếng Anh + công thức Toán/Lý), lặp lại ngắt quãng kiểu **SM-2 đơn giản**. Local-first hoàn toàn; sync sau (không bắt buộc backend lúc này).

| ID | Hạng mục | Tiêu chí |
|----|----------|----------|
| F6.1 | Model `Flashcard` (front/back/deck/subject) + `FlashcardRepository` lưu tiến độ local JSON (atomic, corrupt-recovery theo mẫu) | lưu ease/interval/nextReview mỗi thẻ |
| F6.2 | Bộ thẻ seed ban đầu đóng gói trong app (asset JSON): vài deck mẫu cho Tiếng Anh + Toán | hiển thị được ngay khi cài |
| F6.3 | Thuật toán SM-2 rút gọn: người dùng tự đánh giá (Again/Hard/Good/Easy) → cập nhật interval + nextReview | JVM unit test cho lịch lặp |
| F6.4 | UI ôn thẻ: lật thẻ, đánh giá, "đến hạn hôm nay"; điểm vào từ Home hoặc Practice | empty state khi hết thẻ đến hạn |
| F6.5 | (Tùy chọn) cộng XP/đếm hoạt động gamification (F3) khi ôn thẻ | streak tính cả ngày ôn thẻ |

**Nghiệm thu F6 (LOCAL):** ôn vài thẻ, đánh giá Good/Again → thẻ Again quay lại sớm, Good giãn ra; thoát/mở lại app giữ tiến độ; `./gradlew :app:assembleDebug` PASS.

---

## F7 — Analytics tiến bộ theo thời gian (P2)

**Mục tiêu:** Biểu đồ xu hướng điểm qua các lần thi + phân tích thời gian/câu. **Dữ liệu đã có** (`ExamHistoryEntry` local + `session_answers` backend) — chủ yếu là dựng tổng hợp + màn biểu đồ.

| ID | Hạng mục | Tiêu chí |
|----|----------|----------|
| F7.1 | Local-first: dựng dữ liệu xu hướng từ `ExamHistoryRepository` (điểm theo thời gian, theo môn) | chạy offline |
| F7.2 | (Tùy chọn, khi online) endpoint tổng hợp từ `session_answers`: thời gian trung bình/câu, phân bố đúng-sai theo độ khó | nếu thêm bảng/không cần thì thôi; ưu tiên tận dụng dữ liệu sẵn |
| F7.3 | Màn "Tiến bộ": biểu đồ đường điểm qua các lần thi + breakdown theo môn/chủ đề | empty state khi chưa đủ dữ liệu |
| F7.4 | Thư viện biểu đồ: ưu tiên giải pháp nhẹ; nếu thêm dependency (vd MPAndroidChart) thì nêu rõ và giữ tối thiểu | build vẫn PASS |

**Nghiệm thu F7 (LOCAL):** làm 2-3 bài → màn Tiến bộ hiển thị đường điểm + breakdown; chưa có dữ liệu → empty state; `./gradlew :app:assembleDebug` PASS; nếu thêm endpoint/bảng thì test + DDL kèm theo.

---

## BÁO CÁO KHI XONG
Liệt kê file thêm/sửa (tách F6 / F7), dependency mới nếu có, DDL mới nếu có, cách verify, và chỗ điều chỉnh ngoài phạm vi.
