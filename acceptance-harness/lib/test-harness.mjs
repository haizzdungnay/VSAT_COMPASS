import { spawnSync } from 'node:child_process';
import { writeFileSync, mkdirSync, existsSync } from 'node:fs';
import { resolve, dirname } from 'node:path';

const HARNESS_DIR = resolve(import.meta.dirname, '..');
const registeredCases = [];
let currentCaseContext = null;

export const STANDARD_ACCOUNTS = {
  STUDENT: { email: 'student@vsat.com', password: 'Passw0rd123!' },
  COLLABORATOR: { email: 'collab@vsat.com', password: 'Passw0rd123!' },
  CONTENT_ADMIN: { email: 'contentadmin@vsat.com', password: 'Passw0rd123!' },
  SUPER_ADMIN: { email: 'superadmin@vsat.com', password: 'Passw0rd123!' }
};

const tokenCache = new Map();

export function registerCase(id, description, fn) {
  if (typeof id !== 'string' || !id.trim()) {
    throw new Error('registerCase: ID must be a non-empty string');
  }
  if (typeof fn !== 'function') {
    throw new Error(`registerCase [${id}]: Test function must be a callable function`);
  }
  registeredCases.push({ id: id.trim(), description: description || id, fn });
}

export function recordAssertion(name, passed, actual, expected, message) {
  if (!currentCaseContext) {
    throw new Error(`Assertion "${name}" executed outside test case context!`);
  }
  const assertionEntry = {
    name,
    passed: Boolean(passed),
    actual,
    expected,
    message: message || ''
  };
  currentCaseContext.assertions.push(assertionEntry);
  if (!passed) {
    const err = new Error(
      `Assertion "${name}" FAILED: expected ${JSON.stringify(expected)}, got ${JSON.stringify(actual)} (${message || 'no detail'})`
    );
    err.isAssertionFailure = true;
    err.assertionEntry = assertionEntry;
    throw err;
  }
}

export function assertEqual(actual, expected, message = '') {
  const passed = actual === expected;
  recordAssertion('assertEqual', passed, actual, expected, message);
}

export function assertDeepEqual(actual, expected, message = '') {
  const actualStr = JSON.stringify(actual);
  const expectedStr = JSON.stringify(expected);
  const passed = actualStr === expectedStr;
  recordAssertion('assertDeepEqual', passed, actual, expected, message);
}

export function assertStatus(actualStatus, expectedStatus, message = '') {
  if (Array.isArray(expectedStatus)) {
    throw new Error(
      `[SECURITY/STANDARDS] Loose status assertion array ${JSON.stringify(expectedStatus)} is forbidden. Expect an exact HTTP status.`
    );
  }
  const passed = Number(actualStatus) === Number(expectedStatus);
  recordAssertion('assertStatus', passed, Number(actualStatus), Number(expectedStatus), message);
}

export function assertTrue(value, message = '') {
  const passed = value === true;
  recordAssertion('assertTrue', passed, value, true, message);
}

export function assertFalse(value, message = '') {
  const passed = value === false;
  recordAssertion('assertFalse', passed, value, false, message);
}

export function assertHasKeys(obj, expectedKeys, message = '') {
  const actualKeys = obj && typeof obj === 'object' ? Object.keys(obj) : [];
  const missing = expectedKeys.filter(k => !actualKeys.includes(k));
  const passed = missing.length === 0;
  recordAssertion(
    'assertHasKeys',
    passed,
    actualKeys,
    expectedKeys,
    missing.length > 0 ? `Missing keys: ${missing.join(', ')} | ${message}` : message
  );
}

export function assertDbRow(sql, validatorFn, message = '') {
  const result = queryDb(sql);
  let passed = false;
  let validationError = null;
  try {
    passed = Boolean(validatorFn(result));
  } catch (err) {
    validationError = err.message;
    passed = false;
  }
  recordAssertion(
    'assertDbRow',
    passed,
    result,
    'custom validation passed',
    validationError ? `${validationError} | ${message}` : message
  );
}

export function queryDb(sql) {
  const container = process.env.CLEAN_PG_CONTAINER || 'vsat-clean-pg';
  const user = process.env.CLEAN_PG_USER || 'vsat_clean';
  const db = process.env.CLEAN_PG_DB || 'vsat_compass_acceptance';

  const proc = spawnSync(
    'docker',
    ['exec', '-i', container, 'psql', '-q', '-U', user, '-d', db, '-t', '-A', '-c', sql],
    { encoding: 'utf8' }
  );

  if (proc.status !== 0) {
    throw new Error(`DB query failed: ${proc.stderr || proc.stdout}`);
  }
  return proc.stdout.trim();
}

export async function getAuthToken(role = 'STUDENT') {
  if (!role || role === 'ANONYMOUS') return null;
  const upperRole = role.toUpperCase();
  if (tokenCache.has(upperRole)) {
    return tokenCache.get(upperRole);
  }

  const creds = STANDARD_ACCOUNTS[upperRole];
  if (!creds) {
    throw new Error(`Unknown role for token acquisition: "${role}"`);
  }

  const rawBase = (process.env.API_BASE_URL || 'http://127.0.0.1:18080').replace(/\/api\/v1\/?$/, '').replace(/\/+$/, '');
  const loginUrl = `${rawBase}/api/v1/auth/login`;

  const resp = await fetch(loginUrl, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'X-Forwarded-For': `127.0.0.${Math.floor(Math.random() * 200) + 10}`
    },
    body: JSON.stringify({
      email: creds.email,
      password: creds.password,
      deviceInfo: `AcceptanceHarness/${upperRole}`
    }),
    signal: AbortSignal.timeout(15000)
  });

  if (!resp.ok) {
    const errorText = await resp.text();
    throw new Error(`Failed to login as ${upperRole} (${resp.status}): ${errorText}`);
  }

  const data = await resp.json();
  const token = data?.data?.accessToken || data?.accessToken;
  if (!token) {
    throw new Error(`No accessToken in login response for ${upperRole}: ${JSON.stringify(data)}`);
  }

  tokenCache.set(upperRole, token);
  return token;
}

