#!/usr/bin/env node
/**
 * V-SAT exam format oracle. Spec: docs/testing/VSAT_EXAM_FORMAT_TEST_PLAN.md §1.
 * Question shape: { questionId, questionType, subjectId?, status?, maxRawScore?, options: [{ id?, optionLabel, optionText, isCorrect? }] }
 * Exam shape:     { subjectId?, subjectCode?, durationMinutes?, scoringProfileValid? }
 * Self-check:     node lib/vsat-format.mjs --self-check
 */
import assert from 'node:assert/strict';

export const PARTS = ['TRUE_FALSE', 'SINGLE_CHOICE', 'MATCHING', 'SHORT_ANSWER'];
export const PART_COUNTS = { TRUE_FALSE: 9, SINGLE_CHOICE: 6, MATCHING: 5, SHORT_ANSWER: 5 };
export const TOTAL_QUESTIONS = 25;
export const MAX_RAW = 150;
// ponytail: Ngữ văn (essay part, 21 questions) out of scope; add LIT rules when the essay type exists.
const NINETY_MIN_SUBJECTS = new Set(['MATH', 'LIT']);
const TF_POINTS = [0, 1, 2, 3, 6];
const TF_LABELS = ['a', 'b', 'c', 'd'];
const KEY_FIELDS = ['correctOptionIds', 'correctAnswer', 'explanation', 'explanationHtml'];
const COMPOSABLE = new Set(['APPROVED', 'PUBLISHED']);

/** Mirrors SessionServiceImpl.isMatchingChoiceEqual: target after "⇄" or "->", then after last ":". */
export function matchTarget(text) {
  if (text == null) return '';
  let s = String(text).trim();
  if (s.includes('⇄')) s = s.slice(s.indexOf('⇄') + 1).trim();
  else if (s.includes('->')) s = s.slice(s.indexOf('->') + 2).trim();
  if (s.includes(':')) s = s.slice(s.lastIndexOf(':') + 1).trim();
  return s;
}

const label = (o) => String(o.optionLabel ?? '').trim();
const filled = (v) => v != null && v !== '' && !(Array.isArray(v) && v.length === 0);

/** Per-question structure codes. studentBundle=true skips answer-key checks (keys must not be there). */
export function questionErrors(q, { studentBundle = false } = {}) {
  const opts = q.options ?? [];
  switch (q.questionType) {
    case 'TRUE_FALSE': {
      const labels = opts.map((o) => label(o).toLowerCase()).sort().join();
      const keysOk = studentBundle || opts.every((o) => typeof o.isCorrect === 'boolean');
      return labels === TF_LABELS.join() && keysOk ? [] : ['VF_TF4'];
    }
    case 'SINGLE_CHOICE': {
      const keysOk = studentBundle || opts.filter((o) => o.isCorrect === true).length === 1;
      return opts.length === 4 && keysOk ? [] : ['VF_MCQ4'];
    }
    case 'MATCHING': {
      const distinct = new Set(opts.map(label)).size === 4;
      const keysOk = studentBundle || opts.every((o) => matchTarget(o.optionText) !== '');
      return opts.length === 4 && distinct && keysOk ? [] : ['VF_MATCH'];
    }
    case 'SHORT_ANSWER': {
      if (studentBundle) return [];
      const key = opts.find((o) => o.isCorrect === true || opts.length === 1);
      return key && String(key.optionText ?? '').trim() ? [] : ['VF_SA_KEY'];
    }
    default:
      return [];
  }
}

/** Fields in a student-facing question that reveal the answer. */
export function keyLeaks(q) {
  const opts = q.options ?? [];
  const tag = q.questionCode ?? q.questionId;
  const leaks = KEY_FIELDS.filter((f) => filled(q[f])).map((f) => `${tag}.${f}`);
  if (opts.some((o) => o.isCorrect != null)) leaks.push(`${tag}.options[].isCorrect`);
  if (q.questionType === 'MATCHING' && opts.some((o) => /⇄|->/.test(String(o.optionText ?? '')))) {
    leaks.push(`${tag}.options[].optionText(matching key)`);
  }
  if (q.questionType === 'SHORT_ANSWER' && opts.some((o) => filled(o.optionText))) {
    leaks.push(`${tag}.options[].optionText(short-answer key)`);
  }
  return leaks;
}

