#!/bin/bash
# ============================================================
# V-SAT Compass — Session Smoke Tests
# Runs 5 test cases against the Session endpoints.
# Usage: BASE_URL=https://your-api.com/api/v1 bash smoke_sessions.sh
# Compatible with bash 3.2+ (macOS default)
# ============================================================

BASE_URL="${BASE_URL:-https://vsat-compass-api.onrender.com/api/v1}"
PASS=0
FAIL=0
TOTAL=0

# Test account
TEST_EMAIL="student@vsat.com"
TEST_PASSWORD="Student@123"

echo "========================================"
echo "V-SAT Compass — Session Smoke Tests"
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
        echo "  [PASS] TC-SESSION-$TOTAL: $test_name (HTTP $actual_status)"
        PASS=$((PASS + 1))
    else
        echo "  [FAIL] TC-SESSION-$TOTAL: $test_name (expected $expected_status, got $actual_status)"
        FAIL=$((FAIL + 1))
    fi
}

# -----------------------------------------------------------
# Step 0: Login to get access token
# -----------------------------------------------------------
echo "--- Logging in as $TEST_EMAIL ---"
LOGIN_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"email\":\"$TEST_EMAIL\",\"password\":\"$TEST_PASSWORD\"}")
LOGIN_BODY=$(echo "$LOGIN_RESPONSE" | sed '$d')
LOGIN_STATUS=$(echo "$LOGIN_RESPONSE" | tail -1)

if [ "$LOGIN_STATUS" != "200" ]; then
    echo "  [ERROR] Login failed (HTTP $LOGIN_STATUS). Cannot proceed with session tests."
    exit 1
fi

ACCESS_TOKEN=$(echo "$LOGIN_BODY" | grep -o '"accessToken":"[^"]*"' | head -1 | cut -d'"' -f4)
echo "  Login OK. Token obtained."
echo ""

# -----------------------------------------------------------
# TC-SESSION-1: Start session (authenticated)
# -----------------------------------------------------------
echo "--- TC-SESSION-1: POST /sessions/start (authenticated) ---"
START_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/sessions/start" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $ACCESS_TOKEN" \
    -d "{\"examId\":1,\"mode\":\"MOCK_EXAM\",\"totalQuestions\":30}")
START_BODY=$(echo "$START_RESPONSE" | sed '$d')
START_STATUS=$(echo "$START_RESPONSE" | tail -1)
check_status "Start session (authenticated)" "201" "$START_STATUS"

# Extract session ID
SESSION_ID=$(echo "$START_BODY" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
echo "  Session ID: $SESSION_ID"

# -----------------------------------------------------------
# TC-SESSION-2: Start session (no Bearer)
# -----------------------------------------------------------
echo "--- TC-SESSION-2: POST /sessions/start (no Bearer) ---"
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/sessions/start" \
    -H "Content-Type: application/json" \
    -d "{\"examId\":1,\"mode\":\"MOCK_EXAM\",\"totalQuestions\":30}")
check_status "Start session (no Bearer)" "401" "$STATUS"

# -----------------------------------------------------------
# TC-SESSION-3: Client-submit (valid, first submit)
# -----------------------------------------------------------
echo "--- TC-SESSION-3: POST /sessions/$SESSION_ID/client-submit (valid) ---"
if [ -n "$SESSION_ID" ]; then
    SUBMIT_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/sessions/$SESSION_ID/client-submit" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $ACCESS_TOKEN" \
        -d "{\"score\":73.33,\"correctCount\":22,\"totalQuestions\":30,\"timeSpentSeconds\":2700}")
    SUBMIT_BODY=$(echo "$SUBMIT_RESPONSE" | sed '$d')
    SUBMIT_STATUS=$(echo "$SUBMIT_RESPONSE" | tail -1)
    check_status "Client-submit (valid)" "200" "$SUBMIT_STATUS"

    # Verify status is SUBMITTED in response
    SUBMITTED_STATUS=$(echo "$SUBMIT_BODY" | grep -o '"status":"[^"]*"' | head -1 | cut -d'"' -f4)
    if [ "$SUBMITTED_STATUS" = "SUBMITTED" ]; then
        echo "    -> status=SUBMITTED ✓"
    else
        echo "    -> status=$SUBMITTED_STATUS (expected SUBMITTED) ✗"
    fi
else
    echo "  [SKIP] No session ID from TC-SESSION-1"
    TOTAL=$((TOTAL + 1))
    FAIL=$((FAIL + 1))
fi

# -----------------------------------------------------------
# TC-SESSION-4: Client-submit replay (409 expected)
# -----------------------------------------------------------
echo "--- TC-SESSION-4: POST /sessions/$SESSION_ID/client-submit (replay → 409) ---"
if [ -n "$SESSION_ID" ]; then
    STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/sessions/$SESSION_ID/client-submit" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $ACCESS_TOKEN" \
        -d "{\"score\":73.33,\"correctCount\":22,\"totalQuestions\":30,\"timeSpentSeconds\":2700}")
    check_status "Client-submit replay (anti-replay)" "409" "$STATUS"
else
    echo "  [SKIP] No session ID from TC-SESSION-1"
    TOTAL=$((TOTAL + 1))
    FAIL=$((FAIL + 1))
fi

# -----------------------------------------------------------
# TC-SESSION-5: Client-submit wrong session owner (403)
# -----------------------------------------------------------
echo "--- TC-SESSION-5: Client-submit (wrong owner → 403) ---"
# Login as a different user
ALT_LOGIN_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"email\":\"collab@vsat.com\",\"password\":\"Admin@123\"}")
ALT_LOGIN_BODY=$(echo "$ALT_LOGIN_RESPONSE" | sed '$d')
ALT_LOGIN_STATUS=$(echo "$ALT_LOGIN_RESPONSE" | tail -1)
ALT_TOKEN=$(echo "$ALT_LOGIN_BODY" | grep -o '"accessToken":"[^"]*"' | head -1 | cut -d'"' -f4)

if [ -n "$ALT_TOKEN" ] && [ -n "$SESSION_ID" ]; then
    # Create a new session as student first, then try to submit as collab
    NEW_START=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/sessions/start" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $ACCESS_TOKEN" \
        -d "{\"examId\":1,\"mode\":\"PRACTICE\",\"totalQuestions\":10}")
    NEW_BODY=$(echo "$NEW_START" | sed '$d')
    NEW_SESSION_ID=$(echo "$NEW_BODY" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)

    if [ -n "$NEW_SESSION_ID" ]; then
        STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/sessions/$NEW_SESSION_ID/client-submit" \
            -H "Content-Type: application/json" \
            -H "Authorization: Bearer $ALT_TOKEN" \
            -d "{\"score\":50.0,\"correctCount\":5,\"totalQuestions\":10,\"timeSpentSeconds\":300}")
        check_status "Client-submit wrong owner" "403" "$STATUS"
    else
        echo "  [SKIP] Could not create session for owner test"
        TOTAL=$((TOTAL + 1))
        FAIL=$((FAIL + 1))
    fi
else
    echo "  [SKIP] Could not login as alternate user or no session"
    TOTAL=$((TOTAL + 1))
    FAIL=$((FAIL + 1))
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
