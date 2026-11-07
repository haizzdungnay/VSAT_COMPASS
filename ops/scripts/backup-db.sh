#!/usr/bin/env bash
# ============================================================================
# V-SAT Compass: Automated Database Backup & Retention Script
# ============================================================================
set -euo pipefail

ENV_FILE="${ENV_FILE:-.env}"
if [[ -f "$ENV_FILE" ]]; then
  # shellcheck disable=SC1090
  source "$ENV_FILE"
fi

BACKUP_DIR="${BACKUP_DIR:-/opt/vsat-compass/backups}"
CONTAINER_NAME="${PG_CONTAINER:-vsat-postgres}"
DB_NAME="${POSTGRES_DB:-vsat_compass}"
DB_USER="${POSTGRES_USER:-vsat_prod_user}"
RETENTION_DAYS="${BACKUP_RETENTION_DAYS:-14}"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_FILENAME="vsat_compass_${TIMESTAMP}.sql.gz"
BACKUP_FILEPATH="${BACKUP_DIR}/${BACKUP_FILENAME}"

TG_TOKEN="${TELEGRAM_BOT_TOKEN:-}"
TG_CHAT="${TELEGRAM_CHAT_ID:-}"
RCLONE_REMOTE_NAME="${RCLONE_REMOTE:-gdrive}"

send_telegram() {
  local msg="$1"
  if [[ -n "$TG_TOKEN" && -n "$TG_CHAT" && "$TG_TOKEN" != "123456789:ABCDefGhIJKlmNoPQRsTUVwxyZ" ]]; then
    curl -s -X POST "https://api.telegram.org/bot${TG_TOKEN}/sendMessage" \
      -d chat_id="${TG_CHAT}" \
      -d parse_mode="Markdown" \
      -d text="${msg}" >/dev/null 2>&1 || true
  fi
}

handle_error() {
  local line_no=$1
  echo "[OPS ERROR] Backup failed at line ${line_no}!"
  send_telegram "🚨 *[V-SAT BACKUP FAILED]* Lỗi sao lưu DB tại dòng ${line_no} lúc $(date '+%Y-%m-%d %H:%M:%S')!"
  exit 1
}

trap 'handle_error $LINENO' ERR

mkdir -p "$BACKUP_DIR"

echo "============================================================"
echo "[OPS] Starting PostgreSQL Database Backup"
echo "  Target File    : ${BACKUP_FILEPATH}"
echo "  Retention Days : ${RETENTION_DAYS}"
echo "============================================================"

# 1. Execute pg_dump from container and compress via gzip
echo "[OPS] 1/3 Dumping database '${DB_NAME}' from '${CONTAINER_NAME}'..."
docker exec "$CONTAINER_NAME" pg_dump \
  -U "$DB_USER" \
  -d "$DB_NAME" \
  --clean \
  --if-exists \
  --no-owner \
  | gzip -9 > "$BACKUP_FILEPATH"

if [[ ! -s "$BACKUP_FILEPATH" ]]; then
  echo "[OPS ERROR] Backup file was created but is empty: ${BACKUP_FILEPATH}"
  exit 1
fi

FILE_SIZE=$(du -h "$BACKUP_FILEPATH" | cut -f1)
echo "[OPS] Backup created successfully: ${BACKUP_FILENAME} (${FILE_SIZE})."

# 2. Rotate local backup retention (delete older than RETENTION_DAYS)
echo "[OPS] 2/3 Pruning local backups older than ${RETENTION_DAYS} days..."
find "$BACKUP_DIR" -type f -name "vsat_compass_*.sql.gz" -mtime +"${RETENTION_DAYS}" -delete || true

# 3. Optional Cloud Sync via Rclone (Google Drive / S3)
RCLONE_STATUS="Local only"
if command -v rclone >/dev/null 2>&1; then
  if rclone listremotes 2>/dev/null | grep -q "^${RCLONE_REMOTE_NAME}:"; then
    echo "[OPS] 3/3 Syncing to Cloud via Rclone (${RCLONE_REMOTE_NAME}:vsat-backups/)..."
    rclone copy "$BACKUP_FILEPATH" "${RCLONE_REMOTE_NAME}:vsat-backups/"
    rclone delete "${RCLONE_REMOTE_NAME}:vsat-backups/" --min-age "${RETENTION_DAYS}d" || true
    RCLONE_STATUS="Synced to ${RCLONE_REMOTE_NAME}"
    echo "[OPS] Cloud sync complete."
  else
    echo "[OPS] Rclone remote '${RCLONE_REMOTE_NAME}' not configured. Skipping remote sync."
  fi
else
  echo "[OPS] Rclone CLI not installed. Skipping cloud sync."
fi

# 4. Notify success via Telegram
SUCCESS_MSG="✅ *[V-SAT BACKUP OK]*
- File: \`${BACKUP_FILENAME}\`
- Dung lượng: \`${FILE_SIZE}\`
- Trạng thái: \`${RCLONE_STATUS}\`
- Thời gian: \`$(date '+%Y-%m-%d %H:%M:%S')\`"

send_telegram "$SUCCESS_MSG"
echo "[OPS SUCCESS] Backup workflow finished cleanly."
exit 0
