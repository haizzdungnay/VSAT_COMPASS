#!/usr/bin/env node
/**
 * Clean-Room Acceptance Test Suite: Backend Admin Endpoints (API-001 -> API-042).
 * Verifies RBAC, business workflows, DB persistence, negative boundaries, and import handling.
 */

import { registerCase, runSuite } from '../lib/test-harness.mjs';
import {
  verifyStandardRbac,
  getSubjectId,
  getBlueprintId,
  ensureDraftExam,
  ensureExamWithStatus,
  ensurePendingQuestion,
  ensureFixtureTicket,
  ensureFixtureExamWindow,
  ensureTmpUser,
  getValidExcelFile
} from './helpers/backend-admin-helpers.mjs';

// ============================================================================
// BLUEPRINT MANAGEMENT (API-001, API-002)
// ============================================================================

registerCase('API-001', 'AdminBlueprintController GET /api/v1/admin/blueprints', async ({ apiCall, assertStatus, assertTrue, queryDb }) => {
  await verifyStandardRbac({ apiCall, assertStatus, path: '/api/v1/admin/blueprints' });
  const res = await apiCall('/api/v1/admin/blueprints', {}, 'CONTENT_ADMIN');
  assertStatus(res.status, 200);
  assertTrue(Array.isArray(res.json?.data));
  const count = Number(queryDb('SELECT count(*) FROM exam_blueprints;'));
  assertTrue(count >= res.json.data.length);
});

registerCase('API-002', 'AdminBlueprintController GET /api/v1/admin/blueprints/{id}', async ({ apiCall, assertStatus, assertEqual, queryDb }) => {
  const bpId = getBlueprintId(queryDb);
  await verifyStandardRbac({ apiCall, assertStatus, path: `/api/v1/admin/blueprints/${bpId}` });
  const res = await apiCall(`/api/v1/admin/blueprints/${bpId}`, {}, 'CONTENT_ADMIN');
  assertStatus(res.status, 200);
  assertEqual(res.json?.data?.id, bpId);
  const neg = await apiCall('/api/v1/admin/blueprints/999999', {}, 'CONTENT_ADMIN');
  assertStatus(neg.status, 404);
});

// ============================================================================
// DASHBOARD STATS (API-003)
// ============================================================================

registerCase('API-003', 'AdminDashboardController GET /api/v1/admin/stats', async ({ apiCall, assertStatus, assertTrue, queryDb }) => {
  await verifyStandardRbac({ apiCall, assertStatus, path: '/api/v1/admin/stats' });
  const res = await apiCall('/api/v1/admin/stats', {}, 'CONTENT_ADMIN');
  assertStatus(res.status, 200);
  assertTrue(res.json?.data != null && typeof res.json.data === 'object');
  const userCount = Number(queryDb('SELECT count(*) FROM users;'));
  assertTrue(userCount >= 4);
});

// ============================================================================
// EXAM LIFECYCLE & COMPOSITION (API-004 -> API-022)
// ============================================================================

registerCase('API-004', 'AdminExamController GET /api/v1/admin/exams', async ({ apiCall, assertStatus, assertTrue }) => {
  await verifyStandardRbac({ apiCall, assertStatus, path: '/api/v1/admin/exams' });
  const res = await apiCall('/api/v1/admin/exams', {}, 'CONTENT_ADMIN');
  assertStatus(res.status, 200);
  assertTrue(Array.isArray(res.json?.data?.content));
  const neg = await apiCall('/api/v1/admin/exams?status=BOGUS_STATUS', {}, 'CONTENT_ADMIN');
  assertStatus(neg.status, 400);
});

registerCase('API-005', 'AdminExamController GET /api/v1/admin/exams/{id}', async ({ apiCall, assertStatus, assertEqual, queryDb }) => {
  const examId = ensureDraftExam(queryDb, 'ADM_GET_EX_01');
  await verifyStandardRbac({ apiCall, assertStatus, path: `/api/v1/admin/exams/${examId}` });
  const res = await apiCall(`/api/v1/admin/exams/${examId}`, {}, 'CONTENT_ADMIN');
  assertStatus(res.status, 200);
  assertEqual(res.json?.data?.id, examId);
  const neg = await apiCall('/api/v1/admin/exams/999999', {}, 'CONTENT_ADMIN');
  assertStatus(neg.status, 404);
});

registerCase('API-006', 'AdminExamController POST /api/v1/admin/exams', async ({ apiCall, assertStatus, assertEqual, queryDb }) => {
  const subjId = getSubjectId(queryDb);
  const code = 'ADM_CR_EX_' + Date.now().toString().slice(-6);
  const body = {
    examCode: code,
    title: 'Đề thi tạo mới test API-006',
    subjectId: subjId,
    durationMinutes: 90,
    difficulty: 'MEDIUM',
    pricingType: 'FREE',
    price: 0
  };
  await verifyStandardRbac({ apiCall, assertStatus, path: '/api/v1/admin/exams', method: 'POST', body });
  const res = await apiCall('/api/v1/admin/exams', { method: 'POST', body }, 'CONTENT_ADMIN');
  assertStatus(res.status, 201);
  assertEqual(res.json?.data?.examCode, code);
  const exists = queryDb(`SELECT count(*) FROM exams WHERE exam_code = '${code}';`);
  assertEqual(exists.trim(), '1');
  const neg = await apiCall('/api/v1/admin/exams', { method: 'POST', body: {} }, 'CONTENT_ADMIN');
  assertStatus(neg.status, 400);
});

