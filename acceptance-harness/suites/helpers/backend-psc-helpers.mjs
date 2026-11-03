import { spawnSync } from 'node:child_process';
import { resolve } from 'node:path';
import { existsSync } from 'node:fs';

const REPO_ROOT = resolve(import.meta.dirname, '../../../');
const HARNESS_DIR = resolve(REPO_ROOT, 'acceptance-harness');
const BACKEND_CLASSES = resolve(REPO_ROOT, 'VSAT/vsat-compass-api/build/classes/java/main');
const TRIGGERS_DIR = resolve(HARNESS_DIR, 'jvm-triggers');

// PostgreSQL driver jar in gradle cache
const PG_JAR = 'C:/Users/PC/.gradle/caches/modules-2/files-2.1/org.postgresql/postgresql/42.7.8/81b840fbfe0a6c0b7aa14c6bd4856108d36ed780/postgresql-42.7.8.jar';
const JAVA_BIN = 'C:/Program Files/Android/Android Studio1/jbr/bin/java.exe';
const JAVAC_BIN = 'C:/Program Files/Android/Android Studio1/jbr/bin/javac.exe';

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
  const llmBaseUrl = process.env.LLM_BASE_URL || 'http://127.0.0.1:18081';
  const resp = await fetch(`${llmBaseUrl}/__control/mode`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ mode })
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
  if (existsSync(PG_JAR)) {
    classpathParts.push(PG_JAR);
  }
  const cp = classpathParts.join(';');

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
