import { spawnSync } from 'node:child_process';
import { readFileSync, existsSync, readdirSync, statSync, writeFileSync, mkdirSync } from 'node:fs';
import { resolve, join } from 'node:path';
import crypto from 'node:crypto';

const REPO_ROOT = resolve(import.meta.dirname, '../../..');
const APP_DIR = resolve(REPO_ROOT, 'app');
const MAIN_DIR = resolve(APP_DIR, 'src/main');
const RES_DIR = resolve(MAIN_DIR, 'res');
const ASSETS_DIR = resolve(MAIN_DIR, 'assets');
const JAVA_DIR = resolve(MAIN_DIR, 'java/com/example/v_sat_compass');

export const PACKAGE_NAME = 'com.example.v_sat_compass';
export const ALT_PACKAGE_NAME = 'com.vsat.compass';

// Cryptographic constants from OfflineExamRepository.java
export const OFFLINE_MAGIC_HEADER = Buffer.from([0x56, 0x53, 0x41, 0x54, 0x02]); // 'V', 'S', 'A', 'T', 0x02
export const OFFLINE_AES_KEY = Buffer.from([
  0x56, 0x53, 0x41, 0x54, 0x5f, 0x43, 0x4f, 0x4d, 0x50, 0x41, 0x53, 0x53, 0x5f, 0x4b, 0x45, 0x59
]); // 'VSAT_COMPASS_KEY'

// ============================================================================
// 1. ADB & DEVICE INSPECTION
// ============================================================================

export function findAdbPath() {
  const custom = process.env.ADB_PATH;
  if (custom && existsSync(custom)) return custom;

  const isWin = process.platform === 'win32';
  const adbBin = isWin ? 'adb.exe' : 'adb';

  const candidates = [
    'adb',
    adbBin,
    'C:\\Users\\PC\\AppData\\Local\\Android\\Sdk\\platform-tools\\adb.exe',
    process.env.LOCALAPPDATA ? join(process.env.LOCALAPPDATA, 'Android/Sdk/platform-tools', adbBin) : null,
    process.env.ANDROID_HOME ? join(process.env.ANDROID_HOME, 'platform-tools', adbBin) : null,
    process.env.ANDROID_SDK_ROOT ? join(process.env.ANDROID_SDK_ROOT, 'platform-tools', adbBin) : null
  ].filter(Boolean);

  for (const c of candidates) {
    try {
      const probe = spawnSync(c, ['version'], { encoding: 'utf8' });
      if (probe.status === 0) return c;
    } catch {
      // ignore
    }
  }
  return null;
}

export function getConnectedDevices(adbPath) {
  if (!adbPath) return [];
  try {
    const proc = spawnSync(adbPath, ['devices'], { encoding: 'utf8' });
    if (proc.status !== 0) return [];
    const lines = proc.stdout.split('\n');
    const devices = [];
    for (let i = 1; i < lines.length; i++) {
      const line = lines[i].trim();
      if (!line) continue;
      const parts = line.split(/\s+/);
      if (parts.length >= 2 && parts[1] === 'device') {
        devices.push(parts[0]);
      }
    }
    return devices;
  } catch {
    return [];
  }
}

export function adbShell(adbPath, command, serial = null) {
  if (!adbPath) return { status: -1, stdout: '', stderr: 'ADB not available' };
  const args = serial ? ['-s', serial, 'shell', command] : ['shell', command];
  return spawnSync(adbPath, args, { encoding: 'utf8', timeout: 15000 });
}

export function dumpUiAutomator(adbPath, serial = null) {
  const dumpCmd = 'uiautomator dump /data/local/tmp/dump.xml && cat /data/local/tmp/dump.xml';
  const res = adbShell(adbPath, dumpCmd, serial);
  return res.status === 0 ? res.stdout : '';
}

export function getTopActivity(adbPath, serial = null) {
  const res = adbShell(adbPath, 'dumpsys activity top', serial);
  return res.status === 0 ? res.stdout : '';
}

