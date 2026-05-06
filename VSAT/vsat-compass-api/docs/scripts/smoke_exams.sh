#!/bin/bash
# ============================================================
# V-SAT Compass - Smoke (C1.2a Exams)
# Runs read-only production checks for the public Exam API.
# Usage:
#   EXAM_ID=2 bash VSAT/vsat-compass-api/docs/scripts/smoke_exams.sh
#   BASE_URL=https://your-api.com/api/v1 EXAM_ID=2 bash VSAT/vsat-compass-api/docs/scripts/smoke_exams.sh
#
# Optional env:
#   BASE_URL          API base URL. Default: https://vsat-compass-api.onrender.com/api/v1
#   SMOKE_EXAM_CODE   Published smoke exam code. Default: SMOKE_C1_2A_001
#   EXAM_ID           Exam id used by detail checks. If provided, the script uses
#                     this override and logs it. Production fallback historically
#                     uses EXAM_ID=2 when jq is unavailable.
#   jq                Optional. If jq is unavailable, list checks use grep/sed
#                     fallback and detail checks require EXAM_ID.
#   SUPER_ADMIN_EMAIL / SUPER_ADMIN_PASSWORD
#                     Not used by this public read-only smoke script.
# Compatible with bash 3.2+ (macOS default)
# ============================================================

set -uo pipefail

BASE_URL="${BASE_URL:-https://vsat-compass-api.onrender.com/api/v1}"
SMOKE_EXAM_CODE="${SMOKE_EXAM_CODE:-SMOKE_C1_2A_001}"
EXAM_ID="${EXAM_ID:-}"

PASS=0
FAIL=0
TOTAL=0
LIST_BODY=""
LIST_STATUS=""
DETAIL_BODY=""
DETAIL_STATUS=""
NOT_FOUND_BODY=""
NOT_FOUND_STATUS=""
DETAIL_QUERY_BODY=""
DETAIL_QUERY_STATUS=""
PAGE_BODY=""
PAGE_STATUS=""

if command -v jq >/dev/null 2>&1 && jq --version >/dev/null 2>&1; then
    HAS_JQ=1
else
    HAS_JQ=0
fi

echo "========================================"
echo "V-SAT Compass — Smoke (C1.2a Exams)"
echo "Base URL: $BASE_URL"
echo "Smoke exam code: $SMOKE_EXAM_CODE"
echo "Date: $(date -u '+%Y-%m-%dT%H:%M:%SZ')"
if [ "$HAS_JQ" -eq 1 ]; then
    echo "JSON parser: jq"
else
    echo "JSON parser: grep/sed fallback (jq unavailable)"
fi
if [ -n "$EXAM_ID" ]; then
    echo "Exam id source: EXAM_ID override ($EXAM_ID)"
else
    echo "Exam id source: auto-discover with jq; set EXAM_ID explicitly without jq"
fi
echo "========================================"
echo ""

record_pass() {
    local test_id="$1"
    local description="$2"
    TOTAL=$((TOTAL + 1))
    PASS=$((PASS + 1))
    echo "  [PASS] $test_id $description"
}

record_fail() {
    local test_id="$1"
    local description="$2"
    local detail="${3:-}"
    TOTAL=$((TOTAL + 1))
    FAIL=$((FAIL + 1))
    if [ -n "$detail" ]; then
        echo "  [FAIL] $test_id $description ($detail)"
    else
        echo "  [FAIL] $test_id $description"
    fi
}

request_get() {
    local url="$1"
    curl -s -w "\n%{http_code}" -X GET "$url" -H "Accept: application/json"
}

body_from_response() {
    echo "$1" | sed '$d'
}

status_from_response() {
    echo "$1" | tail -1
}

json_has_key() {
    local body="$1"
    local key="$2"
    if [ "$HAS_JQ" -eq 1 ]; then
        echo "$body" | jq -e --arg key "$key" '.data | has($key)' >/dev/null 2>&1
    else
        echo "$body" | grep -q "\"$key\"[[:space:]]*:"
    fi
}

json_lacks_key() {
    local body="$1"
    local key="$2"
    if [ "$HAS_JQ" -eq 1 ]; then
        echo "$body" | jq -e --arg key "$key" '(.data | has($key) | not)' >/dev/null 2>&1
    else
        ! echo "$body" | grep -q "\"$key\"[[:space:]]*:"
    fi
}

