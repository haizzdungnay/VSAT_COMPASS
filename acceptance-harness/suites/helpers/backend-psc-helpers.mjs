import { spawnSync } from 'node:child_process';
import { resolve } from 'node:path';
import { existsSync, readdirSync } from 'node:fs';

const REPO_ROOT = resolve(import.meta.dirname, '../../../');
const HARNESS_DIR = resolve(REPO_ROOT, 'acceptance-harness');
const BACKEND_CLASSES = resolve(REPO_ROOT, 'VSAT/vsat-compass-api/build/classes/java/main');
const TRIGGERS_DIR = resolve(HARNESS_DIR, 'jvm-triggers');

function resolveJavaBinary(binName) {
  const isWin = process.platform === 'win32';
  const exe = isWin ? `${binName}.exe` : binName;

  const candidates = isWin
    ? [
        `C:/Program Files/Android/Android Studio1/jbr/bin/${exe}`,
        process.env.JAVA_HOME ? resolve(process.env.JAVA_HOME, 'bin', exe) : null,
        `C:/Program Files/Android/Android Studio/jbr/bin/${exe}`,
        `C:/Program Files/Java/jdk-21/bin/${exe}`,
        `C:/Program Files/Eclipse Adoptium/jdk-21/bin/${exe}`
      ].filter(Boolean)
    : [
        process.env.JAVA_HOME ? resolve(process.env.JAVA_HOME, 'bin', exe) : null,
        `/usr/lib/jvm/temurin-21-jdk-amd64/bin/${exe}`,
        `/usr/lib/jvm/java-21-openjdk-amd64/bin/${exe}`,
        `/usr/bin/${exe}`
      ].filter(Boolean);

  for (const c of candidates) {
    if (existsSync(c)) {
      const probe = spawnSync(c, ['-version'], { stdio: 'pipe' });
      if (probe.status === 0) {
        return c;
      }
    }
  }
  return exe;
}

function findPgJar() {
  if (process.env.PG_JAR && existsSync(process.env.PG_JAR)) {
    return process.env.PG_JAR;
  }
  const hardcodedWin = 'C:/Users/PC/.gradle/caches/modules-2/files-2.1/org.postgresql/postgresql/42.7.8/81b840fbfe0a6c0b7aa14c6bd4856108d36ed780/postgresql-42.7.8.jar';
  if (existsSync(hardcodedWin)) return hardcodedWin;

  const home = process.env.USERPROFILE || process.env.HOME || '';
  if (home) {
    const gradleDir = resolve(home, '.gradle/caches/modules-2/files-2.1/org.postgresql/postgresql');
    if (existsSync(gradleDir)) {
      try {
        const stack = [gradleDir];
        while (stack.length > 0) {
          const curr = stack.pop();
          const entries = readdirSync(curr, { withFileTypes: true });
          for (const e of entries) {
            const full = resolve(curr, e.name);
            if (e.isDirectory()) stack.push(full);
            else if (e.name.endsWith('.jar')) return full;
          }
        }
      } catch {}
    }
  }
  return null;
}

const JAVA_BIN = resolveJavaBinary('java');
const JAVAC_BIN = resolveJavaBinary('javac');

export const sharedState = {
  tempUser: {
    email: `temp_student_${Date.now()}@vsat.com`,
    password: 'Passw0rd123!',
    fullName: 'Học Sinh Tạm Thời',
    phone: '0908888888',
    accessToken: null,
    refreshToken: null,
    userId: null
  },
  createdCollabQuestionId: null,
  createdSessionId: null
};

export async function setLlmMockMode(mode = 'normal') {
  const rawBase = (process.env.LLM_BASE_URL || 'http://127.0.0.1:18081')
    .replace(/\/v1\/?$/, '')
    .replace(/\/+$/, '');
  const resp = await fetch(`${rawBase}/__control/mode`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ mode }),
    signal: AbortSignal.timeout(5000)
  });
  if (!resp.ok) {
    throw new Error(`Failed to set LLM mock mode to ${mode}: HTTP ${resp.status}`);
  }
  return await resp.json();
}

export function compileJvmTriggerIfNeeded() {
  const classFile = resolve(TRIGGERS_DIR, 'jvmtriggers/TriggerRefreshTokenCleanup.class');
  const srcFile = resolve(TRIGGERS_DIR, 'TriggerRefreshTokenCleanup.java');
  if (!existsSync(classFile) && existsSync(JAVAC_BIN) && existsSync(srcFile)) {
    const res = spawnSync(JAVAC_BIN, ['-d', TRIGGERS_DIR, srcFile], { encoding: 'utf8' });
    if (res.status !== 0) {
      console.warn('[PSC-HELPERS] Warning compiling TriggerRefreshTokenCleanup:', res.stderr || res.stdout);
    }
  }
}

export function runJvmRefreshTokenCleanup() {
  compileJvmTriggerIfNeeded();

  const classpathParts = [TRIGGERS_DIR, BACKEND_CLASSES];
  const pgJar = findPgJar();
  if (pgJar && existsSync(pgJar)) {
    classpathParts.push(pgJar);
  }
  const cpSep = process.platform === 'win32' ? ';' : ':';
  const cp = classpathParts.join(cpSep);

  const env = {
    ...process.env,
    DATABASE_URL: process.env.DATABASE_URL || 'jdbc:postgresql://127.0.0.1:55432/vsat_compass_acceptance',
    DATABASE_USERNAME: process.env.DATABASE_USERNAME || 'vsat_clean',
    DATABASE_PASSWORD: process.env.DATABASE_PASSWORD || 'vsat_clean_pass'
  };

  const proc = spawnSync(JAVA_BIN, ['-cp', cp, 'jvmtriggers.TriggerRefreshTokenCleanup'], {
    env,
    encoding: 'utf8'
  });

  return {
    exitCode: proc.status,
    stdout: proc.stdout || '',
    stderr: proc.stderr || ''
  };
}

let cachedFixtureIds = null;

export function getFixtureIds(queryDb) {
  if (cachedFixtureIds) return cachedFixtureIds;
  try {
    const subjectId = Number(queryDb("SELECT id FROM subjects WHERE code = 'MATH' LIMIT 1;"));
    const topicId = Number(queryDb("SELECT id FROM topics WHERE code = 'MATH_ALG' LIMIT 1;"));
    const topicCalcId = Number(queryDb("SELECT id FROM topics WHERE code = 'MATH_CALC' LIMIT 1;"));
    const subtopicId = Number(queryDb("SELECT id FROM subtopics WHERE code = 'MATH_CALC_DERIVATIVE' LIMIT 1;"));
    const questionId = Number(queryDb("SELECT id FROM questions WHERE question_code = 'Q_CLEAN_MCQ_001' LIMIT 1;"));
    const optionId = Number(queryDb(`SELECT id FROM question_options WHERE question_id = ${questionId} AND is_correct = TRUE LIMIT 1;`));
    const examId = Number(queryDb("SELECT id FROM exams WHERE exam_code = 'CLEAN_ROOM_MOCK_001' LIMIT 1;"));

    cachedFixtureIds = {
      subjectId,
      topicId,
      topicCalcId,
      subtopicId,
      questionId,
      optionId,
      examId
    };
    return cachedFixtureIds;
  } catch (err) {
    // If DB is offline (e.g. during syntax check), return fallback placeholder IDs
    return {
      subjectId: 1,
      topicId: 1,
      topicCalcId: 2,
      subtopicId: 1,
      questionId: 1,
      optionId: 3,
      examId: 1
    };
  }
}
