#!/bin/bash
# ============================================================
# V-SAT Compass - Admin Exam CRUD Smoke Tests
# Runs production checks for admin exam metadata CRUD and DRAFT discard.
# Usage:
#   SMOKE_ADMIN_PASSWORD=... bash docs/scripts/smoke_admin_exams.sh
#   BASE_URL=https://your-api.com/api/v1 SMOKE_ADMIN_PASSWORD=... bash docs/scripts/smoke_admin_exams.sh
# Compatible with bash 3.2+ (macOS default)
# ============================================================

set -uo pipefail

BASE_URL="${BASE_URL:-https://vsat-compass-api.onrender.com/api/v1}"
SMOKE_ADMIN_EMAIL="${SMOKE_ADMIN_EMAIL:-admin@vsat.com}"
SMOKE_ADMIN_PASSWORD="${SMOKE_ADMIN_PASSWORD:-}"

if [ -z "$SMOKE_ADMIN_PASSWORD" ]; then
    echo "ERROR: SMOKE_ADMIN_PASSWORD is required."
    exit 1
fi

PASS=0
FAIL=0
TOTAL=0
ADMIN_TOKEN=""
EXAM_CODE="SMOKE_ADMIN_EXAM_$(date +%s)"
CREATED_ID=""
SUBJECT_ID=""

if command -v jq >/dev/null 2>&1; then
    HAS_JQ=1
else
    HAS_JQ=0
fi

echo "========================================"
echo "V-SAT Compass - Admin Exam CRUD Smoke Tests"
echo "Base URL: $BASE_URL"
echo "Admin email: $SMOKE_ADMIN_EMAIL"
echo "Smoke exam code: $EXAM_CODE"
echo "Date: $(date -u '+%Y-%m-%dT%H:%M:%SZ')"
if [ "$HAS_JQ" -eq 1 ]; then
    echo "JSON parser: jq"
else
    echo "JSON parser: grep fallback"
fi
echo "========================================"
echo ""

record_result() {
    local test_id="$1"
    local description="$2"
    local ok="$3"
    local detail="${4:-}"
    TOTAL=$((TOTAL + 1))
    if [ "$ok" -eq 1 ]; then
        PASS=$((PASS + 1))
        echo "  [PASS] $test_id $description"
    else
        FAIL=$((FAIL + 1))
        if [ -n "$detail" ]; then
            echo "  [FAIL] $test_id $description ($detail)"
        else
            echo "  [FAIL] $test_id $description"
        fi
    fi
}

body_from_response() {
    echo "$1" | sed '$d'
}

status_from_response() {
    echo "$1" | tail -1
}

request_json() {
    local method="$1"
    local url="$2"
    local token="$3"
    local body="${4:-}"
    if [ -n "$body" ]; then
        curl -s -w "\n%{http_code}" -X "$method" "$url" \
            -H "Content-Type: application/json" \
            -H "Accept: application/json" \
            -H "Authorization: Bearer $token" \
            -d "$body"
    else
        curl -s -w "\n%{http_code}" -X "$method" "$url" \
            -H "Accept: application/json" \
            -H "Authorization: Bearer $token"
    fi
}

extract_access_token() {
    if [ "$HAS_JQ" -eq 1 ]; then
        echo "$1" | jq -r '.data.accessToken // empty' 2>/dev/null
    else
        echo "$1" | grep -o '"accessToken":"[^"]*"' | head -1 | cut -d'"' -f4
    fi
}

extract_exam_id() {
    if [ "$HAS_JQ" -eq 1 ]; then
        echo "$1" | jq -r '.data.id // empty' 2>/dev/null
    else
        echo "$1" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2
    fi
}

extract_math_subject_id() {
    if [ "$HAS_JQ" -eq 1 ]; then
        echo "$1" | jq -r '.data[]? | select(.code == "MATH") | .id' 2>/dev/null | head -1
    else
        echo "$1" | sed 's/},{/}\
{/g' | grep '"code":"MATH"' | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2
    fi
}

extract_first_subject_id() {
    if [ "$HAS_JQ" -eq 1 ]; then
        echo "$1" | jq -r '.data[0].id // empty' 2>/dev/null
    else
        echo "$1" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2
    fi
}

body_contains() {
    echo "$1" | grep -q "$2"
}

body_lacks_forbidden_exam_content() {
    local body="$1"
    ! echo "$body" | grep -Eq '"(questions|correctOptionId|explanation)"[[:space:]]*:'
}

echo "--- TC-ADMIN-EXAM-1: Login admin ---"
LOGIN_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/auth/login" \
    -H "Content-Type: application/json" \
    -H "Accept: application/json" \
    -d "{\"email\":\"$SMOKE_ADMIN_EMAIL\",\"password\":\"$SMOKE_ADMIN_PASSWORD\"}")
