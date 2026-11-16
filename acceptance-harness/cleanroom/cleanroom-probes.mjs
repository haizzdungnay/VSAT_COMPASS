import { resolve } from 'node:path';
import { inflateRawSync, deflateRawSync } from 'node:zlib';
import {
  registerCase,
  runSuite,
  getAuthToken,
  apiCall,
  queryDb
} from '../lib/test-harness.mjs';

const RUN_TAG = Date.now().toString().slice(-6);

function uid(prefix) {
  return `${prefix}_${RUN_TAG}_${Math.floor(Math.random() * 100000)}`;
}

function parseIntDb(sql) {
  const res = queryDb(sql).trim();
  const m = res.match(/-?\d+/);
  return m ? parseInt(m[0], 10) : 0;
}

function parseNumDb(sql) {
  const res = queryDb(sql).trim();
  const n = Number(res);
  return Number.isFinite(n) ? n : NaN;
}

function truncResp(res) {
  const body = res.json ?? res.text;
  let str = typeof body === 'string' ? body : JSON.stringify(body);
  if (str && str.length > 700) {
    str = str.slice(0, 700) + '...<truncated>';
  }
  return { status: res.status, body: str };
}

// ---------------------------------------------------------------------------
// Minimal Stdlib ZIP / XLSX helpers (for CR-20 .. CR-25)
// ---------------------------------------------------------------------------
const CRC_TABLE = (() => {
  const t = new Uint32Array(256);
  for (let i = 0; i < 256; i++) {
    let c = i;
    for (let k = 0; k < 8; k++) {
      c = (c & 1) ? (0xedb88320 ^ (c >>> 1)) : (c >>> 1);
    }
    t[i] = c >>> 0;
  }
  return t;
})();

function crc32(buf) {
  let c = 0xffffffff;
  for (let i = 0; i < buf.length; i++) {
    c = CRC_TABLE[(c ^ buf[i]) & 0xff] ^ (c >>> 8);
  }
  return (c ^ 0xffffffff) >>> 0;
}

function readZipEntries(buf) {
  let eocdOffset = -1;
  for (let i = buf.length - 22; i >= Math.max(0, buf.length - 65558); i--) {
    if (buf.readUInt32LE(i) === 0x06054b50) {
      eocdOffset = i;
      break;
    }
  }
  if (eocdOffset === -1) throw new Error('Invalid ZIP: EOCD not found');

  const cdCount = buf.readUInt16LE(eocdOffset + 10);
  let ptr = buf.readUInt32LE(eocdOffset + 16);
  const entries = new Map();

  for (let i = 0; i < cdCount; i++) {
    if (buf.readUInt32LE(ptr) !== 0x02014b50) {
      throw new Error('Invalid ZIP: CD signature mismatch');
    }
    const method = buf.readUInt16LE(ptr + 10);
    const compSize = buf.readUInt32LE(ptr + 20);
    const nameLen = buf.readUInt16LE(ptr + 28);
    const extraLen = buf.readUInt16LE(ptr + 30);
    const commentLen = buf.readUInt16LE(ptr + 32);
    const localOffset = buf.readUInt32LE(ptr + 42);
    const name = buf.subarray(ptr + 46, ptr + 46 + nameLen).toString('utf8');
    ptr += 46 + nameLen + extraLen + commentLen;

    const lNameLen = buf.readUInt16LE(localOffset + 26);
    const lExtraLen = buf.readUInt16LE(localOffset + 28);
    const dataStart = localOffset + 30 + lNameLen + lExtraLen;
    const compData = buf.subarray(dataStart, dataStart + compSize);
    const content = method === 0 ? compData : method === 8 ? inflateRawSync(compData) : null;
    if (content) entries.set(name, content);
  }
  return entries;
}

function xmlUnescape(s) {
  return s
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&quot;/g, '"')
    .replace(/&apos;/g, "'")
    .replace(/&amp;/g, '&');
}

function xmlEscape(s) {
  return String(s ?? '')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&apos;');
}

function colRefToIndex(ref) {
  const letters = ref.replace(/\d+$/, '');
  let idx = 0;
  for (let i = 0; i < letters.length; i++) {
    idx = idx * 26 + (letters.charCodeAt(i) - 64);
  }
  return idx - 1;
}

function indexToColRef(idx) {
  let s = '';
  let n = idx + 1;
  while (n > 0) {
    const rem = (n - 1) % 26;
    s = String.fromCharCode(65 + rem) + s;
    n = Math.floor((n - 1) / 26);
  }
  return s;
}

function extractTemplateHeaders(xlsxBuf) {
  const entries = readZipEntries(xlsxBuf);
  const sharedStrings = [];
  if (entries.has('xl/sharedStrings.xml')) {
    const ssXml = entries.get('xl/sharedStrings.xml').toString('utf8');
    const siMatches = ssXml.matchAll(/<si\b[^>]*>([\s\S]*?)<\/si>/g);
    for (const m of siMatches) {
      const tParts = [...m[1].matchAll(/<t\b[^>]*>([\s\S]*?)<\/t>/g)].map(x => xmlUnescape(x[1]));
      sharedStrings.push(tParts.join(''));
    }
  }

  let sheetKey = 'xl/worksheets/sheet1.xml';
  if (!entries.has(sheetKey)) {
    for (const k of entries.keys()) {
      if (k.startsWith('xl/worksheets/sheet') && k.endsWith('.xml')) {
        sheetKey = k;
        break;
      }
    }
  }
  const sheetXml = entries.get(sheetKey).toString('utf8');
  const row1Match = sheetXml.match(/<row\b[^>]*r="1"[^>]*>([\s\S]*?)<\/row>/);
  if (!row1Match) throw new Error('Template sheet1.xml missing row 1');

  const headers = [];
  const cellMatches = row1Match[1].matchAll(/<c\b([^>]*)>([\s\S]*?)<\/c>/g);
  for (const cm of cellMatches) {
    const attrs = cm[1];
    const body = cm[2];
    const rMatch = attrs.match(/\br="([A-Z]+\d+)"/);
    const tMatch = attrs.match(/\bt="([^"]+)"/);
    const colIdx = rMatch ? colRefToIndex(rMatch[1]) : headers.length;
    const type = tMatch ? tMatch[1] : '';

    let val = '';
    if (type === 's') {
      const vMatch = body.match(/<v>(\d+)<\/v>/);
      if (vMatch) val = sharedStrings[parseInt(vMatch[1], 10)] ?? '';
    } else if (type === 'inlineStr') {
      const tParts = [...body.matchAll(/<t\b[^>]*>([\s\S]*?)<\/t>/g)].map(x => xmlUnescape(x[1]));
      val = tParts.join('');
    } else {
      const vMatch = body.match(/<v>([\s\S]*?)<\/v>/);
      if (vMatch) val = xmlUnescape(vMatch[1]);
    }
    headers[colIdx] = val.trim();
  }
  return headers;
}

function buildZip(files) {
  const localParts = [];
  const cdParts = [];
  let offset = 0;

  for (const { name, content } of files) {
    const nameBuf = Buffer.from(name, 'utf8');
    const rawBuf = Buffer.isBuffer(content) ? content : Buffer.from(content, 'utf8');
    const compBuf = deflateRawSync(rawBuf);
    const crc = crc32(rawBuf);

    const localHeader = Buffer.alloc(30);
    localHeader.writeUInt32LE(0x04034b50, 0);
    localHeader.writeUInt16LE(20, 4);
    localHeader.writeUInt16LE(0, 6);
    localHeader.writeUInt16LE(8, 8);
    localHeader.writeUInt16LE(0, 10);
    localHeader.writeUInt16LE(0, 12);
    localHeader.writeUInt32LE(crc, 14);
    localHeader.writeUInt32LE(compBuf.length, 18);
    localHeader.writeUInt32LE(rawBuf.length, 22);
    localHeader.writeUInt16LE(nameBuf.length, 26);
    localHeader.writeUInt16LE(0, 28);

    localParts.push(localHeader, nameBuf, compBuf);

    const cdHeader = Buffer.alloc(46);
    cdHeader.writeUInt32LE(0x02014b50, 0);
    cdHeader.writeUInt16LE(20, 4);
    cdHeader.writeUInt16LE(20, 6);
    cdHeader.writeUInt16LE(0, 8);
    cdHeader.writeUInt16LE(8, 10);
    cdHeader.writeUInt16LE(0, 12);
    cdHeader.writeUInt16LE(0, 14);
    cdHeader.writeUInt32LE(crc, 16);
    cdHeader.writeUInt32LE(compBuf.length, 20);
    cdHeader.writeUInt32LE(rawBuf.length, 24);
    cdHeader.writeUInt16LE(nameBuf.length, 28);
    cdHeader.writeUInt16LE(0, 30);
    cdHeader.writeUInt16LE(0, 32);
    cdHeader.writeUInt16LE(0, 34);
    cdHeader.writeUInt16LE(0, 36);
    cdHeader.writeUInt32LE(0, 38);
    cdHeader.writeUInt32LE(offset, 42);

    cdParts.push(cdHeader, nameBuf);
    offset += localHeader.length + nameBuf.length + compBuf.length;
  }

  const cdBuf = Buffer.concat(cdParts);
  const eocd = Buffer.alloc(22);
  eocd.writeUInt32LE(0x06054b50, 0);
  eocd.writeUInt16LE(0, 4);
  eocd.writeUInt16LE(0, 6);
  eocd.writeUInt16LE(files.length, 8);
  eocd.writeUInt16LE(files.length, 10);
  eocd.writeUInt32LE(cdBuf.length, 12);
  eocd.writeUInt32LE(offset, 16);
  eocd.writeUInt16LE(0, 20);

  return Buffer.concat([...localParts, cdBuf, eocd]);
}

function buildXlsxBuffer(headers, dataRows) {
  const allRows = [headers, ...dataRows];
  const rowsXml = allRows
    .map((row, rIdx) => {
      const rNum = rIdx + 1;
      const cellsXml = headers
        .map((h, cIdx) => {
          const val = Array.isArray(row) ? row[cIdx] : row[h];
          if (val === undefined || val === null || val === '') return '';
          const ref = `${indexToColRef(cIdx)}${rNum}`;
          return `<c r="${ref}" t="inlineStr"><is><t>${xmlEscape(val)}</t></is></c>`;
        })
        .filter(Boolean)
        .join('');
      return `<row r="${rNum}">${cellsXml}</row>`;
    })
    .join('');

  const sheetXml = `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>`
    + `<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">`
    + `<sheetData>${rowsXml}</sheetData></worksheet>`;

  const contentTypes = `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>`
    + `<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">`
    + `<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>`
    + `<Default Extension="xml" ContentType="application/xml"/>`
    + `<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>`
    + `<Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>`
    + `</Types>`;

  const rootRels = `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>`
    + `<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">`
    + `<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>`
    + `</Relationships>`;

  const workbookXml = `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>`
    + `<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">`
    + `<sheets><sheet name="Questions" sheetId="1" r:id="rId1"/></sheets></workbook>`;

  const wbRels = `<?xml version="1.0" encoding="UTF-8" standalone="yes"?>`
    + `<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">`
    + `<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>`
    + `</Relationships>`;

  return buildZip([
    { name: '[Content_Types].xml', content: contentTypes },
    { name: '_rels/.rels', content: rootRels },
    { name: 'xl/workbook.xml', content: workbookXml },
    { name: 'xl/_rels/workbook.xml.rels', content: wbRels },
    { name: 'xl/worksheets/sheet1.xml', content: sheetXml }
  ]);
}

