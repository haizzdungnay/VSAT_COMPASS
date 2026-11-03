import { getAuthToken } from '../../lib/test-harness.mjs';

/**
 * Helper utilities for Admin Backend Acceptance Suite (API-001 -> API-042).
 * Provides deterministic DB fixtures, RBAC verifications, and Excel generation.
 */

export async function verifyStandardRbac({
  apiCall,
  assertStatus,
  path,
  method = 'GET',
  body = null,
  superAdminOnly = false
}) {
  // 1. Unauthenticated request must return 401
  const anonRes = await apiCall(path, { method, body });
  assertStatus(anonRes.status, 401, `Unauthenticated ${method} ${path} must return 401`);

  // 2. Student role must return 403
  const studentRes = await apiCall(path, { method, body }, 'STUDENT');
  assertStatus(studentRes.status, 403, `STUDENT role on ${method} ${path} must return 403`);

  // 3. Collaborator role must return 403
  const collabRes = await apiCall(path, { method, body }, 'COLLABORATOR');
  assertStatus(collabRes.status, 403, `COLLABORATOR role on ${method} ${path} must return 403`);

  // 4. If Super Admin only, Content Admin must return 403
  if (superAdminOnly) {
    const contentAdminRes = await apiCall(path, { method, body }, 'CONTENT_ADMIN');
    assertStatus(contentAdminRes.status, 403, `CONTENT_ADMIN on SUPER_ADMIN-only ${method} ${path} must return 403`);
  }
}

function parseId(str) {
  if (!str) return 0;
  const match = String(str).match(/\d+/);
  return match ? parseInt(match[0], 10) : 0;
}

export function getSubjectId(queryDb) {
  const result = queryDb("SELECT id FROM subjects WHERE code = 'MATH' LIMIT 1;");
  return parseId(result) || 1;
}

export function getBlueprintId(queryDb) {
  const result = queryDb("SELECT id FROM exam_blueprints WHERE code = 'VSAT_2026_BP_MATH' LIMIT 1;");
  if (!result || !result.trim()) {
    // Fallback: query any blueprint
    const fallback = queryDb("SELECT id FROM exam_blueprints LIMIT 1;");
    return parseId(fallback) || 1;
  }
  return parseId(result);
}

export function ensureDraftExam(queryDb, code) {
  const sql = `
    INSERT INTO exams (
      exam_code, title, subject_id, scoring_profile_id, question_count, duration_minutes,
      difficulty, pricing_type, price, status, created_by, version
    )
    SELECT
      '${code}', 'Fixture Exam ${code}', s.id, sp.id, 0, 60,
      'MEDIUM', 'FREE', 0, 'DRAFT', u.id, 1
    FROM subjects s, scoring_profiles sp, users u
    WHERE s.code = 'MATH' AND sp.code = 'VSAT_2026_DRAFT_SPEC_150' AND u.email = 'superadmin@vsat.com'
    ON CONFLICT (exam_code) DO UPDATE SET status = 'DRAFT'
    RETURNING id;
  `;
  const res = queryDb(sql);
  return parseId(res);
}

export function ensureExamWithStatus(queryDb, code, status) {
  const sql = `
    INSERT INTO exams (
      exam_code, title, subject_id, scoring_profile_id, question_count, duration_minutes,
      difficulty, pricing_type, price, status, created_by, version
    )
    SELECT
      '${code}', 'Fixture Exam ${code}', s.id, sp.id, 1, 60,
      'MEDIUM', 'FREE', 0, '${status}', u.id, 1
    FROM subjects s, scoring_profiles sp, users u
    WHERE s.code = 'MATH' AND sp.code = 'VSAT_2026_DRAFT_SPEC_150' AND u.email = 'superadmin@vsat.com'
    ON CONFLICT (exam_code) DO UPDATE SET status = '${status}'
    RETURNING id;
  `;
  const res = queryDb(sql);
  const examId = parseId(res);

  // Link at least one question so publish/details have data
  const questionId = ensurePendingQuestion(queryDb, `Q_${code}_ATTACHED`, 'APPROVED');
  queryDb(`
    INSERT INTO exam_questions (exam_id, question_id, question_order, max_raw_score)
    VALUES (${examId}, ${questionId}, 1, 6.00)
    ON CONFLICT (exam_id, question_id) DO NOTHING;
    UPDATE exams SET question_count = (SELECT count(*) FROM exam_questions WHERE exam_id = ${examId}) WHERE id = ${examId};
  `);
  return examId;
}

