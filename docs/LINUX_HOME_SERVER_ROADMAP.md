# LỘ TRÌNH TRIỂN KHAI CHI TIẾT (STEP-BY-STEP ROADMAP): V-SAT COMPASS HOME SERVER

> Tài liệu tham chiếu kỹ thuật: [`docs/LINUX_HOME_SERVER_DEPLOY_PLAN.md`](./LINUX_HOME_SERVER_DEPLOY_PLAN.md)
> Mục tiêu: Chuyển đổi hạ tầng V-SAT Compass từ Cloud (Render + Neon) về Linux Home Server tại nhà với thời gian gián đoạn (downtime) bằng 0, có kiểm thử từng chặng (Gate Check) và phương án Rollback tức thì.

---

## TỔNG QUAN THỜI LƯỢNG & CÁC CHẶNG (MILESTONES)

| Chặng | Nội dung thực hiện | Thời gian dự kiến | Điều kiện hoàn thành (Exit Criteria) |
| :--- | :--- | :---: | :--- |
| **Chặng 0** | Chuẩn bị tên miền, Cloudflare, Telegram Bot, USB cài OS | 30 phút | Có sẵn Domain trên Cloudflare, Token Bot Telegram, file backup DB hiện tại |
| **Chặng 1** | Cài đặt Ubuntu Server 24.04 LTS, BIOS, Mạng tĩnh, SSH, UFW | 45 phút | Rút màn hình/bàn phím, rút dây nguồn cắm lại máy tự lên và SSH được qua port `2222` |
| **Chặng 2** | Cài đặt Docker Engine, giới hạn Log, chuẩn bị thư mục `/opt/vsat-compass` | 20 phút | `docker compose version` hoạt động, log rotation đã bật |
| **Chặng 3** | Khởi tạo PostgreSQL 17, nạp Schema + 11 Migrations (hoặc migrate từ Neon) | 30 phút | Bảng `schema_migrations` đủ 12 dòng (base + `0001`..`0011`), dữ liệu toàn vẹn |
| **Chặng 4** | Build & chạy Spring Boot API + Web Admin nội bộ | 25 phút | `curl http://127.0.0.1:8080/api/v1/actuator/health` trả về `{"status":"UP"}` |
| **Chặng 5** | Đấu nối Cloudflare Tunnel (`cloudflared`) & Zero Trust | 20 phút | Truy cập `https://api.yourdomain.com/api/v1/actuator/health` bằng 4G điện thoại trả về `UP` |
| **Chặng 6** | Thiết lập Backup tự động (Rclone + Google Drive) & Cảnh báo Telegram | 30 phút | File `.sql.gz` xuất hiện trên Google Drive, Telegram nhận tin nhắn báo cáo |
| **Chặng 7** | Cutover Client (Web Admin + Release APK Android) & Smoke Test toàn trình | 30 phút | Bộ script `docs/scripts/smoke_*.sh` pass 100% trên domain mới |

---

## CHẶNG 0: Chuẩn bị trước giờ G (Pre-Flight Checklist)

- [ ] **0.1. Tên miền & Cloudflare:**
  - Mua tên miền (hoặc dùng tên miền sẵn có) và trỏ Nameservers về Cloudflare.
  - Đợi trạng thái domain trên Cloudflare chuyển sang **Active**.
- [ ] **0.2. Tạo Telegram Bot cảnh báo:**
  - Nhắn `/newbot` cho `@BotFather` trên Telegram -> Lưu lại `TG_BOT_TOKEN`.
  - Tạo nhóm Telegram (hoặc chat cá nhân), lấy `TG_CHAT_ID` qua `https://api.telegram.org/bot<TOKEN>/getUpdates`.
- [ ] **0.3. Sao lưu dữ liệu Cloud hiện tại (nếu đang chạy trên Neon):**
  ```bash
  pg_dump "$NEON_DATABASE_URL" --clean --if-exists --no-owner | gzip -9 > neon_pre_cutover_backup.sql.gz
  ```
- [ ] **0.4. Chuẩn bị phần cứng PC:**
  - Cắm dây mạng LAN trực tiếp từ PC vào cổng Gigabit của Modem/Router (tuyệt đối không dùng Wi-Fi cho Server).
  - Khuyến nghị cắm PC và Modem/ONT qua bộ lưu điện (UPS) mini để tránh sập nguồn đột ngột.

---

## CHẶNG 1: Thiết lập BIOS, OS, Mạng Tĩnh & Bảo mật Cơ sở