let cachedTemplateHeaders = null;
async function getTemplateHeaders() {
  if (cachedTemplateHeaders) return cachedTemplateHeaders;
  const token = await getAuthToken('CONTENT_ADMIN');
  const rawBase = (process.env.API_BASE_URL || 'http://127.0.0.1:18080').replace(/\/api\/v1\/?$/, '').replace(/\/+$/, '');
  const resp = await fetch(`${rawBase}/api/v1/admin/questions/import/template`, {
    headers: { Authorization: `Bearer ${token}` }
  });
  if (!resp.ok) {
    throw new Error(`Failed to fetch import template: HTTP ${resp.status}`);
  }
  const buf = Buffer.from(await resp.arrayBuffer());
  cachedTemplateHeaders = extractTemplateHeaders(buf);
  return cachedTemplateHeaders;
}

async function importExcelRows(rows, dryRun) {
  const headers = await getTemplateHeaders();
  const xlsxBuf = buildXlsxBuffer(headers, rows);
  const form = new FormData();
  form.append('file', new Blob([xlsxBuf], {
    type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
  }), 'cleanroom_import.xlsx');
  return apiCall(`/admin/questions/import?dryRun=${dryRun}`, {
    method: 'POST',
    body: form
  }, 'CONTENT_ADMIN');
}

// Map flexible header names from template to row values
function buildImportRow(headers, spec) {
  const row = {};
  const mapLower = new Map();
  for (const [k, v] of Object.entries(spec)) {
    mapLower.set(k.toLowerCase(), v);
  }
  for (const h of headers) {
    const hl = h.toLowerCase().trim();
    if (mapLower.has(hl)) {
      row[h] = mapLower.get(hl);
      continue;
    }
    // Common alias fallback matching
    const norm = hl.replace(/[^a-z0-9]/g, '');
    if (hl.includes('subject') && mapLower.has('subjectcode')) row[h] = mapLower.get('subjectcode');
    else if (hl.includes('topic') && !hl.includes('sub') && mapLower.has('topiccode')) row[h] = mapLower.get('topiccode');
    else if (hl.includes('difficulty') && mapLower.has('difficulty')) row[h] = mapLower.get('difficulty');
    else if ((hl.includes('cognitive') || hl.includes('level')) && mapLower.has('cognitivelevel')) row[h] = mapLower.get('cognitivelevel');
    else if (hl.includes('type') && mapLower.has('questiontype')) row[h] = mapLower.get('questiontype');
    else if ((norm.includes('questiontext') || hl === 'content' || hl === 'question') && mapLower.has('questiontext')) row[h] = mapLower.get('questiontext');
    else if (norm === 'optiona' || hl === 'a') row[h] = mapLower.get('optiona');
    else if (norm === 'optionb' || hl === 'b') row[h] = mapLower.get('optionb');
    else if (norm === 'optionc' || hl === 'c') row[h] = mapLower.get('optionc');
    else if (norm === 'optiond' || hl === 'd') row[h] = mapLower.get('optiond');
    else if (hl.includes('correct') && mapLower.has('correctanswer')) row[h] = mapLower.get('correctanswer');
    else if (hl.includes('explanation') && mapLower.has('explanation')) row[h] = mapLower.get('explanation');
  }
  return row;
}

// ---------------------------------------------------------------------------
// Fixture & Question Pool Builders
// ---------------------------------------------------------------------------
function getSubjectId(code) {
  return parseIntDb(`SELECT id FROM subjects WHERE code = '${code}' LIMIT 1;`);
}

function getTopicId(code) {
  return parseIntDb(`SELECT id FROM topics WHERE code = '${code}' LIMIT 1;`);
}

function getScoringProfileId() {
  return parseIntDb("SELECT id FROM scoring_profiles WHERE code = 'VSAT_2026_DRAFT_SPEC_150' LIMIT 1;");
}

function ensureEngTopic() {
  const engId = getSubjectId('ENG');
  queryDb(`
    INSERT INTO topics (subject_id, code, name, description, is_active, display_order)
    VALUES (${engId}, 'ENG_READ', 'Đọc hiểu Tiếng Anh', 'Topic test cleanroom', TRUE, 1)
    ON CONFLICT (code) DO NOTHING;
  `);
  return parseIntDb("SELECT id FROM topics WHERE code = 'ENG_READ' LIMIT 1;");
}

async function createApprovedQuestionViaApi({
  subjectId,
  topicId,
  questionType,
  questionText,
  explanation = 'Lời giải chi tiết',
  explanationHtml = '<p>Lời giải chi tiết</p>',
  options
}) {
  const createRes = await apiCall('/collaborator/questions', {
    method: 'POST',
    body: {
      subjectId,
      topicId,
      difficulty: 'MEDIUM',
      questionType,
      questionText,
      questionHtml: `<p>${questionText}</p>`,
      explanation,
      explanationHtml,
      options
    }
  }, 'COLLABORATOR');

  if (createRes.status !== 200 && createRes.status !== 201) {
    throw new Error(`createApprovedQuestionViaApi(${questionType}) failed ${createRes.status}: ${JSON.stringify(createRes.json || createRes.text)}`);
  }
  const qId = createRes.json?.data?.id;
  if (!qId) throw new Error(`Missing question id from create response: ${JSON.stringify(createRes.json)}`);

  const subRes = await apiCall(`/collaborator/questions/${qId}/submit-for-review`, {
    method: 'POST',
    body: {}
  }, 'COLLABORATOR');
  if (subRes.status !== 200) {
    throw new Error(`submit-for-review ${qId} failed ${subRes.status}: ${JSON.stringify(subRes.json || subRes.text)}`);
  }

  const appRes = await apiCall(`/admin/questions/${qId}/approve`, {
    method: 'POST',
    body: { comment: 'Cleanroom approved' }
  }, 'CONTENT_ADMIN');
  if (appRes.status !== 200) {
    throw new Error(`approve ${qId} failed ${appRes.status}: ${JSON.stringify(appRes.json || appRes.text)}`);
  }

  return qId;
}

// Create a full V-SAT 25-question pool (9 TF, 6 SC, 5 MATCH, 5 SA) via real API
async function createVsat25Pool(prefix, subjectId, topicId, saAnswerLiteral = '987654321') {
  const tfIds = [];
  const scIds = [];
  const matchIds = [];
  const saIds = [];

  for (let i = 1; i <= 9; i++) {
    const qId = await createApprovedQuestionViaApi({
      subjectId,
      topicId,
      questionType: 'TRUE_FALSE',
      questionText: `${prefix} TF #${i}: Khẳng định toán học`,
      options: [
        { optionLabel: 'a', optionText: `Mệnh đề a #${i}`, isCorrect: true, displayOrder: 1 },
        { optionLabel: 'b', optionText: `Mệnh đề b #${i}`, isCorrect: false, displayOrder: 2 },
        { optionLabel: 'c', optionText: `Mệnh đề c #${i}`, isCorrect: true, displayOrder: 3 },
        { optionLabel: 'd', optionText: `Mệnh đề d #${i}`, isCorrect: false, displayOrder: 4 }
      ]
    });
    tfIds.push(qId);
  }

  for (let i = 1; i <= 6; i++) {
    const qId = await createApprovedQuestionViaApi({
      subjectId,
      topicId,
      questionType: 'SINGLE_CHOICE',
      questionText: `${prefix} SC #${i}: Chọn đáp án đúng`,
      options: [
        { optionLabel: 'A', optionText: 'Phương án A', isCorrect: true, displayOrder: 1 },
        { optionLabel: 'B', optionText: 'Phương án B', isCorrect: false, displayOrder: 2 },
        { optionLabel: 'C', optionText: 'Phương án C', isCorrect: false, displayOrder: 3 },
        { optionLabel: 'D', optionText: 'Phương án D', isCorrect: false, displayOrder: 4 }
      ]
    });
    scIds.push(qId);
  }

  for (let i = 1; i <= 5; i++) {
    const qId = await createApprovedQuestionViaApi({
      subjectId,
      topicId,
      questionType: 'MATCHING',
      questionText: `${prefix} MATCH #${i}: Ghép cặp đúng`,
      options: [
        { optionLabel: '1', optionText: `Vế 1 #${i} ⇄ Đáp án A`, isCorrect: true, displayOrder: 1 },
        { optionLabel: '2', optionText: `Vế 2 #${i} ⇄ Đáp án B`, isCorrect: true, displayOrder: 2 },
        { optionLabel: '3', optionText: `Vế 3 #${i} ⇄ Đáp án C`, isCorrect: true, displayOrder: 3 },
        { optionLabel: '4', optionText: `Vế 4 #${i} ⇄ Đáp án D`, isCorrect: true, displayOrder: 4 }
      ]
    });
    matchIds.push(qId);
  }

  for (let i = 1; i <= 5; i++) {
    const qId = await createApprovedQuestionViaApi({
      subjectId,
      topicId,
      questionType: 'SHORT_ANSWER',
      questionText: `${prefix} SA #${i}: Nhập kết quả`,
      options: [
        { optionLabel: 'A', optionText: saAnswerLiteral, isCorrect: true, displayOrder: 1 }
      ]
    });
    saIds.push(qId);
  }

  return {
    tfIds,
    scIds,
    matchIds,
    saIds,
    allOrdered: [...tfIds, ...scIds, ...matchIds, ...saIds]
  };
}

function seedPendingReviewExamSql(examCode, subjectId, durationMinutes, orderedQuestionIds) {
  const spId = getScoringProfileId();
  const examId = parseIntDb(`
    INSERT INTO exams (
      exam_code, title, subject_id, scoring_profile_id, question_count, duration_minutes,
      difficulty, pricing_type, price, status, created_by, version
    )
    SELECT
      '${examCode}', 'Cleanroom ${examCode}', ${subjectId}, ${spId}, ${orderedQuestionIds.length}, ${durationMinutes},
      'MEDIUM', 'FREE', 0, 'PENDING_REVIEW', u.id, 1
    FROM users u WHERE u.email = 'contentadmin@vsat.com'
    RETURNING id;
  `);

  if (orderedQuestionIds.length > 0) {
    const valuesSql = orderedQuestionIds
      .map((qId, idx) => `(${examId}, ${qId}, ${idx + 1}, 6.00)`)
      .join(', ');
    queryDb(`
      INSERT INTO exam_questions (exam_id, question_id, question_order, max_raw_score)
      VALUES ${valuesSql};
    `);
  }
  return examId;
}

// Ensure a shared standard 25-question pool exists for reuse where read-only/non-destructive
let sharedMathPool = null;
async function getSharedMathPool() {
  if (sharedMathPool) return sharedMathPool;
  const mathId = getSubjectId('MATH');
  const topicId = getTopicId('MATH_ALG');
  sharedMathPool = await createVsat25Pool(uid('POOL_MATH'), mathId, topicId, '987654321');
  return sharedMathPool;
}

let sharedPublishedExamId = null;

// ===========================================================================
// NHÓM 1: Publish đề chuẩn & các biến thể sai cấu trúc V-SAT (CR-01 .. CR-07)
// ===========================================================================

