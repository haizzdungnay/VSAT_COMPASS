# KẾ HOẠCH TRIỂN KHAI V-SAT COMPASS LÊN LINUX HOME SERVER (PRODUCTION)

Tài liệu đặc tả kiến trúc, cấu hình và script vận hành để biến PC Linux tại nhà (Ubuntu Server 24.04 LTS / Debian 12) thành Production Server phục vụ **V-SAT Compass** (Backend Spring Boot 3.5.9, PostgreSQL 17, Web Admin React 19 + Vite, Android Native Client).

---

## GIAI ĐOẠN 1: Chuẩn bị Phần cứng & Hệ điều hành Linux (Ubuntu Server 24.04 LTS)

### 1.1. Cấu hình BIOS/UEFI & Chống Sleep (Headless)
1. **BIOS/UEFI:**
   - Vào mục `Power Management` / `APM Configuration`.
   - Đổi `Restore on AC Power Loss` (hoặc `AC Back`) thành **`Power On`** (tự bật máy ngay khi có điện lại).
   - Tắt `Wait for 'F1' If Error` hoặc chỉnh `Halt On` = `No Errors` (để boot không kẹt khi rút màn hình và bàn phím).
2. **Chặn Linux tự Sleep/Suspend và đồng bộ múi giờ chuẩn:**
```bash
# Chặn toàn bộ chế độ ngủ của systemd
sudo systemctl mask sleep.target suspend.target hibernate.target hybrid-sleep.target

# Nếu là Laptop tận dụng làm server: tắt gập màn hình bị sleep
sudo sed -i 's/#HandleLidSwitch=suspend/HandleLidSwitch=ignore/g' /etc/systemd/logind.conf
sudo systemctl restart systemd-logind

# Đặt timezone chuẩn Việt Nam cho OS (Container sẽ ép UTC riêng)
sudo timedatectl set-timezone Asia/Ho_Chi_Minh
sudo timedatectl set-ntp true
```

### 1.2. Cấu hình IP Tĩnh Nội bộ (Netplan) & DHCP Reservation
1. **Trên Modem nhà mạng (Viettel/VNPT/FPT):** Vào `LAN -> DHCP Static IP / MAC Binding`, gán cứng MAC Address của card mạng LAN PC với IP `192.168.1.240` (khớp cấu hình `LOCAL_LAN_HOST` trong `app/build.gradle.kts`).
2. **Trên Ubuntu 24.04 (`/etc/netplan/01-netcfg.yaml`):**
```bash
ip -br link # Kiểm tra tên card mạng (ví dụ: enp3s0)
sudo nano /etc/netplan/01-netcfg.yaml
```
```yaml
network:
  version: 2
  renderer: networkd
  ethernets:
    enp3s0:
      dhcp4: no
      addresses:
        - 192.168.1.240/24
      routes:
        - to: default
          via: 192.168.1.1
      nameservers:
        addresses: [1.1.1.1, 8.8.8.8]
```
```bash
sudo chmod 600 /etc/netplan/01-netcfg.yaml
sudo netplan apply
```

### 1.3. Bảo mật OpenSSH Server
1. **Trên máy tính cá nhân (Windows/Mac):** Tạo và đẩy SSH Key sang Home Server:
```bash
ssh-keygen -t ed25519 -C "vsat-admin"
ssh-copy-id -i ~/.ssh/id_ed25519.pub vsat@192.168.1.240
```
2. **Trên Home Server:** Đổi port SSH sang `2222`, tắt đăng nhập mật khẩu và tắt `root`:
```bash
sudo tee /etc/ssh/sshd_config.d/hardening.conf > /dev/null << 'EOF'
Port 2222
PermitRootLogin no
PasswordAuthentication no
PubkeyAuthentication yes
KbdInteractiveAuthentication no
UsePAM yes
X11Forwarding no
MaxAuthTries 3
EOF

# Ubuntu 24.04 dùng ssh.socket, cần reload socket trước khi restart ssh
sudo systemctl daemon-reload
sudo systemctl restart ssh.socket ssh.service
```
> **Cảnh báo bảo mật:** Giữ nguyên cửa sổ terminal hiện tại, mở một cửa sổ mới test `ssh -p 2222 vsat@192.168.1.240` thành công trước khi đóng phiên cũ.