LOGIN_BODY=$(body_from_response "$LOGIN_RESPONSE")
LOGIN_STATUS=$(status_from_response "$LOGIN_RESPONSE")
ADMIN_TOKEN=$(extract_access_token "$LOGIN_BODY")
if [ "$LOGIN_STATUS" = "200" ] && [ -n "$ADMIN_TOKEN" ]; then
    record_result "TC-ADMIN-EXAM-1" "Login admin returns 200 and accessToken present" 1
else
    record_result "TC-ADMIN-EXAM-1" "Login admin returns 200 and accessToken present" 0 "HTTP $LOGIN_STATUS"
fi

if [ -z "$ADMIN_TOKEN" ]; then
    echo "  [ERROR] Missing admin token. Stopping admin exam smoke."
    echo ""
    echo "========================================"
    echo "Smoke (admin exams): $PASS/$TOTAL PASS"
    echo "========================================"
    exit 1
fi

echo "--- TC-ADMIN-EXAM-2: Anonymous GET /admin/exams is not public ---"
ANON_RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/admin/exams" -H "Accept: application/json")
ANON_STATUS=$(status_from_response "$ANON_RESPONSE")
if [ "$ANON_STATUS" = "401" ] || [ "$ANON_STATUS" = "403" ]; then
    record_result "TC-ADMIN-EXAM-2" "Anonymous GET /admin/exams is rejected" 1
else
    record_result "TC-ADMIN-EXAM-2" "Anonymous GET /admin/exams is rejected" 0 "HTTP $ANON_STATUS"
fi

echo "--- TC-ADMIN-EXAM-3: Authenticated GET /admin/exams ---"
ADMIN_LIST_RESPONSE=$(request_json "GET" "$BASE_URL/admin/exams" "$ADMIN_TOKEN")
ADMIN_LIST_STATUS=$(status_from_response "$ADMIN_LIST_RESPONSE")
if [ "$ADMIN_LIST_STATUS" = "200" ]; then
    record_result "TC-ADMIN-EXAM-3" "GET /admin/exams with admin token returns 200" 1
else
    record_result "TC-ADMIN-EXAM-3" "GET /admin/exams with admin token returns 200" 0 "HTTP $ADMIN_LIST_STATUS"
fi

echo "--- TC-ADMIN-EXAM-4: POST /admin/exams creates DRAFT exam ---"
SUBJECTS_RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/subjects" -H "Accept: application/json")
SUBJECTS_BODY=$(body_from_response "$SUBJECTS_RESPONSE")
SUBJECTS_STATUS=$(status_from_response "$SUBJECTS_RESPONSE")
if [ "$SUBJECTS_STATUS" = "200" ]; then
    SUBJECT_ID=$(extract_math_subject_id "$SUBJECTS_BODY")
    if [ -z "$SUBJECT_ID" ]; then
        SUBJECT_ID=$(extract_first_subject_id "$SUBJECTS_BODY")
    fi
fi
if [ -z "$SUBJECT_ID" ]; then
    record_result "TC-ADMIN-EXAM-4" "POST /admin/exams creates DRAFT exam" 0 "could not discover subjectId"
else
    CREATE_BODY="{\"examCode\":\"$EXAM_CODE\",\"title\":\"Smoke Admin Exam $EXAM_CODE\",\"subjectId\":$SUBJECT_ID,\"description\":\"Created by C1.2b-1 admin exam smoke\",\"durationMinutes\":90,\"difficulty\":\"MEDIUM\",\"pricingType\":\"FREE\",\"price\":0,\"tags\":\"smoke,c1-2b-1\"}"
    CREATE_RESPONSE=$(request_json "POST" "$BASE_URL/admin/exams" "$ADMIN_TOKEN" "$CREATE_BODY")
    CREATE_RESPONSE_BODY=$(body_from_response "$CREATE_RESPONSE")
    CREATE_STATUS=$(status_from_response "$CREATE_RESPONSE")
    CREATED_ID=$(extract_exam_id "$CREATE_RESPONSE_BODY")
    if { [ "$CREATE_STATUS" = "201" ] || [ "$CREATE_STATUS" = "200" ]; } \
        && body_contains "$CREATE_RESPONSE_BODY" "\"examCode\":\"$EXAM_CODE\"" \
        && body_contains "$CREATE_RESPONSE_BODY" '"status":"DRAFT"' \
        && body_contains "$CREATE_RESPONSE_BODY" '"questionCount":0' \
        && body_contains "$CREATE_RESPONSE_BODY" '"pricingType":"FREE"' \
        && body_lacks_forbidden_exam_content "$CREATE_RESPONSE_BODY" \
        && [ -n "$CREATED_ID" ]; then
        record_result "TC-ADMIN-EXAM-4" "POST /admin/exams creates DRAFT metadata-only exam" 1
        echo "    -> created id=$CREATED_ID subjectId=$SUBJECT_ID"
    else
        record_result "TC-ADMIN-EXAM-4" "POST /admin/exams creates DRAFT metadata-only exam" 0 "HTTP $CREATE_STATUS; body=$CREATE_RESPONSE_BODY"
    fi