registerCase('CR-01', 'Publish đề chuẩn 25 câu đúng tỉ lệ 9/6/5/5, đúng thứ tự, 90 phút Toán', async ({
  assertStatus, assertEqual, assertDbRow, evidence
}) => {
  const mathId = getSubjectId('MATH');
  const pool = await getSharedMathPool();
  const code = uid('CR01_EXAM');
  const examId = seedPendingReviewExamSql(code, mathId, 90, pool.allOrdered);

  const res = await apiCall(`/admin/exams/${examId}/publish`, { method: 'POST', body: {} }, 'SUPER_ADMIN');
  evidence.request = `POST /admin/exams/${examId}/publish (SUPER_ADMIN)`;
  evidence.response = truncResp(res);

  assertStatus(res.status, 200, 'CR-01 publish standard 25Q MATH exam');
  assertEqual(res.json?.data?.status, 'PUBLISHED', 'data.status === PUBLISHED');
  assertEqual(res.json?.data?.questionCount, 25, 'data.questionCount === 25');

  assertDbRow(
    `SELECT status FROM exams WHERE id = ${examId};`,
    val => val.trim() === 'PUBLISHED',
    'DB exams.status must be PUBLISHED'
  );
  assertDbRow(
    `SELECT string_agg(question_type::text || ':' || required_question_count::text, ',' ORDER BY section_order) FROM exam_sections WHERE exam_id = ${examId};`,
    val => val.trim() === 'TRUE_FALSE:9,SINGLE_CHOICE:6,MATCHING:5,SHORT_ANSWER:5',
    'DB exam_sections must have 4 sections 9/6/5/5 in exact order'
  );
  assertDbRow(
    `SELECT COALESCE(SUM(max_raw_score), 0) FROM exam_questions WHERE exam_id = ${examId};`,
    val => Number(val.trim()) === 150,
    'DB SUM(exam_questions.max_raw_score) must equal 150.00'
  );

  sharedPublishedExamId = examId;
});

registerCase('CR-02', 'Publish đề thiếu câu (< 25 câu) phải bị chặn', async ({
  assertStatus, assertEqual, assertDbRow, evidence
}) => {
  const mathId = getSubjectId('MATH');
  const pool = await getSharedMathPool();
  const examId = seedPendingReviewExamSql(uid('CR02_SHORT'), mathId, 90, [pool.scIds[0]]);

  const res = await apiCall(`/admin/exams/${examId}/publish`, { method: 'POST', body: {} }, 'SUPER_ADMIN');
  evidence.request = `POST /admin/exams/${examId}/publish (1 question)`;
  evidence.response = truncResp(res);

  assertStatus(res.status, 400, 'CR-02 publish incomplete exam must return 400');
  assertEqual(res.json?.success, false, 'success === false');
  assertDbRow(
    `SELECT status FROM exams WHERE id = ${examId};`,
    val => val.trim() === 'PENDING_REVIEW',
    'DB status must remain PENDING_REVIEW'
  );
});

registerCase('CR-03', 'Publish đề đủ 25 câu nhưng sai cơ cấu (25 SINGLE_CHOICE) phải bị chặn', async ({
  assertStatus, assertEqual, assertDbRow, evidence
}) => {
  const mathId = getSubjectId('MATH');
  const topicId = getTopicId('MATH_ALG');
  const extraScIds = [...(await getSharedMathPool()).scIds];
  while (extraScIds.length < 25) {
    extraScIds.push(await createApprovedQuestionViaApi({
      subjectId: mathId,
      topicId,
      questionType: 'SINGLE_CHOICE',
      questionText: uid('CR03_SC'),
      options: [
        { optionLabel: 'A', optionText: '1', isCorrect: true, displayOrder: 1 },
        { optionLabel: 'B', optionText: '2', isCorrect: false, displayOrder: 2 },
        { optionLabel: 'C', optionText: '3', isCorrect: false, displayOrder: 3 },
        { optionLabel: 'D', optionText: '4', isCorrect: false, displayOrder: 4 }
      ]
    }));
  }
  const examId = seedPendingReviewExamSql(uid('CR03_BADMIX'), mathId, 90, extraScIds.slice(0, 25));

  const res = await apiCall(`/admin/exams/${examId}/publish`, { method: 'POST', body: {} }, 'SUPER_ADMIN');
  evidence.request = `POST /admin/exams/${examId}/publish (25 SINGLE_CHOICE)`;
  evidence.response = truncResp(res);

  assertStatus(res.status, 400, 'CR-03 wrong 9/6/5/5 mix must return 400');
  assertEqual(res.json?.success, false, 'success === false');
  assertDbRow(
    `SELECT status FROM exams WHERE id = ${examId};`,
    val => val.trim() === 'PENDING_REVIEW',
    'DB status must remain PENDING_REVIEW'
  );
});

registerCase('CR-04', 'Publish đề đủ 25 câu 9/6/5/5 nhưng sai thứ tự phần thi phải bị chặn', async ({
  assertStatus, assertEqual, assertDbRow, evidence
}) => {
  const mathId = getSubjectId('MATH');
  const pool = await getSharedMathPool();
  const wrongOrder = [...pool.scIds, ...pool.tfIds, ...pool.matchIds, ...pool.saIds];
  const examId = seedPendingReviewExamSql(uid('CR04_ORDER'), mathId, 90, wrongOrder);

  const res = await apiCall(`/admin/exams/${examId}/publish`, { method: 'POST', body: {} }, 'SUPER_ADMIN');
  evidence.request = `POST /admin/exams/${examId}/publish (SC before TF)`;
  evidence.response = truncResp(res);

  assertStatus(res.status, 400, 'CR-04 wrong section order must return 400');
  assertEqual(res.json?.success, false, 'success === false');
  assertDbRow(
    `SELECT status FROM exams WHERE id = ${examId};`,
    val => val.trim() === 'PENDING_REVIEW',
    'DB status must remain PENDING_REVIEW'
  );
});

registerCase('CR-05', 'Tạo hoặc publish đề Toán với thời lượng sai chuẩn (60 phút) phải bị chặn', async ({
  assertStatus, assertDbRow, evidence
}) => {
  const mathId = getSubjectId('MATH');
  const code = uid('VSAT_BAD_DUR');
  const createRes = await apiCall('/admin/exams', {
    method: 'POST',
    body: {
      examCode: code,
      title: 'Toán sai giờ 60p',
      subjectId: mathId,
      durationMinutes: 60,
      difficulty: 'MEDIUM',
      pricingType: 'FREE',
      price: 0
    }
  }, 'CONTENT_ADMIN');
  evidence.createResponse = truncResp(createRes);

  if (createRes.status === 400) {
    assertStatus(createRes.status, 400, 'CR-05 POST /admin/exams rejected 60m MATH exam');
    assertDbRow(
      `SELECT COUNT(*) FROM exams WHERE exam_code = '${code}';`,
      val => parseInt(val, 10) === 0,
      'Rejected exam must not exist in DB'
    );
  } else {
    const examId = createRes.json?.data?.id;
    const pool = await getSharedMathPool();
    const valuesSql = pool.allOrdered.map((qId, idx) => `(${examId}, ${qId}, ${idx + 1}, 6.00)`).join(', ');
    queryDb(`INSERT INTO exam_questions (exam_id, question_id, question_order, max_raw_score) VALUES ${valuesSql};`);
    queryDb(`UPDATE exams SET status = 'PENDING_REVIEW', question_count = 25 WHERE id = ${examId};`);

    const pubRes = await apiCall(`/admin/exams/${examId}/publish`, { method: 'POST', body: {} }, 'SUPER_ADMIN');
    evidence.publishResponse = truncResp(pubRes);
    assertStatus(pubRes.status, 400, 'CR-05 publish MATH exam with 60m duration must return 400');
    assertDbRow(
      `SELECT status FROM exams WHERE id = ${examId};`,
      val => val.trim() !== 'PUBLISHED',
      'Wrong duration exam must never become PUBLISHED'
    );
  }
});

registerCase('CR-06', 'Đề Toán chứa câu hỏi thuộc môn khác (ENG) phải bị chặn', async ({
  assertStatus, assertDbRow, evidence
}) => {
  const mathId = getSubjectId('MATH');
  const engId = getSubjectId('ENG');
  const engTopicId = ensureEngTopic();

  const engQId = await createApprovedQuestionViaApi({
    subjectId: engId,
    topicId: engTopicId,
    questionType: 'SINGLE_CHOICE',
    questionText: uid('CR06_ENG_Q'),
    options: [
      { optionLabel: 'A', optionText: 'Yes', isCorrect: true, displayOrder: 1 },
      { optionLabel: 'B', optionText: 'No', isCorrect: false, displayOrder: 2 },
      { optionLabel: 'C', optionText: 'Maybe', isCorrect: false, displayOrder: 3 },
      { optionLabel: 'D', optionText: 'Never', isCorrect: false, displayOrder: 4 }
    ]
  });

  const draftRes = await apiCall('/admin/exams', {
    method: 'POST',
    body: {
      examCode: uid('CR06_DRAFT'),
      title: 'Toán thử gắn câu Anh',
      subjectId: mathId,
      durationMinutes: 90,
      difficulty: 'MEDIUM',
      pricingType: 'FREE',
      price: 0
    }
  }, 'CONTENT_ADMIN');
  assertStatus(draftRes.status, 201, 'Create DRAFT MATH exam');
  const examId = draftRes.json?.data?.id;

  const addRes = await apiCall(`/admin/exams/${examId}/questions`, {
    method: 'POST',
    body: { questionId: engQId }
  }, 'CONTENT_ADMIN');
  evidence.request = `POST /admin/exams/${examId}/questions {questionId:${engQId}}`;
  evidence.response = truncResp(addRes);

  assertStatus(addRes.status, 400, 'CR-06 adding ENG question to MATH exam must return 400');
  assertDbRow(
    `SELECT COUNT(*) FROM exam_questions WHERE exam_id = ${examId} AND question_id = ${engQId};`,
    val => parseInt(val, 10) === 0,
    'DB exam_questions must not contain ENG question'
  );
});

registerCase('CR-07', 'Publish đề có câu Đúng/Sai chỉ có 3 mệnh đề phải bị chặn', async ({
  assertStatus, assertEqual, assertDbRow, evidence
}) => {
  const mathId = getSubjectId('MATH');
  const topicId = getTopicId('MATH_ALG');
  const pool = await getSharedMathPool();

  // Create a TF question via API then delete 1 sub-item in DB (dirty data)
  const brokenTfId = await createApprovedQuestionViaApi({
    subjectId: mathId,
    topicId,
    questionType: 'TRUE_FALSE',
    questionText: uid('CR07_TF_3ITEMS'),
    options: [
      { optionLabel: 'a', optionText: 'Ý a', isCorrect: true, displayOrder: 1 },
      { optionLabel: 'b', optionText: 'Ý b', isCorrect: false, displayOrder: 2 },
      { optionLabel: 'c', optionText: 'Ý c', isCorrect: true, displayOrder: 3 },
      { optionLabel: 'd', optionText: 'Ý d', isCorrect: false, displayOrder: 4 }
    ]
  });
  queryDb(`DELETE FROM question_true_false_items WHERE question_id = ${brokenTfId} AND item_key = 'd';`);
  queryDb(`DELETE FROM question_options WHERE question_id = ${brokenTfId} AND lower(option_label) = 'd';`);

  const badOrder = [brokenTfId, ...pool.tfIds.slice(1), ...pool.scIds, ...pool.matchIds, ...pool.saIds];
  const examId = seedPendingReviewExamSql(uid('CR07_EXAM'), mathId, 90, badOrder);

  const res = await apiCall(`/admin/exams/${examId}/publish`, { method: 'POST', body: {} }, 'SUPER_ADMIN');
  evidence.request = `POST /admin/exams/${examId}/publish (TF with 3 items)`;
  evidence.response = truncResp(res);

  assertStatus(res.status, 400, 'CR-07 publish exam with 3-item TF question must return 400');
  assertEqual(res.json?.success, false, 'success === false');
  assertDbRow(
    `SELECT status FROM exams WHERE id = ${examId};`,
    val => val.trim() === 'PENDING_REVIEW',
    'DB status must remain PENDING_REVIEW'
  );
});

