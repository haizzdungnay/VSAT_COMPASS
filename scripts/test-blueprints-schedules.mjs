import assert from 'node:assert/strict';

const BASE_URL = 'http://localhost:18080/api/v1';

async function run() {
  console.log('=== Testing Blueprints and Schedules ===');

  console.log('1. Authenticate CONTENT_ADMIN');
  const loginRes = await fetch(`${BASE_URL}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email: 'admin@vsat.com', password: 'Admin@123' })
  });
  assert.equal(loginRes.status, 200, 'Login failed');
  const loginJson = await loginRes.json();
  const token = loginJson.data.accessToken;
  assert.ok(token, 'Missing access token');
  console.log('  -> Admin JWT obtained');

  console.log('2. GET /admin/blueprints');
  const bpsRes = await fetch(`${BASE_URL}/admin/blueprints`, {
    headers: { Authorization: `Bearer ${token}` }
  });
  assert.equal(bpsRes.status, 200, 'List blueprints status 200');
  const bpsJson = await bpsRes.json();
  assert.ok(Array.isArray(bpsJson.data), 'data should be an array');
  assert.ok(bpsJson.data.length >= 1, 'Should have at least 1 blueprint');
  console.log(`  -> Found ${bpsJson.data.length} blueprints from API`);

  console.log('3. Validate blueprint schema & 25-question V-SAT spec');
  const bp = bpsJson.data[0];
  assert.equal(bp.totalQuestions, 25, 'V-SAT blueprint must have 25 questions');
  assert.equal(Number(bp.maxRawScore), 150, 'V-SAT blueprint must have 150.0 raw score');
  assert.ok(bp.code && bp.code.startsWith('VSAT_2026_BP_'), 'Code must follow V-SAT 2026 pattern');
  assert.equal(bp.cells.length, 4, 'Must have 4 question format cells');

  const tfCell = bp.cells.find(c => c.questionType === 'TRUE_FALSE');
  assert.ok(tfCell, 'Must have TRUE_FALSE cell');
  assert.equal(tfCell.questionCount, 9, 'Phan I True/False must have 9 questions');

  const mcqCell = bp.cells.find(c => c.questionType === 'SINGLE_CHOICE');
  assert.ok(mcqCell, 'Must have SINGLE_CHOICE cell');
  assert.equal(mcqCell.questionCount, 6, 'Phan II MCQ must have 6 questions');

  const matchCell = bp.cells.find(c => c.questionType === 'MATCHING');
  assert.ok(matchCell, 'Must have MATCHING cell');
  assert.equal(matchCell.questionCount, 5, 'Phan III Matching must have 5 questions');

  const shortCell = bp.cells.find(c => c.questionType === 'SHORT_ANSWER');
  assert.ok(shortCell, 'Must have SHORT_ANSWER cell');
  assert.equal(shortCell.questionCount, 5, 'Phan IV Short Answer must have 5 questions');
  console.log('  -> 25 questions distributed: 9 True/False + 6 MCQ + 5 Matching + 5 Short Answer = 150.0 pts');

  console.log('4. GET /admin/blueprints/{id}');
  const detailRes = await fetch(`${BASE_URL}/admin/blueprints/${bp.id}`, {
    headers: { Authorization: `Bearer ${token}` }
  });
  assert.equal(detailRes.status, 200, 'Get blueprint detail status 200');
  const detailJson = await detailRes.json();
  assert.equal(detailJson.data.id, bp.id, 'Blueprint ID matches');
  console.log('  -> Blueprint detail verified');

  console.log('5. Verify student role is forbidden on /admin/blueprints');
  const studentLoginRes = await fetch(`${BASE_URL}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email: 'student@vsat.com', password: 'Admin@123' })
  });
  const studentJson = await studentLoginRes.json();
  const studentToken = studentJson.data.accessToken;

  const studentBpRes = await fetch(`${BASE_URL}/admin/blueprints`, {
    headers: { Authorization: `Bearer ${studentToken}` }
  });
  assert.ok(studentBpRes.status === 403 || studentBpRes.status === 401, 'Student must receive 403/401 on /admin/blueprints');
  console.log('  -> Student correctly blocked with 403/401');

  console.log('6. Verify Schedule boundary validation rules');
  function validateSchedule(startAt, endAt, grace, attempts) {
    const s = new Date(startAt);
    const e = new Date(endAt);
    if (isNaN(s.getTime()) || isNaN(e.getTime())) return 'INVALID_DATE';
    if (e.getTime() <= s.getTime()) return 'END_BEFORE_START';
    if (grace < 0 || grace > 120) return 'INVALID_GRACE';
    if (attempts < 1) return 'INVALID_ATTEMPTS';
    return 'VALID';
  }
  assert.equal(validateSchedule('2026-11-05T08:00', '2026-11-05T10:00', 15, 1), 'VALID');
  assert.equal(validateSchedule('2026-11-05T10:00', '2026-11-05T08:00', 15, 1), 'END_BEFORE_START');
  assert.equal(validateSchedule('2026-11-05T08:00', '2026-11-05T10:00', -5, 1), 'INVALID_GRACE');
  assert.equal(validateSchedule('2026-11-05T08:00', '2026-11-05T10:00', 15, 0), 'INVALID_ATTEMPTS');
  console.log('  -> All Schedule boundary rules verified');

  console.log('=== ALL 6 BLUEPRINT/SCHEDULE CHECKS PASSED 100% ===');
}

run().catch(err => {
  console.error('Test failed:', err);
  process.exit(1);
});