export function ensurePendingQuestion(queryDb, code, status = 'PENDING_REVIEW') {
  const sql = `
    INSERT INTO questions (
      question_code, subject_id, topic_id, difficulty, cognitive_level, question_type,
      question_text, question_html, explanation, status, created_by, version
    )
    SELECT
      '${code}', s.id, t.id, 'MEDIUM', 'APPLICATION', 'SINGLE_CHOICE',
      'Nội dung câu hỏi ${code}', '<p>Nội dung</p>', 'Lời giải', '${status}', u.id, 1
    FROM subjects s, topics t, users u
    WHERE s.code = 'MATH' AND t.code = 'MATH_ALG' AND u.email = 'contentadmin@vsat.com'
    ON CONFLICT (question_code) DO UPDATE SET status = '${status}'
    RETURNING id;
  `;
  const res = queryDb(sql);
  const qId = parseId(res);

  queryDb(`
    INSERT INTO question_options (question_id, option_label, option_text, option_html, is_correct, display_order)
    SELECT ${qId}, 'A', 'Đáp án A', 'Đáp án A', TRUE, 1
    ON CONFLICT DO NOTHING;
  `);

  return qId;
}

export function ensureFixtureTicket(queryDb) {
  const existing = queryDb("SELECT id FROM tickets WHERE message = 'Fixture ticket message for admin test' LIMIT 1;");
  if (existing && existing.trim()) {
    const exId = parseId(existing);
    queryDb(`UPDATE tickets SET status = 'OPEN' WHERE id = ${exId};`);
    return exId;
  }

  const sql = `
    INSERT INTO tickets (question_id, reporter_user_id, message, status, resolver_note)
    SELECT NULL, u.id, 'Fixture ticket message for admin test', 'OPEN', NULL
    FROM users u WHERE u.email = 'student@vsat.com'
    RETURNING id;
  `;
  const res = queryDb(sql);
  return parseId(res);
}

export function ensureFixtureExamWindow(queryDb, examId, windowCode = 'WIN_TMP_ADM_001') {
  const sql = `
    INSERT INTO exam_windows (
      exam_id, window_code, title, mode, start_at, end_at,
      grace_period_minutes, max_attempts, result_policy, hard_stop_at_end, allow_offline
    ) VALUES (
      ${examId}, '${windowCode}', 'Ca thi thử nghiệm', 'SCHEDULED_MOCK', now() + interval '1 hour', now() + interval '3 hours',
      0, 1, 'IMMEDIATE_FULL', FALSE, FALSE
    )
    ON CONFLICT (exam_id, window_code) DO NOTHING
    RETURNING id;
  `;
  let res = queryDb(sql);
  if (!res || !res.trim()) {
    res = queryDb(`SELECT id FROM exam_windows WHERE exam_id = ${examId} AND window_code = '${windowCode}' LIMIT 1;`);
  }
  return parseId(res);
}

export function ensureTmpUser(queryDb) {
  const sql = `
    INSERT INTO users (
      email, password_hash, full_name, phone, role, status, email_verified, leaderboard_opt_in
    ) VALUES (
      'tmp_adm_target_user@vsat.com',
      '$2a$10$CMuCUmiHOoGa20tx96R20e8JZgaCThRwYJxWNCU3anO1QecLhCXcW',
      'Người dùng tạm Admin Test',
      '0999999999',
      'STUDENT',
      'ACTIVE',
      TRUE,
      FALSE
    )
    ON CONFLICT (email) DO UPDATE SET role = 'STUDENT', status = 'ACTIVE'
    RETURNING id;
  `;
  const res = queryDb(sql);
  return parseId(res);
}

export async function getValidExcelFile(apiCall) {
  const token = await getAuthToken('CONTENT_ADMIN');
  const rawBase = (process.env.API_BASE_URL || 'http://127.0.0.1:18080').replace(/\/api\/v1\/?$/, '').replace(/\/+$/, '');
  const res = await fetch(`${rawBase}/api/v1/admin/questions/import/template`, {
    headers: { Authorization: `Bearer ${token}` }
  });
  if (res.ok) {
    const ab = await res.arrayBuffer();
    return Buffer.from(ab);
  }
  return Buffer.from([0x50, 0x4b, 0x05, 0x06, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00]);
}
