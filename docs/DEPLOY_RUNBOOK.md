# V-SAT Compass API — Deploy Runbook

> Tài liệu vận hành backend V-SAT Compass API trên Render.com + Neon PostgreSQL.
> Cập nhật: 2026-04-23 | Phiên bản: v0.8.0

---

## 1. Prerequisites

### Tài khoản & Dịch vụ

| Service | URL | Ghi chú |
|---------|-----|---------|
| **Render.com** | https://dashboard.render.com | Host backend (Docker, free tier) |
| **Neon** | https://console.neon.tech | PostgreSQL serverless (ap-southeast-1) |
| **GitHub** | https://github.com/haizzdungnay/VSAT_COMPASS | Source repo |

### Tạo JWT Secret

```bash
# Tạo JWT secret mới (base64, 512-bit) — Linux / macOS / Git Bash
openssl rand -base64 64

# Lưu output — KHÔNG commit vào git, chỉ paste vào Render Dashboard
```

Nếu không có `openssl` trên Windows, dùng PowerShell:
```powershell
$bytes = New-Object byte[] 64
[System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
[Convert]::ToBase64String($bytes)
```

### Environment Variables cần thiết

| Variable | Mô tả | Ví dụ |
|----------|--------|-------|
| `DATABASE_URL` | JDBC URL tới Neon DB | `jdbc:postgresql://ep-xxx.aws.neon.tech/neondb?sslmode=require` |
| `DATABASE_USERNAME` | Neon username | `neondb_owner` |
| `DATABASE_PASSWORD` | Neon password | *(từ Neon Dashboard → Connection Details)* |
| `JWT_SECRET` | Base64 secret ≥256-bit | *(từ `openssl rand -base64 64`)* |
| `SPRING_PROFILES_ACTIVE` | Spring profile | `prod` |
| `CORS_ALLOWED_ORIGINS` | CORS allowlist (comma-separated) | `https://vsat-compass-api.onrender.com` |

---

## 2. First-time Deploy

### Bước 1: Tạo Render Web Service

1. Đăng nhập Render Dashboard → **New** → **Web Service**
2. Connect GitHub repo: `haizzdungnay/VSAT_COMPASS`
3. Cấu hình:
   - **Name:** `vsat-compass-api`
   - **Region:** Singapore
   - **Runtime:** Docker
   - **Dockerfile Path:** `VSAT/vsat-compass-api/Dockerfile`
   - **Docker Context Directory:** `VSAT/vsat-compass-api`
   - **Plan:** Free

### Bước 2: Set Environment Variables

Vào **Environment** tab → thêm từng biến theo bảng ở mục 1.

> ⚠️ **QUAN TRỌNG:** Không paste giá trị secret vào `render.yaml` hay bất kỳ file nào trong repo.

### Bước 3: Trigger Build

1. Nhấn **Manual Deploy** → **Deploy latest commit**
2. Chờ build hoàn tất (~3-5 phút cho lần đầu, Docker build + dependency download)
3. Kiểm tra logs: phải thấy `Started VsatCompassApiApplication in X seconds`

### Bước 4: Verify

```bash
# Health check
curl -s https://vsat-compass-api.onrender.com/api/v1/actuator/health
# Expected: {"status":"UP"}

# Login test
curl -s -X POST https://vsat-compass-api.onrender.com/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"student@vsat.com","password":"Student@123"}'
# Expected: 200 with accessToken + refreshToken
```

---

## 3. Routine Deploy

### Automatic Deploy

Mỗi khi push commit lên branch `main` (hoặc branch được cấu hình), Render tự động:

1. Detect push via GitHub webhook
2. Pull latest code
3. Build Docker image (multi-stage: JDK build → JRE runtime)
4. Deploy new container
5. Health check tại `/api/v1/actuator/health`
6. Nếu health check pass → route traffic sang container mới
7. Nếu health check fail → giữ container cũ, đánh dấu deploy failed

### Manual Deploy

Render Dashboard → Service → **Manual Deploy** → **Deploy latest commit**

### Kiểm tra deploy status

- Render Dashboard → Deploys tab → xem status (Live / Failed / In Progress)
- Logs tab → xem runtime logs

---

## 4. Rollback

### Cách 1: Render Dashboard (nhanh nhất)

1. Vào Render Dashboard → Service → **Deploys** tab
2. Tìm deploy trước đó (status = Live ở lần deploy trước)
3. Nhấn **⋮** → **Redeploy** trên commit đó
4. Chờ redeploy hoàn tất

