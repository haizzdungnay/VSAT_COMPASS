// scripts/test-exam-windows.mjs
// E2E smoke tests for Exam Windows / Ca thi API & Admin flow

const BASE_URL = 'http://localhost:18080/api/v1';

async function main() {
  console.log('--- Starting Exam Windows E2E Smoke Test ---');
  let passed = 0;
  let total = 0;

  function assert(cond, msg) {
    total++;
    if (!cond) {
      console.error(`âŒ FAIL: ${msg}`);
      process.exit(1);
    }
    passed++;
    console.log(`âœ… PASS: ${msg}`);
  }

  // 1. Login Admin
  const loginRes = await fetch(`${BASE_URL}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email: 'admin@vsat.com', password: 'Admin@123' })
  });
  assert(loginRes.ok, 'Admin login succeeded');
  const loginJson = await loginRes.json();
  const token = loginJson.data.accessToken;
  assert(!!token, 'Access token obtained');

  const headers = {
    'Content-Type': 'application/json',
    'Authorization': `Bearer ${token}`
  };

  // 2. List initial windows
  const listRes = await fetch(`${BASE_URL}/admin/exam-windows`, { headers });
  assert(listRes.ok, 'List exam windows returned 200');
  const listJson = await listRes.json();
  assert(Array.isArray(listJson.data), 'Exam windows returned as array');

  // Find a valid exam id from database/exams list
  const examsRes = await fetch(`${BASE_URL}/admin/exams?page=0&size=5`, { headers });
  assert(examsRes.ok, 'List exams returned 200');
  const examsJson = await examsRes.json();
  const targetExam = examsJson.data?.content?.[0] || { id: 1 };
  const targetExamId = targetExam.id;
  console.log(`Using target examId: ${targetExamId} (${targetExam.examCode || 'code'})`);

  // 3. Create a new window
  const code = `E2E_WIN_${Date.now()}`;
  const now = new Date();
  const startAt = new Date(now.getTime() + 3600 * 1000).toISOString();
  const endAt = new Date(now.getTime() + 7200 * 1000).toISOString();

  const createRes = await fetch(`${BASE_URL}/admin/exam-windows`, {
    method: 'POST',
    headers,
    body: JSON.stringify({
      examId: targetExamId,
      windowCode: code,
      title: 'Ca thi E2E Test Runner',
      mode: 'SCHEDULED_MOCK',
      startAt,
      endAt,
      gracePeriodMinutes: 15,
      maxAttempts: 1,
      resultPolicy: 'AFTER_WINDOW_FULL',
      hardStopAtEnd: true,
      allowOffline: false
    })
  });
  assert(createRes.status === 201, `Create exam window returned 201 Created (got ${createRes.status})`);
  const createJson = await createRes.json();
  const createdWindow = createJson.data;
  assert(createdWindow.windowCode === code, 'Created windowCode matches');
  assert(createdWindow.status === 'UPCOMING', 'Initial computed status is UPCOMING');
  const windowId = createdWindow.id;

  // 4. Duplicate code rejection
  const dupRes = await fetch(`${BASE_URL}/admin/exam-windows`, {
    method: 'POST',
    headers,
    body: JSON.stringify({
      examId: targetExamId,
      windowCode: code,
      title: 'Duplicate Window Code Test',
      mode: 'PRACTICE'
    })
  });
  assert(dupRes.status === 409, `Duplicate windowCode returns 409 Conflict (got ${dupRes.status})`);

  // 5. Invalid timing rejection
  const invalidTimeRes = await fetch(`${BASE_URL}/admin/exam-windows`, {
    method: 'POST',
    headers,
    body: JSON.stringify({
      examId: targetExamId,
      windowCode: `${code}_ERR`,
      title: 'Invalid Timing Window',
      mode: 'SCHEDULED_MOCK',
      startAt: endAt,
      endAt: startAt // inverted
    })
  });
  assert(invalidTimeRes.status === 400, `End before start returns 400 Bad Request (got ${invalidTimeRes.status})`);

  // 6. Get window by ID
  const getRes = await fetch(`${BASE_URL}/admin/exam-windows/${windowId}`, { headers });
  assert(getRes.ok, 'Get window by ID returned 200');
  const getJson = await getRes.json();
  assert(getJson.data.id === windowId, 'Window ID matches');

  // 7. Update window details
  const updateRes = await fetch(`${BASE_URL}/admin/exam-windows/${windowId}`, {
    method: 'PUT',
    headers,
    body: JSON.stringify({
      examId: targetExamId,
      windowCode: code,
      title: 'Ca thi E2E Test Runner (Updated)',
      mode: 'SCHEDULED_MOCK',
      startAt,
      endAt,
      gracePeriodMinutes: 20,
      maxAttempts: 2,
      resultPolicy: 'IMMEDIATE_FULL'
    })
  });
  assert(updateRes.ok, 'Update window returned 200');
  const updateJson = await updateRes.json();
  assert(updateJson.data.title === 'Ca thi E2E Test Runner (Updated)', 'Updated title matches');
  assert(updateJson.data.gracePeriodMinutes === 20, 'Updated grace period matches');

  // 8. Quick update status to RUNNING
  const patchRunning = await fetch(`${BASE_URL}/admin/exam-windows/${windowId}/status?status=RUNNING`, {
    method: 'PATCH',
    headers
  });
  assert(patchRunning.ok, 'Patch status to RUNNING returned 200');
  const patchRunningJson = await patchRunning.json();
  assert(patchRunningJson.data.status === 'RUNNING', 'Status updated to RUNNING');

  // 9. Quick update status to CLOSED
  const patchClosed = await fetch(`${BASE_URL}/admin/exam-windows/${windowId}/status?status=CLOSED`, {
    method: 'PATCH',
    headers
  });
  assert(patchClosed.ok, 'Patch status to CLOSED returned 200');
  const patchClosedJson = await patchClosed.json();
  assert(patchClosedJson.data.status === 'CLOSED', 'Status updated to CLOSED');

  // 10. Filter list by status
  const filterRes = await fetch(`${BASE_URL}/admin/exam-windows?status=CLOSED`, { headers });
  assert(filterRes.ok, 'Filter by CLOSED status returned 200');
  const filterJson = await filterRes.json();
  assert(filterJson.data.some(w => w.id === windowId), 'Window present in CLOSED list');

  // 11. Delete window
  const deleteRes = await fetch(`${BASE_URL}/admin/exam-windows/${windowId}`, {
    method: 'DELETE',
    headers
  });
  assert(deleteRes.ok, 'Delete window returned 200');

  // 12. Verify deleted
  const getDeleted = await fetch(`${BASE_URL}/admin/exam-windows/${windowId}`, { headers });
  assert(getDeleted.status === 404, 'Get deleted window returned 404 Not Found');

  console.log(`\ní ¼í¾¯ ALL CHECKS PASSED: ${passed}/${total} in test-exam-windows.mjs\n`);
}

main().catch(err => {
  console.error('Test execution error:', err);
  process.exit(1);
});
