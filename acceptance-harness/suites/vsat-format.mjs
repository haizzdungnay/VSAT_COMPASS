#!/usr/bin/env node
/**
 * VF suite: V-SAT exam-format conformance for add / update / matrix / import / blueprint / bundle.
 * Plan: docs/testing/VSAT_EXAM_FORMAT_TEST_PLAN.md §4. Assertions encode V-SAT-correct behaviour,
 * so FAILED = format gap (G1..G17). Clean-room local only: node run-all.mjs --suite suites/vsat-format.mjs
 * ponytail: section G (web wizard, Playwright) and VF-D07/VF-E05 not automated here; add with the web suite.
 */
import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import { registerCase, runSuite } from '../lib/test-harness.mjs';
import { getSubjectId, getBlueprintId } from './helpers/backend-admin-helpers.mjs';
import {
  PARTS, PART_COUNTS, validateVsatExam, questionErrors, keyLeaks, scoreVsat, makeAnswers, mulberry32
} from '../lib/vsat-format.mjs';

const RUN = Date.now().toString(36).toUpperCase();
let seq = 0;
const code = (prefix) => `${prefix}_${RUN}_${++seq}`;
const firstId = (s) => Number(String(s).match(/\d+/)?.[0]);
const rows = (s) => (s ? s.split('\n').map((l) => l.split('|')) : []);

function ok(res, what) {
  if (res.status < 200 || res.status >= 300) {
    const detail = JSON.stringify(res.json?.error ?? res.json?.message ?? res.text ?? '').slice(0, 300);
    throw new Error(`[setup] ${what} -> HTTP ${res.status} ${detail}`);
  }
  return res.json?.data;
}
const post = (apiCall, path, body, role = 'CONTENT_ADMIN') => apiCall(path, { method: 'POST', body }, role);
const put = (apiCall, path, body, role = 'CONTENT_ADMIN') => apiCall(path, { method: 'PUT', body }, role);
const del = (apiCall, path, role = 'CONTENT_ADMIN') => apiCall(path, { method: 'DELETE' }, role);
const errInfo = (res) => ({ status: res.status, error: res.json?.error ?? res.json?.message ?? null });

// ------------------------------------------------------------------ question fixtures

const OPTION_SETS = {
  TRUE_FALSE: () => ['a', 'b', 'c', 'd'].map((l, i) => [l, `Menh de ${l}`, i % 2 === 0]),
  SINGLE_CHOICE: () => ['A', 'B', 'C', 'D'].map((l) => [l, `Phuong an ${l}`, l === 'C']),
  MATCHING: () => [['1', 'Ve 1 ⇄ C'], ['2', 'Ve 2 ⇄ A'], ['3', 'Ve 3 ⇄ D'], ['4', 'Ve 4 ⇄ B']].map(([l, t]) => [l, t, true]),
  SHORT_ANSWER: () => [['A', '12.5', true]],
};
const toOptions = (opts) => opts.map(([optionLabel, optionText, isCorrect], i) => ({ optionLabel, optionText, isCorrect, displayOrder: i + 1 }));
const questionBody = (subjectId, topicId, type, options, text) =>
  ({ subjectId, topicId, difficulty: 'MEDIUM', questionType: type, questionText: text, options: toOptions(options) });

/** Real flow: collaborator create -> submit-for-review -> content admin approve. */
async function apiQuestion(apiCall, subjectId, topicId, type, text) {
  const q = ok(await post(apiCall, '/collaborator/questions', questionBody(subjectId, topicId, type, OPTION_SETS[type](), text), 'COLLABORATOR'), `create ${type}`);
  ok(await post(apiCall, `/collaborator/questions/${q.id}/submit-for-review`, undefined, 'COLLABORATOR'), 'submit question');
  ok(await post(apiCall, `/admin/questions/${q.id}/approve`), 'approve question');
  return q.id;
}

/** Legacy/dirty rows the API may (correctly) refuse to create; inserted straight into the clean DB. */
function sqlQuestion(queryDb, { qcode, subject = 'MATH', topic = 'MATH_ALG', type = 'SINGLE_CHOICE', status = 'APPROVED', options }) {
  const id = firstId(queryDb(`
    INSERT INTO questions (question_code, subject_id, topic_id, difficulty, cognitive_level, question_type,
                           question_text, status, created_by, version)
    SELECT '${qcode}', s.id, t.id, 'MEDIUM', 'APPLICATION', '${type}', 'VF ${qcode}', '${status}', u.id, 1
    FROM subjects s, topics t, users u
    WHERE s.code = '${subject}' AND t.code = '${topic}' AND u.email = 'contentadmin@vsat.com'
    ON CONFLICT (question_code) DO UPDATE SET status = EXCLUDED.status
    RETURNING id;`));
  const values = options.map(([l, t, c], i) => `(${id}, '${l}', '${t}', '${t}', ${c}, ${i + 1})`).join(', ');
  queryDb(`DELETE FROM question_options WHERE question_id = ${id};
    INSERT INTO question_options (question_id, option_label, option_text, option_html, is_correct, display_order) VALUES ${values};`);
  return id;
}

function ensureSubject(queryDb, subject, name, order) {
  queryDb(`INSERT INTO subjects (code, name, description, is_active, display_order)
    VALUES ('${subject}', '${name}', 'VF fixture', TRUE, ${order}) ON CONFLICT (code) DO NOTHING;
    INSERT INTO topics (subject_id, code, name, description, is_active, display_order)
    SELECT s.id, '${subject}_VF', 'VF ${subject}', 'VF fixture', TRUE, 9 FROM subjects s WHERE s.code = '${subject}'
    ON CONFLICT (code) DO NOTHING;`);
  return firstId(queryDb(`SELECT id FROM subjects WHERE code = '${subject}';`));
}

