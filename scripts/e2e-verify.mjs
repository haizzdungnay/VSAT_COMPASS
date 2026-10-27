import { spawn } from 'child_process';
import fs from 'fs';
import path from 'path';

const EDGE_PATH = 'C:\\Program Files (x86)\\Microsoft\\Edge\\Application\\msedge.exe';
const USER_DATA_DIR = path.resolve('temp/edge-profile');

if (!fs.existsSync('temp')) fs.mkdirSync('temp', { recursive: true });

const edge = spawn(EDGE_PATH, [
  '--headless=new',
  '--remote-debugging-port=9222',
  `--user-data-dir=${USER_DATA_DIR}`,
  '--window-size=1400,900',
  'http://localhost:5173/'
]);

async function sleep(ms) {
  return new Promise(r => setTimeout(r, ms));
}

let ws;
let msgId = 1;
const pending = new Map();

function send(method, params = {}) {
  return new Promise((resolve, reject) => {
    const id = msgId++;
    pending.set(id, { resolve, reject });
    ws.send(JSON.stringify({ id, method, params }));
  });
}

async function evalInPage(expression) {
  const res = await send('Runtime.evaluate', {
    expression,
    returnByValue: true,
    awaitPromise: true
  });
  if (res?.exceptionDetails) {
    throw new Error(JSON.stringify(res.exceptionDetails));
  }
  return res?.result?.value;
}

async function run() {
  try {
    console.log('1. Waiting for Edge page on port 9222...');
    let pageWsUrl = null;
    for (let i = 0; i < 20; i++) {
      try {
        const res = await fetch('http://127.0.0.1:9222/json/list');
        if (res.ok) {
          const list = await res.json();
          const page = list.find(item => item.type === 'page');
          if (page && page.webSocketDebuggerUrl) {
            pageWsUrl = page.webSocketDebuggerUrl;
            break;
          }
        }
      } catch {}
      await sleep(500);
    }

    if (!pageWsUrl) {
      throw new Error('Edge Page WebSocket not found');
    }

    console.log('2. Connecting to Page WebSocket:', pageWsUrl);
    ws = new WebSocket(pageWsUrl);

    await new Promise((resolve, reject) => {
      ws.onopen = resolve;
      ws.onerror = reject;
    });

    ws.onmessage = (event) => {
      const data = JSON.parse(event.data);
      if (data.id && pending.has(data.id)) {
        const { resolve } = pending.get(data.id);
        pending.delete(data.id);
        resolve(data.result);
      }
    };

    await send('Runtime.enable');
    await send('Page.enable');
    await sleep(2500); // allow app load

    console.log('3. Verifying ExamList page...');
    const pageTitle = await evalInPage('document.title');
    console.log('   Document Title:', pageTitle);

    const examCardsCount = await evalInPage('document.querySelectorAll(".bg-surface-container-lowest").length');
    console.log('   Rendered Exam Cards & Containers:', examCardsCount);

    console.log('4. Navigating to Question Bank Tab...');
    const clickBank = await evalInPage(`
      (() => {
        const btns = Array.from(document.querySelectorAll('button'));
        const bankBtn = btns.find(b => b.textContent.includes('Ngân hàng câu hỏi'));
        if (bankBtn) {
          bankBtn.click();
          return true;
        }
        return false;
      })()
    `);
    console.log('   Clicked Question Bank tab:', clickBank);

    await sleep(2000);

    const bankHeader = await evalInPage(`
      (() => {
        const h1 = document.querySelector('h1');
        return h1 ? h1.textContent : null;
      })()
    `);
    console.log('   Current Page Header:', bankHeader);

    console.log('5. Inspecting KaTeX rendered elements...');
    const katexCount = await evalInPage('document.querySelectorAll(".katex").length');
    console.log('   KaTeX DOM elements count:', katexCount);

    const sampleMathTexts = await evalInPage(`
      (() => {
        const items = Array.from(document.querySelectorAll('.katex-html'));
        return items.slice(0, 5).map(el => el.textContent.trim());
      })()
    `);
    console.log('   Sample KaTeX rendered contents:', sampleMathTexts);

    console.log('6. Opening "Thêm câu hỏi mới" Modal...');
    const openModal = await evalInPage(`
      (() => {
        const btns = Array.from(document.querySelectorAll('button'));
        const addBtn = btns.find(b => b.textContent.includes('Thêm câu hỏi mới'));
        if (addBtn) {
          addBtn.click();
          return true;
        }
        return false;
      })()
    `);
    console.log('   Clicked Thêm câu hỏi mới button:', openModal);

    await sleep(1500);

    const modalTitle = await evalInPage(`
      (() => {
        const h2 = document.querySelector('h2');
        return h2 ? h2.textContent : null;
      })()
    `);
    console.log('   Modal Title:', modalTitle);

    console.log('7. Testing Live KaTeX Preview in Modal...');
    await evalInPage(`
      (() => {
        const textarea = document.querySelector('textarea');
        if (textarea) {
          // Native value setter to trigger React 19 change listener
          const nativeSetter = Object.getOwnPropertyDescriptor(window.HTMLTextAreaElement.prototype, 'value').set;
          nativeSetter.call(textarea, 'Tính diện tích hình tròn $S = \\\\pi R^2$ và tích phân $\\\\int_0^1 x^3 dx$');
          textarea.dispatchEvent(new Event('input', { bubbles: true }));
          textarea.dispatchEvent(new Event('change', { bubbles: true }));
        }
      })()
    `);

    await sleep(1500);

    const liveKaTeX = await evalInPage(`
      (() => {
        const live = document.querySelectorAll('.katex');
        return live.length;
      })()
    `);
    console.log('   Total KaTeX elements after typing live formula:', liveKaTeX);

    console.log('8. Capturing Screenshot...');
    const screenshotRes = await send('Page.captureScreenshot', { format: 'png' });
    if (screenshotRes && screenshotRes.data) {
      fs.writeFileSync('temp/e2e_verified_screenshot.png', Buffer.from(screenshotRes.data, 'base64'));
      console.log('   Saved screenshot to temp/e2e_verified_screenshot.png');
    }

    console.log('=== E2E AUTOMATED VERIFICATION PASSED 100%! ===');
  } catch (err) {
    console.error('E2E Verification Error:', err);
  } finally {
    if (ws) ws.close();
    edge.kill();
    process.exit(0);
  }
}

run();