// ===========================================================================
// NHÓM 2: Phân quyền Publish (RBAC) (CR-08 .. CR-10)
// ===========================================================================

registerCase('CR-08', 'CONTENT_ADMIN không được phép publish đề', async ({
  assertStatus, assertDbRow, evidence
}) => {
  const mathId = getSubjectId('MATH');
  const pool = await getSharedMathPool();
  const examId = seedPendingReviewExamSql(uid('CR08_RBAC'), mathId, 90, pool.allOrdered);

  const res = await apiCall(`/admin/exams/${examId}/publish`, { method: 'POST', body: {} }, 'CONTENT_ADMIN');
  evidence.request = `POST /admin/exams/${examId}/publish (CONTENT_ADMIN)`;
  evidence.response = truncResp(res);

  assertStatus(res.status, 403, 'CR-08 CONTENT_ADMIN publish must return 403');
  assertDbRow(
    `SELECT status || '|' || COALESCE(publish_date::text, 'NULL') FROM exams WHERE id = ${examId};`,
    val => val.trim() === 'PENDING_REVIEW|NULL',
    'DB status must remain PENDING_REVIEW and publish_date IS NULL'
  );
});

registerCase('CR-09', 'COLLABORATOR không được phép publish đề', async ({
  assertStatus, assertDbRow, evidence
}) => {
  const mathId = getSubjectId('MATH');
  const pool = await getSharedMathPool();
  const examId = seedPendingReviewExamSql(uid('CR09_RBAC'), mathId, 90, pool.allOrdered);

  const res = await apiCall(`/admin/exams/${examId}/publish`, { method: 'POST', body: {} }, 'COLLABORATOR');
  evidence.request = `POST /admin/exams/${examId}/publish (COLLABORATOR)`;
  evidence.response = truncResp(res);

  assertStatus(res.status, 403, 'CR-09 COLLABORATOR publish must return 403');
  assertDbRow(
    `SELECT status FROM exams WHERE id = ${examId};`,
    val => val.trim() === 'PENDING_REVIEW',
    'DB status must remain PENDING_REVIEW'
  );
});

registerCase('CR-10', 'STUDENT và ANONYMOUS không được phép publish đề', async ({
  assertStatus, assertDbRow, evidence
}) => {
  const mathId = getSubjectId('MATH');
  const pool = await getSharedMathPool();
  const examId = seedPendingReviewExamSql(uid('CR10_RBAC'), mathId, 90, pool.allOrdered);

  const stuRes = await apiCall(`/admin/exams/${examId}/publish`, { method: 'POST', body: {} }, 'STUDENT');
  const anonRes = await apiCall(`/admin/exams/${examId}/publish`, { method: 'POST', body: {} }, null);
  evidence.studentResponse = truncResp(stuRes);
  evidence.anonResponse = truncResp(anonRes);

  assertStatus(stuRes.status, 403, 'CR-10 STUDENT publish must return 403');
  assertStatus(anonRes.status, 401, 'CR-10 ANONYMOUS publish must return 401');
  assertDbRow(
    `SELECT status FROM exams WHERE id = ${examId};`,
    val => val.trim() === 'PENDING_REVIEW',
    'DB status must remain PENDING_REVIEW'
  );
});

// ===========================================================================
// NHÓM 3: Sinh đề tự động theo Blueprint (CR-11 .. CR-14)
// ===========================================================================

registerCase('CR-11', 'Sinh đề mới theo blueprint MATH chọn đúng 25 câu 9/6/5/5 và đúng thứ tự phần', async ({
  assertStatus, assertEqual, assertDbRow, evidence
}) => {
  const mathId = getSubjectId('MATH');
  await getSharedMathPool();
  const bpId = parseIntDb("SELECT id FROM exam_blueprints WHERE code = 'VSAT_2026_BP_MATH' LIMIT 1;");
  const examCode = uid('GEN_MATH_OK');

  const res = await apiCall('/admin/exams/generate-by-matrix', {
    method: 'POST',
    body: {
      exam: {
        examCode,
        title: 'Sinh ma trận Toán CR-11',
        subjectId: mathId,
        durationMinutes: 90,
        difficulty: 'MEDIUM',
        pricingType: 'FREE',
        price: 0
      },
      blueprintId: bpId,
      variantCount: 1
    }
  }, 'CONTENT_ADMIN');
  evidence.request = `POST /admin/exams/generate-by-matrix (bpId=${bpId})`;
  evidence.response = truncResp(res);

  assertStatus(res.status, 200, 'CR-11 generate-by-matrix must return 200');
  assertEqual(res.json?.data?.selectedQuestions, 25, 'selectedQuestions === 25');
  assertEqual(Number(res.json?.data?.rawScore), 150, 'rawScore === 150');

  const genExamId = res.json?.data?.examId || parseIntDb(`SELECT id FROM exams WHERE exam_code = '${examCode}' LIMIT 1;`);
  assertDbRow(
    `SELECT COUNT(*) FROM exam_questions eq JOIN questions q ON q.id = eq.question_id WHERE eq.exam_id = ${genExamId} AND q.subject_id = ${mathId};`,
    val => parseInt(val, 10) === 25,
    'DB exam_questions must have 25 MATH questions'
  );
  assertDbRow(
    `SELECT string_agg(q.question_type::text, ',' ORDER BY eq.question_order) FROM exam_questions eq JOIN questions q ON q.id = eq.question_id WHERE eq.exam_id = ${genExamId};`,
    val => {
      const types = val.trim().split(',');
      if (types.length !== 25) return false;
      const expected = [
        ...Array(9).fill('TRUE_FALSE'),
        ...Array(6).fill('SINGLE_CHOICE'),
        ...Array(5).fill('MATCHING'),
        ...Array(5).fill('SHORT_ANSWER')
      ];
      return types.every((t, i) => t === expected[i]);
    },
    'DB question_order must match 9 TRUE_FALSE -> 6 SINGLE_CHOICE -> 5 MATCHING -> 5 SHORT_ANSWER'
  );
});

registerCase('CR-12', 'Sinh đề vào đề không ở trạng thái DRAFT (PUBLISHED) phải trả 409 và không ghi đè', async ({
  assertStatus, assertDbRow, evidence
}) => {
  const mathId = getSubjectId('MATH');
  if (!sharedPublishedExamId) {
    const pool = await getSharedMathPool();
    const id = seedPendingReviewExamSql(uid('CR12_PUB'), mathId, 90, pool.allOrdered);
    const pr = await apiCall(`/admin/exams/${id}/publish`, { method: 'POST', body: {} }, 'SUPER_ADMIN');
    assertStatus(pr.status, 200, 'Setup published exam for CR-12');
    sharedPublishedExamId = id;
  }
  const bpId = parseIntDb("SELECT id FROM exam_blueprints WHERE code = 'VSAT_2026_BP_MATH' LIMIT 1;");
  const snapshotBefore = queryDb(`SELECT string_agg(question_id::text || ':' || question_order::text, ',' ORDER BY question_order) FROM exam_questions WHERE exam_id = ${sharedPublishedExamId};`);

  const res = await apiCall(`/admin/exams/${sharedPublishedExamId}/generate-by-matrix`, {
    method: 'POST',
    body: { blueprintId: bpId, variantCount: 1 }
  }, 'CONTENT_ADMIN');
  evidence.request = `POST /admin/exams/${sharedPublishedExamId}/generate-by-matrix`;
  evidence.response = truncResp(res);

  assertStatus(res.status, 409, 'CR-12 generate-by-matrix on PUBLISHED exam must return 409');
  assertDbRow(
    `SELECT string_agg(question_id::text || ':' || question_order::text, ',' ORDER BY question_order) FROM exam_questions WHERE exam_id = ${sharedPublishedExamId};`,
    val => val.trim() === snapshotBefore.trim(),
    'DB exam_questions must remain unchanged'
  );
});

function ensureShortageSubjectAndBlueprint() {
  const subjCode = 'PHYS_SHORT';
  const topicCode = 'PHYS_SHORT_T1';
  const bpCode = 'BP_PHYS_SHORT';
  const spId = getScoringProfileId();

  const subjId = parseIntDb(`
    INSERT INTO subjects (code, name, description, is_active, display_order)
    VALUES ('${subjCode}', 'Vật lý (Thiếu câu)', 'Test shortage', TRUE, 99)
    ON CONFLICT (code) DO UPDATE SET is_active = TRUE
    RETURNING id;
  `);
  const topicId = parseIntDb(`
    INSERT INTO topics (subject_id, code, name, description, is_active, display_order)
    VALUES (${subjId}, '${topicCode}', 'Cơ học', 'Test topic', TRUE, 1)
    ON CONFLICT (code) DO UPDATE SET is_active = TRUE
    RETURNING id;
  `);
  const bpId = parseIntDb(`
    INSERT INTO exam_blueprints (code, name, subject_id, scoring_profile_id, total_questions, max_raw_score, version)
    VALUES ('${bpCode}', 'Blueprint thiếu câu', ${subjId}, ${spId}, 25, 150.00, 1)
    ON CONFLICT (code) DO UPDATE SET subject_id = ${subjId}
    RETURNING id;
  `);

  queryDb(`DELETE FROM exam_blueprint_cells WHERE blueprint_id = ${bpId};`);
  queryDb(`
    INSERT INTO exam_blueprint_cells (blueprint_id, topic_id, question_type, cognitive_level, difficulty, question_count)
    VALUES
      (${bpId}, ${topicId}, 'TRUE_FALSE', 'RECOGNITION', 'EASY', 9),
      (${bpId}, ${topicId}, 'SINGLE_CHOICE', 'UNDERSTANDING', 'MEDIUM', 6),
      (${bpId}, ${topicId}, 'MATCHING', 'APPLICATION', 'MEDIUM', 5),
      (${bpId}, ${topicId}, 'SHORT_ANSWER', 'HIGH_APPLICATION', 'HARD', 5);
  `);

  return { subjId, topicId, bpId };
}

registerCase('CR-13', 'Sinh đề theo blueprint khi ngân hàng thiếu câu phải trả cảnh báo thiếu câu hoặc từ chối', async ({
  assertStatus, assertTrue, evidence
}) => {
  const { subjId, topicId, bpId } = ensureShortageSubjectAndBlueprint();
  const existingSc = parseIntDb(`SELECT COUNT(*) FROM questions WHERE subject_id = ${subjId} AND question_type = 'SINGLE_CHOICE' AND status = 'APPROVED';`);
  for (let i = existingSc; i < 6; i++) {
    await createApprovedQuestionViaApi({
      subjectId: subjId,
      topicId,
      questionType: 'SINGLE_CHOICE',
      questionText: uid(`SHORT_SC_${i}`),
      options: [
        { optionLabel: 'A', optionText: '1', isCorrect: true, displayOrder: 1 },
        { optionLabel: 'B', optionText: '2', isCorrect: false, displayOrder: 2 },
        { optionLabel: 'C', optionText: '3', isCorrect: false, displayOrder: 3 },
        { optionLabel: 'D', optionText: '4', isCorrect: false, displayOrder: 4 }
      ]
    });
  }

  const res = await apiCall('/admin/exams/generate-by-matrix', {
    method: 'POST',
    body: {
      exam: {
        examCode: uid('GEN_SHORTAGE'),
        title: 'Thiếu câu CR-13',
        subjectId: subjId,
        durationMinutes: 60,
        difficulty: 'MEDIUM',
        pricingType: 'FREE',
        price: 0
      },
      blueprintId: bpId,
      variantCount: 1
    }
  }, 'CONTENT_ADMIN');
  evidence.request = `POST /admin/exams/generate-by-matrix (only 6 SC available)`;
  evidence.response = truncResp(res);

  if (res.status === 400) {
    assertStatus(res.status, 400, 'CR-13 shortage rejected with 400');
  } else {
    assertStatus(res.status, 200, 'CR-13 shortage returned 200 with warnings');
    const warnings = res.json?.data?.generationWarnings;
    assertTrue(Array.isArray(warnings) && warnings.length > 0, 'generationWarnings must be non-empty array');
    assertTrue((res.json?.data?.selectedQuestions ?? 99) < 25, 'selectedQuestions < 25');
  }
});