export function getJobSchedulerDump(adbPath, pkg = PACKAGE_NAME, serial = null) {
  const res = adbShell(adbPath, `dumpsys jobscheduler | grep -A 10 -B 2 "${pkg}"`, serial);
  return res.status === 0 ? res.stdout : '';
}

export function getNotificationDump(adbPath, serial = null) {
  const res = adbShell(adbPath, 'dumpsys notification --noredact', serial);
  return res.status === 0 ? res.stdout : '';
}

export function queryAppWorkDb(adbPath, sql, serial = null) {
  for (const pkg of [PACKAGE_NAME, ALT_PACKAGE_NAME]) {
    const cmd = `run-as ${pkg} sqlite3 databases/androidx.work.workdb "${sql}"`;
    const res = adbShell(adbPath, cmd, serial);
    if (res.status === 0 && res.stdout.trim()) {
      return res.stdout.trim();
    }
  }
  return '';
}

// ============================================================================
// 2. ANDROID CODEBASE INSPECTION HELPERS
// ============================================================================

export function readManifest() {
  const path = resolve(MAIN_DIR, 'AndroidManifest.xml');
  return readFileSync(path, 'utf8');
}

export function readLayout(layoutName) {
  const path = resolve(RES_DIR, `layout/${layoutName}.xml`);
  return existsSync(path) ? readFileSync(path, 'utf8') : '';
}

export function readStringsXml() {
  const path = resolve(RES_DIR, 'values/strings.xml');
  return existsSync(path) ? readFileSync(path, 'utf8') : '';
}

export function readJavaSource(subPath) {
  const path = resolve(JAVA_DIR, subPath);
  return existsSync(path) ? readFileSync(path, 'utf8') : '';
}

export function verifyLayoutContains(layoutContent, expectedTokens) {
  const missing = [];
  for (const token of expectedTokens) {
    if (!layoutContent.includes(token)) {
      missing.push(token);
    }
  }
  return { ok: missing.length === 0, missing };
}

// ============================================================================
// 3. KATEX & CRYPTOGRAPHIC VERIFICATION
// ============================================================================

export function containsMath(text) {
  if (!text) return false;
  return text.includes('$')
    || text.includes('\\[')
    || text.includes('\\(')
    || text.includes('\\frac')
    || text.includes('\\sqrt');
}

export function escapeForHtml(text) {
  if (!text) return '';
  return text
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/\n/g, '<br>');
}

export function buildKaTeXHtml(rawText) {
  const body = escapeForHtml(rawText);
  return `<!DOCTYPE html><html><head><meta charset="utf-8">`
    + `<meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">`
    + `<link rel="stylesheet" href="file:///android_asset/katex/katex.min.css">`
    + `<script src="file:///android_asset/katex/katex.min.js"></script>`
    + `<script src="file:///android_asset/katex/auto-render.min.js"></script>`
    + `<style>body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; font-size: 15px; line-height: 1.6; color: #212121; margin: 0; padding: 0; background: transparent; word-wrap: break-word; } .katex-display { margin: 8px 0; overflow-x: auto; overflow-y: hidden; }</style>`
    + `</head><body><div id="math-content">${body}</div>`
    + `<script>document.addEventListener('DOMContentLoaded', function() { renderMathInElement(document.getElementById('math-content'), { delimiters: [ {left: '$$', right: '$$', display: true}, {left: '$', right: '$', display: false}, {left: '\\\\[', right: '\\\\]', display: true}, {left: '\\\\(', right: '\\\\)', display: false} ], throwOnError: false }); });</script></body></html>`;
}

