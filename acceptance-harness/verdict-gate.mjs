#!/usr/bin/env node
import { readFileSync, readdirSync, writeFileSync, existsSync, mkdirSync } from 'node:fs';
import { resolve, join } from 'node:path';

const HARNESS_DIR = import.meta.dirname;
const REPO_ROOT = resolve(HARNESS_DIR, '..');
const INVENTORY_FILE = resolve(REPO_ROOT, 'docs/testing/feature-inventory.json');
const TARGET_SECTIONS = [
  'backend_endpoints',
  'backend_jobs_and_integrations',
  'web_admin_pages',
  'android_screens_and_flows',
  'android_workers_and_services'
];

function getRequiredFeatureIds() {
  if (!existsSync(INVENTORY_FILE)) {
    throw new Error(`Feature inventory file not found: ${INVENTORY_FILE}`);
  }
  const inventory = JSON.parse(readFileSync(INVENTORY_FILE, 'utf8'));
  const ids = new Set();
  const idToSection = new Map();

  for (const section of TARGET_SECTIONS) {
    const list = inventory[section] || [];
    for (const item of list) {
      if (!item.id) {
        throw new Error(`Inventory item in ${section} missing id: ${JSON.stringify(item)}`);
      }
      if (ids.has(item.id)) {
        throw new Error(`Duplicate ID found in inventory: ${item.id}`);
      }
      ids.add(item.id);
      idToSection.set(item.id, section);
    }
  }

  if (ids.size !== 126) {
    throw new Error(
      `Expected exactly 126 target IDs from feature inventory, but extracted ${ids.size} IDs!`
    );
  }
  return { requiredIds: ids, idToSection };
}

function parseCliArgs() {
  const args = process.argv.slice(2);
  let resultsDir = resolve(HARNESS_DIR, 'results');
  for (let i = 0; i < args.length; i++) {
    if (args[i] === '--results-dir' && args[i + 1]) {
      resultsDir = resolve(args[i + 1]);
      i++;
    }
  }
  return { resultsDir };
}

