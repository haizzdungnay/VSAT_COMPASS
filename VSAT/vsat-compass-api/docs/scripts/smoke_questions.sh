#!/bin/bash
# ============================================================
# V-SAT Compass - Question Bank Smoke Tests
# Runs collaborator/admin Question CRUD + review workflow checks.
# Usage:
#   SMOKE_QBANK_PASSWORD=... bash smoke_questions.sh
#   BASE_URL=https://your-api.com/api/v1 SMOKE_QBANK_PASSWORD=... bash smoke_questions.sh
# Compatible with bash 3.2+ (macOS default)
# ============================================================

BASE_URL="${BASE_URL:-https://vsat-compass-api.onrender.com/api/v1}"
SMOKE_COLLAB1_EMAIL="${SMOKE_COLLAB1_EMAIL:-collab1@vsat.com}"
SMOKE_COLLAB2_EMAIL="${SMOKE_COLLAB2_EMAIL:-collab2@vsat.com}"
SMOKE_ADMIN_EMAIL="${SMOKE_ADMIN_EMAIL:-admin@vsat.com}"

if [ -z "$SMOKE_COLLAB1_PASSWORD" ]; then
    SMOKE_COLLAB1_PASSWORD="$SMOKE_QBANK_PASSWORD"
fi
if [ -z "$SMOKE_COLLAB2_PASSWORD" ]; then
    SMOKE_COLLAB2_PASSWORD="$SMOKE_QBANK_PASSWORD"
fi
if [ -z "$SMOKE_ADMIN_PASSWORD" ]; then
    SMOKE_ADMIN_PASSWORD="$SMOKE_QBANK_PASSWORD"
fi

if [ -z "$SMOKE_COLLAB1_PASSWORD" ] || [ -z "$SMOKE_COLLAB2_PASSWORD" ] || [ -z "$SMOKE_ADMIN_PASSWORD" ]; then
    echo "ERROR: Question smoke passwords are required."
    echo "Set SMOKE_QBANK_PASSWORD, or set SMOKE_COLLAB1_PASSWORD, SMOKE_COLLAB2_PASSWORD, and SMOKE_ADMIN_PASSWORD."
    exit 1
fi

SMOKE_MARKER="smoke-c11b-$(date +%Y%m%d%H%M%S)"
PASS=0
FAIL=0
TOTAL=0

echo "========================================"
echo "V-SAT Compass - Question Bank Smoke Tests"
echo "Base URL: $BASE_URL"
echo "Marker: $SMOKE_MARKER"
echo "Date: $(date -u '+%Y-%m-%dT%H:%M:%SZ')"
echo "========================================"
echo ""

check_status() {
    local test_name="$1"
    local expected_status="$2"
    local actual_status="$3"
    TOTAL=$((TOTAL + 1))
    if [ "$actual_status" = "$expected_status" ]; then
        echo "  [PASS] TC-Q-$TOTAL: $test_name (HTTP $actual_status)"
        PASS=$((PASS + 1))
    else
        echo "  [FAIL] TC-Q-$TOTAL: $test_name (expected $expected_status, got $actual_status)"
        FAIL=$((FAIL + 1))
    fi
}

check_body_contains() {
    local test_name="$1"
    local body="$2"
    local expected="$3"
    TOTAL=$((TOTAL + 1))
    if echo "$body" | grep -q "$expected"; then
        echo "  [PASS] TC-Q-$TOTAL: $test_name"
        PASS=$((PASS + 1))
    else
        echo "  [FAIL] TC-Q-$TOTAL: $test_name (missing '$expected')"
        FAIL=$((FAIL + 1))
    fi
}

extract_token() {
    echo "$1" | grep -o '"accessToken":"[^"]*"' | head -1 | cut -d'"' -f4
}

extract_first_id() {
    echo "$1" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2
}

extract_question_id() {
    echo "$1" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2
}

login_user() {
    local email="$1"
    local password="$2"
    curl -s -w "\n%{http_code}" -X POST "$BASE_URL/auth/login" \
        -H "Content-Type: application/json" \
        -d "{\"email\":\"$email\",\"password\":\"$password\"}"
}