registerCase('API-007', 'AdminExamController PUT /api/v1/admin/exams/{id}', async ({ apiCall, assertStatus, assertEqual, queryDb }) => {
  const examId = ensureDraftExam(queryDb, 'ADM_PUT_EX_01');
  const body = { title: 'Tiêu đề cập nhật API-007', durationMinutes: 75 };
  await verifyStandardRbac({ apiCall, assertStatus, path: `/api/v1/admin/exams/${examId}`, method: 'PUT', body });
  const res = await apiCall(`/api/v1/admin/exams/${examId}`, { method: 'PUT', body }, 'CONTENT_ADMIN');
  assertStatus(res.status, 200);
  assertEqual(res.json?.data?.title, body.title);
  const neg = await apiCall('/api/v1/admin/exams/999999', { method: 'PUT', body }, 'CONTENT_ADMIN');
  assertStatus(neg.status, 404);
});

registerCase('API-008', 'AdminExamController DELETE /api/v1/admin/exams/{examId}', async ({ apiCall, assertStatus, assertEqual, queryDb }) => {
  const examId = ensureDraftExam(queryDb, 'ADM_DEL_EX_01');
  await verifyStandardRbac({ apiCall, assertStatus, path: `/api/v1/admin/exams/${examId}`, method: 'DELETE' });
  const res = await apiCall(`/api/v1/admin/exams/${examId}`, { method: 'DELETE' }, 'CONTENT_ADMIN');
  assertStatus(res.status, 200);
  const exists = queryDb(`SELECT count(*) FROM exams WHERE id = ${examId};`);
  assertEqual(exists.trim(), '0');
  const neg = await apiCall('/api/v1/admin/exams/999999', { method: 'DELETE' }, 'CONTENT_ADMIN');
  assertStatus(neg.status, 404);
});

registerCase('API-009', 'AdminExamController POST /api/v1/admin/exams/{examId}/questions', async ({ apiCall, assertStatus, assertEqual, queryDb }) => {
  const examId = ensureDraftExam(queryDb, 'ADM_ADD_Q_EX_01');
  const qId = ensurePendingQuestion(queryDb, 'Q_ADM_ADD_01', 'APPROVED');
  const body = { questionId: qId };
  await verifyStandardRbac({ apiCall, assertStatus, path: `/api/v1/admin/exams/${examId}/questions`, method: 'POST', body });
  const res = await apiCall(`/api/v1/admin/exams/${examId}/questions`, { method: 'POST', body }, 'CONTENT_ADMIN');
  assertStatus(res.status, 200);
  const count = queryDb(`SELECT count(*) FROM exam_questions WHERE exam_id = ${examId} AND question_id = ${qId};`);
  assertEqual(count.trim(), '1');
  const neg = await apiCall(`/api/v1/admin/exams/${examId}/questions`, { method: 'POST', body: {} }, 'CONTENT_ADMIN');
  assertStatus(neg.status, 400);
});

registerCase('API-010', 'AdminExamController DELETE /api/v1/admin/exams/{examId}/questions/{questionId}', async ({ apiCall, assertStatus, assertEqual, queryDb }) => {
  const examId = ensureDraftExam(queryDb, 'ADM_RM_Q_EX_01');
  const qId = ensurePendingQuestion(queryDb, 'Q_ADM_RM_01', 'APPROVED');
  queryDb(`INSERT INTO exam_questions (exam_id, question_id, question_order, max_raw_score) VALUES (${examId}, ${qId}, 1, 6.00) ON CONFLICT DO NOTHING;`);
  await verifyStandardRbac({ apiCall, assertStatus, path: `/api/v1/admin/exams/${examId}/questions/${qId}`, method: 'DELETE' });
  const res = await apiCall(`/api/v1/admin/exams/${examId}/questions/${qId}`, { method: 'DELETE' }, 'CONTENT_ADMIN');
  assertStatus(res.status, 200);
  const count = queryDb(`SELECT count(*) FROM exam_questions WHERE exam_id = ${examId} AND question_id = ${qId};`);
  assertEqual(count.trim(), '0');
  const neg = await apiCall(`/api/v1/admin/exams/${examId}/questions/999999`, { method: 'DELETE' }, 'CONTENT_ADMIN');
  assertStatus(neg.status, 404);
});

