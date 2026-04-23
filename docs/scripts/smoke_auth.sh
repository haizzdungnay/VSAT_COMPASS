#!/bin/bash
# ============================================================
# V-SAT Compass — Auth Smoke Tests
# Runs 9 test cases against the Auth endpoints.
# Usage: BASE_URL=https://your-api.com/api/v1 bash smoke_auth.sh
# Compatible with bash 3.2+ (macOS default)
# ============================================================

BASE_URL="${BASE_URL:-https://vsat-compass-api.onrender.com/api/v1}"
PASS=0
FAIL=0
TOTAL=0

# Test account credentials
TEST_EMAIL="student@vsat.com"
TEST_PASSWORD="Student@123"
REGISTER_EMAIL="smoketest_$(date +%s)@vsat.com"
REGISTER_PASSWORD="SmokeTest@123"

echo "========================================"
echo "V-SAT Compass — Auth Smoke Tests"
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
        echo "  [PASS] TC-AUTH-$TOTAL: $test_name (HTTP $actual_status)"
        PASS=$((PASS + 1))
    else
        echo "  [FAIL] TC-AUTH-$TOTAL: $test_name (expected $expected_status, got $actual_status)"
        FAIL=$((FAIL + 1))
    fi
}

# -----------------------------------------------------------
# TC-AUTH-1: Login with valid credentials
# -----------------------------------------------------------
echo "--- TC-AUTH-1: Login (valid credentials) ---"
LOGIN_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"email\":\"$TEST_EMAIL\",\"password\":\"$TEST_PASSWORD\"}")
LOGIN_BODY=$(echo "$LOGIN_RESPONSE" | sed '$d')
LOGIN_STATUS=$(echo "$LOGIN_RESPONSE" | tail -1)
check_status "Login valid credentials" "200" "$LOGIN_STATUS"

# Extract tokens for subsequent tests
ACCESS_TOKEN=$(echo "$LOGIN_BODY" | grep -o '"accessToken":"[^"]*"' | head -1 | cut -d'"' -f4)
REFRESH_TOKEN=$(echo "$LOGIN_BODY" | grep -o '"refreshToken":"[^"]*"' | head -1 | cut -d'"' -f4)

if [ -z "$ACCESS_TOKEN" ]; then
    echo "  [WARN] Could not extract accessToken from login response. Remaining tests may fail."
fi

# -----------------------------------------------------------
# TC-AUTH-2: Login with wrong password
# -----------------------------------------------------------
echo "--- TC-AUTH-2: Login (wrong password) ---"
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/auth/login" \
    -H "Content-Type: application/json" \
    -d "{\"email\":\"$TEST_EMAIL\",\"password\":\"WrongPassword@999\"}")
check_status "Login wrong password" "401" "$STATUS"

# -----------------------------------------------------------
# TC-AUTH-3: GET /auth/me with valid Bearer
# -----------------------------------------------------------
echo "--- TC-AUTH-3: GET /auth/me (valid Bearer) ---"
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/auth/me" \
    -H "Authorization: Bearer $ACCESS_TOKEN")
check_status "GET /auth/me with Bearer" "200" "$STATUS"

# -----------------------------------------------------------
# TC-AUTH-4: GET /auth/me without Bearer
# -----------------------------------------------------------
echo "--- TC-AUTH-4: GET /auth/me (no Bearer) ---"
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X GET "$BASE_URL/auth/me")
check_status "GET /auth/me without Bearer" "401" "$STATUS"

# -----------------------------------------------------------
# TC-AUTH-5: Refresh token (valid)
# -----------------------------------------------------------
echo "--- TC-AUTH-5: Refresh token (valid) ---"
REFRESH_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/auth/refresh" \
    -H "Content-Type: application/json" \
    -d "{\"refreshToken\":\"$REFRESH_TOKEN\"}")
REFRESH_BODY=$(echo "$REFRESH_RESPONSE" | sed '$d')
REFRESH_STATUS=$(echo "$REFRESH_RESPONSE" | tail -1)
check_status "Refresh valid token" "200" "$REFRESH_STATUS"

# Extract new tokens
NEW_ACCESS_TOKEN=$(echo "$REFRESH_BODY" | grep -o '"accessToken":"[^"]*"' | head -1 | cut -d'"' -f4)
NEW_REFRESH_TOKEN=$(echo "$REFRESH_BODY" | grep -o '"refreshToken":"[^"]*"' | head -1 | cut -d'"' -f4)
if [ -n "$NEW_ACCESS_TOKEN" ]; then
    ACCESS_TOKEN="$NEW_ACCESS_TOKEN"
fi

# -----------------------------------------------------------
# TC-AUTH-6: Refresh token (invalid/expired)
# -----------------------------------------------------------
echo "--- TC-AUTH-6: Refresh token (invalid) ---"
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/auth/refresh" \
    -H "Content-Type: application/json" \
    -d "{\"refreshToken\":\"invalid.token.here\"}")
check_status "Refresh invalid token" "401" "$STATUS"

# -----------------------------------------------------------
# TC-AUTH-7: Register new account
# -----------------------------------------------------------
echo "--- TC-AUTH-7: Register (new email) ---"
REG_RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "$BASE_URL/auth/register" \
    -H "Content-Type: application/json" \
    -d "{\"email\":\"$REGISTER_EMAIL\",\"password\":\"$REGISTER_PASSWORD\",\"fullName\":\"Smoke Tester\"}")
REG_BODY=$(echo "$REG_RESPONSE" | sed '$d')
REG_STATUS=$(echo "$REG_RESPONSE" | tail -1)
check_status "Register new email" "201" "$REG_STATUS"

# -----------------------------------------------------------
# TC-AUTH-8: Register duplicate email
# -----------------------------------------------------------
echo "--- TC-AUTH-8: Register (duplicate email) ---"
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/auth/register" \
    -H "Content-Type: application/json" \
    -d "{\"email\":\"$REGISTER_EMAIL\",\"password\":\"$REGISTER_PASSWORD\",\"fullName\":\"Smoke Tester\"}")
check_status "Register duplicate email" "409" "$STATUS"

# -----------------------------------------------------------
# TC-AUTH-9: Logout
# -----------------------------------------------------------
echo "--- TC-AUTH-9: Logout ---"
# Use the refresh token from the latest login/refresh
LOGOUT_TOKEN="$REFRESH_TOKEN"
if [ -n "$NEW_REFRESH_TOKEN" ]; then
    LOGOUT_TOKEN="$NEW_REFRESH_TOKEN"
fi
STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST "$BASE_URL/auth/logout" \
    -H "Content-Type: application/json" \
    -H "Authorization: Bearer $ACCESS_TOKEN" \
    -d "{\"refreshToken\":\"$LOGOUT_TOKEN\"}")
check_status "Logout" "200" "$STATUS"

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