### 1.4. Thiết lập Tường lửa UFW
Chỉ cho phép SSH từ mạng LAN nội bộ. Toàn bộ traffic Internet đi qua Cloudflare Tunnel outbound nên **không cần mở port 80/443 trên UFW ra ngoài**.
```bash
sudo ufw default deny incoming
sudo ufw default allow outgoing
# Chỉ cho phép SSH port 2222 từ dải mạng LAN
sudo ufw allow from 192.168.1.0/24 to any port 2222 proto tcp comment 'SSH LAN only'
# Cho phép thiết bị LAN test trực tiếp nếu cần
sudo ufw allow from 192.168.1.0/24 to any port 8080 proto tcp comment 'VSAT API LAN'
sudo ufw enable
sudo ufw status verbose
```

---

## GIAI ĐOẠN 2: Vượt CGNAT & Dynamic IP bằng Cloudflare Tunnel (`cloudflared`)

### 2.1. So sánh giải pháp Public mạng
| Tiêu chí | NAT Port Forwarding + DDNS | Tailscale / WireGuard | **Cloudflare Tunnel (Khuyến nghị)** |
| :--- | :--- | :--- | :--- |
| **Xuyên CGNAT nhà mạng VN** | Không (phải xin mở CGNAT) | Có | **Có (100% outbound QUIC/HTTP2)** |
| **Client Android/Web truy cập** | Công khai | Bắt cài VPN trên máy thí sinh (Bất khả thi) | **Công khai, không cần cài app phụ** |
| **Chứng chỉ HTTPS/SSL** | Tự gia hạn Let's Encrypt | Giới hạn | **Tự động Edge SSL, chuẩn Full (Strict)** |
| **Bảo vệ IP nhà & Chống DDoS** | Lộ IP WAN thật | Ẩn IP | **Ẩn IP WAN 100%, WAF + Anti-DDoS miễn phí** |

### 2.2. Cài đặt & Cấu hình `cloudflared` Systemd Service
**Điều kiện:** Có 1 tên miền đã trỏ Nameservers về tài khoản Cloudflare miễn phí.

```bash
# 1. Cài đặt cloudflared chính chủ cho Ubuntu 24.04 (noble)
sudo mkdir -p --mode=0755 /usr/share/keyrings
curl -fsSL https://pkg.cloudflare.com/cloudflare-main.gpg | sudo tee /usr/share/keyrings/cloudflare-main.gpg >/dev/null
echo 'deb [signed-by=/usr/share/keyrings/cloudflare-main.gpg] https://pkg.cloudflare.com/cloudflared noble main' | sudo tee /etc/apt/sources.list.d/cloudflared.list
sudo apt-get update && sudo apt-get install -y cloudflared

# 2. Xác thực tài khoản (copy URL hiện ra trên terminal dán vào trình duyệt máy cá nhân)
cloudflared tunnel login

# 3. Tạo tunnel tên là vsat-prod
cloudflared tunnel create vsat-prod
# Lưu lại <TUNNEL_UUID> sinh ra tại ~/.cloudflared/<TUNNEL_UUID>.json

# 4. Tạo DNS CNAME tự động trên Cloudflare
cloudflared tunnel route dns vsat-prod api.yourdomain.com
cloudflared tunnel route dns vsat-prod admin.yourdomain.com
```