registerCase('API-011', 'AdminExamController PUT /api/v1/admin/exams/{examId}/questions/reorder', async ({ apiCall, assertStatus, queryDb }) => {
  const examId = ensureDraftExam(queryDb, 'ADM_REORDER_EX_01');
  const q1 = ensurePendingQuestion(queryDb, 'Q_ADM_REORDER_01', 'APPROVED');
  const q2 = ensurePendingQuestion(queryDb, 'Q_ADM_REORDER_02', 'APPROVED');
  queryDb(`
    INSERT INTO exam_questions (exam_id, question_id, question_order, max_raw_score) VALUES (${examId}, ${q1}, 1, 6.00) ON CONFLICT DO NOTHING;
    INSERT INTO exam_questions (exam_id, question_id, question_order, max_raw_score) VALUES (${examId}, ${q2}, 2, 6.00) ON CONFLICT DO NOTHING;
  `);
  const body = { questionIds: [q2, q1] };
  await verifyStandardRbac({ apiCall, assertStatus, path: `/api/v1/admin/exams/${examId}/questions/reorder`, method: 'PUT', body });
  const res = await apiCall(`/api/v1/admin/exams/${examId}/questions/reorder`, { method: 'PUT', body }, 'CONTENT_ADMIN');
  assertStatus(res.status, 200);
  const neg = await apiCall(`/api/v1/admin/exams/${examId}/questions/reorder`, { method: 'PUT', body: { questionIds: [999999] } }, 'CONTENT_ADMIN');
  assertStatus(neg.status, 400);
});

registerCase('API-012', 'AdminExamController POST /api/v1/admin/exams/{examId}/submit-review', async ({ apiCall, assertStatus, assertEqual, queryDb }) => {
  const examId = ensureDraftExam(queryDb, 'ADM_SUBMIT_EX_01');
  await verifyStandardRbac({ apiCall, assertStatus, path: `/api/v1/admin/exams/${examId}/submit-review`, method: 'POST' });
  const res = await apiCall(`/api/v1/admin/exams/${examId}/submit-review`, { method: 'POST' }, 'CONTENT_ADMIN');
  assertStatus(res.status, 200);
  assertEqual(res.json?.data?.status, 'PENDING_REVIEW');
  const neg = await apiCall(`/api/v1/admin/exams/${examId}/submit-review`, { method: 'POST' }, 'CONTENT_ADMIN');
  assertStatus(neg.status, 409);
});

registerCase('API-013', 'AdminExamController POST /api/v1/admin/exams/{examId}/publish', async ({ apiCall, assertStatus, assertEqual, queryDb }) => {
  const examId = ensureExamWithStatus(queryDb, 'ADM_PUB_EX_01', 'PENDING_REVIEW');
  await verifyStandardRbac({ apiCall, assertStatus, path: `/api/v1/admin/exams/${examId}/publish`, method: 'POST', superAdminOnly: true });
  const res = await apiCall(`/api/v1/admin/exams/${examId}/publish`, { method: 'POST' }, 'SUPER_ADMIN');
  assertStatus(res.status, 200);
  assertEqual(res.json?.data?.status, 'PUBLISHED');
  const neg = await apiCall('/api/v1/admin/exams/999999/publish', { method: 'POST' }, 'SUPER_ADMIN');
  assertStatus(neg.status, 404);
});

registerCase('API-014', 'AdminExamController POST /api/v1/admin/exams/{examId}/hide', async ({ apiCall, assertStatus, assertEqual, queryDb }) => {
  const examId = ensureExamWithStatus(queryDb, 'ADM_HIDE_EX_01', 'PUBLISHED');
  await verifyStandardRbac({ apiCall, assertStatus, path: `/api/v1/admin/exams/${examId}/hide`, method: 'POST' });
  const res = await apiCall(`/api/v1/admin/exams/${examId}/hide`, { method: 'POST' }, 'CONTENT_ADMIN');
  assertStatus(res.status, 200);
  assertEqual(res.json?.data?.status, 'HIDDEN');
  const neg = await apiCall('/api/v1/admin/exams/999999/hide', { method: 'POST' }, 'CONTENT_ADMIN');
  assertStatus(neg.status, 404);
});

registerCase('API-015', 'AdminExamController POST /api/v1/admin/exams/{examId}/archive', async ({ apiCall, assertStatus, assertEqual, queryDb }) => {
  const examId = ensureExamWithStatus(queryDb, 'ADM_ARCH_EX_01', 'PUBLISHED');
  await verifyStandardRbac({ apiCall, assertStatus, path: `/api/v1/admin/exams/${examId}/archive`, method: 'POST' });
  const res = await apiCall(`/api/v1/admin/exams/${examId}/archive`, { method: 'POST' }, 'CONTENT_ADMIN');
  assertStatus(res.status, 200);
  assertEqual(res.json?.data?.status, 'ARCHIVED');
  const neg = await apiCall('/api/v1/admin/exams/999999/archive', { method: 'POST' }, 'CONTENT_ADMIN');
  assertStatus(neg.status, 404);
});

registerCase('API-016', 'AdminExamController POST /api/v1/admin/exams/{examId}/reject-review', async ({ apiCall, assertStatus, assertEqual, queryDb }) => {
  const examId = ensureExamWithStatus(queryDb, 'ADM_REJ_EX_01', 'PENDING_REVIEW');
  await verifyStandardRbac({ apiCall, assertStatus, path: `/api/v1/admin/exams/${examId}/reject-review`, method: 'POST' });
  const res = await apiCall(`/api/v1/admin/exams/${examId}/reject-review`, { method: 'POST' }, 'CONTENT_ADMIN');
  assertStatus(res.status, 200);
  assertEqual(res.json?.data?.status, 'DRAFT');
  const neg = await apiCall('/api/v1/admin/exams/999999/reject-review', { method: 'POST' }, 'CONTENT_ADMIN');
  assertStatus(neg.status, 404);
});

