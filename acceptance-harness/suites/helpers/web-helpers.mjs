import { spawn } from 'node:child_process';
import { existsSync, mkdtempSync, rmSync } from 'node:fs';
import { resolve, join } from 'node:path';
import { tmpdir } from 'node:os';
import { crc32 } from 'node:zlib';

export function findBrowserExecutable() {
  const candidates = [
    process.env.CHROME_BIN,
    process.env.EDGE_BIN,
    'C:/Program Files/Google/Chrome/Application/chrome.exe',
    'C:/Program Files (x86)/Microsoft/Edge/Application/msedge.exe',
    '/usr/bin/google-chrome',
    '/usr/bin/chromium-browser',
    '/usr/bin/chromium'
  ].filter(Boolean);

  for (const path of candidates) {
    if (existsSync(path)) return path;
  }
  throw new Error('No compatible headless browser (Chrome/Edge/Chromium) found on host system');
}

export async function ensureViteServer(port = 15173) {
  const probeUrl = `http://127.0.0.1:${port}/`;
  try {
    const res = await fetch(probeUrl, { method: 'GET', signal: AbortSignal.timeout(1000) });
    if (res.ok || res.status === 304 || res.status === 404) {
      return { url: probeUrl, stop: () => {} };
    }
  } catch {
    // server not running yet
  }

  const webAdminDir = resolve(import.meta.dirname, '../../../web-admin');
  const isWindows = process.platform === 'win32';
  const npmCmd = isWindows ? 'npx.cmd' : 'npx';

  const proc = spawn(
    npmCmd,
    ['vite', '--host', '127.0.0.1', '--port', String(port), '--strictPort'],
    {
      cwd: webAdminDir,
      env: {
        ...process.env,
        VITE_API_URL: 'http://127.0.0.1:18080/api/v1',
        VITE_API_BASE_URL: 'http://127.0.0.1:18080/api/v1'
      },
      stdio: 'pipe',
      shell: isWindows
    }
  );

  const startMs = Date.now();
  while (Date.now() - startMs < 20000) {
    try {
      const res = await fetch(probeUrl, { method: 'GET', signal: AbortSignal.timeout(1000) });
      if (res.ok || res.status === 304 || res.status === 404) {
        return {
          url: probeUrl,
          stop: () => {
            try { proc.kill('SIGTERM'); } catch {}
          }
        };
      }
    } catch {
      await new Promise(r => setTimeout(r, 400));
    }
  }

  try { proc.kill('SIGKILL'); } catch {}
  throw new Error(`Vite server failed to start on port ${port} within 20s`);
}

export class CdpPage {
  constructor(browserProc, wsUrl, userDataDir) {
    this.browserProc = browserProc;
    this.wsUrl = wsUrl;
    this.userDataDir = userDataDir;
    this.ws = null;
    this.messageId = 1;
    this.pendingResolvers = new Map();
  }

  async connect() {
    return new Promise((res, rej) => {
      this.ws = new WebSocket(this.wsUrl);
      this.ws.onopen = async () => {
        try {
          await this.send('Page.enable');
          await this.send('Runtime.enable');
          await this.send('DOM.enable');
          res();
        } catch (e) {
          rej(e);
        }
      };
      this.ws.onerror = err => rej(err);
      this.ws.onmessage = event => {
        try {
          const msg = JSON.parse(event.data);
          if (msg.method === 'Page.javascriptDialogOpening') {
            this.send('Page.handleJavaScriptDialog', { accept: true }).catch(() => {});
          }
          if (msg.id && this.pendingResolvers.has(msg.id)) {
            const { resolve: rResolve, reject: rReject } = this.pendingResolvers.get(msg.id);
            this.pendingResolvers.delete(msg.id);
            if (msg.error) rReject(new Error(msg.error.message));
            else rResolve(msg.result);
          }
        } catch {}
      };
    });
  }

  async send(method, params = {}) {
    const id = this.messageId++;
    return new Promise((resolve, reject) => {
      this.pendingResolvers.set(id, { resolve, reject });
      this.ws.send(JSON.stringify({ id, method, params }));
    });
  }

  async navigate(url) {
    await this.send('Page.navigate', { url });
    await new Promise(r => setTimeout(r, 800));
  }

  async evaluate(expression) {
    const res = await this.send('Runtime.evaluate', {
      expression,
      returnByValue: true,
      awaitPromise: true
    });
    return res?.result?.value;
  }

  async waitForSelector(selector, timeoutMs = 8000) {
    const start = Date.now();
    while (Date.now() - start < timeoutMs) {
      try {
        const exists = await this.evaluate(`Boolean(document.querySelector(${JSON.stringify(selector)}))`);
        if (exists) return true;
      } catch {}
      await new Promise(r => setTimeout(r, 200));
    }
    throw new Error(`Timeout waiting for selector: ${selector}`);
  }

