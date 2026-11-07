#!/usr/bin/env node
/**
 * Phase 1 & Phase 2 Features Verification (Issue #18 & #20)
 *
 * Verifies:
 * 1. P2 Admission Matching Engine (universities, subject-combos, prediction classifications)
 * 2. TD-4 Optimistic Locking 409 E2E (lock_version mismatch -> 409 CONCURRENT_MODIFICATION)
 * 3. P4 Anti-Cheat Proctoring Mode (auto-submit on 3rd violation)
 * 4. P1 Student CBT Web Portal UI (lobby & admission predictor widget load with zero page errors)
 *
 * NOTE: This check does NOT record into acceptance-harness/results/*.json to preserve
 * the exact 126 test IDs validated by verdict-gate.mjs.
 */

import { apiCall, queryDb } from '../lib/test-harness.mjs';
import { ensureViteServer, launchBrowser } from './helpers/web-helpers.mjs';

function fail(message) {
  console.error(`\n[PHASE2-E2E FAILED] ${message}`);
  process.exit(1);
}

function pass(name, detail = '') {
  console.log(`  [PASS] ${name}${detail ? ` - ${detail}` : ''}`);
}

async function runCheck() {
  console.log('\n============================================================');
  console.log('[PHASE2-E2E] Running Phase 1 & Phase 2 Verification Checks');
  console.log('============================================================');

  // --------------------------------------------------------------------------
  // 1. P2 Admission API
  // --------------------------------------------------------------------------
  console.log('\n--- [1/4] Checking P2 Admission Matching API ---');

  // 1a. GET /admissions/universities
  const uniRes = await apiCall('/admissions/universities');
  if (uniRes.status !== 200) {
    fail(`GET /admissions/universities returned HTTP ${uniRes.status}, expected 200`);
  }
  const universities = uniRes.json?.data || [];
  if (!Array.isArray(universities) || universities.length < 6) {
    fail(`Expected >= 6 universities, got ${universities.length}`);
  }
  pass('GET /admissions/universities', `Found ${universities.length} universities`);

  // 1b. GET /admissions/subject-combos
  const comboRes = await apiCall('/admissions/subject-combos');
  if (comboRes.status !== 200) {
    fail(`GET /admissions/subject-combos returned HTTP ${comboRes.status}, expected 200`);
  }
  const combos = comboRes.json?.data || [];
  if (!Array.isArray(combos) || !combos.includes('A00')) {
    fail(`Expected combos array containing 'A00', got ${JSON.stringify(combos)}`);
  }
  pass('GET /admissions/subject-combos', `Found combos: ${combos.join(', ')}`);

  // 1c. POST /admissions/predict (totalScore = 390, subjectCombo = 'A00')
  const predictRes = await apiCall('/admissions/predict', {
    method: 'POST',
    body: { totalScore: 390, subjectCombo: 'A00' }
  });
  if (predictRes.status !== 200) {
    fail(`POST /admissions/predict returned HTTP ${predictRes.status}, expected 200: ${JSON.stringify(predictRes.json)}`);
  }
  const predictData = predictRes.json?.data;
  if (!predictData || !Array.isArray(predictData.predictions)) {
    fail('POST /admissions/predict missing predictions array');
  }

  // Validate classifications according to delta = totalScore - cutoffScore
  // SAFE: delta >= 15.0, FIT: delta >= -10.0 and delta < 15.0, RISK: delta < -10.0
  let foundSafe = false;
  let foundFit = false;
  let foundRisk = false;

  for (const item of predictData.predictions) {
    const delta = Number(item.delta);
    const classification = item.classification;

    if (delta >= 15.0) {
      if (classification !== 'SAFE') {
        fail(`Prediction item ${item.majorName} delta=${delta} classified as ${classification}, expected SAFE`);
      }
      foundSafe = true;
    } else if (delta >= -10.0) {
      if (classification !== 'FIT') {
        fail(`Prediction item ${item.majorName} delta=${delta} classified as ${classification}, expected FIT`);
      }
      foundFit = true;
    } else {
      if (classification !== 'RISK') {
        fail(`Prediction item ${item.majorName} delta=${delta} classified as ${classification}, expected RISK`);
      }
      foundRisk = true;
    }
  }

  if (!foundSafe || !foundFit || !foundRisk) {
    console.warn(`[WARN] Classification coverage in sample data: SAFE=${foundSafe}, FIT=${foundFit}, RISK=${foundRisk}`);
  }
  pass('POST /admissions/predict', `Validated ${predictData.predictions.length} predictions (SAFE: ${predictData.safeCount}, FIT: ${predictData.fitCount}, RISK: ${predictData.riskCount})`);

  // --------------------------------------------------------------------------
  // 2. TD-4 Optimistic Locking 409 E2E
  // --------------------------------------------------------------------------
  console.log('\n--- [2/4] Checking TD-4 Optimistic Locking 409 Conflict ---');

  let targetExamId = null;
  try {
    const rawExamId = queryDb("SELECT id FROM exams WHERE status IN ('DRAFT', 'HIDDEN') LIMIT 1;");
    if (rawExamId && Number(rawExamId) > 0) {
      targetExamId = Number(rawExamId);
    }
  } catch {}

  if (!targetExamId) {
    try {
      const rawExamId = queryDb('SELECT id FROM exams LIMIT 1;');
      if (rawExamId && Number(rawExamId) > 0) {
        targetExamId = Number(rawExamId);
      }
    } catch {}
  }

  if (!targetExamId) {
    fail('No exam found in database to test optimistic locking');
  }

  const getExamRes = await apiCall(`/admin/exams/${targetExamId}`, {}, 'CONTENT_ADMIN');
  if (getExamRes.status !== 200) {
    fail(`GET /admin/exams/${targetExamId} returned HTTP ${getExamRes.status}`);
  }
  const currentLockVersion = getExamRes.json?.data?.lockVersion ?? 0;

  const updateRes = await apiCall(`/admin/exams/${targetExamId}`, {
    method: 'PUT',
    body: {
      title: 'Conflicting update',
      lockVersion: 999999
    }
  }, 'CONTENT_ADMIN');

  if (updateRes.status !== 409) {
    fail(`Expected HTTP 409 Conflict, got HTTP ${updateRes.status}: ${JSON.stringify(updateRes.json)}`);
  }

  const errPayload = JSON.stringify(updateRes.json || {});
  if (!errPayload.includes('CONCURRENT_MODIFICATION')) {
    fail(`Expected error code 'CONCURRENT_MODIFICATION', got: ${JSON.stringify(updateRes.json)}`);
  }
  pass('TD-4 Optimistic Locking', `HTTP 409 and error CONCURRENT_MODIFICATION received on lockVersion mismatch (current=${currentLockVersion}, sent=999999)`);

  // --------------------------------------------------------------------------
  // 3. P4 Proctoring Auto-Submit E2E
  // --------------------------------------------------------------------------
  console.log('\n--- [3/4] Checking P4 Proctoring Auto-Submit ---');

  let examForSession = targetExamId;
  try {
    const pubId = queryDb("SELECT id FROM exams WHERE status = 'PUBLISHED' LIMIT 1;");
    if (pubId && Number(pubId) > 0) {
      examForSession = Number(pubId);
    }
  } catch {}

  const startRes = await apiCall('/sessions/start', {
    method: 'POST',
    body: {
      examId: examForSession,
      mode: 'MOCK_EXAM'
    }
  }, 'STUDENT');

  if (startRes.status !== 201) {
    fail(`POST /sessions/start returned HTTP ${startRes.status}: ${JSON.stringify(startRes.json)}`);
  }
  const sessionId = startRes.json?.data?.id;
  if (!sessionId) {
    fail('Start session response missing session ID');
  }

  // Violation 1
  const v1Res = await apiCall(`/sessions/${sessionId}/violations`, {
    method: 'POST',
    body: { violationType: 'TAB_SWITCH', details: 'Candidate switched tabs (1st time)' }
  }, 'STUDENT');
  if (v1Res.status !== 200) {
    fail(`Violation 1 returned HTTP ${v1Res.status}`);
  }
  if (v1Res.json?.data?.violationCount !== 1 || v1Res.json?.data?.autoSubmitted !== false) {
    fail(`Violation 1 mismatch: ${JSON.stringify(v1Res.json?.data)}`);
  }
  pass('Proctoring Violation 1', 'violationCount=1, autoSubmitted=false');

  // Violation 2
  const v2Res = await apiCall(`/sessions/${sessionId}/violations`, {
    method: 'POST',
    body: { violationType: 'TAB_SWITCH', details: 'Candidate switched tabs (2nd time)' }
  }, 'STUDENT');
  if (v2Res.status !== 200) {
    fail(`Violation 2 returned HTTP ${v2Res.status}`);
  }
  if (v2Res.json?.data?.violationCount !== 2 || v2Res.json?.data?.autoSubmitted !== false) {
    fail(`Violation 2 mismatch: ${JSON.stringify(v2Res.json?.data)}`);
  }
  pass('Proctoring Violation 2', 'violationCount=2, autoSubmitted=false');

  // Violation 3
  const v3Res = await apiCall(`/sessions/${sessionId}/violations`, {
    method: 'POST',
    body: { violationType: 'TAB_SWITCH', details: 'Candidate switched tabs (3rd time)' }
  }, 'STUDENT');
  if (v3Res.status !== 200) {
    fail(`Violation 3 returned HTTP ${v3Res.status}`);
  }
  const v3Data = v3Res.json?.data;
  if (v3Data?.violationCount !== 3 || v3Data?.autoSubmitted !== true || v3Data?.sessionStatus !== 'SUBMITTED') {
    fail(`Violation 3 mismatch (expected violationCount=3, autoSubmitted=true, sessionStatus='SUBMITTED'): ${JSON.stringify(v3Data)}`);
  }
  pass('Proctoring Violation 3', 'violationCount=3, autoSubmitted=true, sessionStatus=SUBMITTED');

  // --------------------------------------------------------------------------
  // 4. P1 Web Student CBT UI
  // --------------------------------------------------------------------------
  console.log('\n--- [4/4] Checking P1 Web Student CBT UI ---');

  let viteServer = null;
  let browser = null;

  try {
    viteServer = await ensureViteServer(15173);
    const targetWebUrl = viteServer.url;

    browser = await launchBrowser();
    const pageErrors = [];

    const prevOnMessage = browser.ws.onmessage;
    browser.ws.onmessage = (event) => {
      try {
        if (typeof prevOnMessage === 'function') {
          prevOnMessage(event);
        }
        const msg = JSON.parse(event.data);
        if (msg.method === 'Runtime.exceptionThrown') {
          const text = msg.params?.exceptionDetails?.text || 'Unknown exception';
          pageErrors.push(text);
        }
      } catch {}
    };

    await browser.navigate(targetWebUrl);

    await browser.evaluate(`(() => {
      window.__pageErrors = [];
      window.addEventListener('error', e => window.__pageErrors.push(e.message || String(e)));
      window.addEventListener('unhandledrejection', e => window.__pageErrors.push(e.reason?.message || String(e.reason)));
      try { localStorage.setItem('vsat_admin_access_token', 'test_admin_jwt_token_acceptance'); } catch {}
    })()`);

    await browser.waitForSelector('aside nav button', 10000);

    await browser.evaluate(`(() => {
      const buttons = Array.from(document.querySelectorAll('aside nav button'));
      const cbtBtn = buttons.find(b => b.innerText.includes('Cổng thi trực tuyến') || b.innerText.includes('CBT'));
      if (cbtBtn) cbtBtn.click();
    })()`);

    await new Promise(r => setTimeout(r, 1500));

    const bodyText = await browser.evaluate('document.body.innerText');
    if (!bodyText.includes('Cổng Thi') && !bodyText.includes('Khảo Sát Năng Lực') && !bodyText.includes('CBT')) {
      fail('CBT Lobby page content not detected after clicking CBT tab');
    }

    if (!bodyText.includes('Dự đoán Trúng tuyển Đại học') && !bodyText.includes('Admission')) {
      fail('Admission Predictor Widget not detected on CBT Lobby page');
    }

    const domErrors = await browser.evaluate('window.__pageErrors || []');
    if (domErrors.length > 0) {
      fail(`DOM page errors detected: ${domErrors.join('; ')}`);
    }

    if (pageErrors.length > 0) {
      fail(`Browser runtime exceptions detected: ${pageErrors.join('; ')}`);
    }

    pass('P1 Web Student CBT UI', 'Rendered StudentExamLobby and AdmissionPredictorWidget with zero page errors');
  } finally {
    if (browser) {
      try { await browser.close(); } catch {}
    }
    if (viteServer) {
      try { viteServer.stop(); } catch {}
    }
  }

  console.log('\n============================================================');
  console.log('[PHASE2-E2E] All Phase 1 & Phase 2 E2E assertions passed!');
  console.log('============================================================\n');
  process.exit(0);
}

runCheck().catch(err => {
  console.error(`\n[PHASE2-E2E FATAL] ${err.stack || err.message}`);
  process.exit(1);
});