registerCase('API-017', 'AdminExamController POST /api/v1/admin/exams/{examId}/return-to-draft', async ({ apiCall, assertStatus, assertEqual, queryDb }) => {
  const examId = ensureExamWithStatus(queryDb, 'ADM_RET_EX_01', 'HIDDEN');
  await verifyStandardRbac({ apiCall, assertStatus, path: `/api/v1/admin/exams/${examId}/return-to-draft`, method: 'POST' });
  const res = await apiCall(`/api/v1/admin/exams/${examId}/return-to-draft`, { method: 'POST' }, 'CONTENT_ADMIN');
  assertStatus(res.status, 200);
  assertEqual(res.json?.data?.status, 'DRAFT');
  const neg = await apiCall('/api/v1/admin/exams/999999/return-to-draft', { method: 'POST' }, 'CONTENT_ADMIN');
  assertStatus(neg.status, 404);
});

registerCase('API-018', 'AdminExamController GET /api/v1/admin/exams/{examId}/questions', async ({ apiCall, assertStatus, assertTrue, queryDb }) => {
  const examId = ensureExamWithStatus(queryDb, 'ADM_Q_LIST_EX_01', 'DRAFT');
  await verifyStandardRbac({ apiCall, assertStatus, path: `/api/v1/admin/exams/${examId}/questions` });
  const res = await apiCall(`/api/v1/admin/exams/${examId}/questions`, {}, 'CONTENT_ADMIN');
  assertStatus(res.status, 200);
  assertTrue(Array.isArray(res.json?.data));
  const count = queryDb(`SELECT count(*) FROM exam_questions WHERE exam_id = ${examId};`);
  assertTrue(Number(count.trim()) >= 1);
});

registerCase('API-019', 'AdminExamController GET /api/v1/admin/exams/{examId}/variants', async ({ apiCall, assertStatus, assertTrue, queryDb }) => {
  const examId = ensureDraftExam(queryDb, 'ADM_VAR_GET_EX_01');
  await verifyStandardRbac({ apiCall, assertStatus, path: `/api/v1/admin/exams/${examId}/variants` });
  const res = await apiCall(`/api/v1/admin/exams/${examId}/variants`, {}, 'CONTENT_ADMIN');
  assertStatus(res.status, 200);
  assertTrue(Array.isArray(res.json?.data));
  const neg = await apiCall('/api/v1/admin/exams/999999/variants', {}, 'CONTENT_ADMIN');
  assertStatus(neg.status, 404);
});

registerCase('API-020', 'AdminExamController POST /api/v1/admin/exams/{examId}/variants/generate', async ({ apiCall, assertStatus, assertEqual, queryDb }) => {
  const examId = ensureDraftExam(queryDb, 'ADM_VAR_GEN_EX_01');
  const body = { variantCount: 4, shuffleQuestions: true };
  await verifyStandardRbac({ apiCall, assertStatus, path: `/api/v1/admin/exams/${examId}/variants/generate`, method: 'POST', body });
  const res = await apiCall(`/api/v1/admin/exams/${examId}/variants/generate`, { method: 'POST', body }, 'CONTENT_ADMIN');
  assertStatus(res.status, 200);
  assertEqual(res.json?.data?.length, 4);
  const neg = await apiCall('/api/v1/admin/exams/999999/variants/generate', { method: 'POST', body }, 'CONTENT_ADMIN');
  assertStatus(neg.status, 404);
});

registerCase('API-021', 'AdminExamController POST /api/v1/admin/exams/generate-by-matrix', async ({ apiCall, assertStatus, assertTrue, queryDb }) => {
  const bpId = getBlueprintId(queryDb);
  const subjId = getSubjectId(queryDb);
  const code = 'ADM_MTX_' + Date.now().toString().slice(-6);
  const body = {
    blueprintId: bpId,
    variantCount: 2,
    exam: {
      examCode: code,
      title: 'Đề ma trận tự động API-021',
      subjectId: subjId,
      durationMinutes: 90,
      difficulty: 'HARD',
      pricingType: 'FREE',
      price: 0
    }
  };
  await verifyStandardRbac({ apiCall, assertStatus, path: '/api/v1/admin/exams/generate-by-matrix', method: 'POST', body });
  const res = await apiCall('/api/v1/admin/exams/generate-by-matrix', { method: 'POST', body }, 'CONTENT_ADMIN');
  assertStatus(res.status, 200);
  assertTrue(res.json?.data?.examId != null);
  const exists = queryDb(`SELECT count(*) FROM exams WHERE exam_code = '${code}';`);
  assertTrue(Number(exists.trim()) >= 1);
});