registerCase('CR-14', 'rawScore khi sinh đề thiếu câu phải bằng tổng điểm thực tế, không gán cứng 150', async ({
  assertStatus, assertTrue, assertEqual, evidence
}) => {
  const { subjId, topicId, bpId } = ensureShortageSubjectAndBlueprint();
  const existingSc = parseIntDb(`SELECT COUNT(*) FROM questions WHERE subject_id = ${subjId} AND question_type = 'SINGLE_CHOICE' AND status = 'APPROVED';`);
  for (let i = existingSc; i < 6; i++) {
    await createApprovedQuestionViaApi({
      subjectId: subjId,
      topicId,
      questionType: 'SINGLE_CHOICE',
      questionText: uid(`SHORT_SC14_${i}`),
      options: [
        { optionLabel: 'A', optionText: '1', isCorrect: true, displayOrder: 1 },
        { optionLabel: 'B', optionText: '2', isCorrect: false, displayOrder: 2 },
        { optionLabel: 'C', optionText: '3', isCorrect: false, displayOrder: 3 },
        { optionLabel: 'D', optionText: '4', isCorrect: false, displayOrder: 4 }
      ]
    });
  }

  const createRes = await apiCall('/admin/exams', {
    method: 'POST',
    body: {
      examCode: uid('DRAFT_SHORT14'),
      title: 'Draft shortage CR-14',
      subjectId: subjId,
      durationMinutes: 60,
      difficulty: 'MEDIUM',
      pricingType: 'FREE',
      price: 0
    }
  }, 'CONTENT_ADMIN');
  assertStatus(createRes.status, 201, 'Create DRAFT exam for CR-14');
  const examId = createRes.json?.data?.id;

  const res = await apiCall(`/admin/exams/${examId}/generate-by-matrix`, {
    method: 'POST',
    body: { blueprintId: bpId, variantCount: 1 }
  }, 'CONTENT_ADMIN');
  evidence.request = `POST /admin/exams/${examId}/generate-by-matrix`;
  evidence.response = truncResp(res);

  if (res.status === 400) {
    assertStatus(res.status, 400, 'CR-14 shortage rejected with 400');
  } else {
    assertStatus(res.status, 200, 'CR-14 generate-by-matrix returned 200');
    const selected = res.json?.data?.selectedQuestions;
    const rawScore = Number(res.json?.data?.rawScore);
    const dbSum = parseNumDb(`SELECT COALESCE(SUM(max_raw_score), 0) FROM exam_questions WHERE exam_id = ${examId};`);
    evidence.dbSumMaxRawScore = dbSum;
    assertTrue(selected < 25, `selectedQuestions (${selected}) < 25`);
    assertEqual(rawScore, dbSum, `rawScore (${rawScore}) must equal DB SUM(max_raw_score) (${dbSum})`);
    assertTrue(rawScore < 150, `rawScore (${rawScore}) must be < 150`);
  }
});

// ===========================================================================
// NHÓM 4: Blueprint CRUD (CR-15 .. CR-19)
// ===========================================================================

registerCase('CR-15', 'Tạo blueprint có totalQuestions khác 25 (10) phải bị từ chối', async ({
  assertStatus, assertDbRow, evidence
}) => {
  const mathId = getSubjectId('MATH');
  const topicId = getTopicId('MATH_ALG');
  const spId = getScoringProfileId();
  const code = uid('BP_INV_10');

  const res = await apiCall('/admin/blueprints', {
    method: 'POST',
    body: {
      code,
      name: 'Blueprint 10 câu',
      subjectId: mathId,
      scoringProfileId: spId,
      totalQuestions: 10,
      maxRawScore: 150,
      cells: [{
        topicId,
        questionType: 'SINGLE_CHOICE',
        cognitiveLevel: 'APPLICATION',
        difficulty: 'MEDIUM',
        questionCount: 10
      }]
    }
  }, 'CONTENT_ADMIN');
  evidence.request = `POST /admin/blueprints (${code}, totalQuestions=10)`;
  evidence.response = truncResp(res);

  assertStatus(res.status, 400, 'CR-15 blueprint with totalQuestions=10 must return 400');
  assertDbRow(
    `SELECT COUNT(*) FROM exam_blueprints WHERE code = '${code}';`,
    val => parseInt(val, 10) === 0,
    'Invalid blueprint must not be persisted'
  );
});

registerCase('CR-16', 'Tạo blueprint 25 câu nhưng sai cơ cấu 9/6/5/5 (25 SINGLE_CHOICE) phải bị từ chối', async ({
  assertStatus, assertDbRow, evidence
}) => {
  const mathId = getSubjectId('MATH');
  const topicId = getTopicId('MATH_ALG');
  const spId = getScoringProfileId();
  const code = uid('BP_BAD_MIX');

  const res = await apiCall('/admin/blueprints', {
    method: 'POST',
    body: {
      code,
      name: 'Sai cơ cấu 25 SC',
      subjectId: mathId,
      scoringProfileId: spId,
      totalQuestions: 25,
      maxRawScore: 150,
      cells: [{
        topicId,
        questionType: 'SINGLE_CHOICE',
        cognitiveLevel: 'APPLICATION',
        difficulty: 'MEDIUM',
        questionCount: 25
      }]
    }
  }, 'CONTENT_ADMIN');
  evidence.request = `POST /admin/blueprints (${code}, 25 SINGLE_CHOICE)`;
  evidence.response = truncResp(res);

  assertStatus(res.status, 400, 'CR-16 blueprint with wrong type mix must return 400');
  assertDbRow(
    `SELECT COUNT(*) FROM exam_blueprints WHERE code = '${code}';`,
    val => parseInt(val, 10) === 0,
    'Invalid blueprint must not be persisted'
  );
});

registerCase('CR-17', 'Tạo blueprint có cell mang questionCount âm phải bị từ chối', async ({
  assertStatus, assertDbRow, evidence
}) => {
  const mathId = getSubjectId('MATH');
  const topicId = getTopicId('MATH_ALG');
  const spId = getScoringProfileId();
  const code = uid('BP_NEG_CNT');

  const res = await apiCall('/admin/blueprints', {
    method: 'POST',
    body: {
      code,
      name: 'Count âm',
      subjectId: mathId,
      scoringProfileId: spId,
      totalQuestions: 25,
      maxRawScore: 150,
      cells: [{
        topicId,
        questionType: 'TRUE_FALSE',
        cognitiveLevel: 'RECOGNITION',
        difficulty: 'EASY',
        questionCount: -1
      }]
    }
  }, 'CONTENT_ADMIN');
  evidence.request = `POST /admin/blueprints (${code}, questionCount=-1)`;
  evidence.response = truncResp(res);

  assertStatus(res.status, 400, 'CR-17 blueprint with negative questionCount must return 400');
  assertDbRow(
    `SELECT COUNT(*) FROM exam_blueprints WHERE code = '${code}';`,
    val => parseInt(val, 10) === 0,
    'Invalid blueprint must not be persisted'
  );
});

registerCase('CR-18', 'Tạo blueprint trỏ tới scoringProfileId không tồn tại phải bị từ chối', async ({
  assertStatus, assertDbRow, evidence
}) => {
  const mathId = getSubjectId('MATH');
  const topicId = getTopicId('MATH_ALG');
  const code = uid('BP_BAD_PROF');

  const res = await apiCall('/admin/blueprints', {
    method: 'POST',
    body: {
      code,
      name: 'Profile ảo',
      subjectId: mathId,
      scoringProfileId: 999999,
      totalQuestions: 25,
      maxRawScore: 150,
      cells: [
        { topicId, questionType: 'TRUE_FALSE', cognitiveLevel: 'RECOGNITION', difficulty: 'EASY', questionCount: 9 },
        { topicId, questionType: 'SINGLE_CHOICE', cognitiveLevel: 'UNDERSTANDING', difficulty: 'EASY', questionCount: 6 },
        { topicId, questionType: 'MATCHING', cognitiveLevel: 'APPLICATION', difficulty: 'MEDIUM', questionCount: 5 },
        { topicId, questionType: 'SHORT_ANSWER', cognitiveLevel: 'HIGH_APPLICATION', difficulty: 'HARD', questionCount: 5 }
      ]
    }
  }, 'CONTENT_ADMIN');
  evidence.request = `POST /admin/blueprints (${code}, scoringProfileId=999999)`;
  evidence.response = truncResp(res);

  assertStatus(res.status, 400, 'CR-18 blueprint with non-existent scoringProfileId must return 400');
  assertDbRow(
    `SELECT COUNT(*) FROM exam_blueprints WHERE code = '${code}';`,
    val => parseInt(val, 10) === 0,
    'Invalid blueprint must not be persisted'
  );
});

registerCase('CR-19', 'Blueprint seed mặc định phải có profile chấm điểm hợp lệ và đủ 25 câu / 150 điểm', async ({
  assertStatus, assertTrue, assertDbRow, evidence
}) => {
  const res = await apiCall('/admin/blueprints', { method: 'GET' }, 'CONTENT_ADMIN');
  evidence.request = 'GET /admin/blueprints';
  evidence.response = truncResp(res);

  assertStatus(res.status, 200, 'CR-19 GET /admin/blueprints');
  const list = res.json?.data || [];
  assertTrue(Array.isArray(list) && list.length > 0, 'Blueprint list must not be empty');
  const vsatBps = list.filter(b => String(b.code || '').startsWith('VSAT_2026_BP_'));
  assertTrue(vsatBps.length > 0, 'Standard VSAT_2026_BP_* blueprints must exist');
  for (const bp of vsatBps) {
    assertTrue(bp.totalQuestions === 25, `Blueprint ${bp.code} totalQuestions=${bp.totalQuestions} must be 25`);
    assertTrue(Number(bp.maxRawScore) === 150, `Blueprint ${bp.code} maxRawScore=${bp.maxRawScore} must be 150`);
  }

  assertDbRow(
    'SELECT COUNT(*) FROM exam_blueprints WHERE scoring_profile_id IS NULL;',
    val => parseInt(val, 10) === 0,
    'No blueprint may have NULL scoring_profile_id'
  );
  assertDbRow(
    'SELECT COUNT(*) FROM exam_blueprints bp LEFT JOIN scoring_profiles sp ON sp.id = bp.scoring_profile_id WHERE sp.id IS NULL OR sp.is_active IS NOT TRUE;',
    val => parseInt(val, 10) === 0,
    'All blueprints must reference an active scoring_profile'
  );
});

// ===========================================================================
// NHÓM 5: Import câu hỏi (CR-20 .. CR-25)
// ===========================================================================

