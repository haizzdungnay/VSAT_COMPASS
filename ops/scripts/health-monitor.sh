#!/usr/bin/env bash
# ============================================================================
# V-SAT Compass: Production Health Monitor & Auto-Recovery
# ============================================================================
set -u

ENV_FILE="${ENV_FILE:-.env}"
if [[ -f "$ENV_FILE" ]]; then
  # shellcheck disable=SC1090
  source "$ENV_FILE"
fi

API_HEALTH_URL="${API_HEALTH_URL:-http://127.0.0.1:8080/api/v1/actuator/health}"
STATE_FILE="${STATE_FILE:-/tmp/vsat_health.state}"
API_CONTAINER="${API_CONTAINER:-vsat-api}"
DISK_THRESHOLD_PCT="${DISK_THRESHOLD_PCT:-85}"

TG_TOKEN="${TELEGRAM_BOT_TOKEN:-}"
TG_CHAT="${TELEGRAM_CHAT_ID:-}"

send_telegram() {
  local msg="$1"
  if [[ -n "$TG_TOKEN" && -n "$TG_CHAT" && "$TG_TOKEN" != "123456789:ABCDefGhIJKlmNoPQRsTUVwxyZ" ]]; then
    curl -s -X POST "https://api.telegram.org/bot${TG_TOKEN}/sendMessage" \
      -d chat_id="${TG_CHAT}" \
      -d parse_mode="Markdown" \
      -d text="${msg}" >/dev/null 2>&1 || true
  fi
}

PREV_STATE=$(cat "$STATE_FILE" 2>/dev/null || echo "UP")

# 1. Probe API Actuator Health
HTTP_CODE=$(curl -s -o /tmp/vsat_health_resp.json -w "%{http_code}" --max-time 6 "$API_HEALTH_URL" 2>/dev/null || echo "000")
HEALTH_STATUS="DOWN"

if [[ "$HTTP_CODE" == "200" ]]; then
  if grep -q '"status":"UP"' /tmp/vsat_health_resp.json 2>/dev/null; then
    HEALTH_STATUS="UP"
  fi
fi

# 2. Probe System Resources
CPU_LOAD=$(uptime 2>/dev/null | awk -F'load average:' '{ print $2 }' | sed 's/^[ \t]*//' || echo "N/A")
FREE_RAM=$(free -h 2>/dev/null | awk '/^Mem:/ {print $4 "/" $2}' || echo "N/A")
DISK_USAGE_PCT=$(df -h / 2>/dev/null | awk 'NR==2 {print $5}' | tr -d '%' || echo "0")
DISK_FREE=$(df -h / 2>/dev/null | awk 'NR==2 {print $4}' || echo "N/A")

# 3. Check Disk Space Threshold
if [[ "$DISK_USAGE_PCT" =~ ^[0-9]+$ ]] && [[ "$DISK_USAGE_PCT" -ge "$DISK_THRESHOLD_PCT" ]]; then
  send_telegram "⚠️ *[V-SAT CẢNH BÁO: ĐĨA GẦN ĐẦY]*
- Phân vùng: \`/\`
- Đã dùng: \`${DISK_USAGE_PCT}%\` (Ngưỡng: ${DISK_THRESHOLD_PCT}%)
- Dung lượng trống: \`${DISK_FREE}\`
- Vui lòng chạy lệnh dọn dẹp \`docker system prune\` hoặc kiểm tra logs!"
fi

# 4. Handle State Transitions & Auto-Recovery
if [[ "$HEALTH_STATUS" == "UP" ]]; then
  if [[ "$PREV_STATE" == "DOWN" ]]; then
    send_telegram "🟢 *[V-SAT RECOVERED]* Hệ thống API đã phục hồi và hoạt động bình thường!
- Mã HTTP: \`200 OK\`
- Load: \`${CPU_LOAD}\`
- RAM trống: \`${FREE_RAM}\`
- Đĩa trống: \`${DISK_FREE}\`"
    echo "UP" > "$STATE_FILE"
  fi
else
  # API DOWN
  DOCKER_STATUS="N/A"
  if command -v docker >/dev/null 2>&1; then
    DOCKER_STATUS=$(docker ps -a --filter "name=vsat-" --format "{{.Names}}: {{.Status}}" 2>/dev/null | paste -sd ", " - || echo "Docker query failed")
  fi

  if [[ "$PREV_STATE" == "UP" ]]; then
    send_telegram "🔴 *[V-SAT ALERT: API DOWN]*
- Endpoint: \`${API_HEALTH_URL}\`
- Phản hồi: \`HTTP ${HTTP_CODE}\`
- CPU Load: \`${CPU_LOAD}\`
- RAM trống: \`${FREE_RAM}\`
- Ổ đĩa trống: \`${DISK_FREE}\`
- Trạng thái Docker: \`${DOCKER_STATUS}\`
- *Hành động*: Đang tự động khởi động lại container \`${API_CONTAINER}\`..."
    echo "DOWN" > "$STATE_FILE"
  fi

  # Attempt auto-recovery
  if command -v docker >/dev/null 2>&1; then
    echo "[OPS WARN] API is DOWN. Restarting container '${API_CONTAINER}'..."
    docker restart "$API_CONTAINER" >/dev/null 2>&1 || true
  fi
fi

exit 0