registerCase('API-022', 'AdminExamController POST /api/v1/admin/exams/{examId}/generate-by-matrix', async ({ apiCall, assertStatus, assertEqual, queryDb }) => {
  const examId = ensureDraftExam(queryDb, 'ADM_MTX_EX_02');
  const bpId = getBlueprintId(queryDb);
  const body = { blueprintId: bpId, variantCount: 2 };
  await verifyStandardRbac({ apiCall, assertStatus, path: `/api/v1/admin/exams/${examId}/generate-by-matrix`, method: 'POST', body });
  const res = await apiCall(`/api/v1/admin/exams/${examId}/generate-by-matrix`, { method: 'POST', body }, 'CONTENT_ADMIN');
  assertStatus(res.status, 200);
  assertEqual(res.json?.data?.examId, examId);
  const neg = await apiCall('/api/v1/admin/exams/999999/generate-by-matrix', { method: 'POST', body }, 'CONTENT_ADMIN');
  assertStatus(neg.status, 404);
});

// ============================================================================
// EXAM WINDOW SCHEDULES (API-023 -> API-028)
// ============================================================================

registerCase('API-023', 'AdminExamWindowController GET /api/v1/admin/exam-windows', async ({ apiCall, assertStatus, assertTrue, queryDb }) => {
  const examId = ensureDraftExam(queryDb, 'ADM_WIN_EX_01');
  ensureFixtureExamWindow(queryDb, examId, 'WIN_LIST_01');
  await verifyStandardRbac({ apiCall, assertStatus, path: '/api/v1/admin/exam-windows' });
  const res = await apiCall('/api/v1/admin/exam-windows', {}, 'CONTENT_ADMIN');
  assertStatus(res.status, 200);
  assertTrue(Array.isArray(res.json?.data));
});

registerCase('API-024', 'AdminExamWindowController GET /api/v1/admin/exam-windows/{id}', async ({ apiCall, assertStatus, assertEqual, queryDb }) => {
  const examId = ensureDraftExam(queryDb, 'ADM_WIN_EX_02');
  const winId = ensureFixtureExamWindow(queryDb, examId, 'WIN_GET_01');
  await verifyStandardRbac({ apiCall, assertStatus, path: `/api/v1/admin/exam-windows/${winId}` });
  const res = await apiCall(`/api/v1/admin/exam-windows/${winId}`, {}, 'CONTENT_ADMIN');
  assertStatus(res.status, 200);
  assertEqual(res.json?.data?.id, winId);
  const neg = await apiCall('/api/v1/admin/exam-windows/999999', {}, 'CONTENT_ADMIN');
  assertStatus(neg.status, 404);
});

registerCase('API-025', 'AdminExamWindowController POST /api/v1/admin/exam-windows', async ({ apiCall, assertStatus, assertEqual, queryDb }) => {
  const examId = ensureDraftExam(queryDb, 'ADM_WIN_EX_03');
  const code = 'WIN_CR_' + Date.now().toString().slice(-6);
  const body = {
    examId,
    windowCode: code,
    title: 'Ca thi tạo mới API-025',
    mode: 'PRACTICE',
    gracePeriodMinutes: 5,
    maxAttempts: 2
  };
  await verifyStandardRbac({ apiCall, assertStatus, path: '/api/v1/admin/exam-windows', method: 'POST', body });
  const res = await apiCall('/api/v1/admin/exam-windows', { method: 'POST', body }, 'CONTENT_ADMIN');
  assertStatus(res.status, 201);
  assertEqual(res.json?.data?.windowCode, code);
  const exists = queryDb(`SELECT count(*) FROM exam_windows WHERE window_code = '${code}';`);
  assertEqual(exists.trim(), '1');
  const neg = await apiCall('/api/v1/admin/exam-windows', { method: 'POST', body: {} }, 'CONTENT_ADMIN');
  assertStatus(neg.status, 400);
});

registerCase('API-026', 'AdminExamWindowController PUT /api/v1/admin/exam-windows/{id}', async ({ apiCall, assertStatus, assertEqual, queryDb }) => {
  const examId = ensureDraftExam(queryDb, 'ADM_WIN_EX_04');
  const winId = ensureFixtureExamWindow(queryDb, examId, 'WIN_UPD_01');
  const body = {
    examId,
    windowCode: 'WIN_UPD_01',
    title: 'Tiêu đề cập nhật API-026',
    mode: 'PRACTICE',
    gracePeriodMinutes: 10,
    maxAttempts: 3
  };
  await verifyStandardRbac({ apiCall, assertStatus, path: `/api/v1/admin/exam-windows/${winId}`, method: 'PUT', body });
  const res = await apiCall(`/api/v1/admin/exam-windows/${winId}`, { method: 'PUT', body }, 'CONTENT_ADMIN');
  assertStatus(res.status, 200);
  assertEqual(res.json?.data?.title, body.title);
  const neg = await apiCall('/api/v1/admin/exam-windows/999999', { method: 'PUT', body }, 'CONTENT_ADMIN');
  assertStatus(neg.status, 404);
});