let poolP = null;
const pool = (ctx) => (poolP ??= seedPool(ctx).catch((e) => { poolP = null; throw e; }));

async function seedPool({ apiCall, queryDb }) {
  const subjectId = getSubjectId(queryDb);
  const topicId = firstId(queryDb("SELECT id FROM topics WHERE code = 'MATH_ALG';"));
  const make = async (type, n, tag) => {
    const ids = [];
    for (let i = 1; i <= n; i++) ids.push(await apiQuestion(apiCall, subjectId, topicId, type, `VF ${RUN} ${tag} ${type} #${i}`));
    return ids;
  };
  const std = {};
  for (const p of PARTS) std[p] = await make(p, PART_COUNTS[p], 'STD');
  const extraMcq = await make('SINGLE_CHOICE', 19, 'EXTRA');
  const engId = ensureSubject(queryDb, 'ENG', 'Tieng Anh', 2);
  return {
    subjectId, topicId, engId, std, extraMcq,
    standard: PARTS.flatMap((p) => std[p]),
    tf3: sqlQuestion(queryDb, { qcode: `VF_TF3_${RUN}`, type: 'TRUE_FALSE', options: OPTION_SETS.TRUE_FALSE().slice(0, 3) }),
    draft: sqlQuestion(queryDb, { qcode: `VF_DRAFT_${RUN}`, status: 'DRAFT', options: OPTION_SETS.SINGLE_CHOICE() }),
    eng: sqlQuestion(queryDb, { qcode: `VF_ENG_${RUN}`, subject: 'ENG', topic: 'ENG_VF', options: OPTION_SETS.SINGLE_CHOICE() }),
  };
}

// ------------------------------------------------------------------ exam helpers

async function createExam(apiCall, { prefix, subjectId, durationMinutes = 90 }) {
  const body = { examCode: code(prefix), title: `VF ${prefix}`, subjectId, durationMinutes, difficulty: 'MEDIUM', pricingType: 'FREE', price: 0 };
  return ok(await post(apiCall, '/admin/exams', body), 'create exam').id;
}
async function addQuestions(apiCall, examId, ids) {
  for (const id of ids) ok(await post(apiCall, `/admin/exams/${examId}/questions`, { questionId: id }), `add question ${id}`);
}
async function draftExam(ctx, prefix, ids, opts = {}) {
  const p = await pool(ctx);
  const id = await createExam(ctx.apiCall, { prefix, subjectId: p.subjectId, ...opts });
  await addQuestions(ctx.apiCall, id, ids);
  return id;
}
async function submitAndPublish(apiCall, examId, publisher = 'SUPER_ADMIN') {
  ok(await post(apiCall, `/admin/exams/${examId}/submit-review`), 'submit-review');
  return post(apiCall, `/admin/exams/${examId}/publish`, undefined, publisher);
}
async function publishedStandard(ctx, prefix) {
  const p = await pool(ctx);
  const id = await draftExam(ctx, prefix, p.standard);
  ok(await submitAndPublish(ctx.apiCall, id), 'publish standard exam');
  return id;
}
let sharedP = null;
const sharedPublished = (ctx) => (sharedP ??= publishedStandard(ctx, 'VF_STD').catch((e) => { sharedP = null; throw e; }));

const examStatus = (queryDb, id) => queryDb(`SELECT status FROM exams WHERE id = ${id};`);
const mappedIds = (queryDb, id) => rows(queryDb(`SELECT question_id FROM exam_questions WHERE exam_id = ${id} ORDER BY question_order, id;`)).map((r) => Number(r[0]));

function examMeta(queryDb, examId) {
  const [[subjectCode, profileOk]] = rows(queryDb(`SELECT s.code, (sp.id IS NOT NULL) FROM exams e
    JOIN subjects s ON s.id = e.subject_id LEFT JOIN scoring_profiles sp ON sp.id = e.scoring_profile_id WHERE e.id = ${examId};`));
  return { subjectCode, scoringProfileValid: profileOk === 't' };
}

/** Admin view of an exam, enriched with subject/status from DB, in oracle shape. */
async function examView({ apiCall, queryDb }, examId) {
  const exam = ok(await apiCall(`/admin/exams/${examId}`, {}, 'CONTENT_ADMIN'), 'GET exam');
  const qs = ok(await apiCall(`/admin/exams/${examId}/questions`, {}, 'CONTENT_ADMIN'), 'GET exam questions');
  const meta = Object.fromEntries(rows(qs.length
    ? queryDb(`SELECT id, subject_id, status FROM questions WHERE id IN (${qs.map((q) => q.questionId).join(',')});`)
    : '').map(([id, subjectId, status]) => [id, { subjectId: Number(subjectId), status }]));
  return {
    exam: { subjectId: exam.subjectId, durationMinutes: exam.durationMinutes, ...examMeta(queryDb, examId) },
    questions: qs.map((q) => ({ ...q, ...meta[q.questionId] })),
  };
}

const bundle = async (apiCall, examId) => apiCall(`/exams/${examId}/download-bundle`, {}, 'STUDENT');

// ============================================================================
// A. Thêm đề thủ công
// ============================================================================

registerCase('VF-A01', 'Đề 25 câu chuẩn: publish 200 và oracle PASS', async (ctx) => {
  const p = await pool(ctx);
  const id = await draftExam(ctx, 'VF_A01', p.standard);
  const res = await submitAndPublish(ctx.apiCall, id);
  ctx.evidence.examId = id;
  ctx.assertStatus(res.status, 200, 'publish đề chuẩn');
  sharedP = Promise.resolve(id);
  const v = await examView(ctx, id);
  ctx.evidence.oracle = validateVsatExam(v.exam, v.questions);
  ctx.evidence.totalMaxRaw = v.questions.reduce((s, q) => s + Number(q.maxRawScore), 0);
  ctx.assertDeepEqual(ctx.evidence.oracle, [], 'oracle V-SAT trên đề đã publish');
});

