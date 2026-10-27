# Prompt cho Cursor — Commit docs sync lên GitHub (KHÔNG co-author)

> Copy toàn bộ phần dưới dán vào Cursor.

---

## NGỮ CẢNH

Repo **V-SAT Compass** (Android Java + backend Spring Boot trong cùng repo). Branch hiện tại `main`, đã track `origin/main`, HEAD = `40cec10`.

Vừa hoàn tất một đợt **đồng bộ tài liệu** (docs-only) cho khớp HEAD `40cec10`. Các thay đổi đang nằm ở working tree, **chưa commit**. Giờ cần commit + push chỉ phần docs này lên GitHub.

## TRẠNG THÁI WORKING TREE (kiểm chứng bằng `git status` trước khi làm)

Đã sửa (modified):
- `task.md`, `README.md`, `CHANGELOG.md` ← **CHỈ commit 3 file này**
- `VSAT/vsat-compass-api/src/main/resources/application.yml`
- `app/build.gradle.kts`
- `app/src/main/java/com/example/v_sat_compass/data/api/ApiClient.java`
- `gradle.properties`

Untracked: `.agents/`, `AGENTS.md`, `PROMPT_sync_docs_cursor.md`, `PROMPT_commit_docs_cursor.md`, `VSAT/vsat-compass-api/.dockerignore`, `VSAT/vsat-compass-api/docker-compose.yml`, `bootrun.log`, `log2.txt`, `login.json`, `scripts/`, `temp/`

## YÊU CẦU

1. Stage **chỉ** 3 file: `task.md`, `README.md`, `CHANGELOG.md`. Dùng `git add task.md README.md CHANGELOG.md` (KHÔNG dùng `git add .` / `git add -A`).
2. Commit với message:
   ```
   docs: sync task.md/README/CHANGELOG with HEAD 40cec10

   Cập nhật con trỏ HEAD từ be545ba sang 40cec10, đánh dấu fix
   /admin/users Specification đã committed trong 40cec10 (giữ caveat
   chưa prod smoke), và làm rõ v1.0.1 là tổ tiên của HEAD.
   Docs-only, không đụng src/main.
   ```
3. Push lên `origin main`.

## RÀNG BUỘC (rất quan trọng)

- **TUYỆT ĐỐI KHÔNG thêm co-author.** Commit message **không** được có dòng `Co-authored-by:` nào (kể cả Cursor, AI, hay bất kỳ ai). Không thêm trailer "Generated with", "Co-authored-by: Cursor", v.v. Commit chỉ đứng tên author git mặc định của máy.
- **KHÔNG** stage / commit 4 file config demo (`application.yml`, `app/build.gradle.kts`, `ApiClient.java`, `gradle.properties`) — đây là config chạy demo local, cố ý để dirty, KHÔNG đưa lên repo.
- **KHÔNG** commit các file untracked (logs, scripts/, temp/, login.json, docker-compose, các file PROMPT_*.md...).
- **KHÔNG** sửa thêm bất kỳ file nào, không sửa src/main.
- **KHÔNG** tạo / xóa / di chuyển git tag.
- Không amend hay rebase commit `40cec10` hoặc các commit cũ.

## NGHIỆM THU

- `git log -1 --format='%an%n%b'` cho thấy commit mới **không có** dòng `Co-authored-by:`.
- `git show --stat HEAD` chỉ liệt kê đúng 3 file: `task.md`, `README.md`, `CHANGELOG.md`.
- `git status` sau push: 4 file config demo vẫn ở trạng thái modified (chưa commit), các untracked vẫn untracked.
- `git log origin/main -1` trùng commit vừa push.
- Báo lại commit hash + message để xác nhận.