registerCase('API-027', 'AdminExamWindowController PATCH /api/v1/admin/exam-windows/{id}/status', async ({ apiCall, assertStatus, assertEqual, queryDb }) => {
  const examId = ensureDraftExam(queryDb, 'ADM_WIN_EX_05');
  const winId = ensureFixtureExamWindow(queryDb, examId, 'WIN_STS_01');
  await verifyStandardRbac({ apiCall, assertStatus, path: `/api/v1/admin/exam-windows/${winId}/status?status=RUNNING`, method: 'PATCH' });
  const res = await apiCall(`/api/v1/admin/exam-windows/${winId}/status?status=RUNNING`, { method: 'PATCH' }, 'CONTENT_ADMIN');
  assertStatus(res.status, 200);
  assertEqual(res.json?.data?.status, 'RUNNING');
  const neg = await apiCall(`/api/v1/admin/exam-windows/${winId}/status?status=INVALID_STATUS`, { method: 'PATCH' }, 'CONTENT_ADMIN');
  assertStatus(neg.status, 400);
});

registerCase('API-028', 'AdminExamWindowController DELETE /api/v1/admin/exam-windows/{id}', async ({ apiCall, assertStatus, assertEqual, queryDb }) => {
  const examId = ensureDraftExam(queryDb, 'ADM_WIN_EX_06');
  const winId = ensureFixtureExamWindow(queryDb, examId, 'WIN_DEL_01');
  await verifyStandardRbac({ apiCall, assertStatus, path: `/api/v1/admin/exam-windows/${winId}`, method: 'DELETE' });
  const res = await apiCall(`/api/v1/admin/exam-windows/${winId}`, { method: 'DELETE' }, 'CONTENT_ADMIN');
  assertStatus(res.status, 200);
  const exists = queryDb(`SELECT count(*) FROM exam_windows WHERE id = ${winId};`);
  assertEqual(exists.trim(), '0');
  const neg = await apiCall('/api/v1/admin/exam-windows/999999', { method: 'DELETE' }, 'CONTENT_ADMIN');
  assertStatus(neg.status, 404);
});

// ============================================================================
// QUESTION BANK MANAGEMENT (API-029 -> API-036)
// ============================================================================

registerCase('API-029', 'AdminQuestionController GET /api/v1/admin/questions', async ({ apiCall, assertStatus, assertTrue }) => {
  await verifyStandardRbac({ apiCall, assertStatus, path: '/api/v1/admin/questions?status=PENDING_REVIEW' });
  const res = await apiCall('/api/v1/admin/questions?status=PENDING_REVIEW', {}, 'CONTENT_ADMIN');
  assertStatus(res.status, 200);
  assertTrue(Array.isArray(res.json?.data?.content));
  const neg = await apiCall('/api/v1/admin/questions?status=INVALID_STATUS', {}, 'CONTENT_ADMIN');
  assertStatus(neg.status, 400);
});

registerCase('API-030', 'AdminQuestionController GET /api/v1/admin/questions/picker', async ({ apiCall, assertStatus, assertTrue }) => {
  await verifyStandardRbac({ apiCall, assertStatus, path: '/api/v1/admin/questions/picker' });
  const res = await apiCall('/api/v1/admin/questions/picker', {}, 'CONTENT_ADMIN');
  assertStatus(res.status, 200);
  assertTrue(Array.isArray(res.json?.data?.content));
  const neg = await apiCall('/api/v1/admin/questions/picker?questionType=INVALID_TYPE', {}, 'CONTENT_ADMIN');
  assertStatus(neg.status, 400);
});

registerCase('API-031', 'AdminQuestionController GET /api/v1/admin/questions/{id}', async ({ apiCall, assertStatus, assertEqual, queryDb }) => {
  const qId = ensurePendingQuestion(queryDb, 'Q_ADM_GET_01');
  await verifyStandardRbac({ apiCall, assertStatus, path: `/api/v1/admin/questions/${qId}` });
  const res = await apiCall(`/api/v1/admin/questions/${qId}`, {}, 'CONTENT_ADMIN');
  assertStatus(res.status, 200);
  assertEqual(res.json?.data?.id, qId);
  const neg = await apiCall('/api/v1/admin/questions/999999', {}, 'CONTENT_ADMIN');
  assertStatus(neg.status, 404);
});

registerCase('API-032', 'AdminQuestionController POST /api/v1/admin/questions/{id}/approve', async ({ apiCall, assertStatus, assertEqual, queryDb }) => {
  const qId = ensurePendingQuestion(queryDb, 'Q_ADM_APP_01', 'PENDING_REVIEW');
  await verifyStandardRbac({ apiCall, assertStatus, path: `/api/v1/admin/questions/${qId}/approve`, method: 'POST', body: { comment: 'Duyệt' } });
  const res = await apiCall(`/api/v1/admin/questions/${qId}/approve`, { method: 'POST', body: { comment: 'Duyệt' } }, 'CONTENT_ADMIN');
  assertStatus(res.status, 200);
  assertEqual(res.json?.data?.status, 'APPROVED');
  const neg = await apiCall('/api/v1/admin/questions/999999/approve', { method: 'POST' }, 'CONTENT_ADMIN');
  assertStatus(neg.status, 404);
});

