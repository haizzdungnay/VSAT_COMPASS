#!/bin/bash
# ============================================================
# V-SAT Compass — Session Smoke Tests
# Runs 12 test cases against the Session endpoints.
# Usage: BASE_URL=https://your-api.com/api/v1 bash smoke_sessions.sh
# Compatible with bash 3.2+ (macOS default)
# ============================================================

BASE_URL="${BASE_URL:-https://vsat-compass-api.onrender.com/api/v1}"
# EXAM_ID: ID of a published exam in the DB. Override via env if exam table has a different ID.
# Run docs/seed/smoke_test_seed.sql in Neon Console once to seed exam SMOKE_001.
EXAM_ID="${EXAM_ID:-1}"
# QUESTION_ID: ID of a question that belongs to EXAM_ID through exam_questions.
QUESTION_ID="${QUESTION_ID:-1}"
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
    -d "{\"examId\":$EXAM_ID,\"mode\":\"MOCK_EXAM\",\"totalQuestions\":30}")
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
    -d "{\"examId\":$EXAM_ID,\"mode\":\"MOCK_EXAM\",\"totalQuestions\":30}")
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
        -d "{\"examId\":$EXAM_ID,\"mode\":\"PRACTICE\",\"totalQuestions\":10}")
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
# TC-SESSION-6: Client-submit unknown sessionId (404 RESOURCE_NOT_FOUND)
# -----------------------------------------------------------
echo "--- TC-SESSION-6: POST /sessions/999999999/client-submit (unknown sessionId → 404) ---"
TC6_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/sessions/999999999/client-submit" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $ACCESS_TOKEN" \
    -d "{\"score\":50.0,\"correctCount\":5,\"totalQuestions\":10,\"timeSpentSeconds\":300}")
TC6_BODY=$(echo "$TC6_RESPONSE" | sed '$d')
TC6_STATUS=$(echo "$TC6_RESPONSE" | tail -1)
check_status "Client-submit unknown sessionId (404)" "404" "$TC6_STATUS"

TC6_CODE=$(echo "$TC6_BODY" | grep -o '"code":"[^"]*"' | head -1 | cut -d'"' -f4)
if [ "$TC6_CODE" = "RESOURCE_NOT_FOUND" ]; then
    echo "    -> error.code=RESOURCE_NOT_FOUND ✓"
else
    echo "    -> error.code=$TC6_CODE (expected RESOURCE_NOT_FOUND) ✗"
    FAIL=$((FAIL + 1))
fi

# -----------------------------------------------------------
# TC-SESSION-7: Client-submit invalid payload (correctCount > totalQuestions → 400 VALIDATION_FAILED)
# -----------------------------------------------------------
echo "--- TC-SESSION-7: POST /sessions/{id}/client-submit (correctCount > totalQuestions → 400) ---"
# Start a fresh session so it is IN_PROGRESS
TC7_START=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/sessions/start" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $ACCESS_TOKEN" \
    -d "{\"examId\":$EXAM_ID,\"mode\":\"PRACTICE\",\"totalQuestions\":10}")
TC7_START_BODY=$(echo "$TC7_START" | sed '$d')
TC7_SESSION_ID=$(echo "$TC7_START_BODY" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)

if [ -n "$TC7_SESSION_ID" ]; then
    TC7_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/sessions/$TC7_SESSION_ID/client-submit" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $ACCESS_TOKEN" \
        -d "{\"score\":50.0,\"correctCount\":50,\"totalQuestions\":10,\"timeSpentSeconds\":300}")
    TC7_BODY=$(echo "$TC7_RESPONSE" | sed '$d')
    TC7_STATUS=$(echo "$TC7_RESPONSE" | tail -1)
    check_status "Client-submit invalid payload (correctCount > totalQuestions)" "400" "$TC7_STATUS"

    TC7_CODE=$(echo "$TC7_BODY" | grep -o '"code":"[^"]*"' | head -1 | cut -d'"' -f4)
    if [ "$TC7_CODE" = "VALIDATION_FAILED" ]; then
        echo "    -> error.code=VALIDATION_FAILED ✓"
    else
        echo "    -> error.code=$TC7_CODE (expected VALIDATION_FAILED) ✗"
        FAIL=$((FAIL + 1))
    fi
else
    echo "  [SKIP] Could not start fresh session for TC-SESSION-7"
    TOTAL=$((TOTAL + 1))
    FAIL=$((FAIL + 1))
fi

# -----------------------------------------------------------
# TC-SESSION-8: Get in-session question content (200, no answer-key leak)
# -----------------------------------------------------------
echo "--- TC-SESSION-8: GET /sessions/{id}/questions/$QUESTION_ID (IN_PROGRESS -> 200, no answer keys) ---"
TC8_START=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/sessions/start" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $ACCESS_TOKEN" \
    -d "{\"examId\":$EXAM_ID,\"mode\":\"PRACTICE\",\"totalQuestions\":10}")
TC8_START_BODY=$(echo "$TC8_START" | sed '$d')
TC8_SESSION_ID=$(echo "$TC8_START_BODY" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)

if [ -n "$TC8_SESSION_ID" ]; then
    TC8_RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/sessions/$TC8_SESSION_ID/questions/$QUESTION_ID" \
        -H "Authorization: Bearer $ACCESS_TOKEN")
    TC8_BODY=$(echo "$TC8_RESPONSE" | sed '$d')
    TC8_STATUS=$(echo "$TC8_RESPONSE" | tail -1)
    check_status "GET question during IN_PROGRESS" "200" "$TC8_STATUS"

    if echo "$TC8_BODY" | grep -q '"options":' && echo "$TC8_BODY" | grep -q '"content":"'; then
        echo "    -> options[].content present âœ“"
    else
        echo "    -> options[].content missing âœ—"
        FAIL=$((FAIL + 1))
    fi

    if echo "$TC8_BODY" | grep -q '"isCorrect"'; then
        echo "    -> isCorrect leaked âœ—"
        FAIL=$((FAIL + 1))
    else
        echo "    -> isCorrect absent âœ“"
    fi
