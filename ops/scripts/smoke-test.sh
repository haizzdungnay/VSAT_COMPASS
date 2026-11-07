#!/usr/bin/env bash
# ============================================================================
# V-SAT Compass: Post-Deployment Smoke Test Suite
# ============================================================================
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8080/api/v1}"
WEB_URL="${WEB_URL:-http://127.0.0.1:8088}"
TIMEOUT=8

echo "============================================================"
echo "[SMOKE-TEST] Running V-SAT Compass Post-Deployment Verification"
echo "  Backend API URL : ${BASE_URL}"
echo "  Web Admin URL   : ${WEB_URL}"
echo "============================================================"

FAILED=0

check_endpoint() {
  local name="$1"
  local url="$2"
  local expected_http="$3"
  local match_pattern="${4:-}"

  local response_body
  local http_code

  response_body=$(curl -s --max-time "$TIMEOUT" "$url" 2>/dev/null || echo "")
  http_code=$(curl -s -o /dev/null -w "%{http_code}" --max-time "$TIMEOUT" "$url" 2>/dev/null || echo "000")

  if [[ "$http_code" != "$expected_http" ]]; then
    echo "  ❌ [FAIL] ${name} -> Expected HTTP ${expected_http}, got HTTP ${http_code}"
    FAILED=$((FAILED + 1))
    return
  fi

  if [[ -n "$match_pattern" ]]; then
    if ! echo "$response_body" | grep -qi "$match_pattern"; then
      echo "  ❌ [FAIL] ${name} -> HTTP ${http_code} OK, but body does not match '${match_pattern}'"
      FAILED=$((FAILED + 1))
      return
    fi
  fi

  echo "  ✅ [PASS] ${name} -> HTTP ${http_code} OK"
}

# 1. Backend Actuator Health
check_endpoint "Actuator Health (/actuator/health)" \
  "${BASE_URL}/actuator/health" "200" '"status":"UP"'

# 2. Public Exam Listing
check_endpoint "Public Exams Catalog (/exams)" \
  "${BASE_URL}/exams" "200" '"success":true'

# 3. Admission Universities
check_endpoint "Admission Universities (/admissions/universities)" \
  "${BASE_URL}/admissions/universities" "200" '"code":"HUB"'

# 4. Admission Subject Combos
check_endpoint "Admission Subject Combos (/admissions/subject-combos)" \
  "${BASE_URL}/admissions/subject-combos" "200" '"A00"'

# 5. Web Admin / CBT SPA
check_endpoint "Web Admin SPA Root (/)" \
  "${WEB_URL}" "200" '<div id="root">'

echo "------------------------------------------------------------"
if [[ "$FAILED" -eq 0 ]]; then
  echo "[SMOKE-TEST SUCCESS] All 5 smoke test endpoints passed 100%!"
  exit 0
else
  echo "[SMOKE-TEST FAILED] ${FAILED} check(s) failed."
  exit 1
fi