  async click(selector) {
    await this.waitForSelector(selector);
    const success = await this.evaluate(`(() => {
      const el = document.querySelector(${JSON.stringify(selector)});
      if (!el) return false;
      el.click();
      return true;
    })()`);
    if (!success) throw new Error(`Failed to click element: ${selector}`);
    await new Promise(r => setTimeout(r, 300));
  }

  async typeInput(selector, value) {
    await this.waitForSelector(selector);
    await this.evaluate(`(() => {
      const el = document.querySelector(${JSON.stringify(selector)});
      if (!el) return;
      el.value = ${JSON.stringify(value)};
      el.dispatchEvent(new Event('input', { bubbles: true }));
      el.dispatchEvent(new Event('change', { bubbles: true }));
    })()`);
    await new Promise(r => setTimeout(r, 200));
  }

  async selectOption(selector, value) {
    await this.waitForSelector(selector);
    await this.evaluate(`(() => {
      const el = document.querySelector(${JSON.stringify(selector)});
      if (!el) return;
      el.value = ${JSON.stringify(value)};
      el.dispatchEvent(new Event('change', { bubbles: true }));
    })()`);
    await new Promise(r => setTimeout(r, 200));
  }

  async getInnerText(selector) {
    await this.waitForSelector(selector);
    return await this.evaluate(`document.querySelector(${JSON.stringify(selector)})?.innerText || ''`);
  }

  async switchTab(tabId) {
    const tabMap = {
      'dashboard': ['dashboard'],
      'exams': ['danh sách đề thi', 'exams'],
      'create-exam': ['tạo đề thi', 'wizard', 'create-exam'],
      'blueprints': ['ma trận', 'blueprints'],
      'schedules': ['ca thi', 'lịch thi', 'schedules'],
      'question-bank': ['ngân hàng câu hỏi', 'question-bank'],
      'review-queue': ['duyệt câu hỏi', 'review-queue'],
      'import-questions': ['import', 'import-questions'],
      'settings': ['cấu hình', 'quy chuẩn', 'settings']
    };
    await this.waitForSelector('aside nav button');
    await this.evaluate(`(() => {
      const map = ${JSON.stringify(tabMap)};
      const keys = map[${JSON.stringify(tabId)}] || [${JSON.stringify(tabId)}];
      const buttons = Array.from(document.querySelectorAll('aside nav button'));
      const target = buttons.find(b => {
        const text = b.innerText.toLowerCase();
        return keys.some(k => text.includes(k.toLowerCase()));
      });
      if (target) {
        target.click();
      }
    })()`);
    await new Promise(r => setTimeout(r, 400));
  }

  async close() {
    try {
      if (this.ws) this.ws.close();
    } catch {}
    try {
      if (this.browserProc) this.browserProc.kill();
    } catch {}
    try {
      if (this.userDataDir && existsSync(this.userDataDir)) {
        rmSync(this.userDataDir, { recursive: true, force: true });
      }
    } catch {}
  }
}

export async function launchBrowser(options = {}) {
  const browserBin = findBrowserExecutable();
  const port = options.port || (19220 + Math.floor(Math.random() * 500));
  const userDataDir = mkdtempSync(join(tmpdir(), 'vsat-web-admin-'));

  const proc = spawn(browserBin, [
    '--headless=new',
    `--remote-debugging-port=${port}`,
    '--disable-gpu',
    '--no-sandbox',
    '--disable-dev-shm-usage',
    '--disable-extensions',
    `--user-data-dir=${userDataDir}`,
    'about:blank'
  ]);

  const startMs = Date.now();
  let wsUrl = null;
  while (Date.now() - startMs < 10000) {
    try {
      const res = await fetch(`http://127.0.0.1:${port}/json/list`);
      const pages = await res.json();
      const target = Array.isArray(pages)
        ? (pages.find(p => p.type === 'page' && p.webSocketDebuggerUrl) || pages.find(p => p.webSocketDebuggerUrl))
        : null;
      if (target?.webSocketDebuggerUrl) {
        wsUrl = target.webSocketDebuggerUrl;
        break;
      }
    } catch {
      await new Promise(r => setTimeout(r, 250));
    }
  }

  if (!wsUrl) {
    try { proc.kill(); } catch {}
    throw new Error(`Failed to acquire CDP WebSocket URL from browser port ${port}`);
  }

  const page = new CdpPage(proc, wsUrl, userDataDir);
  await page.connect();
  return page;
}