### Bước 1.1: BIOS & Cài đặt Ubuntu Server 24.04 LTS
- [ ] Vào BIOS -> Bật `Restore on AC Power Loss = Power On`.
- [ ] Tắt `Halt on Error` (cho phép boot không cần bàn phím/màn hình).
- [ ] Cài Ubuntu Server 24.04 LTS (chọn cài đặt `OpenSSH server`, **không** chọn Docker trong menu Snap lúc cài để tránh lỗi quyền của Snap Docker).

### Bước 1.2: Chống Sleep & Đồng bộ thời gian
- [ ] Chạy lệnh chặn sleep:
  ```bash
  sudo systemctl mask sleep.target suspend.target hibernate.target hybrid-sleep.target
  sudo timedatectl set-timezone Asia/Ho_Chi_Minh
  sudo timedatectl set-ntp true
  ```
- [ ] **Kiểm tra:** `timedatectl status` -> `System clock synchronized: yes`.

### Bước 1.3: Gán IP tĩnh `192.168.1.240`
- [ ] Trên Modem nhà mạng: Gán DHCP Reservation cho MAC của PC -> `192.168.1.240`.
- [ ] Trên Ubuntu: Cấu hình `/etc/netplan/01-netcfg.yaml` và chạy `sudo netplan apply`.
- [ ] **Kiểm tra:** `ip a` hiện `192.168.1.240/24` và `ping -c 3 1.1.1.1` thông mạng.

### Bước 1.4: Hardening SSH & Bật UFW
- [ ] Từ máy cá nhân: `ssh-copy-id vsat@192.168.1.240`.
- [ ] Tạo `/etc/ssh/sshd_config.d/hardening.conf` (Port `2222`, tắt password, tắt root).
- [ ] Khởi động lại SSH: `sudo systemctl daemon-reload && sudo systemctl restart ssh.socket ssh.service`.
- [ ] Cấu hình UFW chỉ mở port `2222` và `8080` cho dải LAN `192.168.1.0/24`, sau đó `sudo ufw enable`.
- [ ] **GATE CHECK 1 (Bắt buộc pass mới qua Chặng 2):**
  1. Rút cáp HDMI màn hình và bàn phím khỏi PC.
  2. Tắt nguồn PC, rút phích cắm điện 10 giây rồi cắm lại.
  3. Đợi 90 giây, từ máy tính cá nhân chạy `ssh -p 2222 vsat@192.168.1.240` -> Đăng nhập thành công không cần mật khẩu.

---

## CHẶNG 2: Cài đặt Docker Engine Chính thức & Chuẩn bị Môi trường

### Bước 2.1: Cài đặt Docker CE & Docker Compose Plugin (APT chính chủ, không dùng Snap)
- [ ] Chạy cụm lệnh cài đặt:
  ```bash
  sudo apt-get update
  sudo apt-get install -y ca-certificates curl gnupg git jq
  sudo install -m 0755 -d /etc/apt/keyrings
  curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /etc/apt/keyrings/docker.gpg
  sudo chmod a+r /etc/apt/keyrings/docker.gpg

  echo \
    "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
    $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
    sudo tee /etc/apt/sources.list.d/docker.list > /dev/null

  sudo apt-get update
  sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
  sudo usermod -aG docker $USER
  ```
- [ ] Đăng xuất SSH và đăng nhập lại để nhận group `docker`.

### Bước 2.2: Giới hạn dung lượng Docker Logs
- [ ] Tạo file `/etc/docker/daemon.json`:
  ```bash
  sudo tee /etc/docker/daemon.json > /dev/null << 'EOF'
  {
    "log-driver": "json-file",
    "log-opts": {
      "max-size": "20m",
      "max-file": "5"
    }
  }
  EOF
  sudo systemctl restart docker
  ```

### Bước 2.3: Khởi tạo thư mục `/opt/vsat-compass` & Clone mã nguồn
- [ ] Tạo cấu trúc thư mục và clone repo:
  ```bash
  sudo mkdir -p /opt/vsat-compass/{backups,scripts}
  sudo chown -R $USER:$USER /opt/vsat-compass
  cd /opt/vsat-compass
  git clone <URL_REPO_CUA_BAN> repo
  ```
- [ ] Tạo file `/opt/vsat-compass/.env` theo mẫu Mục 3.3 trong `LINUX_HOME_SERVER_DEPLOY_PLAN.md` và đặt quyền `chmod 600 /opt/vsat-compass/.env`.
- [ ] Tạo file `/opt/vsat-compass/docker-compose.prod.yml` và `/opt/vsat-compass/nginx-spa.conf`.

---