/** Returns sorted unique VF_* codes; [] means the exam matches V-SAT format. */
export function validateVsatExam(exam, questions, { studentBundle = false } = {}) {
  const errs = new Set();
  const n = questions.length;

  // VF_MIX / VF_TOTAL are only meaningful once the count is right; VF_COUNT reports the rest.
  if (n !== TOTAL_QUESTIONS) {
    errs.add('VF_COUNT');
  } else {
    const counts = Object.fromEntries(PARTS.map((p) => [p, 0]));
    for (const q of questions) if (q.questionType in counts) counts[q.questionType]++;
    if (PARTS.some((p) => counts[p] !== PART_COUNTS[p])) errs.add('VF_MIX');
    const scores = questions.map((q) => q.maxRawScore);
    const total = scores.reduce((s, v) => s + Number(v ?? NaN), 0);
    if (scores.some((v) => v == null) || Math.abs(total - MAX_RAW) > 1e-6) errs.add('VF_TOTAL');
  }

  const part = questions.map((q) => PARTS.indexOf(q.questionType));
  if (part.some((p, i) => p < 0 || (i > 0 && p < part[i - 1]))) errs.add('VF_ORDER');

  for (const q of questions) for (const e of questionErrors(q, { studentBundle })) errs.add(e);

  if (exam.subjectId != null && questions.some((q) => q.subjectId != null && q.subjectId !== exam.subjectId)) {
    errs.add('VF_SUBJECT');
  }
  if (questions.some((q) => q.status != null && !COMPOSABLE.has(q.status))) errs.add('VF_STATUS');
  if (new Set(questions.map((q) => q.questionId)).size !== n) errs.add('VF_DUP');
  if (exam.subjectCode != null && exam.durationMinutes !== (NINETY_MIN_SUBJECTS.has(exam.subjectCode) ? 90 : 60)) {
    errs.add('VF_DURATION');
  }
  if (exam.scoringProfileValid === false) errs.add('VF_PROFILE');
  if (studentBundle && questions.some((q) => keyLeaks(q).length > 0)) errs.add('VF_KEY_LEAK');

  return [...errs].sort();
}

// ---------------------------------------------------------------- scoring

function toBool(v) {
  if (typeof v === 'boolean') return v;
  if (typeof v === 'number') return Math.trunc(v) === 1;
  return ['true', '1', 't', 'd', 'dung'].includes(String(v).trim().toLowerCase());
}

// ponytail: Number() is looser than Java parseDouble (accepts "0x10"); fine for generated answers.
const toNum = (s) => (s.trim() === '' ? NaN : Number(s));

function shortAnswerEqual(u, e) {
  if (u.toLowerCase() === e.toLowerCase()) return true;
  const a = toNum(u);
  const b = toNum(e);
  return Number.isFinite(a) && Number.isFinite(b) && Math.abs(a - b) < 1e-4;
}

/** answer: { selectedOptionId?, payload? } — same contract as POST /sessions/{id}/answers. */
export function scoreQuestion(q, answer) {
  if (!answer) return 0;
  const opts = q.options ?? [];
  const p = answer.payload ?? {};
  switch (q.questionType) {
    case 'TRUE_FALSE': {
      let k = 0;
      opts.forEach((o, i) => {
        const key = o.optionLabel != null ? String(o.optionLabel).trim().toLowerCase() : String.fromCharCode(97 + i);
        const v = p[key] ?? p[key.toUpperCase()];
        if (v != null && toBool(v) === (o.isCorrect === true)) k++;
      });
      return k > 4 ? 6 : TF_POINTS[k];
    }
    case 'MATCHING':
      return 1.5 * opts.filter((o) => {
        const v = p[label(o)];
        return v != null && o.optionText != null
          && matchTarget(o.optionText).toLowerCase() === String(v).trim().toLowerCase();
      }).length;
    case 'SHORT_ANSWER': {
      let v = p.value ?? p.answer ?? p.text;
      if (v == null && Object.keys(p).length) v = Object.values(p)[0];
      const key = opts.find((o) => o.isCorrect === true || opts.length === 1);
      return shortAnswerEqual(v == null ? '' : String(v).trim(), String(key?.optionText ?? '').trim()) ? 6 : 0;
    }
    default: {
      const id = Number(answer.selectedOptionId ?? p.selectedOptionId);
      return opts.some((o) => o.id === id && o.isCorrect === true) ? 6 : 0;
    }
  }
}

export const scoreVsat = (questions, answers) =>
  questions.reduce((s, q) => s + scoreQuestion(q, answers[q.questionId]), 0);

// ---------------------------------------------------------------- answer generators

export function mulberry32(seed) {
  return () => {
    seed = (seed + 0x6d2b79f5) | 0;
    let t = Math.imul(seed ^ (seed >>> 15), 1 | seed);
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t;
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  };
}

/** rng=null gives the answer key itself (perfect score). */
export function makeAnswers(questions, rng = null) {
  const pick = (arr) => arr[Math.floor(rng() * arr.length)];
  const out = {};
  for (const q of questions) {
    const opts = q.options ?? [];
    switch (q.questionType) {
      case 'TRUE_FALSE':
        out[q.questionId] = { payload: Object.fromEntries(opts.map((o) => [label(o).toLowerCase(), rng ? rng() < 0.5 : o.isCorrect === true])) };
        break;
      case 'MATCHING':
        out[q.questionId] = { payload: Object.fromEntries(opts.map((o) => [label(o), rng ? pick(['A', 'B', 'C', 'D']) : matchTarget(o.optionText)])) };
        break;
      case 'SHORT_ANSWER': {
        const key = String(opts.find((o) => o.isCorrect === true || opts.length === 1)?.optionText ?? '');
        out[q.questionId] = { payload: { value: rng ? pick([key, 'sai']) : key } };
        break;
      }
      default:
        out[q.questionId] = { selectedOptionId: (rng ? pick(opts) : opts.find((o) => o.isCorrect === true))?.id };
    }
  }
  return out;
}