**Tạo file cấu hình `/etc/cloudflared/config.yml`:**
```bash
sudo mkdir -p /etc/cloudflared
sudo cp ~/.cloudflared/*.json /etc/cloudflared/
sudo tee /etc/cloudflared/config.yml > /dev/null << 'EOF'
tunnel: <TUNNEL_UUID>
credentials-file: /etc/cloudflared/<TUNNEL_UUID>.json
protocol: quic

ingress:
  # Route 1: Spring Boot Backend API
  - hostname: api.yourdomain.com
    service: http://127.0.0.1:8080
    originRequest:
      connectTimeout: 10s
      noTLSVerify: true

  # Route 2: Web Admin Portal (host Nginx trên cùng server)
  - hostname: admin.yourdomain.com
    service: http://127.0.0.1:8081
    originRequest:
      connectTimeout: 10s

  # Catch-all bắt buộc ở cuối
  - service: http_status:404
EOF

# 5. Cài đặt thành systemd service tự chạy cùng OS
sudo cloudflared service install
sudo systemctl enable --now cloudflared
sudo systemctl status cloudflared
```

### 2.3. Cấu hình SSL/TLS & Zero Trust Access trên Cloudflare Dashboard
1. Vào **SSL/TLS -> Overview**: Chọn chế độ **Full**. Bật **Always Use HTTPS** và **Minimum TLS Version = TLS 1.2**.
2. **(Khuyến nghị)** Bảo vệ trang quản trị `admin.yourdomain.com` bằng **Cloudflare Zero Trust -> Access -> Applications**:
   - Tạo **Self-hosted Application** cho domain `admin.yourdomain.com`.
   - Thêm Policy: `Allow` -> `Emails` = email admin của bạn.

---

## GIAI ĐOẠN 3: Đóng gói & Vận hành Backend + PostgreSQL 17 bằng Docker Compose

### 3.1. Cập nhật `VSAT/vsat-compass-api/Dockerfile` lên JDK 21 & Tối ưu JVM
```dockerfile
# ===== Stage 1: Build =====
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

COPY gradle gradle
COPY gradlew .
COPY build.gradle settings.gradle gradle.properties ./
RUN chmod +x gradlew
RUN ./gradlew dependencies --no-daemon || true

COPY src src
RUN ./gradlew bootJar --no-daemon -x test

# ===== Stage 2: Run =====
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S vsat && adduser -S vsat -G vsat
COPY --from=build /app/build/libs/*.jar app.jar
RUN chown vsat:vsat app.jar
USER vsat

EXPOSE 8080

ENTRYPOINT ["java", \
  "-Xms512m", \
  "-Xmx2g", \
  "-XX:+UseG1GC", \
  "-XX:+ExitOnOutOfMemoryError", \
  "-Duser.timezone=UTC", \
  "-jar", "app.jar"]
```

### 3.2. Cấu trúc thư mục triển khai trên Server (`/opt/vsat-compass`)
```bash
sudo mkdir -p /opt/vsat-compass/{backups,scripts}
sudo chown -R $USER:$USER /opt/vsat-compass
cd /opt/vsat-compass
git clone <URL_REPO_CUA_BAN> repo
```

### 3.3. File biến môi trường `/opt/vsat-compass/.env`
```bash
# Tạo chuỗi JWT_SECRET ngẫu nhiên chuẩn 256-bit Base64:
openssl rand -base64 64 | tr -d '\n'
```
Tạo file `/opt/vsat-compass/.env` (quyền `chmod 600 /opt/vsat-compass/.env`):
```dotenv
# PostgreSQL 17
POSTGRES_DB=vsat_compass
POSTGRES_USER=vsat_prod_user
POSTGRES_PASSWORD=Thay_Bang_Mat_Khau_Manh_32_Ky_Tu_2026!

# Spring Boot Backend
SPRING_PROFILES_ACTIVE=prod
DATABASE_URL=jdbc:postgresql://postgres:5432/vsat_compass
DATABASE_USERNAME=vsat_prod_user
DATABASE_PASSWORD=Thay_Bang_Mat_Khau_Manh_32_Ky_Tu_2026!
JWT_SECRET=Thay_Bang_Chuoi_Base64_Sinh_Tu_OpenSSL_O_Tren
JWT_ACCESS_TOKEN_EXPIRATION_MS=900000
CORS_ALLOWED_ORIGINS=https://admin.yourdomain.com,https://vsat-compass.vercel.app
```

