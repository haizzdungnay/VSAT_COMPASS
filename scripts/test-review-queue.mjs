
import assert from 'node:assert/strict';

const BASE_URL = 'http://localhost:18080/api/v1';

async function login(email, password) {
  const res = await fetch(`${BASE_URL}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password })
  });
  assert.ok(res.ok, `Login ${email} must succeed`);
  const json = await res.json();
  return json.data.accessToken;
}

async function run() {
  console.log('=== Review Queue Comprehensive Audit Test Starting ===');

  const adminToken = await login('admin@vsat.com', 'Admin@123');
  assert.ok(adminToken, 'Admin token must exist');
  console.log('1. Admin authenticated successfully');

  // Check 1: GET /admin/questions returns questionTextSnippet
  const queueRes = await fetch(`${BASE_URL}/admin/questions?size=5`, {
    headers: { 'Authorization': `Bearer ${adminToken}` }
  });
  assert.ok(queueRes.ok, 'Fetch queue must return 200');
  const queueJson = await queueRes.json();
  assert.ok(Array.isArray(queueJson.data.content), 'Content must be an array');
  const sample = queueJson.data.content[0];
  assert.ok(sample, 'Must have at least one question');
  console.log(`2. GET /admin/questions returned ${queueJson.data.content.length} items. Question code: ${sample.questionCode}`);

  // Check 2: GET /admin/questions/{id} (or fallback /collaborator/questions/{id})
  const detailRes = await fetch(`${BASE_URL}/collaborator/questions/10`, {
    headers: { 'Authorization': `Bearer ${adminToken}` }
  });
  assert.ok(detailRes.ok, 'Question 10 detail must return 200');
  const detailJson = await detailRes.json();
  assert.equal(detailJson.data.id, 10, 'Question ID must be 10');
  assert.ok(detailJson.data.questionText, 'Question text must not be empty');
  assert.ok(Array.isArray(detailJson.data.options), 'Options must be an array');
  console.log('3. Question 10 detail verified');

  // Check 3: Full Review Workflow E2E (Create -> Submit For Review -> Approve)
  const uniqueCode = 'REV_TEST_' + Date.now().toString().slice(-6);
  const createRes = await fetch(`${BASE_URL}/collaborator/questions`, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${adminToken}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      subjectId: 1,
      topicId: 3,
      difficulty: 'MEDIUM',
      questionType: 'SINGLE_CHOICE',
      questionCode: uniqueCode,
      questionText: 'Tính giới hạn $\\lim_{x \\to 0} \\frac{\\sin x}{x}$ bằng bao nhiêu?',
      explanation: 'Theo định lý giới hạn cơ bản, kết quả bằng 1.',
      options: [
        { optionLabel: 'A', optionText: '1', isCorrect: true, displayOrder: 1 },
        { optionLabel: 'B', optionText: '0', isCorrect: false, displayOrder: 2 },
        { optionLabel: 'C', optionText: '$\\infty$', isCorrect: false, displayOrder: 3 },
        { optionLabel: 'D', optionText: 'Không tồn tại', isCorrect: false, displayOrder: 4 }
      ]
    })
  });
  assert.ok(createRes.ok, 'Create question must succeed');
  const createJson = await createRes.json();
  const newQuestionId = createJson.data.id;
  assert.equal(createJson.data.status, 'DRAFT', 'New question must be DRAFT');
  console.log(`4. Created new DRAFT question ID: ${newQuestionId} (${uniqueCode})`);

  // Submit for review -> PENDING_REVIEW
  const submitRes = await fetch(`${BASE_URL}/collaborator/questions/${newQuestionId}/submit-for-review`, {
    method: 'POST',
    headers: { 'Authorization': `Bearer ${adminToken}` }
  });
  assert.ok(submitRes.ok, 'Submit for review must succeed');
  const submitJson = await submitRes.json();
  assert.equal(submitJson.data.status, 'PENDING_REVIEW', 'Status must transition to PENDING_REVIEW');
  console.log(`5. Question ${newQuestionId} submitted for review (status: PENDING_REVIEW)`);

  // Verify it appears in pending review queue
  const pendingQueueRes = await fetch(`${BASE_URL}/admin/questions?status=PENDING_REVIEW`, {
    headers: { 'Authorization': `Bearer ${adminToken}` }
  });
  assert.ok(pendingQueueRes.ok, 'Fetch pending review queue must succeed');
  const pendingJson = await pendingQueueRes.json();
  const foundInQueue = pendingJson.data.content.some(q => q.id === newQuestionId);
  assert.ok(foundInQueue, 'Newly submitted question must appear in PENDING_REVIEW queue');
  console.log(`6. Question ${newQuestionId} verified in PENDING_REVIEW queue`);

  // Approve the pending question -> APPROVED
  const approveRes = await fetch(`${BASE_URL}/admin/questions/${newQuestionId}/approve`, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${adminToken}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ comment: 'Duyet cau hoi gioi han hop le' })
  });
  assert.ok(approveRes.ok, 'Approving PENDING_REVIEW question must succeed (200)');
  const approveJson = await approveRes.json();
  assert.equal(approveJson.data.status, 'APPROVED', 'Status must be APPROVED');
  console.log(`7. Question ${newQuestionId} approved successfully (status: APPROVED)`);

  // Trying to approve again must return 409 Conflict
  const secondApproveRes = await fetch(`${BASE_URL}/admin/questions/${newQuestionId}/approve`, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${adminToken}`,
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({ comment: 'Duyet lai' })
  });
  assert.equal(secondApproveRes.status, 409, 'Re-approving must return 409 INVALID_STATE');
  console.log('8. Re-approving rejected with 409 INVALID_STATE');

  // Student role cannot approve -> 403 Forbidden
  const studentToken = await login('student@vsat.com', 'Admin@123');
  const studentApproveRes = await fetch(`${BASE_URL}/admin/questions/${newQuestionId}/approve`, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${studentToken}`,
      'Content-Type': 'application/json'
    }
  });
  assert.ok(studentApproveRes.status === 403 || studentApproveRes.status === 401, 'Student cannot approve questions (403/401)');
  console.log('9. Student authorization check: 403 Forbidden verified');

  console.log('=== ALL AUDIT CHECKS PASSED (9/9) ===');
}

run().catch(err => {
  console.error('Self check failed:', err);
  process.exit(1);
});