request_json() {
    local method="$1"
    local url="$2"
    local token="$3"
    local body="$4"
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

# -----------------------------------------------------------
# Q-001 / Q-002 / Q-003: Login required roles
# -----------------------------------------------------------
echo "--- Q-001: Login collaborator 1 ---"
LOGIN1_RESPONSE=$(login_user "$SMOKE_COLLAB1_EMAIL" "$SMOKE_COLLAB1_PASSWORD")
LOGIN1_BODY=$(echo "$LOGIN1_RESPONSE" | sed '$d')
LOGIN1_STATUS=$(echo "$LOGIN1_RESPONSE" | tail -1)
check_status "Login collaborator 1" "200" "$LOGIN1_STATUS"
COLLAB1_TOKEN=$(extract_token "$LOGIN1_BODY")
check_body_contains "Collaborator 1 accessToken present" "$LOGIN1_BODY" '"accessToken"'

echo "--- Q-002: Login collaborator 2 ---"
LOGIN2_RESPONSE=$(login_user "$SMOKE_COLLAB2_EMAIL" "$SMOKE_COLLAB2_PASSWORD")
LOGIN2_BODY=$(echo "$LOGIN2_RESPONSE" | sed '$d')
LOGIN2_STATUS=$(echo "$LOGIN2_RESPONSE" | tail -1)
check_status "Login collaborator 2" "200" "$LOGIN2_STATUS"
COLLAB2_TOKEN=$(extract_token "$LOGIN2_BODY")
check_body_contains "Collaborator 2 accessToken present" "$LOGIN2_BODY" '"accessToken"'

echo "--- Q-003: Login content admin ---"
ADMIN_LOGIN_RESPONSE=$(login_user "$SMOKE_ADMIN_EMAIL" "$SMOKE_ADMIN_PASSWORD")
ADMIN_LOGIN_BODY=$(echo "$ADMIN_LOGIN_RESPONSE" | sed '$d')
ADMIN_LOGIN_STATUS=$(echo "$ADMIN_LOGIN_RESPONSE" | tail -1)
check_status "Login content admin" "200" "$ADMIN_LOGIN_STATUS"
ADMIN_TOKEN=$(extract_token "$ADMIN_LOGIN_BODY")
check_body_contains "Content admin accessToken present" "$ADMIN_LOGIN_BODY" '"accessToken"'

if [ -z "$COLLAB1_TOKEN" ] || [ -z "$COLLAB2_TOKEN" ] || [ -z "$ADMIN_TOKEN" ]; then
    echo "  [ERROR] Missing one or more role tokens. Cannot continue question workflow smoke."
    echo ""
    echo "========================================"
    echo "Results: $PASS/$TOTAL passed, $FAIL failed"
    echo "========================================"
    exit 1
fi

# -----------------------------------------------------------
# Q-004: Discover subject/topic/subtopic IDs
# -----------------------------------------------------------
echo "--- Q-004: Discover subject/topic/subtopic ---"
SUBJECTS_RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/subjects" -H "Accept: application/json")
SUBJECTS_BODY=$(echo "$SUBJECTS_RESPONSE" | sed '$d')
SUBJECTS_STATUS=$(echo "$SUBJECTS_RESPONSE" | tail -1)
check_status "GET /subjects" "200" "$SUBJECTS_STATUS"

SUBJECT_ID=""
TOPIC_ID=""
TOPICS_STATUS=""
TOPICS_BODY=""
SUBJECT_IDS=$(echo "$SUBJECTS_BODY" | grep -o '"id":[0-9]*' | cut -d':' -f2)

for CANDIDATE_SUBJECT_ID in $SUBJECT_IDS; do
    CANDIDATE_TOPICS_RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/subjects/$CANDIDATE_SUBJECT_ID/topics" -H "Accept: application/json")
    CANDIDATE_TOPICS_BODY=$(echo "$CANDIDATE_TOPICS_RESPONSE" | sed '$d')
    CANDIDATE_TOPICS_STATUS=$(echo "$CANDIDATE_TOPICS_RESPONSE" | tail -1)
    CANDIDATE_TOPIC_ID=$(extract_first_id "$CANDIDATE_TOPICS_BODY")
    if [ "$CANDIDATE_TOPICS_STATUS" = "200" ] && [ -n "$CANDIDATE_TOPIC_ID" ]; then
        SUBJECT_ID="$CANDIDATE_SUBJECT_ID"
        TOPIC_ID="$CANDIDATE_TOPIC_ID"
        TOPICS_STATUS="$CANDIDATE_TOPICS_STATUS"
        TOPICS_BODY="$CANDIDATE_TOPICS_BODY"
        break
    fi
done

if [ -z "$SUBJECT_ID" ]; then
    FIRST_SUBJECT_ID=$(echo "$SUBJECT_IDS" | head -1)
    TOPICS_RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/subjects/$FIRST_SUBJECT_ID/topics" -H "Accept: application/json")
    TOPICS_BODY=$(echo "$TOPICS_RESPONSE" | sed '$d')
    TOPICS_STATUS=$(echo "$TOPICS_RESPONSE" | tail -1)
    check_status "GET /subjects/{id}/topics" "200" "$TOPICS_STATUS"
else
    check_status "GET /subjects/{id}/topics" "200" "$TOPICS_STATUS"
fi

SUBTOPIC_ID=""
if [ -n "$SUBJECT_ID" ] && [ -n "$TOPIC_ID" ]; then
    SUBTOPICS_RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "$BASE_URL/subjects/$SUBJECT_ID/topics/$TOPIC_ID/subtopics" -H "Accept: application/json")
    SUBTOPICS_BODY=$(echo "$SUBTOPICS_RESPONSE" | sed '$d')
    SUBTOPICS_STATUS=$(echo "$SUBTOPICS_RESPONSE" | tail -1)
    check_status "GET /subjects/{id}/topics/{topicId}/subtopics" "200" "$SUBTOPICS_STATUS"
    SUBTOPIC_ID=$(extract_first_id "$SUBTOPICS_BODY")
