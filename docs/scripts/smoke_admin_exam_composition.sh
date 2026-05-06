#!/bin/bash
# ============================================================
# V-SAT Compass - Admin Exam Composition + Publish Workflow Smoke Tests
# Covers Phase C1.2b-2 admin composition, workflow, public visibility,
# and archived-state rejection paths.
#
# Usage:
#   bash docs/scripts/smoke_admin_exam_composition.sh
#   BASE_URL=https://your-api.com/api/v1 bash docs/scripts/smoke_admin_exam_composition.sh
#   SMOKE_QUESTION_IDS="1,2,3" bash docs/scripts/smoke_admin_exam_composition.sh
#
# Compatible with bash 3.2+ (macOS default)
# ============================================================

set -uo pipefail

BASE_URL="${BASE_URL:-https://vsat-compass-api.onrender.com/api/v1}"
SMOKE_CONTENT_ADMIN_EMAIL="${SMOKE_CONTENT_ADMIN_EMAIL:-content@vsat.com}"
SMOKE_SUPER_ADMIN_EMAIL="${SMOKE_SUPER_ADMIN_EMAIL:-admin@vsat.com}"
SMOKE_ADMIN_PASSWORD="${SMOKE_ADMIN_PASSWORD:-Admin@123}"
SMOKE_QUESTION_IDS="${SMOKE_QUESTION_IDS:-}"

PASS=0
FAIL=0
SKIP=0
BLOCKED=0
TOTAL=0

CONTENT_TOKEN=""
SUPER_TOKEN=""
SUPER_USER_ID=""
SUBJECT_ID=""
QUESTION_ID_1=""
QUESTION_ID_2=""
QUESTION_ID_3=""

RUN_ID="$(date +%s)"
EXAM_CODE="SMOKE_C1_2B2_${RUN_ID}"
ARCHIVE_EXAM_CODE="SMOKE_C1_2B2_ARCH_${RUN_ID}"
EXAM_ID=""
ARCHIVE_EXAM_ID=""

if command -v jq >/dev/null 2>&1; then
    HAS_JQ=1
else
    HAS_JQ=0
fi

echo "========================================"
echo "V-SAT Compass - Admin Exam Composition + Workflow Smoke Tests"
echo "Base URL: $BASE_URL"
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