### Cách 2: Git Revert + Push

```bash
# Revert commit cuối
git revert HEAD
git push origin main
# Render sẽ tự deploy lại với commit revert
```

### Cách 3: Git Reset (destructive — dùng khi cần)

```bash
git reset --hard <commit-hash-tốt>
git push --force origin main
# ⚠️ Force push — chỉ dùng khi không có ai khác đang làm việc trên repo
```

---

## 5. Secret Rotation

### JWT_SECRET Rotation

> ⚠️ Rotate JWT_SECRET sẽ **invalidate tất cả token hiện tại** — user phải đăng nhập lại.

**Quy trình:**

1. Tạo secret mới:
   ```bash
   openssl rand -base64 64
   ```

2. Cập nhật trên Render Dashboard:
   - Service → **Environment** tab
   - Sửa giá trị `JWT_SECRET` → paste secret mới
   - Nhấn **Save Changes**

3. Service tự động redeploy với secret mới

4. **Ảnh hưởng:** Tất cả access token + refresh token hiện tại bị invalid.
   - User sẽ gặp lỗi `401 AUTH_UNAUTHORIZED` hoặc `AUTH_REFRESH_INVALID`
   - User cần đăng nhập lại — expected behavior
   - Thời gian ảnh hưởng: ~3-5 phút (thời gian redeploy)

5. Verify:
   ```bash
   curl -s -X POST https://vsat-compass-api.onrender.com/api/v1/auth/login \
     -H "Content-Type: application/json" \
     -d '{"email":"student@vsat.com","password":"Student@123"}'
   ```

### DATABASE_PASSWORD Rotation

1. Vào Neon Dashboard → Project → **Connection Details** → **Reset Password**
2. Copy password mới
3. Render Dashboard → Environment → sửa `DATABASE_PASSWORD` → Save
4. Service redeploy tự động
5. Verify bằng login test (login sẽ fail nếu DB password sai)

---

## 6. Incident Triage Checklist

### Backend trả về 502 Bad Gateway

```
□ Render Dashboard → Logs → kiểm tra Java exception / OOM
□ Render Dashboard → Metrics → kiểm tra memory usage (free tier = 512MB)
□ Nếu OOM: restart service, xem xét giảm -Xmx trong Dockerfile
□ Nếu không có log: instance đang cold start → chờ 60-90s
```

### Backend trả về 500 Internal Server Error

```
□ Render Logs → tìm stack trace
□ Kiểm tra Neon Dashboard → Status (có đang maintenance không?)
□ Kiểm tra Neon → Branches → compute có đang active không? (auto-suspend sau 5 phút idle)
□ Nếu Neon compute suspended: request đầu tiên sẽ chậm ~5s nhưng phải tự resume
□ Kiểm tra DATABASE_URL / DATABASE_PASSWORD có khớp không
```

### Login trả về 401 nhưng mật khẩu đúng

```
□ JWT_SECRET có bị thay đổi gần đây không?
□ Kiểm tra Render env var JWT_SECRET còn tồn tại
□ Kiểm tra Neon: SELECT id, email, status FROM users WHERE email = 'student@vsat.com'
□ User status có phải ACTIVE không? (LOCKED / DEACTIVATED sẽ bị reject)
```

### Deploy fails: `SQLState: 28P01 password authentication failed for user 'neondb_owner'`

```
□ Nguyên nhân: DATABASE_PASSWORD env var không khớp với password hiện tại của Neon role
□ Fix:
  1. Vào Neon Console → Project → Connection Details → copy password từ connection string
  2. Render Dashboard → Environment → update DATABASE_PASSWORD → Save Changes
  3. Render sẽ tự redeploy với password mới
□ Note: Sau khi rotate password trong Neon, PHẢI sync ngay vào Render — không có retry tự động
```

### Render service không response (connection refused)

```
□ Render Dashboard → service status: Suspended? Deploying? Failed?
□ Free tier: service bị spin-down sau 15 phút không có traffic → cold start ~60-90s
□ Gửi 1 request → chờ 90s → thử lại
□ Nếu vẫn fail: check Deploys tab → xem deploy cuối có succeed không
□ Nếu deploy failed: check build logs → fix lỗi → redeploy
```

---

## 7. Known Pitfalls

### Render Blueprint vs Web Service

**Pitfall:** Render Blueprint (`render.yaml`) requires the YAML file to define services at the root of the repo. If the YAML is present but doesn't match what the Dashboard expects, Blueprint deploy silently fails or behaves unexpectedly.