else
    check_status "GET /subjects/{id}/topics/{topicId}/subtopics" "200" "000"
fi

if [ -z "$SUBJECT_ID" ] || [ -z "$TOPIC_ID" ]; then
    echo "  [ERROR] Could not discover required subject/topic IDs."
    FAIL=$((FAIL + 1))
    TOTAL=$((TOTAL + 1))
    echo ""
    echo "========================================"
    echo "Results: $PASS/$TOTAL passed, $FAIL failed"
    echo "========================================"
    exit 1
fi

echo "  Using subjectId=$SUBJECT_ID topicId=$TOPIC_ID subtopicId=${SUBTOPIC_ID:-<omitted>}"

# -----------------------------------------------------------
# Q-005: Collaborator creates draft question
# -----------------------------------------------------------
echo "--- Q-005: POST /collaborator/questions ---"
if [ -n "$SUBTOPIC_ID" ]; then
    SUBTOPIC_JSON="\"subtopicId\":$SUBTOPIC_ID,"
else
    SUBTOPIC_JSON=""
fi

CREATE_BODY="{\"subjectId\":$SUBJECT_ID,\"topicId\":$TOPIC_ID,$SUBTOPIC_JSON\"difficulty\":\"EASY\",\"questionType\":\"SINGLE_CHOICE\",\"questionText\":\"Smoke C1.1b $SMOKE_MARKER\",\"explanation\":\"Smoke test explanation\",\"source\":\"smoke-c11b\",\"tags\":\"smoke,c11b\",\"options\":[{\"optionLabel\":\"A\",\"optionText\":\"Option A\",\"isCorrect\":true,\"displayOrder\":1},{\"optionLabel\":\"B\",\"optionText\":\"Option B\",\"isCorrect\":false,\"displayOrder\":2},{\"optionLabel\":\"C\",\"optionText\":\"Option C\",\"isCorrect\":false,\"displayOrder\":3},{\"optionLabel\":\"D\",\"optionText\":\"Option D\",\"isCorrect\":false,\"displayOrder\":4}]}"
CREATE_RESPONSE=$(request_json "POST" "$BASE_URL/collaborator/questions" "$COLLAB1_TOKEN" "$CREATE_BODY")
CREATE_BODY_RESPONSE=$(echo "$CREATE_RESPONSE" | sed '$d')
CREATE_STATUS=$(echo "$CREATE_RESPONSE" | tail -1)
check_status "Collaborator creates draft question" "201" "$CREATE_STATUS"
check_body_contains "Created question status DRAFT" "$CREATE_BODY_RESPONSE" '"status":"DRAFT"'
check_body_contains "Created question has createdBy" "$CREATE_BODY_RESPONSE" '"createdBy"'
QUESTION_ID=$(extract_question_id "$CREATE_BODY_RESPONSE")
echo "  Question ID: $QUESTION_ID"

if [ -z "$QUESTION_ID" ]; then
    echo "  [ERROR] Could not extract created question id."
    FAIL=$((FAIL + 1))
    TOTAL=$((TOTAL + 1))
    echo ""
    echo "========================================"
    echo "Results: $PASS/$TOTAL passed, $FAIL failed"
    echo "========================================"
    exit 1
fi