fi

if [ -z "$CREATED_ID" ]; then
    echo "  [ERROR] Missing created exam id. Stopping admin exam smoke."
    echo ""
    echo "========================================"
    echo "Smoke (admin exams): $PASS/$TOTAL PASS"
    echo "========================================"
    exit 1
fi

echo "--- TC-ADMIN-EXAM-5: Duplicate examCode rejected ---"
DUP_RESPONSE=$(request_json "POST" "$BASE_URL/admin/exams" "$ADMIN_TOKEN" "$CREATE_BODY")
DUP_STATUS=$(status_from_response "$DUP_RESPONSE")
if [ "$DUP_STATUS" = "409" ]; then
    record_result "TC-ADMIN-EXAM-5" "Duplicate examCode returns 409" 1
else
    record_result "TC-ADMIN-EXAM-5" "Duplicate examCode returns 409" 0 "HTTP $DUP_STATUS"
fi

echo "--- TC-ADMIN-EXAM-6: GET /admin/exams/{id} returns metadata ---"
DETAIL_RESPONSE=$(request_json "GET" "$BASE_URL/admin/exams/$CREATED_ID" "$ADMIN_TOKEN")
DETAIL_BODY=$(body_from_response "$DETAIL_RESPONSE")
DETAIL_STATUS=$(status_from_response "$DETAIL_RESPONSE")
if [ "$DETAIL_STATUS" = "200" ] \
    && body_contains "$DETAIL_BODY" "\"id\":$CREATED_ID" \
    && body_contains "$DETAIL_BODY" "\"examCode\":\"$EXAM_CODE\"" \
    && body_lacks_forbidden_exam_content "$DETAIL_BODY"; then
    record_result "TC-ADMIN-EXAM-6" "GET /admin/exams/{id} returns metadata without content leak" 1
else
    record_result "TC-ADMIN-EXAM-6" "GET /admin/exams/{id} returns metadata without content leak" 0 "HTTP $DETAIL_STATUS; body=$DETAIL_BODY"
fi

echo "--- TC-ADMIN-EXAM-7: PUT /admin/exams/{id} updates DRAFT metadata ---"
UPDATED_TITLE="Smoke Admin Exam Updated $EXAM_CODE"
UPDATED_DESC="Updated by C1.2b-1 admin exam smoke"
UPDATE_BODY="{\"title\":\"$UPDATED_TITLE\",\"description\":\"$UPDATED_DESC\",\"durationMinutes\":120,\"difficulty\":\"HARD\",\"tags\":\"smoke,c1-2b-1,updated\"}"
UPDATE_RESPONSE=$(request_json "PUT" "$BASE_URL/admin/exams/$CREATED_ID" "$ADMIN_TOKEN" "$UPDATE_BODY")
UPDATE_RESPONSE_BODY=$(body_from_response "$UPDATE_RESPONSE")
UPDATE_STATUS=$(status_from_response "$UPDATE_RESPONSE")
if [ "$UPDATE_STATUS" = "200" ] \
    && body_contains "$UPDATE_RESPONSE_BODY" "\"title\":\"$UPDATED_TITLE\"" \
    && body_contains "$UPDATE_RESPONSE_BODY" "\"description\":\"$UPDATED_DESC\"" \
    && body_contains "$UPDATE_RESPONSE_BODY" '"durationMinutes":120' \
    && body_contains "$UPDATE_RESPONSE_BODY" '"tags":"smoke,c1-2b-1,updated"'; then
    record_result "TC-ADMIN-EXAM-7" "PUT /admin/exams/{id} updates metadata while DRAFT" 1
else
    record_result "TC-ADMIN-EXAM-7" "PUT /admin/exams/{id} updates metadata while DRAFT" 0 "HTTP $UPDATE_STATUS; body=$UPDATE_RESPONSE_BODY"
fi