else
    echo "  [SKIP] Could not start fresh session for TC-SESSION-8"
    TOTAL=$((TOTAL + 1))
    FAIL=$((FAIL + 1))
fi

# -----------------------------------------------------------
# TC-SESSION-9: Get in-session question with non-owner Bearer (403)
# -----------------------------------------------------------
echo "--- TC-SESSION-9: GET /sessions/$TC8_SESSION_ID/questions/$QUESTION_ID (wrong owner -> 403) ---"
if [ -n "$ALT_TOKEN" ] && [ -n "$TC8_SESSION_ID" ]; then
    STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/sessions/$TC8_SESSION_ID/questions/$QUESTION_ID" \
        -H "Authorization: Bearer $ALT_TOKEN")
    check_status "GET question wrong owner" "403" "$STATUS"
else
    echo "  [SKIP] Missing alternate token or session for TC-SESSION-9"
    TOTAL=$((TOTAL + 1))
    FAIL=$((FAIL + 1))
fi

# -----------------------------------------------------------
# TC-SESSION-10: Get answer keys for SUBMITTED session (200)
# -----------------------------------------------------------
echo "--- TC-SESSION-10: GET /sessions/{id}/answer-keys (SUBMITTED -> 200) ---"
TC10_START=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/sessions/start" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $ACCESS_TOKEN" \
    -d "{\"examId\":$EXAM_ID,\"mode\":\"PRACTICE\",\"totalQuestions\":10}")
TC10_START_BODY=$(echo "$TC10_START" | sed '$d')
TC10_SESSION_ID=$(echo "$TC10_START_BODY" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)

if [ -n "$TC10_SESSION_ID" ]; then
    curl -s -o /dev/null -X POST "$BASE_URL/sessions/$TC10_SESSION_ID/client-submit" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer $ACCESS_TOKEN" \
        -d "{\"score\":80.0,\"correctCount\":8,\"totalQuestions\":10,\"timeSpentSeconds\":600}"
    TC10_RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/sessions/$TC10_SESSION_ID/answer-keys" \
        -H "Authorization: Bearer $ACCESS_TOKEN")
    TC10_BODY=$(echo "$TC10_RESPONSE" | sed '$d')
    TC10_STATUS=$(echo "$TC10_RESPONSE" | tail -1)
    check_status "GET answer keys for SUBMITTED session" "200" "$TC10_STATUS"

    if echo "$TC10_BODY" | grep -q '"correctOptionIds"'; then
        echo "    -> questions[].correctOptionIds present âœ“"
    else
        echo "    -> questions[].correctOptionIds missing âœ—"
        FAIL=$((FAIL + 1))
    fi
else
    echo "  [SKIP] Could not start fresh session for TC-SESSION-10"
    TOTAL=$((TOTAL + 1))
    FAIL=$((FAIL + 1))
fi

# -----------------------------------------------------------
# TC-SESSION-11: Get answer keys for IN_PROGRESS session (400 BAD_REQUEST)
# -----------------------------------------------------------
echo "--- TC-SESSION-11: GET /sessions/{id}/answer-keys (IN_PROGRESS -> 400) ---"
TC11_START=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/sessions/start" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $ACCESS_TOKEN" \
    -d "{\"examId\":$EXAM_ID,\"mode\":\"PRACTICE\",\"totalQuestions\":10}")
TC11_START_BODY=$(echo "$TC11_START" | sed '$d')
TC11_SESSION_ID=$(echo "$TC11_START_BODY" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)

if [ -n "$TC11_SESSION_ID" ]; then
    TC11_RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/sessions/$TC11_SESSION_ID/answer-keys" \
        -H "Authorization: Bearer $ACCESS_TOKEN")
    TC11_BODY=$(echo "$TC11_RESPONSE" | sed '$d')
    TC11_STATUS=$(echo "$TC11_RESPONSE" | tail -1)
    check_status "GET answer keys for IN_PROGRESS session" "400" "$TC11_STATUS"

    TC11_CODE=$(echo "$TC11_BODY" | grep -o '"code":"[^"]*"' | head -1 | cut -d'"' -f4)
    if [ "$TC11_CODE" = "BAD_REQUEST" ]; then
        echo "    -> error.code=BAD_REQUEST âœ“"
    else
        echo "    -> error.code=$TC11_CODE (expected BAD_REQUEST) âœ—"
        FAIL=$((FAIL + 1))
    fi
else
    echo "  [SKIP] Could not start fresh session for TC-SESSION-11"
    TOTAL=$((TOTAL + 1))
    FAIL=$((FAIL + 1))
fi

# -----------------------------------------------------------
# TC-SESSION-12: Get answer keys with non-owner Bearer (403)
# -----------------------------------------------------------
echo "--- TC-SESSION-12: GET /sessions/$TC10_SESSION_ID/answer-keys (wrong owner -> 403) ---"
if [ -n "$ALT_TOKEN" ] && [ -n "$TC10_SESSION_ID" ]; then
    STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/sessions/$TC10_SESSION_ID/answer-keys" \
        -H "Authorization: Bearer $ALT_TOKEN")
    check_status "GET answer keys wrong owner" "403" "$STATUS"
else
    echo "  [SKIP] Missing alternate token or session for TC-SESSION-12"
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