registerCase('CR-20', 'Import dryRun=true không được ghi bất kỳ câu hỏi nào vào DB', async ({
  assertStatus, assertEqual, assertDbRow, evidence
}) => {
  const headers = await getTemplateHeaders();
  evidence.templateHeaders = headers;
  const marker = uid('CR20_DRY');
  const rows = [
    buildImportRow(headers, {
      subjectCode: 'MATH',
      topicCode: 'MATH_ALG',
      difficulty: 'EASY',
      cognitiveLevel: 'RECOGNITION',
      questionType: 'SINGLE_CHOICE',
      questionText: `${marker} Câu 1`,
      optionA: '1', optionB: '2', optionC: '3', optionD: '4',
      correctAnswer: 'A',
      explanation: 'Giải 1'
    }),
    buildImportRow(headers, {
      subjectCode: 'MATH',
      topicCode: 'MATH_ALG',
      difficulty: 'MEDIUM',
      cognitiveLevel: 'UNDERSTANDING',
      questionType: 'SINGLE_CHOICE',
      questionText: `${marker} Câu 2`,
      optionA: '10', optionB: '20', optionC: '30', optionD: '40',
      correctAnswer: 'B',
      explanation: 'Giải 2'
    })
  ];

  const res = await importExcelRows(rows, true);
  evidence.request = `POST /admin/questions/import?dryRun=true (marker=${marker})`;
  evidence.response = truncResp(res);

  assertStatus(res.status, 200, 'CR-20 dryRun import must return 200');
  assertEqual(res.json?.data?.dryRun, true, 'data.dryRun === true');
  assertEqual(res.json?.data?.importedQuestions, 0, 'data.importedQuestions === 0');
  assertDbRow(
    `SELECT COUNT(*) FROM questions WHERE question_text LIKE '%${marker}%';`,
    val => parseInt(val, 10) === 0,
    'dryRun=true must insert 0 rows in DB'
  );
});

registerCase('CR-21', 'Import dryRun=false phải tạo câu hỏi ở trạng thái PENDING_REVIEW, không tự APPROVED', async ({
  assertStatus, assertEqual, assertTrue, assertDbRow, evidence
}) => {
  const headers = await getTemplateHeaders();
  const marker = uid('CR21_LIVE');
  const rows = [
    buildImportRow(headers, {
      subjectCode: 'MATH',
      topicCode: 'MATH_ALG',
      difficulty: 'MEDIUM',
      cognitiveLevel: 'APPLICATION',
      questionType: 'SINGLE_CHOICE',
      questionText: `${marker} Câu import thật`,
      optionA: 'Đúng', optionB: 'Sai 1', optionC: 'Sai 2', optionD: 'Sai 3',
      correctAnswer: 'A',
      explanation: 'Lời giải'
    })
  ];

  const res = await importExcelRows(rows, false);
  evidence.request = `POST /admin/questions/import?dryRun=false (marker=${marker})`;
  evidence.response = truncResp(res);

  assertStatus(res.status, 200, 'CR-21 live import must return 200');
  assertEqual(res.json?.data?.dryRun, false, 'data.dryRun === false');
  assertTrue((res.json?.data?.importedQuestions ?? 0) >= 1, 'importedQuestions >= 1');
  assertDbRow(
    `SELECT COUNT(*) FROM questions WHERE question_text LIKE '%${marker}%' AND status = 'PENDING_REVIEW';`,
    val => parseInt(val, 10) >= 1,
    'Imported questions must have status PENDING_REVIEW'
  );
  assertDbRow(
    `SELECT COUNT(*) FROM questions WHERE question_text LIKE '%${marker}%' AND status = 'APPROVED';`,
    val => parseInt(val, 10) === 0,
    'Imported questions must NOT be auto-APPROVED'
  );
});

registerCase('CR-22', 'Import dòng có loại câu lạ (ABC) phải bị đánh dấu lỗi, không ép thành SINGLE_CHOICE', async ({
  assertStatus, assertTrue, assertDbRow, evidence
}) => {
  const headers = await getTemplateHeaders();
  const marker = uid('CR22_ABC');
  const rows = [
    buildImportRow(headers, {
      subjectCode: 'MATH',
      topicCode: 'MATH_ALG',
      difficulty: 'MEDIUM',
      cognitiveLevel: 'APPLICATION',
      questionType: 'ABC',
      questionText: `${marker} Loại câu lạ`,
      optionA: 'A1', optionB: 'B1', optionC: 'C1', optionD: 'D1',
      correctAnswer: 'A',
      explanation: 'Giải'
    })
  ];

  const res = await importExcelRows(rows, false);
  evidence.request = `POST /admin/questions/import?dryRun=false (questionType=ABC, marker=${marker})`;
  evidence.response = truncResp(res);

  if (res.status === 400) {
    assertStatus(res.status, 400, 'CR-22 unknown questionType rejected with 400');
  } else {
    assertStatus(res.status, 200, 'CR-22 import returned 200 with row errors');
    assertTrue((res.json?.data?.errorCount ?? 0) >= 1, 'errorCount >= 1');
    const items = res.json?.data?.items || res.json?.data?.rows || [];
    assertTrue(items.some(it => it.valid === false), 'Row must be marked valid === false');
  }
  assertDbRow(
    `SELECT COUNT(*) FROM questions WHERE question_text LIKE '%${marker}%';`,
    val => parseInt(val, 10) === 0,
    'Unknown questionType row must not be saved in DB'
  );
});

registerCase('CR-23', 'Import câu SINGLE_CHOICE đáp án sai định dạng "A,B" phải báo lỗi và không lưu DB', async ({
  assertStatus, assertTrue, assertEqual, assertDbRow, evidence
}) => {
  const headers = await getTemplateHeaders();
  const marker = uid('CR23_AB');
  const rows = [
    buildImportRow(headers, {
      subjectCode: 'MATH',
      topicCode: 'MATH_ALG',
      difficulty: 'EASY',
      cognitiveLevel: 'RECOGNITION',
      questionType: 'SINGLE_CHOICE',
      questionText: `${marker} SC sai đáp án`,
      optionA: '1', optionB: '2', optionC: '3', optionD: '4',
      correctAnswer: 'A,B',
      explanation: 'Giải'
    })
  ];

  const res = await importExcelRows(rows, false);
  evidence.request = `POST /admin/questions/import?dryRun=false (correctAnswer=A,B)`;
  evidence.response = truncResp(res);

  if (res.status === 400) {
    assertStatus(res.status, 400, 'CR-23 invalid SC correctAnswer rejected with 400');
  } else {
    assertStatus(res.status, 200, 'CR-23 returned 200 with error details');
    assertTrue((res.json?.data?.errorCount ?? 0) >= 1, 'errorCount >= 1');
    assertEqual(res.json?.data?.importedQuestions, 0, 'importedQuestions === 0');
    const items = res.json?.data?.items || res.json?.data?.rows || [];
    assertTrue(items.some(it => it.valid === false), 'Row must be marked valid === false');
  }
  assertDbRow(
    `SELECT COUNT(*) FROM questions WHERE question_text LIKE '%${marker}%';`,
    val => parseInt(val, 10) === 0,
    'Invalid SC row must not be saved in DB'
  );
});

registerCase('CR-24', 'Import câu MATCHING hợp lệ phải lưu được 4 tiểu mục ghép hợp vào DB', async ({
  assertStatus, assertEqual, assertDbRow, evidence
}) => {
  const headers = await getTemplateHeaders();
  const marker = uid('CR24_MATCH');
  const rows = [
    buildImportRow(headers, {
      subjectCode: 'MATH',
      topicCode: 'MATH_ALG',
      difficulty: 'MEDIUM',
      cognitiveLevel: 'APPLICATION',
      questionType: 'MATCHING',
      questionText: `${marker} Ghép nối 4 cặp`,
      optionA: 'Đáp A',
      optionB: 'Đáp B',
      optionC: 'Đáp C',
      optionD: 'Đáp D',
      correctAnswer: '1-C,2-A,3-D,4-B',
      explanation: 'Giải ghép'
    })
  ];

  const res = await importExcelRows(rows, false);
  evidence.request = `POST /admin/questions/import?dryRun=false (MATCHING, marker=${marker})`;
  evidence.response = truncResp(res);

  assertStatus(res.status, 200, 'CR-24 import MATCHING must return 200');
  assertEqual(res.json?.data?.errorCount, 0, 'errorCount === 0');
  assertEqual(res.json?.data?.importedQuestions, 1, 'importedQuestions === 1');

  assertDbRow(
    `SELECT COUNT(*) FROM question_matching_items qmi JOIN questions q ON q.id = qmi.question_id JOIN question_matching_choices qmc ON qmc.question_id = q.id AND qmc.choice_key = qmi.correct_choice_key WHERE q.question_text LIKE '%${marker}%';`,
    val => parseInt(val, 10) === 4,
    'DB question_matching_items must have 4 rows matching question_matching_choices'
  );
});

registerCase('CR-25', 'Import câu SHORT_ANSWER đáp án số 12.5 phải lưu khóa chấm NUMERIC vào DB', async ({
  assertStatus, assertEqual, assertDbRow, evidence
}) => {
  const headers = await getTemplateHeaders();
  const marker = uid('CR25_SA');
  const rows = [
    buildImportRow(headers, {
      subjectCode: 'MATH',
      topicCode: 'MATH_ALG',
      difficulty: 'MEDIUM',
      cognitiveLevel: 'APPLICATION',
      questionType: 'SHORT_ANSWER',
      questionText: `${marker} Tính giá trị`,
      optionA: '12.5', optionB: '', optionC: '', optionD: '',
      correctAnswer: '12.5',
      explanation: 'Đáp án 12.5'
    })
  ];

  const res = await importExcelRows(rows, false);
  evidence.request = `POST /admin/questions/import?dryRun=false (SHORT_ANSWER 12.5)`;
  evidence.response = truncResp(res);

  assertStatus(res.status, 200, 'CR-25 import SHORT_ANSWER must return 200');
  assertEqual(res.json?.data?.errorCount, 0, 'errorCount === 0');
  assertEqual(res.json?.data?.importedQuestions, 1, 'importedQuestions === 1');

  assertDbRow(
    `SELECT COUNT(*) FROM question_short_answer_keys k JOIN questions q ON q.id = k.question_id WHERE q.question_text LIKE '%${marker}%' AND k.value_type = 'NUMERIC' AND k.numeric_value = 12.5;`,
    val => parseInt(val, 10) === 1,
    'DB question_short_answer_keys must have NUMERIC key = 12.5'
  );
});

// ===========================================================================
// NHÓM 6: Tạo câu hỏi sai cấu trúc (CR-26 .. CR-29)
// ===========================================================================

registerCase('CR-26', 'Tạo câu TRUE_FALSE chỉ có 3 mệnh đề phải trả HTTP 400', async ({
  assertStatus, assertDbRow, evidence
}) => {
  const mathId = getSubjectId('MATH');
  const topicId = getTopicId('MATH_ALG');
  const qText = uid('TF thiếu ý');

  const res = await apiCall('/collaborator/questions', {
    method: 'POST',
    body: {
      subjectId: mathId,
      topicId,
      difficulty: 'MEDIUM',
      questionType: 'TRUE_FALSE',
      questionText: qText,
      options: [
        { optionLabel: 'a', optionText: 'Ý 1', isCorrect: true, displayOrder: 1 },
        { optionLabel: 'b', optionText: 'Ý 2', isCorrect: false, displayOrder: 2 },
        { optionLabel: 'c', optionText: 'Ý 3', isCorrect: true, displayOrder: 3 }
      ]
    }
  }, 'COLLABORATOR');
  evidence.request = 'POST /collaborator/questions (TF 3 options)';
  evidence.response = truncResp(res);

  assertStatus(res.status, 400, 'CR-26 TF with 3 propositions must return 400');
  assertDbRow(
    `SELECT COUNT(*) FROM questions WHERE question_text = '${qText}';`,
    val => parseInt(val, 10) === 0,
    'Rejected TF question must not be saved'
  );
});

