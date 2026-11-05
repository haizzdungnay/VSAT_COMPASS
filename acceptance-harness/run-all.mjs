#!/usr/bin/env node
import { spawn, spawnSync } from 'node:child_process';
import { existsSync } from 'node:fs';
import { resolve, join, delimiter } from 'node:path';
import { runSafetyPreflight } from './safety-preflight.mjs';
import { bootstrapDatabase } from './db-bootstrap.mjs';
import { evaluateVerdict } from './verdict-gate.mjs';

const HARNESS_DIR = import.meta.dirname;
const REPO_ROOT = resolve(HARNESS_DIR, '..');
const BACKEND_DIR = resolve(REPO_ROOT, 'VSAT/vsat-compass-api');
const IS_WIN = process.platform === 'win32';

let llmProcess = null;
let backendProcess = null;

export function resolveJbrHome() {
  const isWin = process.platform === 'win32';
  const javaExe = isWin ? 'bin/java.exe' : 'bin/java';

  const candidates = isWin
    ? [
        'C:\\Program Files\\Android\\Android Studio1\\jbr',
        process.env.JAVA_HOME,
        'C:\\Program Files\\Android\\Android Studio\\jbr',
        'C:\\Program Files\\Java\\jdk-21',
        'C:\\Program Files\\Eclipse Adoptium\\jdk-21'
      ].filter(Boolean)
    : [
        process.env.JAVA_HOME,
        '/usr/lib/jvm/temurin-21-jdk-amd64',
        '/usr/lib/jvm/java-21-openjdk-amd64'
      ].filter(Boolean);

  for (const c of candidates) {
    const fullExe = join(c, javaExe);
    if (existsSync(c) && existsSync(fullExe)) {
      const probe = spawnSync(fullExe, ['-version'], { stdio: 'pipe' });
      if (probe.status === 0) {
        return c;
      }
    }
  }
  return process.env.JAVA_HOME || null;
}

async function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

async function waitForHttp(url, timeoutMs = 60000, intervalMs = 1000) {
  const start = Date.now();
  while (Date.now() - start < timeoutMs) {
    try {
      const resp = await fetch(url);
      if (resp.ok) return true;
    } catch {
      // not yet up
    }
    await sleep(intervalMs);
  }
  return false;
}

async function startCleanDatabase() {
  console.log('[RUN-ALL] Step 2: Starting clean PostgreSQL container...');
  const composeFile = resolve(HARNESS_DIR, 'docker-compose.clean.yml');

  const upResult = spawnSync('docker', ['compose', '-f', composeFile, 'up', '-d'], {
    stdio: 'inherit'
  });
  if (upResult.status !== 0) {
    throw new Error('Failed to start docker compose clean postgres container');
  }

  // Poll pg_isready
  console.log('[RUN-ALL] Waiting for PostgreSQL readiness on 127.0.0.1:55432...');
  let ready = false;
  for (let i = 0; i < 30; i++) {
    const probe = spawnSync(
      'docker',
      ['exec', 'vsat-clean-pg', 'pg_isready', '-U', 'vsat_clean', '-d', 'vsat_compass_acceptance'],
      { stdio: 'pipe' }
    );
    if (probe.status === 0) {
      ready = true;
      break;
    }
    await sleep(1000);
  }

  if (!ready) {
    throw new Error('Clean PostgreSQL failed readiness probe within 30 seconds');
  }
  console.log('[RUN-ALL] PostgreSQL is ready.');
}