export function generateMinimalXlsx(rows = []) {
  const defaultRows = rows.length > 0 ? rows : [
    { code: 'MATH_AUTO_001', type: 'SINGLE_CHOICE', text: 'Tích phân xác định của hàm số $f(x)$', answer: 'A' },
    { code: 'MATH_AUTO_002', type: 'TRUE_FALSE', text: 'Xét tính đúng sai mệnh đề hàm số', answer: 'T,T,F,T' }
  ];

  let sheetDataXml = `<row r="1">
    <c r="A1" t="inlineStr"><is><t>Mã câu hỏi</t></is></c>
    <c r="B1" t="inlineStr"><is><t>Dạng thức</t></is></c>
    <c r="C1" t="inlineStr"><is><t>Nội dung câu hỏi</t></is></c>
    <c r="D1" t="inlineStr"><is><t>Đáp án</t></is></c>
  </row>`;

  defaultRows.forEach((r, idx) => {
    const rowIdx = idx + 2;
    sheetDataXml += `<row r="${rowIdx}">
      <c r="A${rowIdx}" t="inlineStr"><is><t>${r.code}</t></is></c>
      <c r="B${rowIdx}" t="inlineStr"><is><t>${r.type}</t></is></c>
      <c r="C${rowIdx}" t="inlineStr"><is><t>${r.text}</t></is></c>
      <c r="D${rowIdx}" t="inlineStr"><is><t>${r.answer}</t></is></c>
    </row>`;
  });

  const files = [
    {
      name: '[Content_Types].xml',
      content: '<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/><Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/></Types>'
    },
    {
      name: '_rels/.rels',
      content: '<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/></Relationships>'
    },
    {
      name: 'xl/workbook.xml',
      content: '<?xml version="1.0" encoding="UTF-8" standalone="yes"?><workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"><sheets><sheet name="Sheet1" sheetId="1" r:id="rId1"/></sheets></workbook>'
    },
    {
      name: 'xl/_rels/workbook.xml.rels',
      content: '<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/></Relationships>'
    },
    {
      name: 'xl/worksheets/sheet1.xml',
      content: `<?xml version="1.0" encoding="UTF-8" standalone="yes"?><worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><sheetData>${sheetDataXml}</sheetData></worksheet>`
    }
  ];

  const localHeaders = [];
  const centralHeaders = [];
  let offset = 0;

  for (const file of files) {
    const data = Buffer.from(file.content, 'utf8');
    const name = Buffer.from(file.name, 'utf8');
    const c = crc32(data);
    const size = data.length;

    const lh = Buffer.alloc(30 + name.length);
    lh.writeUInt32LE(0x04034b50, 0);
    lh.writeUInt16LE(20, 4);
    lh.writeUInt16LE(0, 6);
    lh.writeUInt16LE(0, 8);
    lh.writeUInt16LE(0, 10);
    lh.writeUInt16LE(0, 12);
    lh.writeUInt32LE(c, 14);
    lh.writeUInt32LE(size, 18);
    lh.writeUInt32LE(size, 22);
    lh.writeUInt16LE(name.length, 26);
    lh.writeUInt16LE(0, 28);
    name.copy(lh, 30);
    localHeaders.push(lh, data);

    const ch = Buffer.alloc(46 + name.length);
    ch.writeUInt32LE(0x02014b50, 0);
    ch.writeUInt16LE(20, 4);
    ch.writeUInt16LE(20, 6);
    ch.writeUInt16LE(0, 8);
    ch.writeUInt16LE(0, 10);
    ch.writeUInt16LE(0, 12);
    ch.writeUInt16LE(0, 14);
    ch.writeUInt32LE(c, 16);
    ch.writeUInt32LE(size, 20);
    ch.writeUInt32LE(size, 24);
    ch.writeUInt16LE(name.length, 28);
    ch.writeUInt16LE(0, 30);
    ch.writeUInt16LE(0, 32);
    ch.writeUInt16LE(0, 34);
    ch.writeUInt16LE(0, 36);
    ch.writeUInt32LE(0, 38);
    ch.writeUInt32LE(offset, 42);
    name.copy(ch, 46);
    centralHeaders.push(ch);

    offset += lh.length + data.length;
  }

  const centralDirOffset = offset;
  let centralDirSize = 0;
  for (const ch of centralHeaders) centralDirSize += ch.length;

  const eocd = Buffer.alloc(22);
  eocd.writeUInt32LE(0x06054b50, 0);
  eocd.writeUInt16LE(0, 4);
  eocd.writeUInt16LE(0, 6);
  eocd.writeUInt16LE(files.length, 8);
  eocd.writeUInt16LE(files.length, 10);
  eocd.writeUInt32LE(centralDirSize, 12);
  eocd.writeUInt32LE(centralDirOffset, 16);
  eocd.writeUInt16LE(0, 20);

  return Buffer.concat([...localHeaders, ...centralHeaders, eocd]);
}