registerCase('CR-27', 'Tạo câu SINGLE_CHOICE 2 phương án hoặc 2 đáp án đúng phải trả HTTP 400', async ({
  assertStatus, assertDbRow, evidence
}) => {
  const mathId = getSubjectId('MATH');
  const topicId = getTopicId('MATH_ALG');
  const qText1 = uid('SC sai số phương án');
  const qText2 = uid('SC hai đáp án đúng');

  const res1 = await apiCall('/collaborator/questions', {
    method: 'POST',
    body: {
      subjectId: mathId,
      topicId,
      difficulty: 'EASY',
      questionType: 'SINGLE_CHOICE',
      questionText: qText1,
      options: [
        { optionLabel: 'A', optionText: '1', isCorrect: true, displayOrder: 1 },
        { optionLabel: 'B', optionText: '2', isCorrect: false, displayOrder: 2 }
      ]
    }
  }, 'COLLABORATOR');

  const res2 = await apiCall('/collaborator/questions', {
    method: 'POST',
    body: {
      subjectId: mathId,
      topicId,
      difficulty: 'EASY',
      questionType: 'SINGLE_CHOICE',
      questionText: qText2,
      options: [
        { optionLabel: 'A', optionText: '1', isCorrect: true, displayOrder: 1 },
        { optionLabel: 'B', optionText: '2', isCorrect: true, displayOrder: 2 },
        { optionLabel: 'C', optionText: '3', isCorrect: false, displayOrder: 3 },
        { optionLabel: 'D', optionText: '4', isCorrect: false, displayOrder: 4 }
      ]
    }
  }, 'COLLABORATOR');

  evidence.res2Options = truncResp(res1);
  evidence.res2Correct = truncResp(res2);

  assertStatus(res1.status, 400, 'CR-27 SC with 2 options must return 400');
  assertStatus(res2.status, 400, 'CR-27 SC with 2 correct options must return 400');
  assertDbRow(
    `SELECT COUNT(*) FROM questions WHERE question_text IN ('${qText1}', '${qText2}');`,
    val => parseInt(val, 10) === 0,
    'Rejected SC questions must not be saved'
  );
});

registerCase('CR-28', 'Tạo câu MATCHING chỉ có 2 cặp/tiểu mục thay vì 4 phải trả HTTP 400', async ({
  assertStatus, assertDbRow, evidence
}) => {
  const mathId = getSubjectId('MATH');
  const topicId = getTopicId('MATH_ALG');
  const qText = uid('Matching thiếu cặp');

  const res = await apiCall('/collaborator/questions', {
    method: 'POST',
    body: {
      subjectId: mathId,
      topicId,
      difficulty: 'MEDIUM',
      questionType: 'MATCHING',
      questionText: qText,
      options: [
        { optionLabel: '1', optionText: 'Vế 1 <-> A', isCorrect: true, displayOrder: 1 },
        { optionLabel: '2', optionText: 'Vế 2 <-> B', isCorrect: true, displayOrder: 2 }
      ]
    }
  }, 'COLLABORATOR');
  evidence.request = 'POST /collaborator/questions (MATCHING 2 items)';
  evidence.response = truncResp(res);

  assertStatus(res.status, 400, 'CR-28 MATCHING with 2 pairs must return 400');
  assertDbRow(
    `SELECT COUNT(*) FROM questions WHERE question_text = '${qText}';`,
    val => parseInt(val, 10) === 0,
    'Rejected MATCHING question must not be saved'
  );
});

registerCase('CR-29', 'Tạo câu SHORT_ANSWER không có đáp án hợp lệ phải trả HTTP 400', async ({
  assertStatus, assertDbRow, evidence
}) => {
  const mathId = getSubjectId('MATH');
  const topicId = getTopicId('MATH_ALG');
  const qText = uid('SA thiếu đáp án');

  const res = await apiCall('/collaborator/questions', {
    method: 'POST',
    body: {
      subjectId: mathId,
      topicId,
      difficulty: 'MEDIUM',
      questionType: 'SHORT_ANSWER',
      questionText: qText,
      options: [
        { optionLabel: 'A', optionText: '42', isCorrect: false, displayOrder: 1 }
      ]
    }
  }, 'COLLABORATOR');
  evidence.request = 'POST /collaborator/questions (SA isCorrect=false)';
  evidence.response = truncResp(res);

  assertStatus(res.status, 400, 'CR-29 SA without valid answer must return 400');
  assertDbRow(
    `SELECT COUNT(*) FROM questions WHERE question_text = '${qText}';`,
    val => parseInt(val, 10) === 0,
    'Rejected SA question must not be saved'
  );
});

// ===========================================================================
// NHÓM 7: Bundle tải đề của học sinh (CR-30 .. CR-31)
// ===========================================================================

const FORBIDDEN_BUNDLE_KEYS = new Set([
  'isCorrect',
  'correctAnswer',
  'correctOptionIds',
  'explanation',
  'explanationHtml',
  'correctChoiceKey',
  'acceptedAnswers'
]);

function scanBundleForLeaks(node, path, saSecret, hits) {
  if (node === null || node === undefined) return;
  if (typeof node === 'string') {
    if (node.includes('⇄')) hits.push(`${path}: contains "⇄"`);
    if (saSecret && node.includes(saSecret)) hits.push(`${path}: leaks SA answer "${saSecret}"`);
    return;
  }
  if (Array.isArray(node)) {
    for (let i = 0; i < node.length; i++) {
      scanBundleForLeaks(node[i], `${path}[${i}]`, saSecret, hits);
    }
    return;
  }
  if (typeof node === 'object') {
    for (const [k, v] of Object.entries(node)) {
      if (FORBIDDEN_BUNDLE_KEYS.has(k)) {
        hits.push(`${path}.${k}: forbidden key present`);
      }
      scanBundleForLeaks(v, `${path}.${k}`, saSecret, hits);
    }
  }
}

async function ensureCleanBundlePublishedExam() {
  const mathId = getSubjectId('MATH');
  const topicId = getTopicId('MATH_ALG');
  const secretSa = '987654321';
  const pool = await createVsat25Pool(uid('BUNDLE_POOL'), mathId, topicId, secretSa);
  const examId = seedPendingReviewExamSql(uid('BUNDLE_EXAM'), mathId, 90, pool.allOrdered);
  const pubRes = await apiCall(`/admin/exams/${examId}/publish`, { method: 'POST', body: {} }, 'SUPER_ADMIN');
  if (pubRes.status !== 200) {
    throw new Error(`Failed to publish bundle test exam: ${pubRes.status} ${JSON.stringify(pubRes.json)}`);
  }
  return { examId, pool, secretSa };
}

let bundleFixture = null;

registerCase('CR-30', 'Bundle tải đề của học sinh không được lộ đáp án hay lời giải dưới bất kỳ dạng nào', async ({
  assertStatus, assertEqual, evidence
}) => {
  if (!bundleFixture) bundleFixture = await ensureCleanBundlePublishedExam();

  const res = await apiCall(`/exams/${bundleFixture.examId}/download-bundle`, { method: 'GET' }, 'STUDENT');
  evidence.request = `GET /exams/${bundleFixture.examId}/download-bundle (STUDENT)`;
  evidence.status = res.status;

  assertStatus(res.status, 200, 'CR-30 download-bundle must return 200');
  const hits = [];
  scanBundleForLeaks(res.json?.data, 'data', bundleFixture.secretSa, hits);
  evidence.leakHits = hits;
  assertEqual(hits.length, 0, `Bundle leaked answers/explanations: ${hits.slice(0, 5).join('; ')}`);
});

registerCase('CR-31', 'Bundle tải đề phải có maxRawScore từng câu, tổng điểm 150, đúng thứ tự 9/6/5/5', async ({
  assertStatus, assertEqual, assertTrue, evidence
}) => {
  if (!bundleFixture) bundleFixture = await ensureCleanBundlePublishedExam();

  const res = await apiCall(`/exams/${bundleFixture.examId}/download-bundle`, { method: 'GET' }, 'STUDENT');
  evidence.request = `GET /exams/${bundleFixture.examId}/download-bundle (STUDENT)`;
  evidence.status = res.status;

  assertStatus(res.status, 200, 'CR-31 download-bundle must return 200');
  const questions = res.json?.data?.questions || [];
  assertEqual(questions.length, 25, 'data.questions.length === 25');

  let sum = 0;
  for (let i = 0; i < questions.length; i++) {
    const q = questions[i];
    assertTrue(typeof q.maxRawScore === 'number' && q.maxRawScore > 0, `q[${i}].maxRawScore must be positive number, got ${q.maxRawScore}`);
    sum += q.maxRawScore;
  }
  assertEqual(Math.round(sum * 100) / 100, 150, `Sum of maxRawScore must be 150, got ${sum}`);

  const expectedTypes = [
    ...Array(9).fill('TRUE_FALSE'),
    ...Array(6).fill('SINGLE_CHOICE'),
    ...Array(5).fill('MATCHING'),
    ...Array(5).fill('SHORT_ANSWER')
  ];
  const actualTypes = questions.map(q => q.questionType);
  evidence.questionTypes = actualTypes;
  assertTrue(
    actualTypes.every((t, i) => t === expectedTypes[i]),
    `Bundle question types order mismatch: ${actualTypes.join(',')}`
  );
});

// ===========================================================================
// NHÓM 8: Chấm điểm phiên thi (CR-32 .. CR-33)
// ===========================================================================

registerCase('CR-32', 'Nộp bài đúng hết 25 câu qua server-side phải đạt tổng điểm thô 150', async ({
  assertStatus, assertEqual, assertDbRow, evidence
}) => {
  if (!bundleFixture) bundleFixture = await ensureCleanBundlePublishedExam();

  const startRes = await apiCall('/sessions/start', {
    method: 'POST',
    body: { examId: bundleFixture.examId, mode: 'MOCK_EXAM' }
  }, 'STUDENT');
  assertStatus(startRes.status, 201, 'Start MOCK_EXAM session');
  const sessionId = startRes.json?.data?.id || startRes.json?.data?.sessionId;
  evidence.sessionId = sessionId;

  const bundleRes = await apiCall(`/exams/${bundleFixture.examId}/download-bundle`, { method: 'GET' }, 'STUDENT');
  assertStatus(bundleRes.status, 200, 'Fetch bundle questions for session');
  const questions = bundleRes.json?.data?.questions || [];

  for (const q of questions) {
    let body = { questionId: q.id, timeSpentSeconds: 5 };
    if (q.questionType === 'TRUE_FALSE') {
      body.answerPayload = { a: true, b: false, c: true, d: false };
    } else if (q.questionType === 'SINGLE_CHOICE') {
      const optA = (q.options || []).find(o => o.optionLabel === 'A');
      body.selectedOptionId = optA?.id;
    } else if (q.questionType === 'MATCHING') {
      body.answerPayload = { '1': 'Đáp án A', '2': 'Đáp án B', '3': 'Đáp án C', '4': 'Đáp án D' };
    } else if (q.questionType === 'SHORT_ANSWER') {
      body.answerPayload = { value: bundleFixture.secretSa };
    }
    const ansRes = await apiCall(`/sessions/${sessionId}/answers`, { method: 'POST', body }, 'STUDENT');
    assertStatus(ansRes.status, 200, `Save answer for q=${q.id} (${q.questionType})`);
  }

  const subRes = await apiCall(`/sessions/${sessionId}/submit`, { method: 'POST', body: {} }, 'STUDENT');
  evidence.submitResponse = truncResp(subRes);

  assertStatus(subRes.status, 200, 'CR-32 submit must return 200');
  assertEqual(subRes.json?.data?.status, 'SUBMITTED', 'data.status === SUBMITTED');
  assertEqual(Number(subRes.json?.data?.rawScore), 150, 'data.rawScore === 150');
  assertEqual(Number(subRes.json?.data?.maxRawScore), 150, 'data.maxRawScore === 150');
  assertEqual(subRes.json?.data?.correctCount, 25, 'data.correctCount === 25');

  assertDbRow(
    `SELECT raw_score FROM exam_sessions WHERE id = ${sessionId};`,
    val => Number(val.trim()) === 150,
    'DB exam_sessions.raw_score must equal 150.00'
  );
});