registerCase('VF-A02', 'Publish đề 1 câu bị chặn (G1)', async (ctx) => {
  const p = await pool(ctx);
  const res = await submitAndPublish(ctx.apiCall, await draftExam(ctx, 'VF_A02', [p.std.SINGLE_CHOICE[0]]));
  ctx.evidence.publish = errInfo(res);
  ctx.assertStatus(res.status, 400, 'đề 1 câu phải bị từ chối');
});

registerCase('VF-A03', 'Publish đề 25 câu toàn 4 lựa chọn bị chặn VF_MIX (G1)', async (ctx) => {
  const p = await pool(ctx);
  const res = await submitAndPublish(ctx.apiCall, await draftExam(ctx, 'VF_A03', [...p.std.SINGLE_CHOICE, ...p.extraMcq]));
  ctx.evidence.publish = errInfo(res);
  ctx.assertStatus(res.status, 400, 'sai tỉ lệ 9/6/5/5 phải bị từ chối');
});

registerCase('VF-A04', 'Publish đề 26 câu bị chặn (G1)', async (ctx) => {
  const p = await pool(ctx);
  const res = await submitAndPublish(ctx.apiCall, await draftExam(ctx, 'VF_A04', [...p.standard, p.extraMcq[0]]));
  ctx.evidence.publish = errInfo(res);
  ctx.assertStatus(res.status, 400, 'đề 26 câu phải bị từ chối');
});

registerCase('VF-A05', 'Thêm câu môn ENG vào đề MATH bị chặn (G2)', async (ctx) => {
  const p = await pool(ctx);
  const id = await draftExam(ctx, 'VF_A05', []);
  const res = await post(ctx.apiCall, `/admin/exams/${id}/questions`, { questionId: p.eng });
  ctx.evidence.add = errInfo(res);
  ctx.assertStatus(res.status, 400, 'câu khác môn phải bị từ chối');
});

registerCase('VF-A06', 'Thêm câu DRAFT bị chặn', async (ctx) => {
  const p = await pool(ctx);
  const id = await draftExam(ctx, 'VF_A06', []);
  const res = await post(ctx.apiCall, `/admin/exams/${id}/questions`, { questionId: p.draft });
  ctx.evidence.add = errInfo(res);
  ctx.assertStatus(res.status, 400, 'câu chưa duyệt phải bị từ chối');
});

registerCase('VF-A07', 'Thêm trùng câu trả 409', async (ctx) => {
  const p = await pool(ctx);
  const qid = p.std.SINGLE_CHOICE[0];
  const id = await draftExam(ctx, 'VF_A07', [qid]);
  const res = await post(ctx.apiCall, `/admin/exams/${id}/questions`, { questionId: qid });
  ctx.evidence.add = errInfo(res);
  ctx.assertStatus(res.status, 409, 'câu trùng');
});

registerCase('VF-A08', 'Tạo đề MATH 60 phút bị chặn (G17)', async (ctx) => {
  const p = await pool(ctx);
  const body = { examCode: code('VF_A08'), title: 'VF A08', subjectId: p.subjectId, durationMinutes: 60, difficulty: 'MEDIUM', pricingType: 'FREE', price: 0 };
  const res = await post(ctx.apiCall, '/admin/exams', body);
  ctx.evidence.create = errInfo(res);
  ctx.assertStatus(res.status, 400, 'Toán V-SAT là 90 phút');
});

registerCase('VF-A09', 'Publish đề có câu Đ/S chỉ 3 ý bị chặn (G4)', async (ctx) => {
  const p = await pool(ctx);
  const res = await submitAndPublish(ctx.apiCall, await draftExam(ctx, 'VF_A09', [p.tf3, ...p.standard.slice(1)]));
  ctx.evidence.publish = errInfo(res);
  ctx.assertStatus(res.status, 400, 'câu Đ/S thiếu ý phải chặn publish');
});

registerCase('VF-A10', 'CONTENT_ADMIN publish trả 403, không tự chuyển submit-review (G15)', async (ctx) => {
  const p = await pool(ctx);
  const id = await draftExam(ctx, 'VF_A10', p.standard);
  const res = await submitAndPublish(ctx.apiCall, id, 'CONTENT_ADMIN');
  ctx.assertStatus(res.status, 403, 'backend chặn CONTENT_ADMIN publish');
  ctx.assertEqual(examStatus(ctx.queryDb, id), 'PENDING_REVIEW', 'trạng thái giữ nguyên');
  const api = readFileSync(resolve(import.meta.dirname, '../../web-admin/src/services/api.ts'), 'utf8');
  const fn = api.slice(api.indexOf('export async function publishExam'), api.indexOf('export', api.indexOf('export async function publishExam') + 10));
  ctx.evidence.webPublishExam = fn.trim().split('\n').slice(0, 14);
  ctx.assertFalse(fn.includes('submit-review'), 'web api.ts publishExam không được âm thầm gọi submit-review khi 403');
});

// ============================================================================
// B. Sinh đề theo ma trận
// ============================================================================

registerCase('VF-B01', 'Sinh đề từ VSAT_2026_BP_MATH ra đúng 9/6/5/5, oracle PASS (G3)', async (ctx) => {
  await pool(ctx);
  const id = await draftExam(ctx, 'VF_B01', []);
  const res = await post(ctx.apiCall, `/admin/exams/${id}/generate-by-matrix`, { blueprintId: getBlueprintId(ctx.queryDb), variantCount: 4 });
  ctx.assertStatus(res.status, 200, 'generate-by-matrix');
  const v = await examView(ctx, id);
  ctx.evidence.types = v.questions.map((q) => q.questionType);
  ctx.evidence.oracle = validateVsatExam(v.exam, v.questions);
  ctx.assertDeepEqual(ctx.evidence.oracle, [], 'đề sinh theo ma trận phải đạt chuẩn');
});