**Solution:** Always create the service via **Render Dashboard → + New → Web Service** (manual configuration), NOT via Blueprint auto-deploy. The `render.yaml` in this repo is kept for reference only — it does NOT drive the production deploy.

### UptimeRobot / Keep-Alive Required

**Pitfall:** Render free tier spins down the container after 15 minutes of inactivity. The next request triggers a cold start of ~60-90 seconds (JVM startup + Neon resume). This breaks mobile clients if they haven't pinged recently.

**Solution:** Configure UptimeRobot (or equivalent) to ping `/api/v1/actuator/health` every 5 minutes. This keeps the service warm during active hours.

### Neon Password Sync After Rotation

**Pitfall:** Rotating a Neon role password does NOT automatically update the Render env var. The next deploy fails with `28P01 authentication failed`.

**Solution:** Immediately after rotating in Neon, update `DATABASE_PASSWORD` in Render Dashboard → Environment → Save Changes → wait for redeploy.

### Render Post-Deploy Warm-Up for New Endpoints

**Pitfall:** `/actuator/health` may return `200 UP` before newly deployed controllers/endpoints are fully stable. On Render free tier, freshly deployed endpoints may need additional warm-up time after the health check first turns green — early calls can return `500` even though the health probe is already passing.

**Observed example:** During the v0.9.1 deploy watch, `/actuator/health` returned `200` shortly after deploy, but `GET /api/v1/exams` initially returned `500` before stabilizing to `200` after a short warm-up window. The endpoint then stayed healthy for the full 5-minute stability watch.

**Probable deploy-readiness timing example:** A prior C1.2c.1 smoke run saw `DELETE /admin/exams/{id}` return `500` before Render finished serving the new build consistently. A later retry passed without code changes, and the v0.9.4 release smoke passed all scripts before tagging.

**Solution:**
- Future deploy watches must probe **both**:
  1. `/api/v1/actuator/health`
  2. At least one newly deployed endpoint (e.g. `/api/v1/exams` for v0.9.1)
- For C1.2b-2 post-deploy, include warm-up probes for every new admin exam composition/workflow endpoint: add/remove/reorder, submit-review, publish, hide, archive, reject-review, and return-to-draft.
- For C1.2c-1 and later admin endpoint releases, include the newly added admin endpoint in the stability window or production smoke before tagging.
- Do not tag a release until the new endpoint-specific probe also returns the expected status consistently across the stability window.

### Curl JSON Quoting on Windows / Bash During Manual Probes

**Pitfall:** Malformed JSON request bodies in manual `curl` probes can produce `HttpMessageNotReadableException` from the backend. The error is purely about request-body parsing, not credentials or auth.

**Example symptom:**

```
JSON parse error: Unexpected character ('p'): was expecting double-quote to start field name
```

This typically happens when `curl -d '{"email":"..."}'` is run from CMD (which strips single quotes) or when shell variable expansion mangles the body.

**Solution:**
- Use safe JSON construction in bash smoke / debug probes — for example, build the body via `printf` into a variable, or pass it via `--data-raw` with explicit escaping.
- On CMD / PowerShell, use CMD-compatible escaping (e.g. `--data "{\"email\":\"...\"}"` with backslash-escaped double quotes) or pipe the body in via `curl --data-binary @body.json`.
- Do **not** interpret `HttpMessageNotReadableException` as a password / auth failure unless the request body has been confirmed to be valid JSON (e.g. by piping the same string through `jq .`).

### Smoke Script Runner Notes

**Observed during v0.9.3 closeout:**
- `docs/scripts/smoke_auth.sh` still checked out with CRLF on the Windows machine and failed under Bash until run through an LF-normalized stream. Normalize that script to LF in a follow-up docs/tooling pass.
- `VSAT/vsat-compass-api/docs/scripts/smoke_exams.sh` may need the documented `EXAM_ID` fallback when `jq` is unavailable. For v0.9.3 production smoke, the seeded public smoke exam was resolved as `EXAM_ID=2`.

These are smoke-runner issues only; production endpoint verification passed after using the documented fallbacks.

**C1.6-A student content smoke note:**
- For production smoke cases TC-SESSION-8, TC-SESSION-10, and TC-SESSION-12, run `smoke_sessions.sh` with `EXAM_ID=6 QUESTION_ID=4`.
- Reason: production SMOKE_001 has exam metadata `questionCount`, but currently has zero `exam_question` rows, so it cannot validate the student question-content and answer-key paths.