echo "--- TC-ADMIN-EXAM-8: Non-free pricing rejected on update ---"
PAID_UPDATE_RESPONSE=$(request_json "PUT" "$BASE_URL/admin/exams/$CREATED_ID" "$ADMIN_TOKEN" '{"pricingType":"PAID","price":1000}')
PAID_UPDATE_STATUS=$(status_from_response "$PAID_UPDATE_RESPONSE")
if [ "$PAID_UPDATE_STATUS" = "400" ]; then
    record_result "TC-ADMIN-EXAM-8" "PUT pricingType=PAID / price>0 is rejected" 1
else
    record_result "TC-ADMIN-EXAM-8" "PUT pricingType=PAID / price>0 is rejected" 0 "HTTP $PAID_UPDATE_STATUS"
fi

echo "--- TC-ADMIN-EXAM-9: GET /admin/exams?status=DRAFT includes smoke exam ---"
DRAFT_LIST_RESPONSE=$(request_json "GET" "$BASE_URL/admin/exams?status=DRAFT&size=100" "$ADMIN_TOKEN")
DRAFT_LIST_BODY=$(body_from_response "$DRAFT_LIST_RESPONSE")
DRAFT_LIST_STATUS=$(status_from_response "$DRAFT_LIST_RESPONSE")
if [ "$DRAFT_LIST_STATUS" = "200" ] && body_contains "$DRAFT_LIST_BODY" "\"examCode\":\"$EXAM_CODE\""; then
    record_result "TC-ADMIN-EXAM-9" "GET /admin/exams?status=DRAFT includes created examCode" 1
else
    record_result "TC-ADMIN-EXAM-9" "GET /admin/exams?status=DRAFT includes created examCode" 0 "HTTP $DRAFT_LIST_STATUS"
fi

echo "--- TC-ADMIN-EXAM-10: Public /exams does not include DRAFT smoke exam ---"
PUBLIC_LIST_RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/exams?size=100" -H "Accept: application/json")
PUBLIC_LIST_BODY=$(body_from_response "$PUBLIC_LIST_RESPONSE")
PUBLIC_LIST_STATUS=$(status_from_response "$PUBLIC_LIST_RESPONSE")
if [ "$PUBLIC_LIST_STATUS" = "200" ] && ! body_contains "$PUBLIC_LIST_BODY" "\"examCode\":\"$EXAM_CODE\""; then
    record_result "TC-ADMIN-EXAM-10" "Public /exams does not include DRAFT admin smoke exam" 1
else
    record_result "TC-ADMIN-EXAM-10" "Public /exams does not include DRAFT admin smoke exam" 0 "HTTP $PUBLIC_LIST_STATUS"
fi

echo "--- TC-ADMIN-EXAM-11: DELETE /admin/exams/{id} discards DRAFT smoke exam ---"
DISCARD_RESPONSE=$(request_json "DELETE" "$BASE_URL/admin/exams/$CREATED_ID" "$ADMIN_TOKEN")
DISCARD_BODY=$(body_from_response "$DISCARD_RESPONSE")
DISCARD_STATUS=$(status_from_response "$DISCARD_RESPONSE")
if [ "$DISCARD_STATUS" = "200" ] && body_contains "$DISCARD_BODY" "Draft exam discarded"; then
    record_result "TC-ADMIN-EXAM-11" "DELETE /admin/exams/{id} discards the DRAFT smoke exam" 1
else
    record_result "TC-ADMIN-EXAM-11" "DELETE /admin/exams/{id} discards the DRAFT smoke exam" 0 "HTTP $DISCARD_STATUS; body=$DISCARD_BODY"
fi

echo "--- TC-ADMIN-EXAM-12: GET discarded exam returns 404 ---"
DISCARDED_GET_RESPONSE=$(request_json "GET" "$BASE_URL/admin/exams/$CREATED_ID" "$ADMIN_TOKEN")
DISCARDED_GET_BODY=$(body_from_response "$DISCARDED_GET_RESPONSE")
DISCARDED_GET_STATUS=$(status_from_response "$DISCARDED_GET_RESPONSE")
if [ "$DISCARDED_GET_STATUS" = "404" ] && body_contains "$DISCARDED_GET_BODY" "RESOURCE_NOT_FOUND"; then
    record_result "TC-ADMIN-EXAM-12" "GET discarded DRAFT smoke exam returns 404 RESOURCE_NOT_FOUND" 1
else
    record_result "TC-ADMIN-EXAM-12" "GET discarded DRAFT smoke exam returns 404 RESOURCE_NOT_FOUND" 0 "HTTP $DISCARDED_GET_STATUS; body=$DISCARDED_GET_BODY"
fi

echo ""
echo "========================================"
echo "Smoke (admin exams): $PASS/$TOTAL PASS"
echo "Note: created DRAFT smoke exam is discarded by this smoke script."
echo "========================================"

if [ "$FAIL" -gt 0 ]; then
    exit 1
fi
exit 0