record_blocked() {
    local test_id="$1"
    local description="$2"
    local detail="${3:-}"
    TOTAL=$((TOTAL + 1))
    BLOCKED=$((BLOCKED + 1))
    if [ -n "$detail" ]; then
        echo "  [BLOCKED] $test_id $description ($detail)"
    else
        echo "  [BLOCKED] $test_id $description"
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

public_request() {
    local method="$1"
    local url="$2"
    curl -s -w "\n%{http_code}" -X "$method" "$url" -H "Accept: application/json"
}

login_user() {
    local email="$1"
    curl -s -w "\n%{http_code}" -X POST "$BASE_URL/auth/login" \
        -H "Content-Type: application/json" \
        -H "Accept: application/json" \
        -d "{\"email\":\"$email\",\"password\":\"$SMOKE_ADMIN_PASSWORD\"}"
}

extract_access_token() {
    if [ "$HAS_JQ" -eq 1 ]; then
        echo "$1" | jq -r '.data.accessToken // empty' 2>/dev/null
    else
        echo "$1" | grep -o '"accessToken":"[^"]*"' | head -1 | cut -d'"' -f4
    fi
}

extract_login_user_id() {
    if [ "$HAS_JQ" -eq 1 ]; then
        echo "$1" | jq -r '.data.user.id // empty' 2>/dev/null
    else
        echo "$1" | grep -o '"user":{[^}]*}' | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2
    fi
}

extract_data_id() {
    if [ "$HAS_JQ" -eq 1 ]; then
        echo "$1" | jq -r '.data.id // empty' 2>/dev/null
    else
        echo "$1" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2
    fi
}

extract_string_field() {
    local body="$1"
    local field="$2"
    if [ "$HAS_JQ" -eq 1 ]; then
        echo "$body" | jq -r ".data.$field // empty" 2>/dev/null
    else
        echo "$body" | grep -o "\"$field\":\"[^\"]*\"" | head -1 | cut -d'"' -f4
    fi
}

extract_number_field() {
    local body="$1"
    local field="$2"
    if [ "$HAS_JQ" -eq 1 ]; then
        echo "$body" | jq -r ".data.$field // empty" 2>/dev/null
    else
        echo "$body" | grep -o "\"$field\":[0-9]*" | head -1 | cut -d':' -f2
    fi
}

extract_subject_id() {
    if [ "$HAS_JQ" -eq 1 ]; then
        local math_id
        math_id=$(echo "$1" | jq -r '.data[]? | select(.code == "MATH") | .id' 2>/dev/null | head -1)
        if [ -n "$math_id" ]; then
            echo "$math_id"
        else
            echo "$1" | jq -r '.data[0].id // empty' 2>/dev/null
        fi
    else
        local math_id
        math_id=$(echo "$1" | sed 's/},{/}\
{/g' | grep '"code":"MATH"' | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2)
        if [ -n "$math_id" ]; then
            echo "$math_id"
        else
            echo "$1" | grep -o '"id":[0-9]*' | head -1 | cut -d':' -f2
        fi
    fi
}

extract_question_ids_from_body() {
    if [ "$HAS_JQ" -eq 1 ]; then
        echo "$1" | jq -r '.data.content[]?.id // empty' 2>/dev/null
    else
        echo "$1" | grep -o '"id":[0-9]*' | cut -d':' -f2
    fi
}

body_contains() {
    echo "$1" | grep -q "$2"
}

finish() {
    echo ""
    echo "========================================"
    echo "Smoke (admin exam composition): $PASS PASS / $FAIL FAIL / $SKIP SKIP / $BLOCKED BLOCKED / $TOTAL TOTAL"
    echo "========================================"
    if [ "$FAIL" -gt 0 ]; then
        exit 1
    fi
    exit 0
}

blocked_and_exit() {
    record_blocked "$1" "$2" "$3"
    finish
}

create_draft_exam() {
    local exam_code="$1"
    local title="$2"
    local create_body
    create_body="{\"examCode\":\"$exam_code\",\"title\":\"$title\",\"subjectId\":$SUBJECT_ID,\"description\":\"Created by C1.2b-2 smoke\",\"durationMinutes\":90,\"difficulty\":\"MEDIUM\",\"pricingType\":\"FREE\",\"price\":0,\"tags\":\"smoke,c1-2b-2\"}"
    request_json "POST" "$BASE_URL/admin/exams" "$CONTENT_TOKEN" "$create_body"
}

add_question() {
    local exam_id="$1"
    local question_id="$2"
    request_json "POST" "$BASE_URL/admin/exams/$exam_id/questions" "$CONTENT_TOKEN" "{\"questionId\":$question_id}"
}

submit_review() {
    local exam_id="$1"
    request_json "POST" "$BASE_URL/admin/exams/$exam_id/submit-review" "$CONTENT_TOKEN"
}

publish_exam() {
    local exam_id="$1"
    request_json "POST" "$BASE_URL/admin/exams/$exam_id/publish" "$SUPER_TOKEN"
}

hide_exam() {
    local exam_id="$1"
    request_json "POST" "$BASE_URL/admin/exams/$exam_id/hide" "$CONTENT_TOKEN"
}

archive_exam() {
    local exam_id="$1"
    request_json "POST" "$BASE_URL/admin/exams/$exam_id/archive" "$CONTENT_TOKEN"
}

assert_status_field() {
    local test_id="$1"
    local description="$2"
    local body="$3"
    local expected="$4"
    local actual
    actual=$(extract_string_field "$body" "status")
    if [ "$actual" = "$expected" ]; then
        record_result "$test_id" "$description" 1
    else
        record_result "$test_id" "$description" 0 "status=$actual expected=$expected"
    fi
}

echo "--- TC-ADM-COMP-1: Login CONTENT_ADMIN and SUPER_ADMIN ---"
CONTENT_LOGIN_RESPONSE=$(login_user "$SMOKE_CONTENT_ADMIN_EMAIL")
CONTENT_LOGIN_BODY=$(body_from_response "$CONTENT_LOGIN_RESPONSE")
CONTENT_LOGIN_STATUS=$(status_from_response "$CONTENT_LOGIN_RESPONSE")
CONTENT_TOKEN=$(extract_access_token "$CONTENT_LOGIN_BODY")

SUPER_LOGIN_RESPONSE=$(login_user "$SMOKE_SUPER_ADMIN_EMAIL")
SUPER_LOGIN_BODY=$(body_from_response "$SUPER_LOGIN_RESPONSE")
SUPER_LOGIN_STATUS=$(status_from_response "$SUPER_LOGIN_RESPONSE")
SUPER_TOKEN=$(extract_access_token "$SUPER_LOGIN_BODY")
SUPER_USER_ID=$(extract_login_user_id "$SUPER_LOGIN_BODY")

if [ "$CONTENT_LOGIN_STATUS" = "200" ] && [ -n "$CONTENT_TOKEN" ] \
    && [ "$SUPER_LOGIN_STATUS" = "200" ] && [ -n "$SUPER_TOKEN" ] && [ -n "$SUPER_USER_ID" ]; then
    record_result "TC-ADM-COMP-1" "Role logins return tokens and SUPER_ADMIN user id" 1
else
    record_result "TC-ADM-COMP-1" "Role logins return tokens and SUPER_ADMIN user id" 0 "content HTTP $CONTENT_LOGIN_STATUS, super HTTP $SUPER_LOGIN_STATUS"
    finish
fi

echo "--- TC-ADM-COMP-2: Discover active subject ---"
SUBJECTS_RESPONSE=$(public_request "GET" "$BASE_URL/subjects")
SUBJECTS_BODY=$(body_from_response "$SUBJECTS_RESPONSE")
SUBJECTS_STATUS=$(status_from_response "$SUBJECTS_RESPONSE")
if [ "$SUBJECTS_STATUS" = "200" ]; then
    SUBJECT_ID=$(extract_subject_id "$SUBJECTS_BODY")
fi
if [ -n "$SUBJECT_ID" ]; then
    record_result "TC-ADM-COMP-2" "Active subject discovered" 1
else
    record_result "TC-ADM-COMP-2" "Active subject discovered" 0 "HTTP $SUBJECTS_STATUS"
    finish
fi

echo "--- TC-ADM-COMP-3: Resolve three APPROVED/PUBLISHED question IDs ---"
if [ -n "$SMOKE_QUESTION_IDS" ]; then
    OLD_IFS="$IFS"
    IFS=','
    set -- $SMOKE_QUESTION_IDS
    IFS="$OLD_IFS"
    QUESTION_ID_1="${1:-}"
    QUESTION_ID_2="${2:-}"
    QUESTION_ID_3="${3:-}"
else
    APPROVED_RESPONSE=$(request_json "GET" "$BASE_URL/admin/questions?status=APPROVED&size=20" "$CONTENT_TOKEN")
    APPROVED_BODY=$(body_from_response "$APPROVED_RESPONSE")
    APPROVED_STATUS=$(status_from_response "$APPROVED_RESPONSE")
    PUBLISHED_RESPONSE=$(request_json "GET" "$BASE_URL/admin/questions?status=PUBLISHED&size=20" "$CONTENT_TOKEN")
    PUBLISHED_BODY=$(body_from_response "$PUBLISHED_RESPONSE")
    PUBLISHED_STATUS=$(status_from_response "$PUBLISHED_RESPONSE")
    if [ "$APPROVED_STATUS" = "200" ] || [ "$PUBLISHED_STATUS" = "200" ]; then
        IDS=$(printf "%s\n%s\n" \
            "$(extract_question_ids_from_body "$APPROVED_BODY")" \
            "$(extract_question_ids_from_body "$PUBLISHED_BODY")" \
            | sed '/^$/d' | awk '!seen[$0]++' | head -3 | tr '\n' ',' | sed 's/,$//')
        OLD_IFS="$IFS"
        IFS=','
        set -- $IDS
        IFS="$OLD_IFS"
        QUESTION_ID_1="${1:-}"
        QUESTION_ID_2="${2:-}"
        QUESTION_ID_3="${3:-}"
    fi
fi

if [ -n "$QUESTION_ID_1" ] && [ -n "$QUESTION_ID_2" ] && [ -n "$QUESTION_ID_3" ]; then
    record_result "TC-ADM-COMP-3" "Three question fixtures resolved" 1
else
    blocked_and_exit "TC-ADM-COMP-3" "Three question fixtures resolved" "set SMOKE_QUESTION_IDS=\"1,2,3\" or seed at least three APPROVED/PUBLISHED questions"
fi

echo "--- TC-ADM-COMP-4: Create DRAFT exam ---"
CREATE_RESPONSE=$(create_draft_exam "$EXAM_CODE" "Smoke Composition Exam $EXAM_CODE")
CREATE_BODY=$(body_from_response "$CREATE_RESPONSE")
CREATE_STATUS=$(status_from_response "$CREATE_RESPONSE")
EXAM_ID=$(extract_data_id "$CREATE_BODY")
if { [ "$CREATE_STATUS" = "201" ] || [ "$CREATE_STATUS" = "200" ]; } \
    && [ -n "$EXAM_ID" ] && body_contains "$CREATE_BODY" '"status":"DRAFT"'; then
    record_result "TC-ADM-COMP-4" "Create DRAFT exam" 1
else
    record_result "TC-ADM-COMP-4" "Create DRAFT exam" 0 "HTTP $CREATE_STATUS"
    finish
fi

echo "--- TC-ADM-COMP-5: Add three questions ---"
ADD1_RESPONSE=$(add_question "$EXAM_ID" "$QUESTION_ID_1")
ADD1_BODY=$(body_from_response "$ADD1_RESPONSE")
ADD1_STATUS=$(status_from_response "$ADD1_RESPONSE")
ADD2_RESPONSE=$(add_question "$EXAM_ID" "$QUESTION_ID_2")
ADD2_BODY=$(body_from_response "$ADD2_RESPONSE")
ADD2_STATUS=$(status_from_response "$ADD2_RESPONSE")
ADD3_RESPONSE=$(add_question "$EXAM_ID" "$QUESTION_ID_3")
ADD3_BODY=$(body_from_response "$ADD3_RESPONSE")
ADD3_STATUS=$(status_from_response "$ADD3_RESPONSE")
ADD3_COUNT=$(extract_number_field "$ADD3_BODY" "questionCount")
if [ "$ADD1_STATUS" = "200" ] && [ "$ADD2_STATUS" = "200" ] && [ "$ADD3_STATUS" = "200" ] && [ "$ADD3_COUNT" = "3" ]; then
    record_result "TC-ADM-COMP-5" "Add three approved/published questions and count=3" 1
else
    record_result "TC-ADM-COMP-5" "Add three approved/published questions and count=3" 0 "HTTP $ADD1_STATUS/$ADD2_STATUS/$ADD3_STATUS count=$ADD3_COUNT"
    finish
fi

echo "--- TC-ADM-COMP-6: Duplicate add rejected ---"
DUP_RESPONSE=$(add_question "$EXAM_ID" "$QUESTION_ID_1")
DUP_STATUS=$(status_from_response "$DUP_RESPONSE")
if [ "$DUP_STATUS" = "409" ]; then
    record_result "TC-ADM-COMP-6" "Duplicate add returns 409" 1
else
    record_result "TC-ADM-COMP-6" "Duplicate add returns 409" 0 "HTTP $DUP_STATUS"
fi

echo "--- TC-ADM-COMP-7: Full reverse reorder ---"
REORDER_BODY="{\"questionIds\":[$QUESTION_ID_3,$QUESTION_ID_2,$QUESTION_ID_1]}"
REORDER_RESPONSE=$(request_json "PUT" "$BASE_URL/admin/exams/$EXAM_ID/questions/reorder" "$CONTENT_TOKEN" "$REORDER_BODY")
REORDER_STATUS=$(status_from_response "$REORDER_RESPONSE")
if [ "$REORDER_STATUS" = "200" ]; then
    record_result "TC-ADM-COMP-7" "Full reverse reorder returns 200" 1
else
    record_result "TC-ADM-COMP-7" "Full reverse reorder returns 200" 0 "HTTP $REORDER_STATUS"
fi

echo "--- TC-ADM-COMP-8: Remove one question and verify count decremented ---"
REMOVE_RESPONSE=$(request_json "DELETE" "$BASE_URL/admin/exams/$EXAM_ID/questions/$QUESTION_ID_2" "$CONTENT_TOKEN")
REMOVE_BODY=$(body_from_response "$REMOVE_RESPONSE")
REMOVE_STATUS=$(status_from_response "$REMOVE_RESPONSE")
REMOVE_COUNT=$(extract_number_field "$REMOVE_BODY" "questionCount")
if [ "$REMOVE_STATUS" = "200" ] && [ "$REMOVE_COUNT" = "2" ]; then
    record_result "TC-ADM-COMP-8" "Remove one mapping and count=2" 1
else
    record_result "TC-ADM-COMP-8" "Remove one mapping and count=2" 0 "HTTP $REMOVE_STATUS count=$REMOVE_COUNT"
fi

echo "--- TC-ADM-WF-1: submit-review then reject-review ---"
SUBMIT1_RESPONSE=$(submit_review "$EXAM_ID")
SUBMIT1_BODY=$(body_from_response "$SUBMIT1_RESPONSE")
SUBMIT1_STATUS=$(status_from_response "$SUBMIT1_RESPONSE")
REJECT_RESPONSE=$(request_json "POST" "$BASE_URL/admin/exams/$EXAM_ID/reject-review" "$CONTENT_TOKEN")
REJECT_BODY=$(body_from_response "$REJECT_RESPONSE")
REJECT_STATUS=$(status_from_response "$REJECT_RESPONSE")
if [ "$SUBMIT1_STATUS" = "200" ] && body_contains "$SUBMIT1_BODY" '"status":"PENDING_REVIEW"' \
    && [ "$REJECT_STATUS" = "200" ] && body_contains "$REJECT_BODY" '"status":"DRAFT"'; then
    record_result "TC-ADM-WF-1" "DRAFT -> PENDING_REVIEW -> DRAFT" 1
else
    record_result "TC-ADM-WF-1" "DRAFT -> PENDING_REVIEW -> DRAFT" 0 "HTTP $SUBMIT1_STATUS/$REJECT_STATUS"
fi

echo "--- TC-ADM-WF-2: submit-review again and publish as SUPER_ADMIN ---"
SUBMIT2_RESPONSE=$(submit_review "$EXAM_ID")
SUBMIT2_STATUS=$(status_from_response "$SUBMIT2_RESPONSE")
PUBLISH_RESPONSE=$(publish_exam "$EXAM_ID")
PUBLISH_BODY=$(body_from_response "$PUBLISH_RESPONSE")
PUBLISH_STATUS=$(status_from_response "$PUBLISH_RESPONSE")
PUBLISH_REVIEWED_BY=$(extract_number_field "$PUBLISH_BODY" "reviewedBy")
PUBLISH_DATE=$(extract_string_field "$PUBLISH_BODY" "publishDate")
if [ "$SUBMIT2_STATUS" = "200" ] && [ "$PUBLISH_STATUS" = "200" ] \
    && body_contains "$PUBLISH_BODY" '"status":"PUBLISHED"' \
    && [ "$PUBLISH_REVIEWED_BY" = "$SUPER_USER_ID" ] && [ -n "$PUBLISH_DATE" ]; then
    record_result "TC-ADM-WF-2" "Publish sets status, reviewedBy, publishDate" 1
else
    record_result "TC-ADM-WF-2" "Publish sets status, reviewedBy, publishDate" 0 "HTTP $SUBMIT2_STATUS/$PUBLISH_STATUS reviewedBy=$PUBLISH_REVIEWED_BY"
fi

echo "--- TC-ADM-WF-3: CONTENT_ADMIN cannot publish ---"
CONTENT_PUBLISH_RESPONSE=$(request_json "POST" "$BASE_URL/admin/exams/$EXAM_ID/publish" "$CONTENT_TOKEN")
CONTENT_PUBLISH_STATUS=$(status_from_response "$CONTENT_PUBLISH_RESPONSE")
if [ "$CONTENT_PUBLISH_STATUS" = "403" ]; then
    record_result "TC-ADM-WF-3" "CONTENT_ADMIN publish returns 403" 1
else
    record_result "TC-ADM-WF-3" "CONTENT_ADMIN publish returns 403" 0 "HTTP $CONTENT_PUBLISH_STATUS"
fi

echo "--- TC-ADM-PUB-1: Public list includes published exam ---"
PUBLIC1_RESPONSE=$(public_request "GET" "$BASE_URL/exams?size=100")
PUBLIC1_BODY=$(body_from_response "$PUBLIC1_RESPONSE")
PUBLIC1_STATUS=$(status_from_response "$PUBLIC1_RESPONSE")
if [ "$PUBLIC1_STATUS" = "200" ] && body_contains "$PUBLIC1_BODY" "\"examCode\":\"$EXAM_CODE\""; then
    record_result "TC-ADM-PUB-1" "Public /exams includes published FREE exam" 1
else
    record_result "TC-ADM-PUB-1" "Public /exams includes published FREE exam" 0 "HTTP $PUBLIC1_STATUS"
fi

echo "--- TC-ADM-WF-4: hide removes exam from public list ---"
HIDE1_RESPONSE=$(hide_exam "$EXAM_ID")
HIDE1_BODY=$(body_from_response "$HIDE1_RESPONSE")
HIDE1_STATUS=$(status_from_response "$HIDE1_RESPONSE")
PUBLIC2_RESPONSE=$(public_request "GET" "$BASE_URL/exams?size=100")
PUBLIC2_BODY=$(body_from_response "$PUBLIC2_RESPONSE")
PUBLIC2_STATUS=$(status_from_response "$PUBLIC2_RESPONSE")
if [ "$HIDE1_STATUS" = "200" ] && body_contains "$HIDE1_BODY" '"status":"HIDDEN"' \
    && [ "$PUBLIC2_STATUS" = "200" ] && ! body_contains "$PUBLIC2_BODY" "\"examCode\":\"$EXAM_CODE\""; then
    record_result "TC-ADM-WF-4" "Hide sets HIDDEN and removes public visibility" 1
else
    record_result "TC-ADM-WF-4" "Hide sets HIDDEN and removes public visibility" 0 "HTTP hide=$HIDE1_STATUS public=$PUBLIC2_STATUS"
fi

echo "--- TC-ADM-WF-5: publish from HIDDEN overwrites audit ---"
sleep 1
REPUBLISH_RESPONSE=$(publish_exam "$EXAM_ID")
REPUBLISH_BODY=$(body_from_response "$REPUBLISH_RESPONSE")
REPUBLISH_STATUS=$(status_from_response "$REPUBLISH_RESPONSE")
REPUBLISH_REVIEWED_BY=$(extract_number_field "$REPUBLISH_BODY" "reviewedBy")
REPUBLISH_DATE=$(extract_string_field "$REPUBLISH_BODY" "publishDate")
if [ "$REPUBLISH_STATUS" = "200" ] && body_contains "$REPUBLISH_BODY" '"status":"PUBLISHED"' \
    && [ "$REPUBLISH_REVIEWED_BY" = "$SUPER_USER_ID" ] && [ -n "$REPUBLISH_DATE" ] \
    && [ "$REPUBLISH_DATE" != "$PUBLISH_DATE" ]; then
    record_result "TC-ADM-WF-5" "HIDDEN -> PUBLISHED overwrites reviewedBy/publishDate" 1
else
    record_result "TC-ADM-WF-5" "HIDDEN -> PUBLISHED overwrites reviewedBy/publishDate" 0 "HTTP $REPUBLISH_STATUS"
fi

echo "--- TC-ADM-WF-6: hide then return-to-draft preserves audit ---"
HIDE2_RESPONSE=$(hide_exam "$EXAM_ID")
HIDE2_BODY=$(body_from_response "$HIDE2_RESPONSE")
HIDE2_STATUS=$(status_from_response "$HIDE2_RESPONSE")
HIDE2_REVIEWED_BY=$(extract_number_field "$HIDE2_BODY" "reviewedBy")
HIDE2_PUBLISH_DATE=$(extract_string_field "$HIDE2_BODY" "publishDate")
RETURN_RESPONSE=$(request_json "POST" "$BASE_URL/admin/exams/$EXAM_ID/return-to-draft" "$CONTENT_TOKEN")
RETURN_BODY=$(body_from_response "$RETURN_RESPONSE")
RETURN_STATUS=$(status_from_response "$RETURN_RESPONSE")
RETURN_REVIEWED_BY=$(extract_number_field "$RETURN_BODY" "reviewedBy")
RETURN_PUBLISH_DATE=$(extract_string_field "$RETURN_BODY" "publishDate")
if [ "$HIDE2_STATUS" = "200" ] && [ "$RETURN_STATUS" = "200" ] \
    && body_contains "$RETURN_BODY" '"status":"DRAFT"' \
    && [ "$RETURN_REVIEWED_BY" = "$HIDE2_REVIEWED_BY" ] \
    && [ "$RETURN_PUBLISH_DATE" = "$HIDE2_PUBLISH_DATE" ]; then
    record_result "TC-ADM-WF-6" "HIDDEN -> DRAFT preserves reviewedBy/publishDate" 1
else
    record_result "TC-ADM-WF-6" "HIDDEN -> DRAFT preserves reviewedBy/publishDate" 0 "HTTP hide=$HIDE2_STATUS return=$RETURN_STATUS"
fi

echo "--- TC-ADM-ARCH-1: Create separate published exam and archive it ---"
ARCH_CREATE_RESPONSE=$(create_draft_exam "$ARCHIVE_EXAM_CODE" "Smoke Archive Exam $ARCHIVE_EXAM_CODE")
ARCH_CREATE_BODY=$(body_from_response "$ARCH_CREATE_RESPONSE")
ARCH_CREATE_STATUS=$(status_from_response "$ARCH_CREATE_RESPONSE")
ARCHIVE_EXAM_ID=$(extract_data_id "$ARCH_CREATE_BODY")
if { [ "$ARCH_CREATE_STATUS" = "201" ] || [ "$ARCH_CREATE_STATUS" = "200" ]; } && [ -n "$ARCHIVE_EXAM_ID" ]; then
    ARCH_ADD_RESPONSE=$(add_question "$ARCHIVE_EXAM_ID" "$QUESTION_ID_1")
    ARCH_ADD_STATUS=$(status_from_response "$ARCH_ADD_RESPONSE")
    ARCH_SUBMIT_RESPONSE=$(submit_review "$ARCHIVE_EXAM_ID")
    ARCH_SUBMIT_STATUS=$(status_from_response "$ARCH_SUBMIT_RESPONSE")
    ARCH_PUBLISH_RESPONSE=$(publish_exam "$ARCHIVE_EXAM_ID")
    ARCH_PUBLISH_STATUS=$(status_from_response "$ARCH_PUBLISH_RESPONSE")
    ARCHIVE_RESPONSE=$(archive_exam "$ARCHIVE_EXAM_ID")
    ARCHIVE_BODY=$(body_from_response "$ARCHIVE_RESPONSE")
    ARCHIVE_STATUS=$(status_from_response "$ARCHIVE_RESPONSE")
    if [ "$ARCH_ADD_STATUS" = "200" ] && [ "$ARCH_SUBMIT_STATUS" = "200" ] \
        && [ "$ARCH_PUBLISH_STATUS" = "200" ] && [ "$ARCHIVE_STATUS" = "200" ] \
        && body_contains "$ARCHIVE_BODY" '"status":"ARCHIVED"'; then
        record_result "TC-ADM-ARCH-1" "Separate PUBLISHED exam archives to ARCHIVED" 1
    else
        record_result "TC-ADM-ARCH-1" "Separate PUBLISHED exam archives to ARCHIVED" 0 "HTTP create/add/submit/publish/archive=$ARCH_CREATE_STATUS/$ARCH_ADD_STATUS/$ARCH_SUBMIT_STATUS/$ARCH_PUBLISH_STATUS/$ARCHIVE_STATUS"
    fi
else
    record_result "TC-ADM-ARCH-1" "Separate PUBLISHED exam archives to ARCHIVED" 0 "create HTTP $ARCH_CREATE_STATUS"
fi

echo "--- TC-ADM-ARCH-2: Archived exam rejects composition and workflow transitions ---"
if [ -n "$ARCHIVE_EXAM_ID" ]; then
    ARCH_ADD_REJECT=$(add_question "$ARCHIVE_EXAM_ID" "$QUESTION_ID_2")
    ARCH_REMOVE_REJECT=$(request_json "DELETE" "$BASE_URL/admin/exams/$ARCHIVE_EXAM_ID/questions/$QUESTION_ID_1" "$CONTENT_TOKEN")
    ARCH_REORDER_REJECT=$(request_json "PUT" "$BASE_URL/admin/exams/$ARCHIVE_EXAM_ID/questions/reorder" "$CONTENT_TOKEN" "{\"questionIds\":[$QUESTION_ID_1]}")
    ARCH_SUBMIT_REJECT=$(submit_review "$ARCHIVE_EXAM_ID")
    ARCH_PUBLISH_REJECT=$(publish_exam "$ARCHIVE_EXAM_ID")
    ARCH_HIDE_REJECT=$(hide_exam "$ARCHIVE_EXAM_ID")
    ARCH_ARCHIVE_REJECT=$(archive_exam "$ARCHIVE_EXAM_ID")
    ARCH_REJECT_REVIEW_REJECT=$(request_json "POST" "$BASE_URL/admin/exams/$ARCHIVE_EXAM_ID/reject-review" "$CONTENT_TOKEN")
    ARCH_RETURN_REJECT=$(request_json "POST" "$BASE_URL/admin/exams/$ARCHIVE_EXAM_ID/return-to-draft" "$CONTENT_TOKEN")

    if [ "$(status_from_response "$ARCH_ADD_REJECT")" = "409" ] \
        && [ "$(status_from_response "$ARCH_REMOVE_REJECT")" = "409" ] \
        && [ "$(status_from_response "$ARCH_REORDER_REJECT")" = "409" ] \
        && [ "$(status_from_response "$ARCH_SUBMIT_REJECT")" = "409" ] \
        && [ "$(status_from_response "$ARCH_PUBLISH_REJECT")" = "409" ] \
        && [ "$(status_from_response "$ARCH_HIDE_REJECT")" = "409" ] \
        && [ "$(status_from_response "$ARCH_ARCHIVE_REJECT")" = "409" ] \
        && [ "$(status_from_response "$ARCH_REJECT_REVIEW_REJECT")" = "409" ] \
        && [ "$(status_from_response "$ARCH_RETURN_REJECT")" = "409" ]; then
        record_result "TC-ADM-ARCH-2" "ARCHIVED rejects all composition/workflow transitions" 1
    else
        record_result "TC-ADM-ARCH-2" "ARCHIVED rejects all composition/workflow transitions" 0 "one or more endpoints did not return 409"
    fi
else
    record_blocked "TC-ADM-ARCH-2" "ARCHIVED rejects all composition/workflow transitions" "archive fixture was not created"
fi

finish
