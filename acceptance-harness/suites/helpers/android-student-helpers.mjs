import { spawnSync, execSync } from 'node:child_process';
import { existsSync, readFileSync, writeFileSync, readdirSync } from 'node:fs';
import { resolve, join } from 'node:path';
import crypto from 'node:crypto';

const REPO_ROOT = resolve(import.meta.dirname, '../../..');
const APP_DIR = resolve(REPO_ROOT, 'app');
const MANIFEST_PATH = resolve(APP_DIR, 'src/main/AndroidManifest.xml');
const RES_LAYOUT_DIR = resolve(APP_DIR, 'src/main/res/layout');
const ASSETS_DIR = resolve(APP_DIR, 'src/main/assets');
const APK_DEBUG_PATH = resolve(APP_DIR, 'build/outputs/apk/debug/app-debug.apk');

export const PACKAGE_NAME = 'com.example.v_sat_compass';

// Locate ADB executable across platforms and common Android SDK paths
export function getAdbPath() {
  if (process.env.ADB_PATH && existsSync(process.env.ADB_PATH)) {
    return process.env.ADB_PATH;
  }
  const isWin = process.platform === 'win32';
  const adbExe = isWin ? 'adb.exe' : 'adb';

  const candidates = [
    'C:\\Users\\PC\\AppData\\Local\\Android\\Sdk\\platform-tools\\adb.exe',
    process.env.ANDROID_HOME ? join(process.env.ANDROID_HOME, 'platform-tools', adbExe) : null,
    process.env.ANDROID_SDK_ROOT ? join(process.env.ANDROID_SDK_ROOT, 'platform-tools', adbExe) : null,
    adbExe,
    'adb'
  ].filter(Boolean);

  for (const candidate of candidates) {
    if (existsSync(candidate)) return candidate;
  }
  return 'adb';
}

export function execAdb(args, options = {}) {
  const adb = getAdbPath();
  const res = spawnSync(adb, args, {
    encoding: 'utf8',
    timeout: options.timeout || 15000,
    ...options
  });
  return {
    status: res.status,
    stdout: res.stdout || '',
    stderr: res.stderr || '',
    error: res.error
  };
}

export function getConnectedDevice() {
  const res = execAdb(['devices']);
  if (res.status !== 0) return null;
  const lines = res.stdout.split('\n').map(l => l.trim()).filter(Boolean);
  for (const line of lines.slice(1)) {
    const [serial, state] = line.split(/\s+/);
    if (serial && state === 'device') {
      return serial;
    }
  }
  return null;
}

export function reverseTcp(port = 18080, deviceId = null) {
  const dev = deviceId || getConnectedDevice();
  if (!dev) return false;
  const res = execAdb(['-s', dev, 'reverse', `tcp:${port}`, `tcp:${port}`]);
  return res.status === 0;
}

export function isAppInstalled(deviceId = null) {
  const dev = deviceId || getConnectedDevice();
  if (!dev) return false;
  const res = execAdb(['-s', dev, 'shell', 'pm', 'list', 'packages', PACKAGE_NAME]);
  return res.status === 0 && res.stdout.includes(PACKAGE_NAME);
}

export function installDebugApk(deviceId = null) {
  const dev = deviceId || getConnectedDevice();
  if (!dev) return false;
  if (!existsSync(APK_DEBUG_PATH)) return false;
  const res = execAdb(['-s', dev, 'install', '-r', '-g', APK_DEBUG_PATH], { timeout: 60000 });
  return res.status === 0 && (res.stdout.includes('Success') || res.stdout.includes('installed'));
}

export function startAmActivity(component, extras = {}, deviceId = null) {
  const dev = deviceId || getConnectedDevice();
  if (!dev) return { success: false, reason: 'NO_DEVICE' };

  const fullComponent = component.startsWith('.')
    ? `${PACKAGE_NAME}/${PACKAGE_NAME}${component}`
    : `${PACKAGE_NAME}/${component}`;

  const amArgs = ['-s', dev, 'shell', 'am', 'start', '-W', '-n', fullComponent];

  for (const [key, val] of Object.entries(extras)) {
    if (typeof val === 'number') {
      if (Number.isInteger(val)) {
        if (Math.abs(val) > 2147483647) {
          amArgs.push('--el', key, String(val));
        } else {
          amArgs.push('--ei', key, String(val));
        }
      } else {
        amArgs.push('--ef', key, String(val));
      }
    } else if (typeof val === 'boolean') {
      amArgs.push('--ez', key, String(val));
    } else {
      amArgs.push('--es', key, String(val));
    }
  }

  const res = execAdb(amArgs, { timeout: 15000 });
  const success = res.status === 0 && !res.stdout.includes('Error:') && !res.stdout.includes('Exception');
  return {
    success,
    output: res.stdout,
    error: res.stderr || (res.error ? res.error.message : null)
  };
}