# -----------------------------------------------------------
# Q-006: Collaborator lists own questions
# -----------------------------------------------------------
echo "--- Q-006: GET /collaborator/questions ---"
LIST_RESPONSE=$(request_json "GET" "$BASE_URL/collaborator/questions" "$COLLAB1_TOKEN" "")
LIST_BODY=$(echo "$LIST_RESPONSE" | sed '$d')
LIST_STATUS=$(echo "$LIST_RESPONSE" | tail -1)
check_status "Collaborator lists own questions" "200" "$LIST_STATUS"

# -----------------------------------------------------------
# Q-007: Collaborator gets own question detail
# -----------------------------------------------------------
echo "--- Q-007: GET /collaborator/questions/{id} ---"
DETAIL_RESPONSE=$(request_json "GET" "$BASE_URL/collaborator/questions/$QUESTION_ID" "$COLLAB1_TOKEN" "")
DETAIL_BODY=$(echo "$DETAIL_RESPONSE" | sed '$d')
DETAIL_STATUS=$(echo "$DETAIL_RESPONSE" | tail -1)
check_status "Collaborator gets own question detail" "200" "$DETAIL_STATUS"

# -----------------------------------------------------------
# Q-008: Other collaborator cannot update owner's draft
# -----------------------------------------------------------
echo "--- Q-008: PUT by non-owner collaborator (expect 403) ---"
OTHER_UPDATE_BODY="{\"questionText\":\"Smoke unauthorized edit $SMOKE_MARKER\"}"
OTHER_UPDATE_RESPONSE=$(request_json "PUT" "$BASE_URL/collaborator/questions/$QUESTION_ID" "$COLLAB2_TOKEN" "$OTHER_UPDATE_BODY")
OTHER_UPDATE_STATUS=$(echo "$OTHER_UPDATE_RESPONSE" | tail -1)
check_status "Other collaborator cannot update owner's draft" "403" "$OTHER_UPDATE_STATUS"

# -----------------------------------------------------------
# Q-009: Owner updates draft
# -----------------------------------------------------------
echo "--- Q-009: PUT by owner ---"
UPDATED_TEXT="Smoke C1.1b updated $SMOKE_MARKER"
OWNER_UPDATE_BODY="{\"questionText\":\"$UPDATED_TEXT\"}"
OWNER_UPDATE_RESPONSE=$(request_json "PUT" "$BASE_URL/collaborator/questions/$QUESTION_ID" "$COLLAB1_TOKEN" "$OWNER_UPDATE_BODY")
OWNER_UPDATE_RESPONSE_BODY=$(echo "$OWNER_UPDATE_RESPONSE" | sed '$d')
OWNER_UPDATE_STATUS=$(echo "$OWNER_UPDATE_RESPONSE" | tail -1)
check_status "Owner updates draft" "200" "$OWNER_UPDATE_STATUS"
check_body_contains "Updated text reflected" "$OWNER_UPDATE_RESPONSE_BODY" "$UPDATED_TEXT"

# -----------------------------------------------------------
# Q-010: Owner submits for review
# -----------------------------------------------------------
echo "--- Q-010: POST submit-for-review ---"
SUBMIT_RESPONSE=$(request_json "POST" "$BASE_URL/collaborator/questions/$QUESTION_ID/submit-for-review" "$COLLAB1_TOKEN" "")
SUBMIT_BODY=$(echo "$SUBMIT_RESPONSE" | sed '$d')
SUBMIT_STATUS=$(echo "$SUBMIT_RESPONSE" | tail -1)
check_status "Owner submits for review" "200" "$SUBMIT_STATUS"
check_body_contains "Submitted status PENDING_REVIEW" "$SUBMIT_BODY" '"status":"PENDING_REVIEW"'

# -----------------------------------------------------------
# Q-011: Owner cannot update after submit
# -----------------------------------------------------------
echo "--- Q-011: PUT after submit (expect 409 INVALID_STATE) ---"
LOCKED_UPDATE_RESPONSE=$(request_json "PUT" "$BASE_URL/collaborator/questions/$QUESTION_ID" "$COLLAB1_TOKEN" "{\"questionText\":\"Should fail $SMOKE_MARKER\"}")
LOCKED_UPDATE_BODY=$(echo "$LOCKED_UPDATE_RESPONSE" | sed '$d')
LOCKED_UPDATE_STATUS=$(echo "$LOCKED_UPDATE_RESPONSE" | tail -1)
check_status "Owner cannot update after submit" "409" "$LOCKED_UPDATE_STATUS"
check_body_contains "Update after submit INVALID_STATE" "$LOCKED_UPDATE_BODY" '"INVALID_STATE"'