**Closed in Phase C1.2b-3 (2026-05-06):**
- LF normalization is now enforced repo-wide via `.gitattributes` (`*.sh text eol=lf`). All current and future shell scripts under `docs/scripts/` and `VSAT/**/docs/scripts/` will be checked out LF on Windows. No re-normalization step is required for fresh clones.
- The `smoke_exams.sh` `jq`/`EXAM_ID` fallback contract is documented below ("Smoke Script jq Fallback").

### Smoke Script jq Fallback

All exam-family smoke scripts (`smoke_admin_exams.sh`, `smoke_admin_exam_composition.sh`, and `VSAT/vsat-compass-api/docs/scripts/smoke_exams.sh`) auto-detect `jq` and fall back to `grep`/`sed` JSON parsing when `jq` is unavailable. The public exam script validates that `jq` is runnable, not only present on `PATH`. The runner header line `JSON parser: jq` vs. `JSON parser: grep/sed fallback` reports which mode is active.

**Behavior contract when `jq` is unavailable:**

- `smoke_admin_exams.sh` — fully self-discovers: subject id is resolved by scanning the `/subjects` response for `"code":"MATH"`, then the first `"id":` numeric field. No env override needed.
- `smoke_admin_exam_composition.sh` — same self-discovery for subject id and APPROVED/PUBLISHED question fixtures. If three fixtures cannot be resolved without `jq`, set `SMOKE_QUESTION_IDS="<id1>,<id2>,<id3>"` explicitly. The script exits `BLOCKED` (not `FAIL`) when fixtures are missing.
- `VSAT/vsat-compass-api/docs/scripts/smoke_exams.sh` — the public exam list is paginated and the grep/sed fallback cannot reliably pick the seeded smoke exam id from the response. **Set `EXAM_ID=<seeded-public-exam-id>` explicitly** when running without `jq`. The script logs `EXAM_ID` override use and exits with an actionable message if `jq` is unavailable and `EXAM_ID` is missing. For v0.9.3 production smoke, this was `EXAM_ID=2`.

**Recommended invocation when `jq` is missing:**

```bash
# Admin smokes — no env override required
SMOKE_ADMIN_PASSWORD=... bash docs/scripts/smoke_admin_exams.sh
SMOKE_QUESTION_IDS="1,2,3" bash docs/scripts/smoke_admin_exam_composition.sh

# Public exam smoke — EXAM_ID is required without jq
EXAM_ID=2 bash VSAT/vsat-compass-api/docs/scripts/smoke_exams.sh
```

Install `jq` (`apt-get install jq` / `brew install jq` / `choco install jq`) to skip the fallback contract entirely; with `jq` present, all three scripts auto-discover their fixtures.

---

## 8. Cost Watch

### Render Free Tier

| Resource | Limit | Ghi chú |
|----------|-------|---------|
| RAM | 512 MB | JVM default + HikariCP pool |
| CPU | 0.1 vCPU | Shared |
| Bandwidth | 100 GB/month | Dư cho API |
| Spin-down | 15 phút idle | Cold start ~60-90s |
| Build minutes | 500/month | ~10 builds/tháng đủ |
| Custom domain | ❌ | Chỉ có `*.onrender.com` |
| SSL | ✅ tự động | Let's Encrypt |

**Hành vi spin-down:**
- Sau 15 phút không có request → Render tắt container
- Request tiếp theo trigger cold start: build JAR từ Docker cache → start JVM → connect DB
- Thời gian cold start: ~60-90 giây
- **Workaround:** Cron job ping health endpoint mỗi 14 phút (không khuyến khích trên free tier — Render có thể cấm)

### Neon Free Tier

| Resource | Limit | Ghi chú |
|----------|-------|---------|
| Storage | 0.5 GB | 27 bảng + test data = ~10 MB, rất dư |
| Compute | 0.25 vCPU, 1 GB RAM | Shared |
| Branches | 10 | Chỉ dùng 1 (main) |
| Auto-suspend | 5 phút idle | Resume tự động khi có connection |
| Projects | 1 (free tier) | Đủ |

**Hành vi auto-suspend:**
- Neon compute tắt sau 5 phút không có query
- Connection đầu tiên sau suspend: thêm ~2-5 giây latency
- Kết hợp với Render spin-down: worst case cold start = ~90s (Render) + ~5s (Neon) = ~95s