registerCase('API-033', 'AdminQuestionController POST /api/v1/admin/questions/{id}/request-revision', async ({ apiCall, assertStatus, assertEqual, queryDb }) => {
  const qId = ensurePendingQuestion(queryDb, 'Q_ADM_REV_01', 'PENDING_REVIEW');
  const body = { comment: 'Yêu cầu sửa giải thích chi tiết hơn' };
  await verifyStandardRbac({ apiCall, assertStatus, path: `/api/v1/admin/questions/${qId}/request-revision`, method: 'POST', body });
  const res = await apiCall(`/api/v1/admin/questions/${qId}/request-revision`, { method: 'POST', body }, 'CONTENT_ADMIN');
  assertStatus(res.status, 200);
  assertEqual(res.json?.data?.status, 'NEEDS_REVISION');
  const negQId = ensurePendingQuestion(queryDb, 'Q_ADM_REV_NEG_01', 'PENDING_REVIEW');
  const neg = await apiCall(`/api/v1/admin/questions/${negQId}/request-revision`, { method: 'POST', body: { comment: '' } }, 'CONTENT_ADMIN');
  assertStatus(neg.status, 400);
});

registerCase('API-034', 'AdminQuestionController POST /api/v1/admin/questions/{id}/reject', async ({ apiCall, assertStatus, assertEqual, queryDb }) => {
  const qId = ensurePendingQuestion(queryDb, 'Q_ADM_REJ_01', 'PENDING_REVIEW');
  const body = { comment: 'Từ chối câu hỏi do trùng nội dung' };
  await verifyStandardRbac({ apiCall, assertStatus, path: `/api/v1/admin/questions/${qId}/reject`, method: 'POST', body });
  const res = await apiCall(`/api/v1/admin/questions/${qId}/reject`, { method: 'POST', body }, 'CONTENT_ADMIN');
  assertStatus(res.status, 200);
  assertEqual(res.json?.data?.status, 'ARCHIVED');
  const negQId = ensurePendingQuestion(queryDb, 'Q_ADM_REJ_NEG_01', 'PENDING_REVIEW');
  const neg = await apiCall(`/api/v1/admin/questions/${negQId}/reject`, { method: 'POST', body: { comment: '' } }, 'CONTENT_ADMIN');
  assertStatus(neg.status, 400);
});

registerCase('API-035', 'AdminQuestionController GET /api/v1/admin/questions/import/template', async ({ apiCall, assertStatus, assertTrue }) => {
  await verifyStandardRbac({ apiCall, assertStatus, path: '/api/v1/admin/questions/import/template' });
  const res = await apiCall('/api/v1/admin/questions/import/template', { headers: { Accept: '*/*' } }, 'CONTENT_ADMIN');
  assertStatus(res.status, 200);
  const ct = res.headers['content-type'] || '';
  assertTrue(ct.includes('spreadsheetml') || ct.includes('octet-stream'));
  const neg = await apiCall('/api/v1/admin/questions/import/template', { method: 'POST' }, 'CONTENT_ADMIN');
  assertStatus(neg.status, 405);
});