registerCase('VF-B02', 'Ngân hàng ENG toàn 4 lựa chọn: sinh đề phải 400 hoặc cảnh báo thiếu dạng (G3)', async (ctx) => {
  const p = await pool(ctx);
  for (let i = 1; i <= 30; i++) {
    sqlQuestion(ctx.queryDb, { qcode: `VF_ENGMCQ_${i}`, subject: 'ENG', topic: 'ENG_VF', options: OPTION_SETS.SINGLE_CHOICE() });
  }
  const id = await createExam(ctx.apiCall, { prefix: 'VF_B02', subjectId: p.engId, durationMinutes: 60 });
  const bp = firstId(ctx.queryDb("SELECT id FROM exam_blueprints WHERE code = 'VSAT_2026_BP_ENG';"));
  const res = await post(ctx.apiCall, `/admin/exams/${id}/generate-by-matrix`, { blueprintId: bp });
  const warnings = res.json?.data?.generationWarnings ?? [];
  ctx.evidence.generate = { ...errInfo(res), selected: res.json?.data?.selectedQuestions, warnings };
  ctx.assertTrue(res.status === 400 || warnings.length > 0, 'thiếu Đ/S, ghép hợp, trả lời ngắn phải bị báo');
});

registerCase('VF-B03', 'Sinh đề vào đề PUBLISHED bị chặn 409 (G3)', async (ctx) => {
  const id = await publishedStandard(ctx, 'VF_B03');
  const before = mappedIds(ctx.queryDb, id);
  const res = await post(ctx.apiCall, `/admin/exams/${id}/generate-by-matrix`, { blueprintId: getBlueprintId(ctx.queryDb) });
  ctx.evidence.generate = errInfo(res);
  ctx.evidence.changed = before.join() !== mappedIds(ctx.queryDb, id).join();
  ctx.assertStatus(res.status, 409, 'không được ghi đè đề đang phát hành');
  ctx.assertFalse(ctx.evidence.changed, 'danh sách câu của đề PUBLISHED không đổi');
});

registerCase('VF-B04', 'blueprintId 10 câu được tôn trọng hoặc bị từ chối (G3/G12)', async (ctx) => {
  const p = await pool(ctx);
  const cells = [['TRUE_FALSE', 4], ['SINGLE_CHOICE', 2], ['MATCHING', 2], ['SHORT_ANSWER', 2]]
    .map(([questionType, questionCount]) => ({ questionType, questionCount, difficulty: 'MEDIUM' }));
  const bpRes = await post(ctx.apiCall, '/admin/blueprints', { code: code('VF_BP10'), name: 'VF 10 cau', subjectId: p.subjectId, totalQuestions: 10, maxRawScore: 60, cells });
  ctx.evidence.blueprint = errInfo(bpRes);
  if (bpRes.status !== 200) {
    ctx.assertStatus(bpRes.status, 400, 'blueprint không chuẩn bị từ chối ngay');
    return;
  }
  const id = await draftExam(ctx, 'VF_B04', []);
  const res = await post(ctx.apiCall, `/admin/exams/${id}/generate-by-matrix`, { blueprintId: bpRes.json.data.id });
  ctx.evidence.generate = { ...errInfo(res), selected: res.json?.data?.selectedQuestions };
  ctx.assertTrue(res.status === 400 || res.json?.data?.selectedQuestions === 10, 'đề phải có 10 câu theo blueprint, hoặc 400');
});

registerCase('VF-B05', 'rawScore trả về khớp tổng điểm thực của đề (G3)', async (ctx) => {
  await pool(ctx);
  const phy = ensureSubject(ctx.queryDb, 'PHY', 'Vat ly', 3);
  for (let i = 1; i <= 3; i++) {
    sqlQuestion(ctx.queryDb, { qcode: `VF_PHY_${i}`, subject: 'PHY', topic: 'PHY_VF', options: OPTION_SETS.SINGLE_CHOICE() });
  }
  const id = await createExam(ctx.apiCall, { prefix: 'VF_B05', subjectId: phy, durationMinutes: 60 });
  const gen = ok(await post(ctx.apiCall, `/admin/exams/${id}/generate-by-matrix`, {}), 'generate PHY');
  const v = await examView(ctx, id);
  const actual = v.questions.reduce((s, q) => s + Number(q.maxRawScore), 0);
  ctx.evidence.generate = { rawScore: gen.rawScore, selected: gen.selectedQuestions, actualMaxRaw: actual, warnings: gen.generationWarnings };
  ctx.assertEqual(Number(gen.rawScore), actual, 'rawScore = tổng điểm các câu đã chọn');
});

// ============================================================================
// C. Cập nhật đề
// ============================================================================

registerCase('VF-C01', 'PUT đề PUBLISHED trả 409', async (ctx) => {
  const id = await sharedPublished(ctx);
  const res = await put(ctx.apiCall, `/admin/exams/${id}`, { title: 'VF sua de da phat hanh' });
  ctx.evidence.put = errInfo(res);
  ctx.assertStatus(res.status, 409, 'không sửa đề đang phát hành');
});

registerCase('VF-C02', 'Đề HIDDEN: thêm/xóa/sắp xếp câu đều 409', async (ctx) => {
  const p = await pool(ctx);
  const id = await publishedStandard(ctx, 'VF_C02');
  ok(await post(ctx.apiCall, `/admin/exams/${id}/hide`), 'hide');
  const add = await post(ctx.apiCall, `/admin/exams/${id}/questions`, { questionId: p.extraMcq[0] });
  const rm = await del(ctx.apiCall, `/admin/exams/${id}/questions/${p.standard[0]}`);
  const reorder = await put(ctx.apiCall, `/admin/exams/${id}/questions/reorder`, { questionIds: [...p.standard].reverse() });
  ctx.evidence.statuses = { add: add.status, remove: rm.status, reorder: reorder.status };
  ctx.assertStatus(add.status, 409, 'add khi HIDDEN');
  ctx.assertStatus(rm.status, 409, 'remove khi HIDDEN');
  ctx.assertStatus(reorder.status, 409, 'reorder khi HIDDEN');
});