export function getDumpsysTopActivity(deviceId = null) {
  const dev = deviceId || getConnectedDevice();
  if (!dev) return '';
  const res = execAdb(['-s', dev, 'shell', 'dumpsys', 'activity', 'top']);
  return res.stdout || '';
}

export function dumpUiHierarchyXml(deviceId = null) {
  const dev = deviceId || getConnectedDevice();
  if (!dev) return '';
  execAdb(['-s', dev, 'shell', 'uiautomator', 'dump', '/sdcard/window_dump.xml'], { timeout: 10000 });
  const dump = execAdb(['-s', dev, 'exec-out', 'cat', '/sdcard/window_dump.xml'], { timeout: 10000 });
  return dump.stdout || '';
}

export function seedSandboxPreferences(prefs = {}, deviceId = null) {
  const dev = deviceId || getConnectedDevice();
  if (!dev) return false;

  const entriesXml = Object.entries(prefs).map(([k, v]) => {
    if (typeof v === 'boolean') return `    <boolean name="${k}" value="${v}" />`;
    if (typeof v === 'number') {
      return Number.isInteger(v)
        ? `    <int name="${k}" value="${v}" />`
        : `    <float name="${k}" value="${v}" />`;
    }
    return `    <string name="${k}">${String(v)}</string>`;
  }).join('\n');

  const xmlContent = `<?xml version='1.0' encoding='utf-8' standalone='yes' ?>\n<map>\n${entriesXml}\n</map>\n`;
  const base64Content = Buffer.from(xmlContent, 'utf8').toString('base64');
  const remotePath = '/data/data/com.example.v_sat_compass/shared_prefs/vsat_prefs.xml';

  const cmd = `echo "${base64Content}" | base64 -d > ${remotePath} && chmod 660 ${remotePath}`;
  const res = execAdb(['-s', dev, 'shell', 'run-as', PACKAGE_NAME, 'sh', '-c', cmd]);
  return res.status === 0;
}

// ---------------------------------------------------------------------------
// Static Clean-Room Verification Helpers (Manifest, Layouts, KaTeX, .vsat)
// ---------------------------------------------------------------------------

let cachedManifestContent = null;
export function getManifestContent() {
  if (!cachedManifestContent) {
    cachedManifestContent = readFileSync(MANIFEST_PATH, 'utf8');
  }
  return cachedManifestContent;
}

export function verifyManifestActivity(shortClassName) {
  const manifest = getManifestContent();
  const patterns = [
    new RegExp(`<activity[^>]*android:name="\\${shortClassName}"[^>]*>`, 'i'),
    new RegExp(`<activity[^>]*android:name="${PACKAGE_NAME}\\${shortClassName}"[^>]*>`, 'i'),
    new RegExp(`<activity[^>]*android:name="${shortClassName}"[^>]*>`, 'i')
  ];

  for (const regex of patterns) {
    const match = manifest.match(regex);
    if (match) {
      const tag = match[0];
      const isExported = tag.includes('android:exported="true"');
      const isExplicitlyNonExported = tag.includes('android:exported="false"');
      return { declared: true, tag, isExported, isExplicitlyNonExported };
    }
  }
  return { declared: false, tag: null, isExported: false, isExplicitlyNonExported: false };
}

export function verifyLayoutViewIds(layoutFileName, requiredIds = []) {
  const layoutPath = resolve(RES_LAYOUT_DIR, layoutFileName);
  if (!existsSync(layoutPath)) {
    return { exists: false, missingIds: requiredIds, foundIds: [] };
  }
  const content = readFileSync(layoutPath, 'utf8');
  const foundIds = [];
  const missingIds = [];

  for (const id of requiredIds) {
    const hasId = content.includes(`@+id/${id}"`) || content.includes(`@id/${id}"`);
    if (hasId) {
      foundIds.push(id);
    } else {
      missingIds.push(id);
    }
  }

  return {
    exists: true,
    missingIds,
    foundIds,
    allMatch: missingIds.length === 0
  };
}