async function ensureBackendRunning() {
  console.log('[RUN-ALL] Step 5: Checking Backend Spring Boot on http://127.0.0.1:18080/api/v1/actuator/health...');
  const isUp = await waitForHttp('http://127.0.0.1:18080/api/v1/actuator/health', 2000, 500);

  if (isUp) {
    console.log('[RUN-ALL] Backend Spring Boot is already running.');
    return;
  }

  console.log('[RUN-ALL] Spawning Backend Spring Boot with clean-room acceptance configuration...');
  const gradlewCmd = IS_WIN ? '.\\gradlew.bat' : './gradlew';
  const jbrHome = resolveJbrHome();
  console.log(`[RUN-ALL] Resolved JDK/JBR home: ${jbrHome || 'system default'}`);

  const env = {
    ...process.env,
    ...(jbrHome
      ? {
          JAVA_HOME: jbrHome,
          PATH: `${join(jbrHome, 'bin')}${delimiter}${process.env.PATH || ''}`
        }
      : {}),
    SERVER_PORT: '18080',
    SPRING_PROFILES_ACTIVE: 'dev',
    DATABASE_URL: 'jdbc:postgresql://127.0.0.1:55432/vsat_compass_acceptance',
    DATABASE_USERNAME: 'vsat_clean',
    DATABASE_PASSWORD: 'vsat_clean_pass',
    JWT_SECRET: 'dnNhdGNvbXBhc3Mtc3VwZXItc2VjcmV0LWp3dC1rZXktMjU2Yml0LXByb2R1Y3Rpb24tMjAyNC12c2F0',
    LLM_BASE_URL: 'http://127.0.0.1:18081',
    LLM_API_KEY: 'cleanroom-dummy-key',
    LLM_MODEL: 'qwen2.5-math-7b'
  };

  const gradleArgs = [
    'bootRun',
    '--no-daemon',
    '-Dspring.profiles.active=dev',
    '-Dserver.port=18080',
    '-Dspring.datasource.url=jdbc:postgresql://127.0.0.1:55432/vsat_compass_acceptance',
    '-Dspring.datasource.username=vsat_clean',
    '-Dspring.datasource.password=vsat_clean_pass',
    '-Dapp.jwt.secret=dnNhdGNvbXBhc3Mtc3VwZXItc2VjcmV0LWp3dC1rZXktMjU2Yml0LXByb2R1Y3Rpb24tMjAyNC12c2F0',
    '-DJWT_SECRET=dnNhdGNvbXBhc3Mtc3VwZXItc2VjcmV0LWp3dC1rZXktMjU2Yml0LXByb2R1Y3Rpb24tMjAyNC12c2F0',
    '-DLLM_BASE_URL=http://127.0.0.1:18081',
    '-DLLM_API_KEY=cleanroom-dummy-key',
    '-DLLM_MODEL=qwen2.5-math-7b',
    '-Dapp.llm.base-url=http://127.0.0.1:18081'
  ];

  backendProcess = spawn(gradlewCmd, gradleArgs, {
    cwd: BACKEND_DIR,
    env,
    stdio: 'pipe',
    shell: IS_WIN
  });

  backendProcess.stdout?.on('data', chunk => {
    if (process.env.DEBUG_BACKEND) {
      process.stdout.write(`[BACKEND] ${chunk}`);
    }
  });
  backendProcess.stderr?.on('data', chunk => {
    if (process.env.DEBUG_BACKEND || process.env.CI) {
      process.stderr.write(`[BACKEND ERR] ${chunk}`);
    }
  });

  const backendReady = await waitForHttp('http://127.0.0.1:18080/api/v1/actuator/health', 120000, 2000);
  if (!backendReady) {
    throw new Error('Backend failed to start on port 18080 within 120 seconds');
  }
  console.log('[RUN-ALL] Backend Spring Boot is UP and healthy.');
}

async function runSuiteFile(suitePath) {
  const fullPath = resolve(HARNESS_DIR, suitePath);
  if (!existsSync(fullPath)) {
    console.warn(`[RUN-ALL] Suite file not yet present: ${suitePath} (skipping execution)`);
    return { skipped: true };
  }

  console.log(`\n[RUN-ALL] Executing suite: ${suitePath}...`);
  return new Promise(resolveSuite => {
    const proc = spawn(process.execPath, [fullPath], {
      cwd: HARNESS_DIR,
      stdio: 'inherit',
      env: process.env
    });
    proc.on('close', code => {
      resolveSuite({ exitCode: code });
    });
  });
}