registerCase('VF-C03', 'DRAFT đổi môn sang ENG nhưng giữ câu MATH, publish bị chặn (G2)', async (ctx) => {
  const p = await pool(ctx);
  const id = await draftExam(ctx, 'VF_C03', p.standard);
  ctx.evidence.put = errInfo(await put(ctx.apiCall, `/admin/exams/${id}`, { subjectId: p.engId, durationMinutes: 60 }));
  const res = await submitAndPublish(ctx.apiCall, id);
  ctx.evidence.publish = errInfo(res);
  ctx.assertStatus(res.status, 400, 'câu khác môn với đề phải chặn publish');
});

registerCase('VF-C04', 'HIDDEN đổi môn rồi publish lại phải chạy lại validator (G2)', async (ctx) => {
  const p = await pool(ctx);
  const id = await publishedStandard(ctx, 'VF_C04');
  ok(await post(ctx.apiCall, `/admin/exams/${id}/hide`), 'hide');
  ctx.evidence.put = errInfo(await put(ctx.apiCall, `/admin/exams/${id}`, { subjectId: p.engId, durationMinutes: 60 }));
  const res = await post(ctx.apiCall, `/admin/exams/${id}/publish`, undefined, 'SUPER_ADMIN');
  ctx.evidence.republish = errInfo(res);
  ctx.assertStatus(res.status, 400, 'publish lại đề HIDDEN đã sai môn phải bị chặn');
});

registerCase('VF-C05', 'Return-to-draft, bỏ 1 câu Đ/S rồi publish lại bị chặn (G1)', async (ctx) => {
  const p = await pool(ctx);
  const id = await publishedStandard(ctx, 'VF_C05');
  ok(await post(ctx.apiCall, `/admin/exams/${id}/hide`), 'hide');
  ok(await post(ctx.apiCall, `/admin/exams/${id}/return-to-draft`), 'return-to-draft');
  ok(await del(ctx.apiCall, `/admin/exams/${id}/questions/${p.std.TRUE_FALSE[0]}`), 'remove TF');
  const res = await submitAndPublish(ctx.apiCall, id);
  ctx.evidence.publish = errInfo(res);
  ctx.assertStatus(res.status, 400, 'đề 24 câu (8 Đ/S) phải bị chặn');
});

registerCase('VF-C06', 'Sửa câu APPROVED nằm trong đề PUBLISHED bị chặn 409', async (ctx) => {
  const p = await pool(ctx);
  await sharedPublished(ctx);
  const res = await put(ctx.apiCall, `/collaborator/questions/${p.std.TRUE_FALSE[0]}`, { questionText: 'VF sua cau da duyet' }, 'COLLABORATOR');
  ctx.evidence.put = errInfo(res);
  ctx.assertStatus(res.status, 409, 'câu đã duyệt không được sửa');
});

registerCase('VF-C07', 'Hai admin cùng lockVersion: người thứ hai nhận 409', async (ctx) => {
  const id = await draftExam(ctx, 'VF_C07', []);
  const lock = ok(await ctx.apiCall(`/admin/exams/${id}`, {}, 'CONTENT_ADMIN'), 'GET exam').lockVersion;
  const first = await put(ctx.apiCall, `/admin/exams/${id}`, { title: 'VF admin 1', lockVersion: lock });
  const second = await put(ctx.apiCall, `/admin/exams/${id}`, { title: 'VF admin 2', lockVersion: lock }, 'SUPER_ADMIN');
  ctx.evidence.statuses = { lock, first: first.status, second: second.status };
  ctx.assertStatus(first.status, 200, 'lần sửa đầu');
  ctx.assertStatus(second.status, 409, 'lần sửa sau với lockVersion cũ');
});

registerCase('VF-C08', 'Reorder làm 4 lựa chọn đứng trước Đ/S bị chặn VF_ORDER', async (ctx) => {
  const p = await pool(ctx);
  const id = await draftExam(ctx, 'VF_C08', p.standard);
  const order = [...p.standard];
  [order[0], order[9]] = [order[9], order[0]];
  const res = await put(ctx.apiCall, `/admin/exams/${id}/questions/reorder`, { questionIds: order });
  ctx.evidence.reorder = errInfo(res);
  ctx.assertStatus(res.status, 400, 'thứ tự phần thi cố định Đ/S, 4LC, ghép, TLN');
});

// ============================================================================
// D. Import câu hỏi (JSON)
// ============================================================================

async function importJson(apiCall, items, dryRun) {
  const fd = new FormData();
  fd.append('file', new Blob([JSON.stringify(items)], { type: 'application/json' }), 'vf-import.json');
  return apiCall(`/admin/questions/import?dryRun=${dryRun}`, { method: 'POST', body: fd, timeoutMs: 120000 }, 'CONTENT_ADMIN');
}
const item = (over) => ({ subject: 'MATH', topic: 'MATH_ALG', difficulty: 'MEDIUM', ...over });
const four = (prefix) => ['A', 'B', 'C', 'D'].map((l) => `${prefix} ${l}`);
const dryErrors = (res) => ({ status: res.status, valid: res.json?.data?.validCount, errorCount: res.json?.data?.errorCount, errors: res.json?.data?.errors });