registerCase('API-036', 'AdminQuestionController POST /api/v1/admin/questions/import', async ({ apiCall, assertStatus, assertTrue }) => {
  const excelBuf = await getValidExcelFile(apiCall);
  const formData = new FormData();
  formData.append('file', new Blob([excelBuf], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' }), 'import.xlsx');
  await verifyStandardRbac({ apiCall, assertStatus, path: '/api/v1/admin/questions/import?dryRun=true', method: 'POST', body: formData });
  const res = await apiCall('/api/v1/admin/questions/import?dryRun=true', { method: 'POST', body: formData }, 'CONTENT_ADMIN');
  assertStatus(res.status, 200);
  assertTrue(res.json?.data != null && typeof res.json.data === 'object');
  const neg = await apiCall('/api/v1/admin/questions/import', { method: 'POST', body: new FormData() }, 'CONTENT_ADMIN');
  assertStatus(neg.status, 400);
});

// ============================================================================
// TICKET MANAGEMENT (API-037, API-038)
// ============================================================================

registerCase('API-037', 'AdminTicketController GET /api/v1/admin/tickets', async ({ apiCall, assertStatus, assertTrue, queryDb }) => {
  ensureFixtureTicket(queryDb);
  await verifyStandardRbac({ apiCall, assertStatus, path: '/api/v1/admin/tickets' });
  const res = await apiCall('/api/v1/admin/tickets', {}, 'CONTENT_ADMIN');
  assertStatus(res.status, 200);
  assertTrue(Array.isArray(res.json?.data?.content));
  const neg = await apiCall('/api/v1/admin/tickets?status=BOGUS_STATUS', {}, 'CONTENT_ADMIN');
  assertStatus(neg.status, 400);
});

registerCase('API-038', 'AdminTicketController PATCH /api/v1/admin/tickets/{id}', async ({ apiCall, assertStatus, assertEqual, queryDb }) => {
  const ticketId = ensureFixtureTicket(queryDb);
  const body = { status: 'IN_PROGRESS', resolverNote: 'Tiếp nhận xử lý API-038' };
  await verifyStandardRbac({ apiCall, assertStatus, path: `/api/v1/admin/tickets/${ticketId}`, method: 'PATCH', body });
  const res = await apiCall(`/api/v1/admin/tickets/${ticketId}`, { method: 'PATCH', body }, 'CONTENT_ADMIN');
  assertStatus(res.status, 200);
  assertEqual(res.json?.data?.status, 'IN_PROGRESS');
  const note = queryDb(`SELECT resolver_note FROM tickets WHERE id = ${ticketId};`);
  assertEqual(note.trim(), body.resolverNote);
  const neg = await apiCall('/api/v1/admin/tickets/999999', { method: 'PATCH', body }, 'CONTENT_ADMIN');
  assertStatus(neg.status, 404);
});

// ============================================================================
// USER MANAGEMENT - SUPER ADMIN ONLY (API-039 -> API-042)
// ============================================================================

registerCase('API-039', 'AdminUserController GET /api/v1/admin/users', async ({ apiCall, assertStatus, assertTrue, queryDb }) => {
  ensureTmpUser(queryDb);
  await verifyStandardRbac({ apiCall, assertStatus, path: '/api/v1/admin/users', superAdminOnly: true });
  const res = await apiCall('/api/v1/admin/users', {}, 'SUPER_ADMIN');
  assertStatus(res.status, 200);
  assertTrue(Array.isArray(res.json?.data?.content));
  const neg = await apiCall('/api/v1/admin/users?role=BOGUS_ROLE', {}, 'SUPER_ADMIN');
  assertStatus(neg.status, 400);
});

registerCase('API-040', 'AdminUserController PATCH /api/v1/admin/users/{id}/role', async ({ apiCall, assertStatus, assertEqual, queryDb }) => {
  const tmpUserId = ensureTmpUser(queryDb);
  const body = { role: 'COLLABORATOR' };
  await verifyStandardRbac({ apiCall, assertStatus, path: `/api/v1/admin/users/${tmpUserId}/role`, method: 'PATCH', body, superAdminOnly: true });
  const res = await apiCall(`/api/v1/admin/users/${tmpUserId}/role`, { method: 'PATCH', body }, 'SUPER_ADMIN');
  assertStatus(res.status, 200);
  assertEqual(res.json?.data?.role, 'COLLABORATOR');
  const roleInDb = queryDb(`SELECT role FROM users WHERE id = ${tmpUserId};`);
  assertEqual(roleInDb.trim(), 'COLLABORATOR');
  queryDb(`UPDATE users SET role = 'STUDENT' WHERE id = ${tmpUserId};`);
  const neg = await apiCall(`/api/v1/admin/users/${tmpUserId}/role`, { method: 'PATCH', body: {} }, 'SUPER_ADMIN');
  assertStatus(neg.status, 400);
});

registerCase('API-041', 'AdminUserController PATCH /api/v1/admin/users/{id}/lock', async ({ apiCall, assertStatus, assertEqual, queryDb }) => {
  const tmpUserId = ensureTmpUser(queryDb);
  await verifyStandardRbac({ apiCall, assertStatus, path: `/api/v1/admin/users/${tmpUserId}/lock`, method: 'PATCH', superAdminOnly: true });
  const res = await apiCall(`/api/v1/admin/users/${tmpUserId}/lock`, { method: 'PATCH' }, 'SUPER_ADMIN');
  assertStatus(res.status, 200);
  const statusInDb = queryDb(`SELECT status FROM users WHERE id = ${tmpUserId};`);
  assertEqual(statusInDb.trim(), 'LOCKED');
  const superAdminId = queryDb("SELECT id FROM users WHERE email = 'superadmin@vsat.com';");
  const neg = await apiCall(`/api/v1/admin/users/${superAdminId.trim()}/lock`, { method: 'PATCH' }, 'SUPER_ADMIN');
  assertStatus(neg.status, 400);
});

registerCase('API-042', 'AdminUserController PATCH /api/v1/admin/users/{id}/unlock', async ({ apiCall, assertStatus, assertEqual, queryDb }) => {
  const tmpUserId = ensureTmpUser(queryDb);
  queryDb(`UPDATE users SET status = 'LOCKED' WHERE id = ${tmpUserId};`);
  await verifyStandardRbac({ apiCall, assertStatus, path: `/api/v1/admin/users/${tmpUserId}/unlock`, method: 'PATCH', superAdminOnly: true });
  const res = await apiCall(`/api/v1/admin/users/${tmpUserId}/unlock`, { method: 'PATCH' }, 'SUPER_ADMIN');
  assertStatus(res.status, 200);
  const statusInDb = queryDb(`SELECT status FROM users WHERE id = ${tmpUserId};`);
  assertEqual(statusInDb.trim(), 'ACTIVE');
  const neg = await apiCall('/api/v1/admin/users/999999/unlock', { method: 'PATCH' }, 'SUPER_ADMIN');
  assertStatus(neg.status, 404);
});

// ============================================================================
// RUNNER ENTRYPOINT
// ============================================================================

if (process.argv[1] && process.argv[1].endsWith('backend-admin.mjs')) {
  await runSuite('backend-admin-report');
}