async function cleanup(options = {}) {
  console.log('\n[RUN-ALL] Step 8: Tearing down acceptance harness resources...');

  if (llmProcess) {
    try {
      llmProcess.kill();
      console.log(' - LLM mock server process terminated.');
    } catch (e) {
      console.warn(` - Warning terminating LLM server: ${e.message}`);
    }
  }

  if (backendProcess) {
    try {
      backendProcess.kill();
      console.log(' - Spawned Backend process terminated.');
    } catch (e) {
      console.warn(` - Warning killing Backend process: ${e.message}`);
    }
  }

  if (options.teardownDocker || process.argv.includes('--teardown-docker')) {
    const composeFile = resolve(HARNESS_DIR, 'docker-compose.clean.yml');
    spawnSync('docker', ['compose', '-f', composeFile, 'down', '-v'], { stdio: 'inherit' });
    console.log(' - Clean PostgreSQL container removed.');
  }
}

async function main() {
  console.log('============================================================');
  console.log('V-SAT COMPASS - FULL ACCEPTANCE HARNESS ORCHESTRATION');
  console.log('============================================================');

  try {
    // 1. Safety Preflight
    console.log('\n[RUN-ALL] Step 1: Safety Preflight validation...');
    const preflightPassed = await runSafetyPreflight({ checkPorts: true });
    if (!preflightPassed) {
      throw new Error('Safety preflight failed! Halting orchestration.');
    }

    // 2. Clean Database Container
    await startCleanDatabase();

    // 3. Database Bootstrap (Schema + Migrations + Fixtures)
    console.log('\n[RUN-ALL] Step 3: Bootstrapping clean database schema & fixtures...');
    await bootstrapDatabase();

    // 4. Start LLM Mock Server
    console.log('\n[RUN-ALL] Step 4: Starting LLM Mock Server on port 18081...');
    const mockScript = resolve(HARNESS_DIR, 'stubs', 'llm-mock-server.mjs');
    llmProcess = spawn(process.execPath, [mockScript], {
      env: { ...process.env, LLM_MOCK_PORT: '18081', LLM_MOCK_HOST: '0.0.0.0' },
      stdio: 'pipe'
    });
    const llmReady = await waitForHttp('http://127.0.0.1:18081/health', 5000, 500);
    if (!llmReady) {
      throw new Error('LLM Mock Server failed healthcheck');
    }
    console.log('[RUN-ALL] LLM Mock Server is UP.');

    // 5. Backend Start / Verify
    if (!process.argv.includes('--skip-backend')) {
      await ensureBackendRunning();
    }

    // 6. Run Suites
    console.log('\n[RUN-ALL] Step 6: Executing test suites...');
    const suiteArgIdx = process.argv.indexOf('--suite');
    const suites = suiteArgIdx !== -1 && process.argv[suiteArgIdx + 1]
      ? [process.argv[suiteArgIdx + 1]]
      : [
          'suites/backend-admin.mjs',
          'suites/backend-public-student-collab.mjs',
          'suites/run-web-suite.mjs',
          'suites/android-student-suite.mjs',
          'suites/android-roles-workers-suite.mjs'
        ];

    for (const suite of suites) {
      await runSuiteFile(suite);
    }

    // 7. Verdict Gate Evaluation
    console.log('\n[RUN-ALL] Step 7: Evaluating acceptance verdict...');
    if (!process.argv.includes('--skip-verdict')) {
      await evaluateVerdict();
    }
  } catch (err) {
    console.error(`\n[RUN-ALL FATAL ERROR] ${err.message}`);
    process.exitCode = 1;
  } finally {
    await cleanup();
  }
}

if (process.argv[1] && process.argv[1].endsWith('run-all.mjs')) {
  main();
}