/** Imported questions (by text marker) in oracle shape, straight from DB. */
function importedQuestions(queryDb, marker) {
  const qs = rows(queryDb(`SELECT id, status, question_type FROM questions WHERE question_text LIKE '${marker}%' ORDER BY id;`));
  if (!qs.length) return [];
  const opts = rows(queryDb(`SELECT question_id, id, option_label, option_text, is_correct FROM question_options
    WHERE question_id IN (${qs.map((q) => q[0]).join(',')}) ORDER BY display_order, id;`));
  return qs.map(([id, status, questionType]) => ({
    questionId: Number(id), status, questionType,
    options: opts.filter((o) => o[0] === id).map(([, oid, optionLabel, optionText, c]) => ({ id: Number(oid), optionLabel, optionText, isCorrect: c === 't' })),
  }));
}

registerCase('VF-D01', 'Import Đ/S + 4LC hợp lệ: câu vào trạng thái chờ duyệt, đúng cấu trúc (G8)', async (ctx) => {
  const marker = `VF-D01-${RUN}-${++seq}`;
  ok(await importJson(ctx.apiCall, [
    item({ type: 'TRUE_FALSE', questionText: `${marker} TF`, options: four('Menh de'), correctAnswer: 'A,C' }),
    item({ type: 'SINGLE_CHOICE', questionText: `${marker} MCQ`, options: four('Phuong an'), correctAnswer: 'B' }),
  ], false), 'import commit');
  const qs = importedQuestions(ctx.queryDb, marker);
  ctx.evidence.imported = qs.map((q) => ({ type: q.questionType, status: q.status, errors: questionErrors(q) }));
  ctx.assertEqual(qs.length, 2, 'đủ 2 câu được nhập');
  ctx.assertDeepEqual(qs.flatMap((q) => questionErrors(q)), [], 'cấu trúc câu đúng chuẩn');
  ctx.assertDeepEqual(qs.map((q) => q.status), ['PENDING_REVIEW', 'PENDING_REVIEW'], 'câu import phải qua bước duyệt');
});

registerCase('VF-D02', 'Import dạng câu lạ "ABC" phải là dòng lỗi (G8)', async (ctx) => {
  const res = await importJson(ctx.apiCall, [item({ type: 'ABC', questionText: `VF-D02-${RUN}`, options: four('x'), correctAnswer: 'A' })], true);
  ctx.evidence.dryRun = dryErrors(res);
  ctx.assertStatus(res.status, 200, 'dryRun');
  ctx.assertEqual(res.json?.data?.errorCount, 1, 'không được tự đổi thành SINGLE_CHOICE');
});

registerCase('VF-D03', 'Import SINGLE_CHOICE đáp án "A,B" phải là dòng lỗi (G8)', async (ctx) => {
  const res = await importJson(ctx.apiCall, [item({ type: 'SINGLE_CHOICE', questionText: `VF-D03-${RUN}`, options: four('x'), correctAnswer: 'A,B' })], true);
  ctx.evidence.dryRun = dryErrors(res);
  ctx.assertStatus(res.status, 200, 'dryRun');
  ctx.assertEqual(res.json?.data?.errorCount, 1, '4 lựa chọn chỉ có 1 đáp án đúng');
});

registerCase('VF-D04', 'Import Đ/S chỉ 3 ý phải là dòng lỗi (G4/G8)', async (ctx) => {
  const res = await importJson(ctx.apiCall, [item({ type: 'TRUE_FALSE', questionText: `VF-D04-${RUN}`, options: four('y').slice(0, 3), correctAnswer: 'A' })], true);
  ctx.evidence.dryRun = dryErrors(res);
  ctx.assertStatus(res.status, 200, 'dryRun');
  ctx.assertEqual(res.json?.data?.errorCount, 1, 'Đ/S phải đủ 4 ý');
});

registerCase('VF-D05', 'Import trả lời ngắn đáp án số 12.5 hợp lệ (G7/G8)', async (ctx) => {
  const res = await importJson(ctx.apiCall, [item({ type: 'SHORT_ANSWER', questionText: `VF-D05-${RUN}`, answer: '12.5' })], true);
  ctx.evidence.dryRun = dryErrors(res);
  ctx.assertStatus(res.status, 200, 'dryRun');
  ctx.assertEqual(res.json?.data?.errorCount, 0, 'đáp án số phải được chấp nhận');
});

registerCase('VF-D06', 'Import ghép hợp 4 cặp 1-C,2-A,3-D,4-B lưu được khóa ghép (G6/G8)', async (ctx) => {
  const marker = `VF-D06-${RUN}-${++seq}`;
  const rowsIn = [item({ type: 'MATCHING', questionText: marker, options: ['Ha Noi', 'Hue', 'Da Nang', 'Sai Gon'], correctAnswer: '1-C,2-A,3-D,4-B' })];
  const dry = await importJson(ctx.apiCall, rowsIn, true);
  ctx.evidence.dryRun = dryErrors(dry);
  ctx.assertEqual(dry.json?.data?.errorCount, 0, 'dòng ghép hợp hợp lệ');
  ok(await importJson(ctx.apiCall, rowsIn, false), 'import commit');
  const qs = importedQuestions(ctx.queryDb, marker);
  ctx.evidence.imported = qs;
  ctx.assertDeepEqual(qs.flatMap((q) => questionErrors(q)), [], 'có đủ 4 tiểu mục kèm khóa ghép');
});