export async function evaluateVerdict(customResultsDir = null) {
  const { resultsDir } = customResultsDir ? { resultsDir: resolve(customResultsDir) } : parseCliArgs();
  console.log(`\n============================================================`);
  console.log(`[VERDICT GATE] Evaluating Clean-Room Acceptance Results`);
  console.log(`  Source directory: ${resultsDir}`);
  console.log(`============================================================`);

  const { requiredIds, idToSection } = getRequiredFeatureIds();
  console.log(`[VERDICT GATE] Required acceptance IDs: 126 (across 5 feature domains)`);

  if (!existsSync(resultsDir)) {
    console.error(`[VERDICT REJECTED] Results directory does not exist: ${resultsDir}`);
    process.exit(1);
  }

  const resultFiles = readdirSync(resultsDir).filter(
    f => f.endsWith('.json') && f !== 'final-verdict.json'
  );

  if (resultFiles.length === 0) {
    console.error(`[VERDICT REJECTED] No test result JSON files found in: ${resultsDir}`);
    process.exit(1);
  }

  const collectedResults = new Map();
  const violations = [];

  for (const file of resultFiles) {
    const fullPath = join(resultsDir, file);
    try {
      const data = JSON.parse(readFileSync(fullPath, 'utf8'));
      const suiteResults = Array.isArray(data.results) ? data.results : [];

      for (const res of suiteResults) {
        const id = res.id;
        if (!id) {
          violations.push(`File ${file} contains test case without ID: ${JSON.stringify(res)}`);
          continue;
        }

        if (collectedResults.has(id)) {
          violations.push(`Duplicate result reported for ID: ${id} (in file ${file})`);
          continue;
        }

        if (!requiredIds.has(id)) {
          violations.push(`Unexpected / unregistered test ID reported: ${id} (in file ${file})`);
          continue;
        }

        collectedResults.set(id, { ...res, sourceFile: file });
      }
    } catch (err) {
      violations.push(`Error parsing result file ${file}: ${err.message}`);
    }
  }

  // 1. Missing IDs check
  const missingIds = [];
  for (const reqId of requiredIds) {
    if (!collectedResults.has(reqId)) {
      missingIds.push(reqId);
    }
  }

  if (missingIds.length > 0) {
    violations.push(`Missing ${missingIds.length} required acceptance IDs: ${missingIds.join(', ')}`);
  }

  // 2. Total count check
  if (collectedResults.size !== 126) {
    violations.push(
      `Total collected test IDs count mismatch: expected 126, got ${collectedResults.size}`
    );
  }

  // 3. Status and assertion integrity checks for each collected test
  let passedCount = 0;
  let failedCount = 0;

  for (const [id, res] of collectedResults.entries()) {
    if (res.status !== 'PASSED') {
      failedCount++;
      violations.push(`Test [${id}] status is "${res.status}" (must be "PASSED"): ${res.error || 'N/A'}`);
      continue;
    }

    if (res.attempts > 2) {
      violations.push(`Test [${id}] exceeded max allowed attempts (attempts=${res.attempts}, max=2)`);
    }

    if (!Array.isArray(res.assertions) || res.assertions.length === 0) {
      violations.push(`Test [${id}] has no recorded assertions! (Empty passes are rejected)`);
      continue;
    }

    const failedAssertions = res.assertions.filter(a => a.passed === false);
    if (failedAssertions.length > 0) {
      violations.push(
        `Test [${id}] marked PASSED but has ${failedAssertions.length} failed assertion(s): ${JSON.stringify(failedAssertions)}`
      );
      continue;
    }

    passedCount++;
  }

  const isAccepted = violations.length === 0 && passedCount === 126;
  const verdictStatus = isAccepted ? 'ACCEPTED' : 'REJECTED';

  const verdictPayload = {
    verdict: verdictStatus,
    timestamp: new Date().toISOString(),
    summary: {
      requiredCount: 126,
      collectedCount: collectedResults.size,
      passedCount,
      failedCount,
      violationCount: violations.length
    },
    violations,
    suiteBreakdown: {}
  };

  for (const section of TARGET_SECTIONS) {
    const sectionIds = [...collectedResults.values()].filter(
      r => idToSection.get(r.id) === section
    );
    verdictPayload.suiteBreakdown[section] = {
      total: sectionIds.length,
      passed: sectionIds.filter(r => r.status === 'PASSED').length
    };
  }

  // Ensure results dir exists and save verdict files
  if (!existsSync(resultsDir)) {
    mkdirSync(resultsDir, { recursive: true });
  }

  const verdictJsonPath = join(resultsDir, 'final-verdict.json');
  writeFileSync(verdictJsonPath, JSON.stringify(verdictPayload, null, 2), 'utf8');

  const summaryText = [
    `============================================================`,
    `V-SAT COMPASS CLEAN-ROOM ACCEPTANCE FINAL VERDICT`,
    `============================================================`,
    `Verdict          : ${verdictStatus}`,
    `Generated At     : ${verdictPayload.timestamp}`,
    `Total Required   : 126 IDs`,
    `Total Collected  : ${collectedResults.size} IDs`,
    `Passed Count     : ${passedCount} / 126`,
    `Failed Count     : ${failedCount}`,
    `Violations Count : ${violations.length}`,
    `------------------------------------------------------------`,
    `Domain Breakdown :`,
    ...TARGET_SECTIONS.map(
      sec => `  - ${sec.padEnd(30)}: ${verdictPayload.suiteBreakdown[sec]?.passed || 0}/${verdictPayload.suiteBreakdown[sec]?.total || 0}`
    ),
    `============================================================`,
    violations.length > 0 ? `\nVIOLATIONS:\n${violations.map(v => `  * ${v}`).join('\n')}` : `All 126/126 acceptance criteria passed with strict assertions.`
  ].join('\n');

  const summaryTxtPath = join(resultsDir, 'machine-summary.txt');
  writeFileSync(summaryTxtPath, summaryText, 'utf8');

  console.log(summaryText);

  if (!isAccepted) {
    console.error(`\n[FINAL RESULT] VERDICT: REJECTED`);
    process.exit(1);
  } else {
    console.log(`\n[FINAL RESULT] VERDICT: ACCEPTED (126/126 passed)`);
    process.exit(0);
  }
}

if (process.argv[1] && process.argv[1].endsWith('verdict-gate.mjs')) {
  evaluateVerdict();
}