export function verifyKatexAssetBundle() {
  const css = resolve(ASSETS_DIR, 'katex/katex.min.css');
  const jsCore = resolve(ASSETS_DIR, 'katex/katex.min.js');
  const jsAuto = resolve(ASSETS_DIR, 'katex/auto-render.min.js');
  const fontsDir = resolve(ASSETS_DIR, 'katex/fonts');

  const cssOk = existsSync(css) && statSync(css).size > 1000;
  const jsCoreOk = existsSync(jsCore) && statSync(jsCore).size > 10000;
  const jsAutoOk = existsSync(jsAuto) && statSync(jsAuto).size > 1000;
  const fontsOk = existsSync(fontsDir) && readdirSync(fontsDir).length >= 10;

  return { cssOk, jsCoreOk, jsAutoOk, fontsOk, allOk: cssOk && jsCoreOk && jsAutoOk && fontsOk };
}

export function encryptOfflineBundle(payloadBuffer) {
  const iv = crypto.randomBytes(12);
  const cipher = crypto.createCipheriv('aes-128-gcm', OFFLINE_AES_KEY, iv);
  const enc = Buffer.concat([cipher.update(payloadBuffer), cipher.final()]);
  const tag = cipher.getAuthTag();
  // Format matching OfflineExamRepository.java: MAGIC_HEADER (5) + IV (12) + CIPHERTEXT + TAG (16)
  return Buffer.concat([OFFLINE_MAGIC_HEADER, iv, enc, tag]);
}

export function decryptOfflineBundle(encryptedBuffer) {
  if (encryptedBuffer.length < OFFLINE_MAGIC_HEADER.length + 12 + 16) {
    throw new Error('Encrypted buffer too short (< 33 bytes)');
  }

  const magic = encryptedBuffer.subarray(0, 5);
  if (!magic.equals(OFFLINE_MAGIC_HEADER)) {
    throw new Error('Invalid magic header');
  }

  const iv = encryptedBuffer.subarray(5, 17);
  const cipherWithTag = encryptedBuffer.subarray(17);
  const tag = cipherWithTag.subarray(cipherWithTag.length - 16);
  const ciphertext = cipherWithTag.subarray(0, cipherWithTag.length - 16);

  const decipher = crypto.createDecipheriv('aes-128-gcm', OFFLINE_AES_KEY, iv);
  decipher.setAuthTag(tag);
  return Buffer.concat([decipher.update(ciphertext), decipher.final()]);
}

// ============================================================================
// 4. JUNIT XML EXPORTER
// ============================================================================

export function exportJunitXml(reportPayload, targetPath) {
  const cases = reportPayload.results || [];
  const total = reportPayload.totalCases || cases.length;
  const failures = reportPayload.failedCount || 0;
  const totalDurationSec = (cases.reduce((sum, c) => sum + (c.durationMs || 0), 0) / 1000).toFixed(3);

  let xml = `<?xml version="1.0" encoding="UTF-8"?>\n`;
  xml += `<testsuites name="${reportPayload.suite}" tests="${total}" failures="${failures}" errors="0" time="${totalDurationSec}">\n`;
  xml += `  <testsuite name="${reportPayload.suite}" tests="${total}" failures="${failures}" errors="0" skipped="0" time="${totalDurationSec}">\n`;

  for (const c of cases) {
    const timeSec = ((c.durationMs || 0) / 1000).toFixed(3);
    const domain = c.id.startsWith('WRK') ? 'workers' : 'screens';
    const className = `com.example.v_sat_compass.${domain}.${c.id}`;
    const escapedDesc = (c.description || c.id)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;');

    xml += `    <testcase classname="${className}" name="${c.id}: ${escapedDesc}" time="${timeSec}">\n`;
    if (c.status !== 'PASSED') {
      const errMsg = (c.error || 'Test failed')
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;');
      xml += `      <failure message="${errMsg}">${errMsg}</failure>\n`;
    }
    xml += `    </testcase>\n`;
  }

  xml += `  </testsuite>\n`;
  xml += `</testsuites>\n`;

  const dir = resolve(targetPath, '..');
  if (!existsSync(dir)) {
    mkdirSync(dir, { recursive: true });
  }
  writeFileSync(targetPath, xml, 'utf8');
}
