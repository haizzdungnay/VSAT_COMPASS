#!/usr/bin/env node
import http from 'node:http';

const PORT = parseInt(process.env.LLM_MOCK_PORT || '18081', 10);
const HOST = process.env.LLM_MOCK_HOST || '0.0.0.0';

let currentMode = 'normal'; // 'normal' | 'error' | 'timeout'
const requestLog = [];

function jsonResponse(res, statusCode, data) {
  const body = JSON.stringify(data);
  res.writeHead(statusCode, {
    'Content-Type': 'application/json',
    'Content-Length': Buffer.byteLength(body),
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
    'Access-Control-Allow-Headers': 'Content-Type, Authorization, x-mock-mode'
  });
  res.end(body);
}

function parseBody(req) {
  return new Promise((resolve, reject) => {
    let raw = '';
    req.on('data', chunk => {
      raw += chunk;
      if (raw.length > 2 * 1024 * 1024) {
        reject(new Error('Payload too large'));
      }
    });
    req.on('end', () => {
      if (!raw) return resolve({});
      try {
        resolve(JSON.parse(raw));
      } catch (err) {
        resolve({ rawText: raw });
      }
    });
    req.on('error', reject);
  });
}

const server = http.createServer(async (req, res) => {
  const url = new URL(req.url, `http://${HOST}:${PORT}`);
  const path = url.pathname;
  const method = req.method.toUpperCase();

  // CORS preflight
  if (method === 'OPTIONS') {
    res.writeHead(204, {
      'Access-Control-Allow-Origin': '*',
      'Access-Control-Allow-Methods': 'GET, POST, OPTIONS',
      'Access-Control-Allow-Headers': 'Content-Type, Authorization, x-mock-mode'
    });
    return res.end();
  }

  // 1. Health check
  if (method === 'GET' && (path === '/health' || path === '/v1/health')) {
    return jsonResponse(res, 200, {
      status: 'UP',
      service: 'llm-mock-server',
      mode: currentMode,
      timestamp: new Date().toISOString()
    });
  }

  // 2. Control endpoint to inspect or switch behavior
  if (path === '/__control/mode' || path === '/v1/__control/mode') {
    if (method === 'GET') {
      return jsonResponse(res, 200, { mode: currentMode, totalRequests: requestLog.length });
    }
    if (method === 'POST') {
      const body = await parseBody(req);
      if (body.mode && ['normal', 'error', 'timeout'].includes(body.mode)) {
        currentMode = body.mode;
        return jsonResponse(res, 200, { success: true, mode: currentMode });
      }
      return jsonResponse(res, 400, { error: 'Invalid mode. Use "normal", "error", or "timeout"' });
    }
  }

  if (method === 'GET' && (path === '/__control/history' || path === '/v1/__control/history')) {
    return jsonResponse(res, 200, { history: requestLog.slice(-50) });
  }

  // 3. OpenAI Chat Completions endpoint
  if (method === 'POST' && (path === '/v1/chat/completions' || path === '/chat/completions')) {
    const body = await parseBody(req);
    requestLog.push({ timestamp: new Date().toISOString(), path, body });

    const headerMode = req.headers['x-mock-mode'];
    const activeMode = headerMode || (body.simulate_error ? 'error' : currentMode);

    if (activeMode === 'error') {
      return jsonResponse(res, 500, {
        error: {
          message: 'Simulated LLM internal error for fallback test',
          type: 'server_error',
          code: 'llm_mock_error'
        }
      });
    }

    if (activeMode === 'timeout') {
      // Hold response for 10 seconds to simulate timeout
      setTimeout(() => {
        if (!res.writableEnded) {
          jsonResponse(res, 504, { error: { message: 'Simulated Gateway Timeout' } });
        }
      }, 10000);
      return;
    }

    // Normal mode: Standard OpenAI response with pedagogical explanation containing LaTeX
    const promptPreview = JSON.stringify(body.messages || []).slice(0, 100);
    const content = `Lời giải sư phạm chi tiết:
1. Nhắc lại kiến thức cơ bản: Hằng đẳng thức đáng nhớ $(a + b)^2 = a^2 + 2ab + b^2$.
2. Phân tích bài toán: Với phương trình bậc hai $x^2 + 2x + 1 = 0$, ta nhận thấy vế trái có dạng $(x + 1)^2$.
3. Biến đổi tương đương:
   $$(x + 1)^2 = 0 \\iff x + 1 = 0 \\iff x = -1$$
4. Kết luận: Phương trình có nghiệm kép duy nhất là $x = -1$.
(Prompt preview: ${promptPreview})`;

    return jsonResponse(res, 200, {
      id: `chatcmpl-mock-${Date.now()}`,
      object: 'chat.completion',
      created: Math.floor(Date.now() / 1000),
      model: body.model || 'qwen2.5-math-7b',
      choices: [
        {
          index: 0,
          message: {
            role: 'assistant',
            content: content
          },
          finish_reason: 'stop'
        }
      ],
      usage: {
        prompt_tokens: 45,
        completion_tokens: 95,
        total_tokens: 140
      }
    });
  }

  return jsonResponse(res, 404, { error: `Not found: ${method} ${path}` });
});

export function startLlmMock(port = PORT) {
  return new Promise((resolve, reject) => {
    server.listen(port, HOST, () => {
      console.log(`[LLM MOCK] Server listening on http://${HOST}:${port}`);
      resolve(server);
    });
    server.on('error', reject);
  });
}

if (process.argv[1] && process.argv[1].endsWith('llm-mock-server.mjs')) {
  startLlmMock().catch(err => {
    console.error(`[LLM MOCK ERROR] ${err.message}`);
    process.exit(1);
  });
}