export async function apiCall(endpointPath, options = {}, role = null) {
  const rawBase = (process.env.API_BASE_URL || 'http://127.0.0.1:18080').replace(/\/api\/v1\/?$/, '').replace(/\/+$/, '');
  const cleanPath = endpointPath.startsWith('/') ? endpointPath : `/${endpointPath}`;
  const fullUrl = `${rawBase}${cleanPath.startsWith('/api/v1') ? '' : '/api/v1'}${cleanPath}`;

  const headers = {
    'Accept': 'application/json',
    'X-Forwarded-For': options.headers?.['X-Forwarded-For'] || `127.0.0.${Math.floor(Math.random() * 200) + 10}`,
    ...(options.headers || {})
  };

  if (role) {
    const token = await getAuthToken(role);
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }
  }

  let body = options.body;
  if (body && typeof body === 'object' && !(body instanceof FormData) && !(body instanceof Buffer)) {
    headers['Content-Type'] = headers['Content-Type'] || 'application/json';
    body = JSON.stringify(body);
  }

  const startMs = Date.now();
  const resp = await fetch(fullUrl, {
    method: options.method || 'GET',
    headers,
    body,
    signal: AbortSignal.timeout(options.timeoutMs || 20000)
  });
  const durationMs = Date.now() - startMs;

  let json = null;
  let text = '';
  const contentType = resp.headers.get('content-type') || '';
  if (contentType.includes('application/json')) {
    try {
      json = await resp.json();
    } catch {
      // fallback to text if malformed json
      text = await resp.text();
    }
  } else {
    text = await resp.text();
  }

  return {
    status: resp.status,
    headers: Object.fromEntries(resp.headers.entries()),
    durationMs,
    json,
    text,
    rawResponse: resp
  };
}

export async function runSuite(suiteName, options = {}) {
  // Support --list flag: Dump registered case IDs without network/DB
  if (process.argv.includes('--list') || options.list) {
    const listPayload = {
      suite: suiteName,
      count: registeredCases.length,
      cases: registeredCases.map(c => ({ id: c.id, description: c.description }))
    };
    console.log(JSON.stringify(listPayload, null, 2));
    process.exit(0);
  }

  console.log(`\n============================================================`);
  console.log(`[SUITE START] Running ${suiteName} (${registeredCases.length} cases registered)`);
  console.log(`============================================================`);

  const results = [];
  const resultsDir = options.resultsDir || resolve(HARNESS_DIR, 'results');
  if (!existsSync(resultsDir)) {
    mkdirSync(resultsDir, { recursive: true });
  }

  for (const testCase of registeredCases) {
    let attempts = 0;
    let passed = false;
    let lastError = null;
    let assertions = [];
    let evidence = null;
    const startMs = Date.now();

    while (attempts < 2 && !passed) {
      attempts++;
      assertions = [];
      evidence = {};
      currentCaseContext = { id: testCase.id, assertions, evidence };

      try {
        await testCase.fn({
          assertEqual,
          assertDeepEqual,
          assertStatus,
          assertTrue,
          assertFalse,
          assertHasKeys,
          assertDbRow,
          apiCall,
          queryDb,
          evidence
        });

        // Ensure test did not pass with 0 assertions
        if (assertions.length === 0) {
          throw new Error(`Test passed without executing any assertions! (Disallowed empty pass)`);
        }
        passed = true;
      } catch (err) {
        lastError = err;
        if (attempts < 2) {
          console.warn(`  [RETRY] Case ${testCase.id} failed attempt 1: ${err.message}. Retrying once...`);
        }
      } finally {
        currentCaseContext = null;
      }
    }

    const durationMs = Date.now() - startMs;
    const caseResult = {
      id: testCase.id,
      description: testCase.description,
      status: passed ? 'PASSED' : 'FAILED',
      attempts,
      durationMs,
      assertions,
      evidence: evidence || {},
      error: passed ? null : (lastError?.message || 'Unknown failure')
    };

    results.push(caseResult);
    const symbol = passed ? 'OK' : 'FAIL';
    console.log(`  [${symbol}] ${testCase.id} - ${testCase.description} (${attempts} attempts, ${durationMs}ms, ${assertions.length} assertions)`);
    if (!passed) {
      console.error(`      ERROR: ${lastError?.message}`);
    }
  }

  const reportPayload = {
    suite: suiteName,
    generatedAt: new Date().toISOString(),
    totalCases: results.length,
    passedCount: results.filter(r => r.status === 'PASSED').length,
    failedCount: results.filter(r => r.status === 'FAILED').length,
    results
  };

  const reportFilePath = resolve(resultsDir, `${suiteName}.json`);
  writeFileSync(reportFilePath, JSON.stringify(reportPayload, null, 2), 'utf8');
  console.log(`\n[SUITE FINISHED] Report written to: ${reportFilePath}`);

  const hasFailures = reportPayload.failedCount > 0;
  if (hasFailures && !options.noExit) {
    process.exit(1);
  }
  return reportPayload;
}