registerCase('VF-D08', 'dryRun không ghi DB; commit nhập đúng số dòng hợp lệ', async (ctx) => {
  const marker = `VF-D08-${RUN}-${++seq}`;
  const count = () => Number(ctx.queryDb(`SELECT count(*) FROM questions WHERE question_text LIKE '${marker}%';`));
  const rowsIn = [1, 2].map((i) => item({ type: 'SINGLE_CHOICE', questionText: `${marker} #${i}`, options: four('z'), correctAnswer: 'D' }));
  const dry = ok(await importJson(ctx.apiCall, rowsIn, true), 'dryRun');
  ctx.assertEqual(count(), 0, 'dryRun không ghi DB');
  const commit = ok(await importJson(ctx.apiCall, rowsIn, false), 'commit');
  ctx.evidence.result = { dryValid: dry.validCount, imported: commit.importedQuestions, db: count() };
  ctx.assertEqual(dry.validCount, 2, 'dryRun đếm 2 dòng hợp lệ');
  ctx.assertEqual(commit.importedQuestions, 2, 'commit nhập 2 câu');
  ctx.assertEqual(count(), 2, 'DB có 2 câu');
});

registerCase('VF-D09', 'Import 1001 dòng trả 400', async (ctx) => {
  const rowsIn = Array.from({ length: 1001 }, (_, i) => item({ type: 'SINGLE_CHOICE', questionText: `VF-D09-${RUN} #${i}`, options: four('w'), correctAnswer: 'A' }));
  const res = await importJson(ctx.apiCall, rowsIn, true);
  ctx.evidence.import = errInfo(res);
  ctx.assertStatus(res.status, 400, 'vượt 1000 dòng');
});

// ============================================================================
// E. Blueprint
// ============================================================================

const STD_CELLS = PARTS.map((questionType) => ({ questionType, questionCount: PART_COUNTS[questionType], difficulty: 'MEDIUM' }));
const blueprintBody = (subjectId, over) => ({ code: code('VF_BP'), name: 'VF blueprint', subjectId, totalQuestions: 25, maxRawScore: 150, cells: STD_CELLS, ...over });

registerCase('VF-E01', 'Blueprint seed: 25 câu / 150đ / 9-6-5-5 / có profile (G10)', async (ctx) => {
  const bp = ok(await ctx.apiCall(`/admin/blueprints/${getBlueprintId(ctx.queryDb)}`, {}, 'CONTENT_ADMIN'), 'GET blueprint');
  const mix = Object.fromEntries(PARTS.map((p) => [p, 0]));
  for (const c of bp.cells ?? []) mix[c.questionType] = (mix[c.questionType] ?? 0) + c.questionCount;
  const noProfile = rows(ctx.queryDb(`SELECT b.code FROM exam_blueprints b LEFT JOIN scoring_profiles sp ON sp.id = b.scoring_profile_id WHERE sp.id IS NULL AND b.code LIKE 'VSAT_2026_BP_%';`)).map((r) => r[0]);
  ctx.evidence.blueprint = { totalQuestions: bp.totalQuestions, maxRawScore: bp.maxRawScore, mix, noProfile };
  ctx.assertEqual(bp.totalQuestions, 25, 'totalQuestions');
  ctx.assertEqual(Number(bp.maxRawScore), 150, 'maxRawScore');
  ctx.assertDeepEqual(mix, PART_COUNTS, 'tỉ lệ 9/6/5/5');
  ctx.assertDeepEqual(noProfile, [], 'mọi blueprint có scoring profile tồn tại');
});

registerCase('VF-E02', 'Tạo blueprint totalQuestions=10 bị chặn (G12)', async (ctx) => {
  const p = await pool(ctx);
  const cells = [['TRUE_FALSE', 4], ['SINGLE_CHOICE', 2], ['MATCHING', 2], ['SHORT_ANSWER', 2]].map(([questionType, questionCount]) => ({ questionType, questionCount }));
  const res = await post(ctx.apiCall, '/admin/blueprints', blueprintBody(p.subjectId, { totalQuestions: 10, maxRawScore: 60, cells }));
  ctx.evidence.create = errInfo(res);
  ctx.assertStatus(res.status, 400, 'blueprint V-SAT phải 25 câu');
});

registerCase('VF-E03', 'Blueprint có ô âm / dạng câu trùng bị chặn (G12)', async (ctx) => {
  const p = await pool(ctx);
  const cells = [['TRUE_FALSE', 9], ['TRUE_FALSE', 6], ['MATCHING', 11], ['SHORT_ANSWER', -1]].map(([questionType, questionCount]) => ({ questionType, questionCount }));
  const res = await post(ctx.apiCall, '/admin/blueprints', blueprintBody(p.subjectId, { cells }));
  ctx.evidence.create = errInfo(res);
  ctx.assertStatus(res.status, 400, 'ô âm và dạng trùng phải bị từ chối');
});

registerCase('VF-E04', 'Blueprint scoringProfileId không tồn tại bị chặn 400 (G10)', async (ctx) => {
  const p = await pool(ctx);
  const res = await post(ctx.apiCall, '/admin/blueprints', blueprintBody(p.subjectId, { scoringProfileId: 999999 }));
  ctx.evidence.create = errInfo(res);
  ctx.assertStatus(res.status, 400, 'profile không tồn tại');
});

// ============================================================================
// F. Xuất đề (download bundle)
// ============================================================================

async function bundleView(ctx, id) {
  const b = ok(await bundle(ctx.apiCall, id), 'download-bundle');
  return {
    raw: b,
    exam: { subjectId: b.subjectId, durationMinutes: b.durationMinutes, subjectCode: examMeta(ctx.queryDb, id).subjectCode },
    questions: (b.questions ?? []).map((q) => ({ ...q, questionId: q.id })),
  };
}

registerCase('VF-F01', 'Bundle của đề chuẩn mô tả đủ 4 phần và điểm (G14)', async (ctx) => {
  const v = await bundleView(ctx, await sharedPublished(ctx));
  ctx.evidence.bundleKeys = Object.keys(v.raw);
  ctx.evidence.oracle = validateVsatExam(v.exam, v.questions, { studentBundle: true }).filter((c) => c !== 'VF_KEY_LEAK');
  ctx.assertDeepEqual(ctx.evidence.oracle, [], 'bundle đạt chuẩn V-SAT (trừ lộ đáp án, xem F02)');
});

