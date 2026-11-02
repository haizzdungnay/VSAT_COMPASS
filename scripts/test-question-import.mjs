import assert from 'node:assert/strict';

const BASE_URL = 'http://localhost:18080/api/v1';

async function run() {
  console.log('--- 1. Login as CONTENT_ADMIN ---');
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

  console.log('--- 2. GET /admin/questions/import/template ---');
  const tplRes = await fetch(`${BASE_URL}/admin/questions/import/template`, {
    headers: { Authorization: `Bearer ${token}` }
  });
  assert.equal(tplRes.status, 200, 'Template download status 200');
  const disposition = tplRes.headers.get('content-disposition');
  assert.ok(disposition && disposition.includes('vsat_question_import_template.xlsx'), 'Missing attachment header');
  const contentType = tplRes.headers.get('content-type');
  assert.ok(contentType && contentType.includes('spreadsheetml'), 'Content type must be spreadsheetml');

  const arrayBuffer = await tplRes.arrayBuffer();
  const buffer = Buffer.from(arrayBuffer);
  assert.ok(buffer.length > 1000, 'Template buffer size too small');
  assert.equal(buffer[0], 0x50, 'Not a valid XLSX/ZIP file');
  assert.equal(buffer[1], 0x4B, 'Not a valid XLSX/ZIP file');
  console.log(`  -> Downloaded valid template (.xlsx), size = ${buffer.length} bytes`);

  console.log('--- 3. Negative check: Non-xlsx file rejected ---');
  const formBadExt = new FormData();
  formBadExt.append('file', new Blob(['not an excel'], { type: 'text/plain' }), 'hack.txt');
  const badExtRes = await fetch(`${BASE_URL}/admin/questions/import?dryRun=true`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}` },
    body: formBadExt
  });
  assert.equal(badExtRes.status, 400, 'Non-xlsx file should be rejected with 400');
  console.log('  -> Correctly rejected .txt file with HTTP 400');

  console.log('--- 4. POST /admin/questions/import?dryRun=true (Preview Valid Template) ---');
  const formDryRun = new FormData();
  formDryRun.append('file', new Blob([buffer], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' }), 'template.xlsx');

  const previewRes = await fetch(`${BASE_URL}/admin/questions/import?dryRun=true`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}` },
    body: formDryRun
  });
  assert.equal(previewRes.status, 200, 'Preview status 200');
  const previewJson = await previewRes.json();
  assert.ok(previewJson.data, 'Missing data payload');
  assert.equal(previewJson.data.dryRun, true, 'dryRun must be true');
  assert.equal(previewJson.data.totalRows, 3, 'Expected 3 template rows (1 single + 2 group)');
  assert.equal(previewJson.data.validCount, 3, 'All 3 rows should be valid');
  assert.equal(previewJson.data.errorCount, 0, 'No errors expected');
  assert.equal(previewJson.data.importedQuestions, 0, 'No questions should be imported during dry run');
  console.log('  -> Preview passed 3/3 valid items, 0 errors, 0 DB inserts');

  console.log('--- 5. POST /admin/questions/import?dryRun=false (Commit Valid Template) ---');
  const formCommit = new FormData();
  formCommit.append('file', new Blob([buffer], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' }), 'template.xlsx');

  const commitRes = await fetch(`${BASE_URL}/admin/questions/import?dryRun=false`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}` },
    body: formCommit
  });
  assert.equal(commitRes.status, 200, 'Commit status 200');
  const commitJson = await commitRes.json();
  assert.ok(commitJson.data, 'Missing commit data');
  assert.equal(commitJson.data.dryRun, false, 'dryRun must be false');
  assert.equal(commitJson.data.importedQuestions, 3, 'Should import 3 questions');
  assert.ok(commitJson.data.createdGroups >= 0, 'createdGroups is non-negative');
  console.log(`  -> Successfully committed: ${commitJson.data.importedQuestions} questions, ${commitJson.data.createdGroups} group(s)`);

  console.log('--- 6. Verify imported questions in GET /admin/questions ---');
  const listRes = await fetch(`${BASE_URL}/admin/questions?status=APPROVED&size=10`, {
    headers: { Authorization: `Bearer ${token}` }
  });
  assert.equal(listRes.status, 200, 'List questions status 200');
  const listJson = await listRes.json();
  assert.ok(listJson.data.content.length >= 3, 'Expected at least 3 approved questions');
  console.log(`  -> Found ${listJson.data.content.length} approved questions in repository`);

  console.log('--- 7. Security: Student role forbidden on import endpoints ---');
  const studentLoginRes = await fetch(`${BASE_URL}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email: 'student@vsat.com', password: 'Admin@123' })
  });
  const studentJson = await studentLoginRes.json();
  const studentToken = studentJson.data.accessToken;

  const formStudent = new FormData();
  formStudent.append('file', new Blob([buffer], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' }), 'template.xlsx');

  const studentImportRes = await fetch(`${BASE_URL}/admin/questions/import?dryRun=true`, {
    method: 'POST',
    headers: { Authorization: `Bearer ${studentToken}` },
    body: formStudent
  });
  assert.ok(studentImportRes.status === 403 || studentImportRes.status === 401, 'Student must receive 403/401 on import');
  console.log('  -> Student correctly blocked with HTTP 403 Forbidden');

  console.log('=== ALL 7 AUDIT CHECKS PASSED 100% ===');
}

run().catch(err => {
  console.error('Test failed:', err);
  process.exit(1);
});