echo "--- TC-EXAM-1: GET /exams ---"
LIST_RESPONSE=$(request_get "$BASE_URL/exams")
LIST_BODY=$(body_from_response "$LIST_RESPONSE")
LIST_STATUS=$(status_from_response "$LIST_RESPONSE")
if [ "$LIST_STATUS" = "200" ]; then
    record_pass "TC-EXAM-1" "GET /exams returns HTTP 200"
else
    record_fail "TC-EXAM-1" "GET /exams returns HTTP 200" "got HTTP $LIST_STATUS; body=$LIST_BODY"
fi

echo "--- TC-EXAM-2: /exams has data.content array ---"
if [ "$HAS_JQ" -eq 1 ]; then
    if echo "$LIST_BODY" | jq -e '.data.content | type == "array"' >/dev/null 2>&1; then
        record_pass "TC-EXAM-2" "GET /exams body has data.content array"
    else
        record_fail "TC-EXAM-2" "GET /exams body has data.content array" "body=$LIST_BODY"
    fi
else
    if echo "$LIST_BODY" | grep -q '"data"[[:space:]]*:[[:space:]]*{' \
        && echo "$LIST_BODY" | grep -q '"content"[[:space:]]*:[[:space:]]*\['; then
        record_pass "TC-EXAM-2" "GET /exams body has data.content array"
    else
        record_fail "TC-EXAM-2" "GET /exams body has data.content array" "body=$LIST_BODY"
    fi
fi

echo "--- TC-EXAM-3: smoke exam appears in /exams ---"
if [ "$HAS_JQ" -eq 1 ]; then
    DISCOVERED_EXAM_ID=$(echo "$LIST_BODY" | jq -r --arg code "$SMOKE_EXAM_CODE" '.data.content[]? | select(.examCode == $code) | .id' 2>/dev/null | head -1)
    if [ -z "$EXAM_ID" ]; then
        EXAM_ID="$DISCOVERED_EXAM_ID"
    fi
    if [ -n "$DISCOVERED_EXAM_ID" ]; then
        record_pass "TC-EXAM-3" "$SMOKE_EXAM_CODE appears in data.content"
    else
        record_fail "TC-EXAM-3" "$SMOKE_EXAM_CODE appears in data.content" "body=$LIST_BODY"
    fi
else
    if echo "$LIST_BODY" | grep -q "\"examCode\"[[:space:]]*:[[:space:]]*\"$SMOKE_EXAM_CODE\""; then
        record_pass "TC-EXAM-3" "$SMOKE_EXAM_CODE appears in data.content"
    else
        record_fail "TC-EXAM-3" "$SMOKE_EXAM_CODE appears in data.content" "body=$LIST_BODY"
    fi
fi

if [ -z "$EXAM_ID" ]; then
    if [ "$HAS_JQ" -eq 0 ]; then
        echo "  [FAIL] EXAM_ID is required for detail checks when jq is unavailable."
        echo "  [FAIL] Run with EXAM_ID=<seeded-public-exam-id>; production v0.9.3 used EXAM_ID=2."
        echo "  [FAIL] Example: EXAM_ID=2 bash VSAT/vsat-compass-api/docs/scripts/smoke_exams.sh"
        exit 1
    fi
    echo "  [WARN] EXAM_ID was not provided and jq could not discover it."
    echo "  [WARN] Detail checks will use /exams/<missing> and fail. Set EXAM_ID to the seeded smoke exam id."
fi

echo "--- TC-EXAM-4: questionCount is numeric and non-negative ---"
if [ "$HAS_JQ" -eq 1 ]; then
    if echo "$LIST_BODY" | jq -e '.data.content | all(.[]; (.questionCount | type == "number") and (.questionCount >= 0))' >/dev/null 2>&1; then
        record_pass "TC-EXAM-4" "Each data.content item has questionCount as a JSON number >= 0"
    else
        record_fail "TC-EXAM-4" "Each data.content item has questionCount as a JSON number >= 0" "body=$LIST_BODY"
    fi