### 3.4. File `/opt/vsat-compass/docker-compose.prod.yml`
> **Lưu ý context-path:** Trong `application.yml`, Backend đặt `server.servlet.context-path: /api/v1`, nên endpoint healthcheck chính xác là `http://localhost:8080/api/v1/actuator/health`.

```yaml
services:
  postgres:
    image: postgres:17-alpine
    container_name: vsat-pg
    restart: unless-stopped
    env_file: .env
    environment:
      TZ: UTC
      PGTZ: UTC
    volumes:
      - pgdata:/var/lib/postgresql/data
      - ./backups:/backups
    networks:
      - vsat-net
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U $${POSTGRES_USER} -d $${POSTGRES_DB}"]
      interval: 10s
      timeout: 5s
      retries: 5
      start_period: 10s
    deploy:
      resources:
        limits:
          cpus: "1.5"
          memory: 1536M
        reservations:
          memory: 512M

  api:
    build:
      context: ./repo/VSAT/vsat-compass-api
      dockerfile: Dockerfile
    container_name: vsat-api
    restart: unless-stopped
    depends_on:
      postgres:
        condition: service_healthy
    env_file: .env
    ports:
      - "127.0.0.1:8080:8080"
      - "192.168.1.240:8080:8080"
    networks:
      - vsat-net
    healthcheck:
      test: ["CMD-SHELL", "wget --no-verbose --tries=1 --spider http://localhost:8080/api/v1/actuator/health || exit 1"]
      interval: 30s
      timeout: 5s
      retries: 3
      start_period: 40s
    deploy:
      resources:
        limits:
          cpus: "2.0"
          memory: 2560M
        reservations:
          memory: 768M

  web-admin:
    image: nginx:1.27-alpine
    container_name: vsat-web-admin
    restart: unless-stopped
    ports:
      - "127.0.0.1:8081:80"
    volumes:
      - ./repo/web-admin/dist:/usr/share/nginx/html:ro
      - ./nginx-spa.conf:/etc/nginx/conf.d/default.conf:ro
    networks:
      - vsat-net
    deploy:
      resources:
        limits:
          cpus: "0.5"
          memory: 128M

networks:
  vsat-net:
    driver: bridge

volumes:
  pgdata:
```

### 3.5. Script khởi tạo Base Schema + 11 SQL Migrations tuần tự (`0001` -> `0011`)
Tạo script `/opt/vsat-compass/scripts/run-migrations.sh`:
```bash
#!/usr/bin/env bash
set -euo pipefail

cd /opt/vsat-compass
source .env

echo "=== [1/3] Khởi động container PostgreSQL 17 ==="
docker compose -f docker-compose.prod.yml up -d postgres

echo "=== Đợi PostgreSQL sẵn sàng ==="
until docker exec vsat-pg pg_isready -U "$POSTGRES_USER" -d "$POSTGRES_DB" >/dev/null 2>&1; do
  sleep 2
done

docker exec -i vsat-pg psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB" << 'EOSQL'
CREATE TABLE IF NOT EXISTS schema_migrations (
    filename VARCHAR(255) PRIMARY KEY,
    applied_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
EOSQL

apply_sql() {
  local filepath="$1"
  local filename
  filename=$(basename "$filepath")

  local already_applied
  already_applied=$(docker exec -i vsat-pg psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -tAc \
    "SELECT 1 FROM schema_migrations WHERE filename = '$filename'")

  if [[ "$already_applied" == "1" ]]; then
    echo "[SKIP] $filename đã được chạy trước đó."
  else
    echo "[RUN ] Đang thực thi $filename ..."
    docker exec -i vsat-pg psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB" < "$filepath"
    docker exec -i vsat-pg psql -v ON_ERROR_STOP=1 -U "$POSTGRES_USER" -d "$POSTGRES_DB" -c \
      "INSERT INTO schema_migrations (filename) VALUES ('$filename');"
    echo "[OK  ] Hoàn tất $filename"
  fi
}

echo "=== [2/3] Áp dụng Base Schema ==="
apply_sql "./repo/VSAT/vsat_database_schema.sql"

echo "=== [3/3] Áp dụng 11 Migration tuần tự (0001 -> 0011) ==="
for sql_file in $(ls ./repo/VSAT/vsat-compass-api/docs/sql/*.sql | sort); do
  apply_sql "$sql_file"
done

echo "=== Khởi động toàn bộ hệ thống (Spring Boot API + Web Admin) ==="
docker compose -f docker-compose.prod.yml up -d --build
```