## CHẶNG 3: Khởi tạo PostgreSQL 17 & Chạy 11 Bản Migration SQL

### Bước 3.1: Cập nhật Dockerfile lên JDK 21
- [ ] Kiểm tra `repo/VSAT/vsat-compass-api/Dockerfile` đã dùng `eclipse-temurin:21-jdk-alpine` / `21-jre-alpine` và tham số `-Xms512m -Xmx2g -Duser.timezone=UTC`.

### Bước 3.2: Thực thi Migration (hoặc Nạp dữ liệu từ Neon)
- [ ] **Trường hợp 1 (Khởi tạo mới hoàn toàn):**
  - Tạo file `/opt/vsat-compass/scripts/run-migrations.sh`, cấp quyền `chmod +x` và thực thi.
  - Nếu cần dữ liệu mẫu kiểm thử, chạy thêm:
    ```bash
    docker exec -i vsat-pg psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" < ./repo/docs/seed/smoke_test_seed.sql
    ```
- [ ] **Trường hợp 2 (Chuyển dữ liệu đang có từ Neon Cloud về Home Server):**
  - Khởi động riêng container `postgres`: `docker compose -f docker-compose.prod.yml up -d postgres`.
  - Nạp bản dump từ Chặng 0.3:
    ```bash
    gunzip -c neon_pre_cutover_backup.sql.gz | docker exec -i vsat-pg psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"
    ```
- [ ] **GATE CHECK 3:**
  ```bash
  docker exec -it vsat-pg psql -U vsat_prod_user -d vsat_compass -c "\dt"
  ```
  Xác nhận có đầy đủ các bảng: `users`, `questions`, `question_options`, `question_statements`, `exams`, `exam_sections`, `exam_questions`, `exam_blueprints`, `exam_variants`, `exam_windows`, `exam_sessions`, `user_answers`, `tickets`...

---

## CHẶNG 4: Build & Khởi chạy Backend API + Web Admin Nội bộ

- [ ] **4.1. Build tĩnh Web Admin (nếu chạy Nginx trên Home Server):**
  ```bash
  # Cài đặt Node.js 22 LTS trên host để build dist
  curl -fsSL https://deb.nodesource.com/setup_22.x | sudo -E bash -
  sudo apt-get install -y nodejs
  cd /opt/vsat-compass/repo/web-admin
  npm ci
  VITE_API_URL=https://api.yourdomain.com/api/v1 npm run build
  ```
- [ ] **4.2. Khởi chạy toàn bộ Docker Compose:**
  ```bash
  cd /opt/vsat-compass
  docker compose -f docker-compose.prod.yml up -d --build
  ```
- [ ] **GATE CHECK 4:**
  - Chạy `docker ps` -> Cả 3 container `vsat-pg`, `vsat-api`, `vsat-web-admin` đều ở trạng thái `Up (healthy)`.
  - Chạy `curl -i http://127.0.0.1:8080/api/v1/actuator/health` -> Trả về `HTTP/1.1 200` và `{"status":"UP"}`.
  - Chạy `curl -I http://127.0.0.1:8081` -> Trả về `HTTP/1.1 200 OK`.

---

## CHẶNG 5: Mở cổng Internet qua Cloudflare Tunnel (`cloudflared`)

- [ ] **5.1. Cài đặt và đăng nhập `cloudflared`:**
  - Thực hiện các lệnh ở Mục 2.2 của `LINUX_HOME_SERVER_DEPLOY_PLAN.md`.
- [ ] **5.2. Tạo Tunnel & Route DNS:**
  ```bash
  cloudflared tunnel create vsat-prod
  cloudflared tunnel route dns vsat-prod api.yourdomain.com
  cloudflared tunnel route dns vsat-prod admin.yourdomain.com
  ```
- [ ] **5.3. Kích hoạt Systemd Service:**
  - Điền `<TUNNEL_UUID>` vào `/etc/cloudflared/config.yml`.
  - Chạy `sudo cloudflared service install && sudo systemctl enable --now cloudflared`.
- [ ] **5.4. Bật Cloudflare Access bảo vệ `admin.yourdomain.com`:**
  - Cấu hình Zero Trust Policy chỉ cho phép email quản trị viên nhận OTP.
- [ ] **GATE CHECK 5:**
  - Dùng điện thoại (tắt Wi-Fi, bật **4G/5G**) mở trình duyệt truy cập `https://api.yourdomain.com/api/v1/actuator/health` -> Hiển thị `{"status":"UP"}` với biểu tượng ổ khóa HTTPS hợp lệ.

---

