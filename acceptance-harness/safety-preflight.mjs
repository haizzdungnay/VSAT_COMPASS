#!/usr/bin/env node
import { URL } from 'node:url';
import net from 'node:net';

const ALLOWED_HOSTS = new Set(['127.0.0.1', 'localhost']);
const FORBIDDEN_SUBSTRINGS = ['onrender.com', 'supabase', 'neon.tech', 'host.docker.internal'];

function parseSafeUrl(rawUrl) {
  if (!rawUrl || typeof rawUrl !== 'string') return null;
  let normalized = rawUrl.trim();
  if (normalized.startsWith('jdbc:')) {
    normalized = normalized.slice(5);
  }
  try {
    return new URL(normalized);
  } catch (err) {
    throw new Error(`Invalid URL format: "${rawUrl}" (${err.message})`);
  }
}

function assertSafeHost(parsedUrl, expectedPort, contextName) {
  const hostname = parsedUrl.hostname.toLowerCase();
  if (!ALLOWED_HOSTS.has(hostname)) {
    throw new Error(
      `[SECURITY] ${contextName} host "${hostname}" forbidden! Only 127.0.0.1 or localhost allowed.`
    );
  }
  for (const forbidden of FORBIDDEN_SUBSTRINGS) {
    if (parsedUrl.href.toLowerCase().includes(forbidden)) {
      throw new Error(
        `[SECURITY] ${contextName} references forbidden external domain target "${forbidden}": ${parsedUrl.href}`
      );
    }
  }
  const port = parsedUrl.port ? parseInt(parsedUrl.port, 10) : null;
  if (expectedPort !== null && port !== expectedPort) {
    throw new Error(
      `[SECURITY] ${contextName} port ${port} mismatch! Expected port ${expectedPort}.`
    );
  }
}

function validateDatabaseUrl(rawUrl, varName) {
  if (!rawUrl) return;
  const parsed = parseSafeUrl(rawUrl);
  assertSafeHost(parsed, 55432, varName);

  const cleanPath = parsed.pathname.replace(/\/+$/, '');
  if (cleanPath !== '/vsat_compass_acceptance') {
    throw new Error(
      `[SECURITY] ${varName} database path "${parsed.pathname}" forbidden! Only "/vsat_compass_acceptance" allowed (must NOT touch dev DB /vsat_compass).`
    );
  }
}

function validateServiceUrl(rawUrl, expectedPort, varName) {
  if (!rawUrl) return;
  const parsed = parseSafeUrl(rawUrl);
  assertSafeHost(parsed, expectedPort, varName);
}

function validateApkPath(apkPath) {
  if (!apkPath) return;
  const lower = apkPath.toLowerCase();
  if (lower.includes('release') || lower.endsWith('-release.apk')) {
    throw new Error(
      `[SECURITY] ANDROID_APK_PATH "${apkPath}" points to release APK (points to production onrender.com). Only debug APK is permitted!`
    );
  }
  if (!lower.includes('debug') || !lower.endsWith('.apk')) {
    throw new Error(
      `[SECURITY] ANDROID_APK_PATH "${apkPath}" must be a valid debug APK ending with .apk (e.g. app-debug.apk).`
    );
  }
}

function scanGitPushProtection() {
  const allArgs = process.argv.slice(2).join(' ').toLowerCase();
  if (allArgs.includes('git push') || allArgs.includes('push origin')) {
    throw new Error('[SECURITY] git push attempt detected in command line arguments! Operation blocked.');
  }
  for (const [key, value] of Object.entries(process.env)) {
    if (typeof value === 'string') {
      const lowerVal = value.toLowerCase();
      if (lowerVal.includes('git push') || lowerVal.includes('push origin')) {
        throw new Error(`[SECURITY] git push command pattern detected in env var ${key}! Operation blocked.`);
      }
    }
  }
}

function probePort(port, host = '127.0.0.1', timeoutMs = 1000) {
  return new Promise((resolve) => {
    const socket = new net.Socket();
    let responded = false;

    socket.setTimeout(timeoutMs);
    socket.once('connect', () => {
      responded = true;
      socket.destroy();
      resolve(true);
    });
    socket.once('timeout', () => {
      responded = true;
      socket.destroy();
      resolve(false);
    });
    socket.once('error', () => {
      responded = true;
      socket.destroy();
      resolve(false);
    });
    socket.connect(port, host);
  });
}

async function checkPorts() {
  const ports = [
    { name: 'Clean PostgreSQL', port: 55432 },
    { name: 'Clean Backend API', port: 18080 },
    { name: 'Clean Mock LLM', port: 18081 }
  ];

  console.log('[PREFLIGHT] Probing acceptance infrastructure ports...');
  for (const p of ports) {
    const open = await probePort(p.port);
    console.log(` - Port ${p.port} (${p.name}): ${open ? 'ACTIVE' : 'INACTIVE/IDLE'}`);
  }
}

export async function runSafetyPreflight(options = {}) {
  try {
    scanGitPushProtection();

    const dbUrls = [
      { name: 'DATABASE_URL', val: process.env.DATABASE_URL },
      { name: 'SPRING_DATASOURCE_URL', val: process.env.SPRING_DATASOURCE_URL }
    ];
    for (const item of dbUrls) {
      if (item.val) {
        validateDatabaseUrl(item.val, item.name);
      }
    }

    const apiUrls = [
      { name: 'API_BASE_URL', val: process.env.API_BASE_URL },
      { name: 'VITE_API_URL', val: process.env.VITE_API_URL },
      { name: 'VITE_API_BASE_URL', val: process.env.VITE_API_BASE_URL }
    ];
    for (const item of apiUrls) {
      if (item.val) {
        validateServiceUrl(item.val, 18080, item.name);
      }
    }

    const llmUrl = process.env.LLM_BASE_URL;
    if (llmUrl) {
      validateServiceUrl(llmUrl, 18081, 'LLM_BASE_URL');
    }

    const apkPath = process.env.ANDROID_APK_PATH;
    if (apkPath) {
      validateApkPath(apkPath);
    }

    if (process.argv.includes('--check-ports') || options.checkPorts) {
      await checkPorts();
    }

    console.log('SAFETY PREFLIGHT PASSED');
    return true;
  } catch (error) {
    console.error('SAFETY PREFLIGHT FAILED:');
    console.error(`  ${error.message}`);
    if (process.env.DEBUG) {
      console.error(error.stack);
    }
    return false;
  }
}

if (process.argv[1] && process.argv[1].endsWith('safety-preflight.mjs')) {
  runSafetyPreflight().then((passed) => {
    process.exit(passed ? 0 : 1);
  });
}