---

## GIAI ĐOẠN 4: Sao lưu & Tự phục hồi Dữ liệu (Backup & Disaster Recovery)

### 4.1. Cấu hình Rclone kết nối Google Drive
```bash
sudo apt-get install -y rclone
rclone config
# Tạo remote tên: gdrive (chọn Google Drive, xác thực headless qua máy cá nhân)
```

### 4.2. Script Backup tự động (`/opt/vsat-compass/scripts/backup-db.sh`)
```bash
#!/usr/bin/env bash
set -euo pipefail

BACKUP_DIR="/opt/vsat-compass/backups"
ENV_FILE="/opt/vsat-compass/.env"
RETENTION_DAYS=14
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_FILE="${BACKUP_DIR}/vsat_compass_${TIMESTAMP}.sql.gz"

TG_BOT_TOKEN="123456789:ABCDefGhIJKlmNoPQRsTUVwxyZ"
TG_CHAT_ID="-1001234567890"

source "$ENV_FILE"

send_telegram() {
  local msg="$1"
  curl -s -X POST "https://api.telegram.org/bot${TG_BOT_TOKEN}/sendMessage" \
    -d chat_id="${TG_CHAT_ID}" \
    -d parse_mode="Markdown" \
    -d text="${msg}" >/dev/null || true
}

trap 'send_telegram "🚨 *[V-SAT BACKUP FAILED]* Lỗi sao lưu DB lúc $(date)"' ERR

docker exec vsat-pg pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" --clean --if-exists --no-owner \
  | gzip -9 > "$BACKUP_FILE"

FILE_SIZE=$(du -h "$BACKUP_FILE" | cut -f1)

find "$BACKUP_DIR" -type f -name "vsat_compass_*.sql.gz" -mtime +${RETENTION_DAYS} -delete

rclone copy "$BACKUP_FILE" gdrive:vsat-backups/
rclone delete gdrive:vsat-backups/ --min-age ${RETENTION_DAYS}d

send_telegram "✅ *[V-SAT BACKUP OK]* File: \`$(basename "$BACKUP_FILE")\` (${FILE_SIZE}) đã lưu lên Google Drive."
```

### 4.3. Cronjob & Lệnh Khôi phục
- Cronjob (`crontab -e`):
```cron
0 2 * * * /opt/vsat-compass/scripts/backup-db.sh >> /var/log/vsat-backup.log 2>&1
```
- Lệnh Restore:
```bash
gunzip -c /opt/vsat-compass/backups/vsat_compass_YYYYMMDD_HHMMSS.sql.gz \
  | docker exec -i vsat-pg psql -U "$POSTGRES_USER" -d "$POSTGRES_DB"
```

---

## GIAI ĐOẠN 5: Cấu hình Client (Android & Web Admin) kết nối Server mới

### 5.1. Android Client (`app/build.gradle.kts`)
Cập nhật dòng 40–55 trong `app/build.gradle.kts`:
```kotlin
    buildTypes {
        debug {
            buildConfigField("String", "BASE_URL_CLOUD", "\"https://api.yourdomain.com/api/v1/\"")
            buildConfigField("String", "LOCAL_LAN_HOST", "\"192.168.1.240\"")
            buildConfigField("String", "LOCAL_BACKEND_PORT", "\"8080\"")
            buildConfigField("boolean", "USE_LOCAL_BACKEND", "false")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            buildConfigField("String", "BASE_URL_CLOUD", "\"https://api.yourdomain.com/api/v1/\"")
            buildConfigField("String", "LOCAL_LAN_HOST", "\"192.168.1.240\"")
            buildConfigField("String", "LOCAL_BACKEND_PORT", "\"8080\"")
            buildConfigField("boolean", "USE_LOCAL_BACKEND", "false")
```

