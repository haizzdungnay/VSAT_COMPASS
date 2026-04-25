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