registerCase('VF-F02', 'Bundle của học sinh không chứa đáp án (G13)', async (ctx) => {
  const v = await bundleView(ctx, await sharedPublished(ctx));
  const leaks = v.questions.flatMap(keyLeaks);
  ctx.evidence.leakCount = leaks.length;
  ctx.evidence.leakSample = leaks.slice(0, 8);
  ctx.assertDeepEqual(leaks, [], 'không có isCorrect/correctOptionIds/correctAnswer/explanation/khóa ghép/khóa TLN');
});

registerCase('VF-F03', 'correctAnswer của câu Đ/S (nếu có) phải đủ 4 cờ Đ/S (G14)', async (ctx) => {
  const v = await bundleView(ctx, await sharedPublished(ctx));
  const tf = v.questions.filter((q) => q.questionType === 'TRUE_FALSE');
  const complete = (a) => a == null || (typeof a === 'object' ? Object.keys(a).length === 4 : String(a).split(/[,;]/).length === 4);
  ctx.evidence.tfCorrectAnswers = tf.map((q) => q.correctAnswer);
  ctx.assertTrue(tf.length === 9, 'bundle có 9 câu Đ/S');
  ctx.assertTrue(tf.every((q) => complete(q.correctAnswer)), 'mỗi câu Đ/S: không có correctAnswer, hoặc có đủ 4 cờ');
});

async function serverScore(apiCall, examId, keyQs, answers) {
  const s = ok(await post(apiCall, '/sessions/start', { examId, mode: 'PRACTICE', totalQuestions: keyQs.length }, 'STUDENT'), 'start session');
  for (const q of keyQs) {
    const a = answers[q.questionId];
    if (!a || (a.payload == null && a.selectedOptionId == null)) continue;
    const body = a.payload ? { questionId: q.questionId, answerPayload: a.payload } : { questionId: q.questionId, selectedOptionId: a.selectedOptionId };
    ok(await post(apiCall, `/sessions/${s.id}/answers`, body, 'STUDENT'), `answer q${q.questionId}`);
  }
  return Number(ok(await post(apiCall, `/sessions/${s.id}/submit`, undefined, 'STUDENT'), 'submit session').rawScore);
}

registerCase('VF-F04', 'Chấm offline scoreVsat khớp điểm server (1 bộ đáp án đúng + 10 bộ ngẫu nhiên)', async (ctx) => {
  const id = await sharedPublished(ctx);
  const keyQs = ok(await ctx.apiCall(`/admin/exams/${id}/questions`, {}, 'CONTENT_ADMIN'), 'GET key');
  const perfect = await serverScore(ctx.apiCall, id, keyQs, makeAnswers(keyQs));
  const mismatches = [];
  for (let i = 0; i < 10; i++) {
    const answers = makeAnswers(keyQs, mulberry32(1000 + i));
    const expected = scoreVsat(keyQs, answers);
    const actual = await serverScore(ctx.apiCall, id, keyQs, answers);
    if (Math.abs(actual - expected) > 1e-6) mismatches.push({ seed: 1000 + i, expected, actual });
  }
  ctx.evidence.scores = { perfect, mismatches };
  ctx.assertEqual(perfect, 150, 'bộ đáp án đúng đạt 150');
  ctx.assertDeepEqual(mismatches, [], 'điểm server = điểm oracle');
});

registerCase('VF-F05', 'Bundle của đề chưa PUBLISHED trả 404', async (ctx) => {
  const id = await draftExam(ctx, 'VF_F05', []);
  const res = await bundle(ctx.apiCall, id);
  ctx.evidence.bundle = errInfo(res);
  ctx.assertStatus(res.status, 404, 'đề DRAFT không tải được');
});

// ============================================================================
// Q. Cấu trúc câu hỏi khi tạo qua API
// ============================================================================

const createRaw = async (ctx, type, options) => {
  const p = await pool(ctx);
  return post(ctx.apiCall, '/collaborator/questions', questionBody(p.subjectId, p.topicId, type, options, `VF ${RUN} Q ${type} ${++seq}`), 'COLLABORATOR');
};

registerCase('VF-Q01', 'Tạo câu Đ/S 3 ý bị chặn (G4)', async (ctx) => {
  const res = await createRaw(ctx, 'TRUE_FALSE', OPTION_SETS.TRUE_FALSE().slice(0, 3));
  ctx.evidence.create = errInfo(res);
  ctx.assertStatus(res.status, 400, 'Đ/S phải đủ 4 ý a–d');
});

registerCase('VF-Q02', 'Tạo câu 4 lựa chọn chỉ 2 phương án bị chặn (G5)', async (ctx) => {
  const res = await createRaw(ctx, 'SINGLE_CHOICE', OPTION_SETS.SINGLE_CHOICE().slice(1, 3));
  ctx.evidence.create = errInfo(res);
  ctx.assertStatus(res.status, 400, 'phải đủ 4 phương án');
});

registerCase('VF-Q03', 'Tạo câu ghép hợp 2 cặp bị chặn (G6)', async (ctx) => {
  const res = await createRaw(ctx, 'MATCHING', OPTION_SETS.MATCHING().slice(0, 2));
  ctx.evidence.create = errInfo(res);
  ctx.assertStatus(res.status, 400, 'ghép hợp phải đủ 4 tiểu mục');
});

registerCase('VF-Q04', 'Tạo câu trả lời ngắn không có đáp án bị chặn', async (ctx) => {
  const res = await createRaw(ctx, 'SHORT_ANSWER', [['A', '12.5', false]]);
  ctx.evidence.create = errInfo(res);
  ctx.assertStatus(res.status, 400, 'TLN bắt buộc có đáp án');
});

await runSuite('vsat-format');