else
    if echo "$LIST_BODY" | grep -q '"questionCount"[[:space:]]*:[[:space:]]*[0-9]' \
        && ! echo "$LIST_BODY" | grep -q '"questionCount"[[:space:]]*:[[:space:]]*-' \
        && ! echo "$LIST_BODY" | grep -q '"questionCount"[[:space:]]*:[[:space:]]*"' \
        && ! echo "$LIST_BODY" | grep -q '"questionCount"[[:space:]]*:[[:space:]]*null'; then
        record_pass "TC-EXAM-4" "Each data.content item has questionCount as a JSON number >= 0"
    else
        record_fail "TC-EXAM-4" "Each data.content item has questionCount as a JSON number >= 0" "body=$LIST_BODY"
    fi
fi

echo "--- TC-EXAM-5: pricingType is FREE ---"
if [ "$HAS_JQ" -eq 1 ]; then
    if echo "$LIST_BODY" | jq -e '.data.content | all(.[]; .pricingType == "FREE")' >/dev/null 2>&1; then
        record_pass "TC-EXAM-5" "Each data.content item has pricingType FREE"
    else
        record_fail "TC-EXAM-5" "Each data.content item has pricingType FREE" "body=$LIST_BODY"
    fi
else
    if echo "$LIST_BODY" | grep -q '"pricingType"[[:space:]]*:[[:space:]]*"FREE"' \
        && ! echo "$LIST_BODY" | grep -q '"pricingType"[[:space:]]*:[[:space:]]*"PAID"' \
        && ! echo "$LIST_BODY" | grep -q '"pricingType"[[:space:]]*:[[:space:]]*"PACKAGE"'; then
        record_pass "TC-EXAM-5" "Each data.content item has pricingType FREE"
    else
        record_fail "TC-EXAM-5" "Each data.content item has pricingType FREE" "body=$LIST_BODY"
    fi
fi

echo "--- TC-EXAM-6: GET /exams/{SMOKE_EXAM_ID} whitelist fields ---"
DETAIL_RESPONSE=$(request_get "$BASE_URL/exams/$EXAM_ID")
DETAIL_BODY=$(body_from_response "$DETAIL_RESPONSE")
DETAIL_STATUS=$(status_from_response "$DETAIL_RESPONSE")
DETAIL_KEYS_OK=1
for key in id examCode title subjectId description questionCount durationMinutes difficulty pricingType tags; do
    if ! json_has_key "$DETAIL_BODY" "$key"; then
        DETAIL_KEYS_OK=0
    fi
done
if [ "$DETAIL_STATUS" = "200" ] && [ "$DETAIL_KEYS_OK" -eq 1 ]; then
    record_pass "TC-EXAM-6" "GET /exams/{SMOKE_EXAM_ID} returns HTTP 200 and core whitelist fields"
else
    record_fail "TC-EXAM-6" "GET /exams/{SMOKE_EXAM_ID} returns HTTP 200 and core whitelist fields" "got HTTP $DETAIL_STATUS; body=$DETAIL_BODY"
fi

echo "--- TC-EXAM-7: detail DTO forbidden keys absent ---"
FORBIDDEN_OK=1
for key in status price createdBy reviewedBy version totalAttempts avgScore createdAt updatedAt questions correctOptionId explanation; do
    if ! json_lacks_key "$DETAIL_BODY" "$key"; then
        FORBIDDEN_OK=0
    fi
done
if [ "$FORBIDDEN_OK" -eq 1 ]; then
    record_pass "TC-EXAM-7" "GET /exams/{SMOKE_EXAM_ID} does not leak forbidden keys"
else
    record_fail "TC-EXAM-7" "GET /exams/{SMOKE_EXAM_ID} does not leak forbidden keys" "body=$DETAIL_BODY"
fi

echo "--- TC-EXAM-8: GET /exams/9999999 anti-leak 404 ---"
NOT_FOUND_RESPONSE=$(request_get "$BASE_URL/exams/9999999")
NOT_FOUND_BODY=$(body_from_response "$NOT_FOUND_RESPONSE")
NOT_FOUND_STATUS=$(status_from_response "$NOT_FOUND_RESPONSE")
if [ "$HAS_JQ" -eq 1 ]; then
    NOT_FOUND_CODE_OK=0
    if echo "$NOT_FOUND_BODY" | jq -e '.error.code == "RESOURCE_NOT_FOUND"' >/dev/null 2>&1; then
        NOT_FOUND_CODE_OK=1
    fi
