#!/bin/bash
# ============================================================
# V-SAT Compass — Subjects Smoke Tests
# Runs 3 test cases against the Subject + Topic public endpoints
# (Phase C1.0 — read-only API).
# Usage: BASE_URL=https://your-api.com/api/v1 bash smoke_subjects.sh
# Compatible with bash 3.2+ (macOS default)
# ============================================================

BASE_URL="${BASE_URL:-https://vsat-compass-api.onrender.com/api/v1}"
PASS=0
FAIL=0
TOTAL=0

echo "========================================"
echo "V-SAT Compass — Subjects Smoke Tests"
echo "Base URL: $BASE_URL"
echo "Date: $(date -u '+%Y-%m-%dT%H:%M:%SZ')"
echo "========================================"
echo ""

check_status() {
    local test_name="$1"
    local expected_status="$2"
    local actual_status="$3"
    TOTAL=$((TOTAL + 1))
    if [ "$actual_status" = "$expected_status" ]; then
        echo "  [PASS] TC-SUBJ-$TOTAL: $test_name (HTTP $actual_status)"
        PASS=$((PASS + 1))
    else
        echo "  [FAIL] TC-SUBJ-$TOTAL: $test_name (expected $expected_status, got $actual_status)"
        FAIL=$((FAIL + 1))
    fi
}

# -----------------------------------------------------------
# TC-SUBJ-1: GET /subjects (public)
# -----------------------------------------------------------
echo "--- TC-SUBJ-1: GET /subjects (public) ---"
SUBJ_RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/subjects" \
    -H "Accept: application/json")
SUBJ_BODY=$(echo "$SUBJ_RESPONSE" | sed '$d')
SUBJ_STATUS=$(echo "$SUBJ_RESPONSE" | tail -1)
check_status "List subjects" "200" "$SUBJ_STATUS"

# Envelope + at least 1 entry
if [ "$SUBJ_STATUS" = "200" ]; then
    if echo "$SUBJ_BODY" | grep -q '"data"' && echo "$SUBJ_BODY" | grep -q '"id"'; then
        echo "    -> ApiResponse envelope with >=1 subject ✓"
    else
        echo "    [WARN] Response missing 'data' envelope or no 'id' entry — body: $SUBJ_BODY"
    fi
fi

# Capture first subject id for TC-SUBJ-2 (fallback to 1 if extraction fails)
FIRST_SUBJECT_ID=$(echo "$SUBJ_BODY" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
if [ -z "$FIRST_SUBJECT_ID" ]; then
    FIRST_SUBJECT_ID=1
    echo "    [WARN] Could not extract first subject id — falling back to 1"
fi

# -----------------------------------------------------------
# TC-SUBJ-2: GET /subjects/{id}/topics (valid id)
# -----------------------------------------------------------
echo "--- TC-SUBJ-2: GET /subjects/${FIRST_SUBJECT_ID}/topics (valid id) ---"
TOPICS_RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/subjects/${FIRST_SUBJECT_ID}/topics" \
    -H "Accept: application/json")
TOPICS_BODY=$(echo "$TOPICS_RESPONSE" | sed '$d')
TOPICS_STATUS=$(echo "$TOPICS_RESPONSE" | tail -1)
check_status "List topics for valid subject" "200" "$TOPICS_STATUS"

# Envelope check (empty array still PASS — topics may not be seeded)
if [ "$TOPICS_STATUS" = "200" ]; then
    if echo "$TOPICS_BODY" | grep -q '"data"'; then
        echo "    -> ApiResponse envelope present ✓ (array length irrelevant for shape gate)"
    else
        echo "    [WARN] Response missing 'data' envelope — body: $TOPICS_BODY"
    fi
fi

# -----------------------------------------------------------
# TC-SUBJ-3: GET /subjects/9999/topics (404 RESOURCE_NOT_FOUND)
# -----------------------------------------------------------
echo "--- TC-SUBJ-3: GET /subjects/9999/topics (expect 404 RESOURCE_NOT_FOUND) ---"
NF_RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/subjects/9999/topics" \
    -H "Accept: application/json")
NF_BODY=$(echo "$NF_RESPONSE" | sed '$d')
NF_STATUS=$(echo "$NF_RESPONSE" | tail -1)
check_status "404 for missing subject" "404" "$NF_STATUS"

# Verify error code in body
if [ "$NF_STATUS" = "404" ]; then
    if echo "$NF_BODY" | grep -q '"RESOURCE_NOT_FOUND"'; then
        echo "    -> error.code=RESOURCE_NOT_FOUND ✓"
    else
        echo "    [WARN] 404 returned but body missing RESOURCE_NOT_FOUND code — body: $NF_BODY"
    fi
fi

# -----------------------------------------------------------
# Summary
# -----------------------------------------------------------
echo ""
echo "========================================"
echo "Results: $PASS/$TOTAL passed, $FAIL failed"
echo "========================================"

if [ "$FAIL" -gt 0 ]; then
    exit 1
fi
exit 0