# -----------------------------------------------------------
# Q-012: Admin queue works
# -----------------------------------------------------------
echo "--- Q-012: GET /admin/questions?status=PENDING_REVIEW ---"
QUEUE_RESPONSE=$(request_json "GET" "$BASE_URL/admin/questions?status=PENDING_REVIEW" "$ADMIN_TOKEN" "")
QUEUE_STATUS=$(echo "$QUEUE_RESPONSE" | tail -1)
check_status "Admin queue works" "200" "$QUEUE_STATUS"

# -----------------------------------------------------------
# Q-013: Admin request revision without comment fails
# -----------------------------------------------------------
echo "--- Q-013: request-revision without comment (expect 400) ---"
REVISION_BLANK_RESPONSE=$(request_json "POST" "$BASE_URL/admin/questions/$QUESTION_ID/request-revision" "$ADMIN_TOKEN" "{\"comment\":\"   \"}")
REVISION_BLANK_BODY=$(echo "$REVISION_BLANK_RESPONSE" | sed '$d')
REVISION_BLANK_STATUS=$(echo "$REVISION_BLANK_RESPONSE" | tail -1)
check_status "Admin request revision without comment fails" "400" "$REVISION_BLANK_STATUS"
check_body_contains "Blank revision comment VALIDATION_FAILED" "$REVISION_BLANK_BODY" '"VALIDATION_FAILED"'

# -----------------------------------------------------------
# Q-014: Admin request revision with comment succeeds
# -----------------------------------------------------------
echo "--- Q-014: request-revision with comment ---"
REVISION_RESPONSE=$(request_json "POST" "$BASE_URL/admin/questions/$QUESTION_ID/request-revision" "$ADMIN_TOKEN" "{\"comment\":\"Please revise smoke $SMOKE_MARKER\"}")
REVISION_BODY=$(echo "$REVISION_RESPONSE" | sed '$d')
REVISION_STATUS=$(echo "$REVISION_RESPONSE" | tail -1)
check_status "Admin request revision succeeds" "200" "$REVISION_STATUS"
check_body_contains "Revision status NEEDS_REVISION" "$REVISION_BODY" '"status":"NEEDS_REVISION"'

# -----------------------------------------------------------
# Q-015: Owner resubmits after revision
# -----------------------------------------------------------
echo "--- Q-015: resubmit after revision ---"
RESUBMIT_RESPONSE=$(request_json "POST" "$BASE_URL/collaborator/questions/$QUESTION_ID/submit-for-review" "$COLLAB1_TOKEN" "")
RESUBMIT_BODY=$(echo "$RESUBMIT_RESPONSE" | sed '$d')
RESUBMIT_STATUS=$(echo "$RESUBMIT_RESPONSE" | tail -1)
check_status "Owner resubmits after revision" "200" "$RESUBMIT_STATUS"
check_body_contains "Resubmitted status PENDING_REVIEW" "$RESUBMIT_BODY" '"status":"PENDING_REVIEW"'

# -----------------------------------------------------------
# Q-016: Admin approve succeeds
# -----------------------------------------------------------
echo "--- Q-016: approve pending question ---"
APPROVE_RESPONSE=$(request_json "POST" "$BASE_URL/admin/questions/$QUESTION_ID/approve" "$ADMIN_TOKEN" "{\"comment\":\"Approved smoke $SMOKE_MARKER\"}")
APPROVE_BODY=$(echo "$APPROVE_RESPONSE" | sed '$d')
APPROVE_STATUS=$(echo "$APPROVE_RESPONSE" | tail -1)
check_status "Admin approve succeeds" "200" "$APPROVE_STATUS"
check_body_contains "Approved status APPROVED" "$APPROVE_BODY" '"status":"APPROVED"'

# -----------------------------------------------------------
# Q-017: Review action on already-approved question fails
# -----------------------------------------------------------
echo "--- Q-017: approve already-approved question (expect 409 INVALID_STATE) ---"
REAPPROVE_RESPONSE=$(request_json "POST" "$BASE_URL/admin/questions/$QUESTION_ID/approve" "$ADMIN_TOKEN" "{\"comment\":\"Second approve should fail $SMOKE_MARKER\"}")
REAPPROVE_BODY=$(echo "$REAPPROVE_RESPONSE" | sed '$d')
REAPPROVE_STATUS=$(echo "$REAPPROVE_RESPONSE" | tail -1)
check_status "Review action on already-approved question fails" "409" "$REAPPROVE_STATUS"
check_body_contains "Already-approved review INVALID_STATE" "$REAPPROVE_BODY" '"INVALID_STATE"'

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
