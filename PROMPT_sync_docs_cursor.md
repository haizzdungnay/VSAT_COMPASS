# Prompt cho Cursor — Đồng bộ tài liệu (task.md + README) với HEAD thật

> Copy toàn bộ phần dưới (trong khối) dán vào Cursor. Đã kèm ngữ cảnh dự án để AI tự hiểu việc cần làm.

---

## NGỮ CẢNH DỰ ÁN

Đây là **V-SAT Compass** — app luyện thi V-SAT gồm 2 phần trong cùng 1 repo:

- **Android app** (Java): `app/src/main/java/com/example/v_sat_compass/...`
  - Luồng học viên: login, kho đề, làm bài client-side, nộp bài, kết quả, review lời giải, lịch sử, luyện tập theo chủ đề.
  - Có admin mode trong cùng app (dashboard, user management).
- **Backend Spring Boot** (Java): `VSAT/vsat-compass-api/src/main/java/com/vsatcompass/api/...`
  - Auth + JWT, sessions, subjects/topics, question bank + review workflow, exam CRUD, admin stats, user management, my-stats.
  - Deploy production trên Render.com; DB Neon PostgreSQL (hiện Neon đang treo vì vượt quota Free → đang chạy local Docker `vsat-pg` để demo).

**Tài liệu theo dõi tiến độ:**
- `task.md` (gốc repo) — task tracker / roadmap chính, có header ghi commit HEAD.
- `README.md` (gốc repo) — có dòng "Trạng thái hiện tại" + bảng endpoint.
- `CHANGELOG.md` (gốc repo) — đã có mục `[Unreleased]`.

## TRẠNG THÁI GIT THỰC TẾ (đã kiểm chứng — coi đây là sự thật)

- HEAD hiện tại = `40cec10` "Complete local demo smoke and profile stats flow", **đang nằm trên `main` và đã push `origin/main`**.
- `be545ba` "feat: real data for admin, exams, and practice with offline demo" = commit **cha** của `40cec10`, **không có tag**.
- `53111e6` "update ui" = tag **`v1.0.1`**, là **cha của `be545ba`** (nên `v1.0.1` KHÔNG bao gồm batch real data lẫn commit `40cec10`).
- Tag `v1.0.0` **không tồn tại** trong repo.
- Working tree đang dirty với **config demo local chưa commit** (cố ý, KHÔNG được commit): `VSAT/vsat-compass-api/src/main/resources/application.yml`, `app/build.gradle.kts`, `app/src/main/java/com/example/v_sat_compass/data/api/ApiClient.java`, `gradle.properties`, cùng vài file/thư mục untracked (`scripts/`, `temp/`, `bootrun.log`, `log2.txt`, `login.json`...).

## VẤN ĐỀ CẦN SỬA (tài liệu lỗi thời so với code)

Code thực tế đã khớp/đủ, nhưng tài liệu chưa được đồng bộ sau commit `40cec10`:

1. **`task.md` dòng 3 (header)** vẫn ghi `HEAD: be545ba`. Sai — HEAD thật là `40cec10`. Header chưa hề nhắc tới commit `40cec10` (commit này mới là nơi chứa các việc ngày 2026-06-18: weak-topics, profile stats, session answers, smoke local).
2. **`task.md` dòng ~621** ghi fix `/admin/users` (đổi JPQL nullable filter → JPA `Specification`) là *"⚠️ chưa commit, đang ở working tree"*. Sai — fix này **đã commit** trong `40cec10` (`UserRepository` đã `extends ... JpaSpecificationExecutor<User>`, `AdminUserServiceImpl` dùng `Specification<User> buildUserFilter(...)`). Working tree giờ chỉ còn config demo.
3. **`README.md` dòng ~174** vẫn ghi *"Trạng thái hiện tại (HEAD `be545ba`, untagged ... verify prod tới v0.10.2)"*. Cần cập nhật con trỏ HEAD sang `40cec10`.

## YÊU CẦU

Cập nhật **chỉ tài liệu** (`task.md`, `README.md`, và nếu cần `CHANGELOG.md`) để khớp HEAD `40cec10`:

- Sửa header `task.md` (dòng 3): HEAD = `40cec10`; mô tả ngắn `40cec10` (local demo smoke + profile/stats flow), nêu rõ `be545ba` là cha, `v1.0.1`@`53111e6` vẫn là tag gần nhất nhưng là tổ tiên của HEAD (chưa bao gồm `be545ba`/`40cec10`).
- Sửa ghi chú dòng ~621: đánh dấu fix `/admin/users` Specification **đã commit trong `40cec10`** (bỏ "chưa commit"). Giữ nguyên caveat **vẫn chưa prod smoke**.
- Cập nhật dòng "Trạng thái hiện tại" trong `README.md` trỏ HEAD `40cec10`; giữ các cảnh báo "chưa prod smoke / chưa tag" vì vẫn đúng.
- Rà soát `task.md` xem còn chỗ nào khẳng định việc đã-có-thật là "TODO/chưa làm" mà thực tế đã có trong code không (ví dụ C4.2 weak-topics `GET /my-stats/weak-topics` đã tồn tại trong `MyStatsController`).

## RÀNG BUỘC (rất quan trọng)

- **KHÔNG** sửa file trong `src/main/**` của cả backend lẫn Android. Đây là task tài liệu thuần.
- **KHÔNG** commit các thay đổi config demo đang ở working tree (`application.yml`, `build.gradle.kts`, `ApiClient.java`, `gradle.properties`). Để nguyên.
- **KHÔNG** tạo / xóa / di chuyển bất kỳ git tag nào (quyết định tag chờ chủ dự án).
- **KHÔNG** chạy deploy, không chạy prod smoke, không sửa quota Neon.
- Không bịa số liệu: mọi mốc commit/tag dùng đúng dữ kiện ở mục "TRẠNG THÁI GIT THỰC TẾ".

## NGHIỆM THU

- `task.md` header + dòng ~621 và `README.md` dòng trạng thái đều trỏ `40cec10`, không còn câu nào ghi fix `/admin/users` "chưa commit".
- Không có thay đổi nào ngoài `task.md` / `README.md` / `CHANGELOG.md`.
- `git status` xác nhận chỉ các file tài liệu trên bị sửa (cộng các file demo vốn đã dirty từ trước).
- Tóm tắt lại các dòng đã đổi để người review xác nhận.