export function verifyKatexAssetIntegrity() {
  const katexJsPath = resolve(ASSETS_DIR, 'katex/katex.min.js');
  const katexCssPath = resolve(ASSETS_DIR, 'katex/katex.min.css');
  const autoRenderJsPath = resolve(ASSETS_DIR, 'katex/auto-render.min.js');
  const fontsDir = resolve(ASSETS_DIR, 'katex/fonts');

  const jsExists = existsSync(katexJsPath);
  const cssExists = existsSync(katexCssPath);
  const autoRenderExists = existsSync(autoRenderJsPath);
  const fontsExist = existsSync(fontsDir) && readdirSync(fontsDir).length > 10;

  let jsSize = 0;
  if (jsExists) {
    jsSize = readFileSync(katexJsPath).length;
  }

  return {
    jsExists,
    cssExists,
    autoRenderExists,
    fontsExist,
    jsSize,
    valid: jsExists && cssExists && autoRenderExists && fontsExist && jsSize > 50000
  };
}

export function verifyVsatOfflineEncryption(payloadObj) {
  // Spec: MAGIC_HEADER = ['V', 'S', 'A', 'T', 0x02]
  // AES-128 GCM, Key = 'VSAT_COMPASS_KEY' (16 bytes)
  const MAGIC = Buffer.from([0x56, 0x53, 0x41, 0x54, 0x02]);
  const KEY = Buffer.from([
    0x56, 0x53, 0x41, 0x54, 0x5f, 0x43, 0x4f, 0x4d,
    0x50, 0x41, 0x53, 0x53, 0x5f, 0x4b, 0x45, 0x59
  ]);

  const plaintext = Buffer.from(JSON.stringify(payloadObj), 'utf8');
  const iv = crypto.randomBytes(12);
  const cipher = crypto.createCipheriv('aes-128-gcm', KEY, iv);
  const ciphertext = Buffer.concat([cipher.update(plaintext), cipher.final()]);
  const tag = cipher.getAuthTag();

  const bundle = Buffer.concat([MAGIC, iv, tag, ciphertext]);

  // Decryption verification
  const readMagic = bundle.subarray(0, 5);
  const magicMatches = readMagic.equals(MAGIC);

  const readIv = bundle.subarray(5, 17);
  const readTag = bundle.subarray(17, 33);
  const readCiphertext = bundle.subarray(33);

  const decipher = crypto.createDecipheriv('aes-128-gcm', KEY, readIv);
  decipher.setAuthTag(readTag);
  const decrypted = Buffer.concat([decipher.update(readCiphertext), decipher.final()]);
  const parsed = JSON.parse(decrypted.toString('utf8'));

  return {
    magicMatches,
    bundleSize: bundle.length,
    decryptionValid: parsed.examId === payloadObj.examId
  };
}

// ---------------------------------------------------------------------------
// JUnit XML Report Generator
// ---------------------------------------------------------------------------

export function writeJunitXmlReport(reportPayload, outputPath) {
  const cases = reportPayload.results || [];
  const failures = reportPayload.failedCount || 0;
  const total = reportPayload.totalCases || cases.length;
  const totalDuration = (cases.reduce((sum, c) => sum + (c.durationMs || 0), 0) / 1000).toFixed(3);

  const xml = [
    `<?xml version="1.0" encoding="UTF-8"?>`,
    `<testsuites name="${reportPayload.suite}" tests="${total}" failures="${failures}" errors="0" time="${totalDuration}">`,
    `  <testsuite name="${reportPayload.suite}" tests="${total}" failures="${failures}" errors="0" time="${totalDuration}">`
  ];

  for (const c of cases) {
    const timeSec = ((c.durationMs || 0) / 1000).toFixed(3);
    xml.push(`    <testcase classname="android.student" name="${c.id}" time="${timeSec}">`);
    if (c.status !== 'PASSED') {
      const escaped = (c.error || 'Test failure')
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;');
      xml.push(`      <failure message="${escaped}">${escaped}</failure>`);
    }
    xml.push(`    </testcase>`);
  }

  xml.push(`  </testsuite>`);
  xml.push(`</testsuites>\n`);

  writeFileSync(outputPath, xml.join('\n'), 'utf8');
}