### 5.2. Web Admin (`web-admin/`)
1. Tạo `/opt/vsat-compass/nginx-spa.conf`:
```nginx
server {
    listen 80;
    server_name _;
    root /usr/share/nginx/html;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }
}
```
2. Build với biến `VITE_API_URL`:
```bash
cd /opt/vsat-compass/repo/web-admin
VITE_API_URL=https://api.yourdomain.com/api/v1 npm run build
```

---

## GIAI ĐOẠN 6: Giám sát & Vận hành (Monitoring & Alerting)

### 6.1. Script Giám sát & Tự phục hồi (`/opt/vsat-compass/scripts/health-monitor.sh`)
```bash
#!/usr/bin/env bash
set -u

API_HEALTH_URL="http://127.0.0.1:8080/api/v1/actuator/health"
STATE_FILE="/tmp/vsat_health.state"
TG_BOT_TOKEN="123456789:ABCDefGhIJKlmNoPQRsTUVwxyZ"
TG_CHAT_ID="-1001234567890"

send_alert() {
  curl -s -X POST "https://api.telegram.org/bot${TG_BOT_TOKEN}/sendMessage" \
    -d chat_id="${TG_CHAT_ID}" \
    -d parse_mode="Markdown" \
    -d text="$1" >/dev/null || true
}

PREV_STATE=$(cat "$STATE_FILE" 2>/dev/null || echo "UP")
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" --max-time 5 "$API_HEALTH_URL" || echo "000")

if [[ "$HTTP_CODE" == "200" ]]; then
  if [[ "$PREV_STATE" == "DOWN" ]]; then
    send_alert "🟢 *[V-SAT RECOVERED]* API đã hoạt động trở lại bình thường (HTTP 200)."
    echo "UP" > "$STATE_FILE"
  fi
else
  if [[ "$PREV_STATE" == "UP" ]]; then
    CPU_LOAD=$(uptime | awk -F'load average:' '{ print $2 }')
    FREE_RAM=$(free -h | awk '/^Mem:/ {print $4 "/" $2}')
    send_alert "🔴 *[V-SAT ALERT: API DOWN]*
- Mã phản hồi: \`HTTP ${HTTP_CODE}\`
- Load Average: \`${CPU_LOAD}\`
- RAM trống: \`${FREE_RAM}\`
- Đang tự động chạy \`docker restart vsat-api\`..."
    echo "DOWN" > "$STATE_FILE"
    docker restart vsat-api >/dev/null 2>&1 || true
  fi
fi
```
Cài vào `crontab -e`:
```cron
*/2 * * * * /opt/vsat-compass/scripts/health-monitor.sh
```

---

## PHỤ LỤC: 5 CẠM BẪY THỰC TẾ TRÊN LINUX HOME SERVER

1. **Lệch múi giờ (`UTC` vs `Asia/Ho_Chi_Minh`):** Đã ép cứng `-Duser.timezone=UTC` trong `Dockerfile` và `TZ=UTC`, `PGTZ=UTC` trong container `postgres`.
2. **Đầy ổ cứng do Docker JSON Logs:** Tạo `/etc/docker/daemon.json` giới hạn `"max-size": "20m"`, `"max-file": "5"`.
3. **Modem nhà mạng bóp UDP/QUIC:** Nếu `cloudflared` báo rớt gói QUIC, đổi `protocol: quic` thành `protocol: http2` trong `/etc/cloudflared/config.yml`.
4. **Quyền thư mục Volume của PostgreSQL (`postgres:17-alpine`):** Luôn dùng Named Volume (`pgdata:/var/lib/postgresql/data`) để tránh xung đột UID `70:70`.
5. **Docker tự xuyên thủng tường lửa UFW:** Luôn bind rõ IP `127.0.0.1:8080:8080` và `192.168.1.240:8080:8080` thay vì `8080:8080`.