else
    NOT_FOUND_CODE_OK=0
    if echo "$NOT_FOUND_BODY" | grep -q '"code"[[:space:]]*:[[:space:]]*"RESOURCE_NOT_FOUND"'; then
        NOT_FOUND_CODE_OK=1
    fi
fi
if [ "$NOT_FOUND_STATUS" = "404" ] && [ "$NOT_FOUND_CODE_OK" -eq 1 ]; then
    record_pass "TC-EXAM-8" "GET /exams/9999999 returns 404 RESOURCE_NOT_FOUND"
else
    record_fail "TC-EXAM-8" "GET /exams/9999999 returns 404 RESOURCE_NOT_FOUND" "got HTTP $NOT_FOUND_STATUS; body=$NOT_FOUND_BODY"
fi

echo "--- TC-EXAM-9: detail path ignores pagination params ---"
DETAIL_QUERY_RESPONSE=$(request_get "$BASE_URL/exams/$EXAM_ID?page=0&size=10")
DETAIL_QUERY_BODY=$(body_from_response "$DETAIL_QUERY_RESPONSE")
DETAIL_QUERY_STATUS=$(status_from_response "$DETAIL_QUERY_RESPONSE")
if [ "$HAS_JQ" -eq 1 ]; then
    DETAIL_QUERY_OK=0
    if echo "$DETAIL_QUERY_BODY" | jq -e --argjson id "$EXAM_ID" '.data.id == $id' >/dev/null 2>&1; then
        DETAIL_QUERY_OK=1
    fi
else
    DETAIL_QUERY_OK=0
    if echo "$DETAIL_QUERY_BODY" | grep -q "\"id\"[[:space:]]*:[[:space:]]*$EXAM_ID" \
        && echo "$DETAIL_QUERY_BODY" | grep -q "\"examCode\"[[:space:]]*:[[:space:]]*\"$SMOKE_EXAM_CODE\""; then
        DETAIL_QUERY_OK=1
    fi
fi
if [ "$DETAIL_QUERY_STATUS" = "200" ] && [ "$DETAIL_QUERY_OK" -eq 1 ]; then
    record_pass "TC-EXAM-9" "GET /exams/{SMOKE_EXAM_ID}?page=0&size=10 returns the same exam"
else
    record_fail "TC-EXAM-9" "GET /exams/{SMOKE_EXAM_ID}?page=0&size=10 returns the same exam" "got HTTP $DETAIL_QUERY_STATUS; body=$DETAIL_QUERY_BODY"
fi

echo "--- TC-EXAM-10: list pagination metadata ---"
PAGE_RESPONSE=$(request_get "$BASE_URL/exams?page=0&size=1")
PAGE_BODY=$(body_from_response "$PAGE_RESPONSE")
PAGE_STATUS=$(status_from_response "$PAGE_RESPONSE")
if [ "$HAS_JQ" -eq 1 ]; then
    PAGE_OK=0
    if echo "$PAGE_BODY" | jq -e '.data | has("totalElements") and has("totalPages") and has("number") and has("size")' >/dev/null 2>&1; then
        PAGE_OK=1
    fi
else
    PAGE_OK=0
    if echo "$PAGE_BODY" | grep -q '"totalElements"[[:space:]]*:' \
        && echo "$PAGE_BODY" | grep -q '"totalPages"[[:space:]]*:' \
        && echo "$PAGE_BODY" | grep -q '"number"[[:space:]]*:' \
        && echo "$PAGE_BODY" | grep -q '"size"[[:space:]]*:'; then
        PAGE_OK=1
    fi
fi
if [ "$PAGE_STATUS" = "200" ] && [ "$PAGE_OK" -eq 1 ]; then
    record_pass "TC-EXAM-10" "GET /exams?page=0&size=1 returns pagination metadata"
else
    record_fail "TC-EXAM-10" "GET /exams?page=0&size=1 returns pagination metadata" "got HTTP $PAGE_STATUS; body=$PAGE_BODY"
fi

echo ""
echo "========================================"
echo "Smoke (exams): $PASS/$TOTAL PASS"
echo "========================================"

if [ "$FAIL" -gt 0 ]; then
    exit 1
fi
exit 0