registerCase('CR-33', 'Nộp bài qua client-submit kèm điểm giả mạo: server phải bỏ qua điểm giả và tự chấm', async ({
  assertStatus, assertEqual, assertDbRow, evidence
}) => {
  if (!bundleFixture) bundleFixture = await ensureCleanBundlePublishedExam();

  const startRes = await apiCall('/sessions/start', {
    method: 'POST',
    body: { examId: bundleFixture.examId, mode: 'MOCK_EXAM' }
  }, 'STUDENT');
  assertStatus(startRes.status, 201, 'Start MOCK_EXAM session for client-submit');
  const sessionId = startRes.json?.data?.id || startRes.json?.data?.sessionId;
  const q1Id = bundleFixture.pool.allOrdered[0];

  const res = await apiCall(`/sessions/${sessionId}/client-submit`, {
    method: 'POST',
    body: {
      score: 100.0,
      correctCount: 25,
      totalQuestions: 25,
      timeSpentSeconds: 120,
      answers: [
        { questionId: q1Id, questionOrder: 1, answerPayload: { wrong: true } }
      ]
    }
  }, 'STUDENT');
  evidence.request = `POST /sessions/${sessionId}/client-submit (spoofed score=100, correctCount=25)`;
  evidence.response = truncResp(res);

  if (res.status === 400) {
    assertStatus(res.status, 400, 'CR-33 client-submit rejected with 400');
  } else {
    assertStatus(res.status, 200, 'CR-33 client-submit returned 200 with server re-grading');
    assertEqual(Number(res.json?.data?.rawScore), 0, 'rawScore must be 0');
    assertEqual(res.json?.data?.correctCount, 0, 'correctCount must be 0');
    assertEqual(Number(res.json?.data?.score), 0, 'score must be 0');
    assertDbRow(
      `SELECT raw_score::text || '|' || correct_count::text FROM exam_sessions WHERE id = ${sessionId};`,
      val => {
        const [rs, cc] = val.trim().split('|');
        return Number(rs) === 0 && Number(cc) === 0;
      },
      'DB exam_sessions raw_score and correct_count must be 0'
    );
  }
});

// ===========================================================================
// NHÓM 9: Khóa sửa cấu trúc đề khi đã PUBLISHED hoặc HIDDEN (CR-34 .. CR-36)
// ===========================================================================

let lockTestExamId = null;
let lockTestPool = null;

async function ensureLockTestPublishedExam() {
  if (lockTestExamId) return { examId: lockTestExamId, pool: lockTestPool };
  const mathId = getSubjectId('MATH');
  lockTestPool = await getSharedMathPool();
  const examId = seedPendingReviewExamSql(uid('LOCK_EXAM'), mathId, 90, lockTestPool.allOrdered);
  const pubRes = await apiCall(`/admin/exams/${examId}/publish`, { method: 'POST', body: {} }, 'SUPER_ADMIN');
  if (pubRes.status !== 200) throw new Error(`Failed publishing lock test exam: ${pubRes.status}`);
  lockTestExamId = examId;
  return { examId, pool: lockTestPool };
}

registerCase('CR-34', 'Đề đang PUBLISHED không được sửa metadata hay thêm/xóa/đổi thứ tự câu hỏi', async ({
  assertStatus, assertDbRow, evidence
}) => {
  const { examId, pool } = await ensureLockTestPublishedExam();
  const mathId = getSubjectId('MATH');
  const topicId = getTopicId('MATH_ALG');
  const extraQId = await createApprovedQuestionViaApi({
    subjectId: mathId,
    topicId,
    questionType: 'SINGLE_CHOICE',
    questionText: uid('CR34_EXTRA'),
    options: [
      { optionLabel: 'A', optionText: '1', isCorrect: true, displayOrder: 1 },
      { optionLabel: 'B', optionText: '2', isCorrect: false, displayOrder: 2 },
      { optionLabel: 'C', optionText: '3', isCorrect: false, displayOrder: 3 },
      { optionLabel: 'D', optionText: '4', isCorrect: false, displayOrder: 4 }
    ]
  });

  const titleBefore = queryDb(`SELECT title FROM exams WHERE id = ${examId};`).trim();
  const eqBefore = queryDb(`SELECT string_agg(question_id::text || ':' || question_order::text, ',' ORDER BY question_order) FROM exam_questions WHERE exam_id = ${examId};`).trim();

  const r1 = await apiCall(`/admin/exams/${examId}`, { method: 'PUT', body: { title: 'Sửa lén CR-34' } }, 'SUPER_ADMIN');
  const r2 = await apiCall(`/admin/exams/${examId}/questions`, { method: 'POST', body: { questionId: extraQId } }, 'SUPER_ADMIN');
  const r3 = await apiCall(`/admin/exams/${examId}/questions/${pool.allOrdered[0]}`, { method: 'DELETE' }, 'SUPER_ADMIN');
  const r4 = await apiCall(`/admin/exams/${examId}/questions/reorder`, { method: 'PUT', body: { questionIds: [...pool.allOrdered].reverse() } }, 'SUPER_ADMIN');

  evidence.responses = { put: truncResp(r1), add: truncResp(r2), del: truncResp(r3), reorder: truncResp(r4) };

  assertStatus(r1.status, 409, 'CR-34 PUT metadata on PUBLISHED exam must return 409');
  assertStatus(r2.status, 409, 'CR-34 ADD question on PUBLISHED exam must return 409');
  assertStatus(r3.status, 409, 'CR-34 DELETE question on PUBLISHED exam must return 409');
  assertStatus(r4.status, 409, 'CR-34 REORDER questions on PUBLISHED exam must return 409');

  assertDbRow(`SELECT title FROM exams WHERE id = ${examId};`, v => v.trim() === titleBefore, 'Title unchanged');
  assertDbRow(
    `SELECT string_agg(question_id::text || ':' || question_order::text, ',' ORDER BY question_order) FROM exam_questions WHERE exam_id = ${examId};`,
    v => v.trim() === eqBefore,
    'exam_questions unchanged'
  );
});

registerCase('CR-35', 'Đề đang HIDDEN không được thêm, xóa hoặc sắp xếp lại câu hỏi', async ({
  assertStatus, assertDbRow, evidence
}) => {
  const { examId, pool } = await ensureLockTestPublishedExam();
  const hideRes = await apiCall(`/admin/exams/${examId}/hide`, { method: 'POST', body: {} }, 'SUPER_ADMIN');
  assertStatus(hideRes.status, 200, 'Hide exam before CR-35 checks');

  const mathId = getSubjectId('MATH');
  const topicId = getTopicId('MATH_ALG');
  const extraQId = await createApprovedQuestionViaApi({
    subjectId: mathId,
    topicId,
    questionType: 'SINGLE_CHOICE',
    questionText: uid('CR35_EXTRA'),
    options: [
      { optionLabel: 'A', optionText: '1', isCorrect: true, displayOrder: 1 },
      { optionLabel: 'B', optionText: '2', isCorrect: false, displayOrder: 2 },
      { optionLabel: 'C', optionText: '3', isCorrect: false, displayOrder: 3 },
      { optionLabel: 'D', optionText: '4', isCorrect: false, displayOrder: 4 }
    ]
  });

  const eqBefore = queryDb(`SELECT string_agg(question_id::text || ':' || question_order::text, ',' ORDER BY question_order) FROM exam_questions WHERE exam_id = ${examId};`).trim();

  const rAdd = await apiCall(`/admin/exams/${examId}/questions`, { method: 'POST', body: { questionId: extraQId } }, 'SUPER_ADMIN');
  const rDel = await apiCall(`/admin/exams/${examId}/questions/${pool.allOrdered[0]}`, { method: 'DELETE' }, 'SUPER_ADMIN');
  const rReo = await apiCall(`/admin/exams/${examId}/questions/reorder`, { method: 'PUT', body: { questionIds: [...pool.allOrdered].reverse() } }, 'SUPER_ADMIN');

  evidence.responses = { add: truncResp(rAdd), del: truncResp(rDel), reorder: truncResp(rReo) };

  assertStatus(rAdd.status, 409, 'CR-35 ADD question on HIDDEN exam must return 409');
  assertStatus(rDel.status, 409, 'CR-35 DELETE question on HIDDEN exam must return 409');
  assertStatus(rReo.status, 409, 'CR-35 REORDER questions on HIDDEN exam must return 409');

  assertDbRow(
    `SELECT string_agg(question_id::text || ':' || question_order::text, ',' ORDER BY question_order) FROM exam_questions WHERE exam_id = ${examId};`,
    v => v.trim() === eqBefore,
    'exam_questions unchanged while HIDDEN'
  );
});

registerCase('CR-36', 'Đề HIDDEN đổi môn khi còn câu môn cũ không được xuất bản lại trực tiếp', async ({
  assertStatus, assertDbRow, evidence
}) => {
  const { examId } = await ensureLockTestPublishedExam();
  queryDb(`UPDATE exams SET status = 'HIDDEN' WHERE id = ${examId};`);
  const engId = getSubjectId('ENG');

  const putRes = await apiCall(`/admin/exams/${examId}`, {
    method: 'PUT',
    body: { subjectId: engId, durationMinutes: 60 }
  }, 'SUPER_ADMIN');
  evidence.putResponse = truncResp(putRes);

  if (putRes.status === 409) {
    assertStatus(putRes.status, 409, 'CR-36 blocked changing subjectId on HIDDEN exam with existing questions (409)');
  } else if (putRes.status === 400) {
    assertStatus(putRes.status, 400, 'CR-36 blocked changing subjectId on HIDDEN exam with existing questions (400)');
  } else {
    assertStatus(putRes.status, 200, 'Subject changed on HIDDEN exam, verifying publish is blocked');
    const pubRes = await apiCall(`/admin/exams/${examId}/publish`, { method: 'POST', body: {} }, 'SUPER_ADMIN');
    evidence.publishResponse = truncResp(pubRes);
    if (pubRes.status === 409) {
      assertStatus(pubRes.status, 409, 'CR-36 publish blocked with 409');
    } else {
      assertStatus(pubRes.status, 400, 'CR-36 publish blocked with 400 due to subject mismatch / review required');
    }
  }

  assertDbRow(
    `SELECT COUNT(*) FROM exams e JOIN exam_questions eq ON eq.exam_id = e.id JOIN questions q ON q.id = eq.question_id WHERE e.id = ${examId} AND e.status = 'PUBLISHED' AND e.subject_id <> q.subject_id;`,
    val => parseInt(val, 10) === 0,
    'No PUBLISHED exam may have mismatched subject between exam and questions'
  );
});

// ===========================================================================
// Run suite cleanly without exiting early on failures
// ===========================================================================
await runSuite('cleanroom-probes', {
  resultsDir: resolve(import.meta.dirname),
  noExit: true
});