## CHẶNG 6: Tự động hóa Sao lưu (Backup) & Giám sát (Monitoring)

- [ ] **6.1. Cấu hình Rclone + Google Drive:**
  - Chạy `rclone config` tạo remote `gdrive`.
  - Kiểm tra kết nối: `rclone lsd gdrive:`.
- [ ] **6.2. Cài đặt & Test thực tế Script Backup:**
  - Tạo `/opt/vsat-compass/scripts/backup-db.sh` và `chmod +x`.
  - **Chạy thử ngay lập tức:** `/opt/vsat-compass/scripts/backup-db.sh`.
  - Kiểm tra có file `.sql.gz` trong `/opt/vsat-compass/backups/`, có file trên Google Drive `vsat-backups/`, và có tin nhắn báo thành công trên Telegram.
- [ ] **6.3. Cài đặt & Test thực tế Script Giám sát:**
  - Tạo `/opt/vsat-compass/scripts/health-monitor.sh` và `chmod +x`.
  - **Giả lập sự cố (Chaos Test):**
    ```bash
    docker stop vsat-api
    /opt/vsat-compass/scripts/health-monitor.sh
    ```
  - Xác nhận: Telegram nhận cảnh báo đỏ `🔴 [V-SAT ALERT: API DOWN]`, container `vsat-api` tự động được bật lại; 1 phút sau chạy lại `/opt/vsat-compass/scripts/health-monitor.sh` nhận được tin nhắn xanh `🟢 [V-SAT RECOVERED]`.
- [ ] **6.4. Đưa cả 2 script vào Crontab (`crontab -e`):**
  ```cron
  0 2 * * * /opt/vsat-compass/scripts/backup-db.sh >> /var/log/vsat-backup.log 2>&1
  */2 * * * * /opt/vsat-compass/scripts/health-monitor.sh
  ```

---

## CHẶNG 7: Cutover Client & Kiểm thử Toàn trình (Smoke Test)

- [ ] **7.1. Chạy bộ Smoke Test tự động của dự án từ máy cá nhân trỏ vào Domain mới:**
  ```bash
  BASE_URL="https://api.yourdomain.com/api/v1" bash docs/scripts/smoke_auth.sh
  BASE_URL="https://api.yourdomain.com/api/v1" bash docs/scripts/smoke_subjects.sh
  BASE_URL="https://api.yourdomain.com/api/v1" bash docs/scripts/smoke_sessions.sh
  BASE_URL="https://api.yourdomain.com/api/v1" bash docs/scripts/smoke_admin_exams.sh
  ```
- [ ] **7.2. Phát hành bản cập nhật Android APK:**
  - Sửa `BASE_URL_CLOUD` trong `app/build.gradle.kts` thành `"https://api.yourdomain.com/api/v1/"`.
  - Đặt `USE_LOCAL_BACKEND` = `false` cho bản `release`.
  - Chạy `./gradlew assembleRelease` để xuất file APK đã ký (`app/build/outputs/apk/release/app-release.apk`).
  - Cài APK lên thiết bị thật (dùng mạng 4G), kiểm tra: Đăng nhập -> Tải danh sách môn học -> Vào phòng thi thử -> Nộp bài -> Xem kết quả & bảng xếp hạng.

---

## PHƯƠNG ÁN ROLLBACK KHẨN CẤP (KHI SỰ CỐ PHẦN CỨNG / MẠNG NHÀ KÉO DÀI > 30 PHÚT)

Nếu Home Server gặp sự cố phần cứng hoặc đứt cáp quang dài hạn trong kỳ thi:
1. **Lấy bản backup mới nhất từ Google Drive** (được đẩy lên lúc 02:00 AM mỗi ngày tại thư mục `gdrive:vsat-backups/`).
2. **Nạp lại vào Neon Cloud PostgreSQL:**
   ```bash
   gunzip -c vsat_compass_LATEST.sql.gz | psql "$NEON_DATABASE_URL"
   ```
3. **Đổi DNS CNAME trên Cloudflare Dashboard (Mất 30 giây, KHÔNG cần build lại APK Android):**
   - Vào Cloudflare -> **DNS -> Records**.
   - Sửa bản ghi `CNAME` của `api.yourdomain.com` từ `<TUNNEL_UUID>.cfargotunnel.com` sang địa chỉ dịch vụ Cloud dự phòng (`vsat-compass-api.onrender.com`).
   - Toàn bộ ứng dụng Android đã cài trên máy thí sinh và Web Admin sẽ tự động chuyển hướng về Cloud dự phòng ngay lập tức mà không cần cập nhật ứng dụng.