// ---------------------------------------------------------------- self-check

function sampleExam() {
  let id = 0;
  const q = (questionType, options) => {
    id++;
    return { questionId: id, questionType, subjectId: 1, status: 'APPROVED', maxRawScore: 6,
      options: options.map(([optionLabel, optionText, isCorrect], i) => ({ id: id * 10 + i, optionLabel, optionText, isCorrect })) };
  };
  const make = {
    TRUE_FALSE: () => q('TRUE_FALSE', [['a', 'x', true], ['b', 'x', false], ['c', 'x', true], ['d', 'x', false]]),
    SINGLE_CHOICE: () => q('SINGLE_CHOICE', [['A', 'x', false], ['B', 'x', false], ['C', 'x', true], ['D', 'x', false]]),
    MATCHING: () => q('MATCHING', [['1', 'p ⇄ C', true], ['2', 'q ⇄ A', true], ['3', 'r ⇄ D', true], ['4', 's ⇄ B', true]]),
    SHORT_ANSWER: () => q('SHORT_ANSWER', [['A', '12.5', true]]),
  };
  const questions = PARTS.flatMap((p) => Array.from({ length: PART_COUNTS[p] }, make[p]));
  return { exam: { subjectId: 1, subjectCode: 'MATH', durationMinutes: 90, scoringProfileValid: true }, questions, make };
}

export function selfCheck() {
  const base = sampleExam();
  assert.deepEqual(validateVsatExam(base.exam, base.questions), [], 'valid exam must pass');

  const broken = {
    VF_COUNT: (e) => e.questions.pop(),
    VF_MIX: (e) => { e.questions[9] = { ...base.make.TRUE_FALSE(), questionId: 900 }; },
    VF_ORDER: (e) => { [e.questions[0], e.questions[9]] = [e.questions[9], e.questions[0]]; },
    VF_TOTAL: (e) => { e.questions[0].maxRawScore = 5; },
    VF_TF4: (e) => { e.questions[0].options.pop(); },
    VF_MCQ4: (e) => { e.questions[9].options[0].isCorrect = true; },
    VF_MATCH: (e) => { e.questions[15].options.splice(2); },
    VF_SA_KEY: (e) => { e.questions[20].options[0].optionText = ' '; },
    VF_SUBJECT: (e) => { e.questions[3].subjectId = 2; },
    VF_STATUS: (e) => { e.questions[3].status = 'DRAFT'; },
    VF_DUP: (e) => { e.questions[1].questionId = e.questions[0].questionId; },
    VF_DURATION: (e) => { e.exam.durationMinutes = 60; },
    VF_PROFILE: (e) => { e.exam.scoringProfileValid = false; },
  };
  for (const [code, mutate] of Object.entries(broken)) {
    const e = structuredClone({ exam: base.exam, questions: base.questions });
    mutate(e);
    assert.deepEqual(validateVsatExam(e.exam, e.questions), [code], `broken exam must fail only ${code}`);
  }
  const leaky = structuredClone(base.questions);
  assert.deepEqual(validateVsatExam(base.exam, leaky, { studentBundle: true }), ['VF_KEY_LEAK']);
  const clean = leaky.map((q) => ({ ...q, options: q.questionType === 'SHORT_ANSWER' ? [] : q.options.map(({ optionLabel }) => ({ optionLabel, optionText: 'x' })) }));
  assert.deepEqual(validateVsatExam(base.exam, clean, { studentBundle: true }), [], 'key-free bundle passes');

  const qs = base.questions;
  assert.equal(scoreVsat(qs, makeAnswers(qs)), MAX_RAW, 'answer key scores 150');
  assert.equal(scoreVsat(qs, {}), 0, 'no answers scores 0');
  const tf = qs[0];
  assert.equal(scoreQuestion(tf, { payload: { a: true, b: false, c: true, d: true } }), 3, 'TF 3/4 = 3');
  assert.equal(scoreQuestion(tf, { payload: { A: 'D', b: 'false' } }), 2, 'TF upper-case key + D/S strings');
  assert.equal(scoreQuestion(qs[15], { payload: { 1: 'c', 2: 'A', 3: 'B' } }), 3, 'matching 2/4 = 3');
  assert.equal(scoreQuestion(qs[20], { payload: { value: '12.50' } }), 6, 'short answer numeric tolerance');
  assert.equal(scoreQuestion(qs[9], { selectedOptionId: qs[9].options[2].id }), 6, 'MCQ correct = 6');
  const r = makeAnswers(qs, mulberry32(7));
  assert.ok(scoreVsat(qs, r) >= 0 && scoreVsat(qs, r) <= MAX_RAW, 'random answers stay in range');
  return Object.keys(broken).length + 1;
}

if (process.argv.includes('--self-check')) {
  const n = selfCheck();
  console.log(`vsat-format self-check OK (1 valid + ${n} broken/leak exams, scoring checks)`);
}
