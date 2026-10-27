# Prompt cho Cursor — Chẩn đoán & fix lỗi AI giải thích (F4)

> Copy toàn bộ phần dưới dán vào Cursor.

---

## NGỮ CẢNH

Repo **V-SAT Compass**, backend `VSAT/vsat-compass-api/`. Tính năng F4 "Giải thích thêm" (`POST /ai/explain`) đang **lỗi khi gọi**, nhưng app chỉ hiện lỗi chung chung (nghi ngờ 500), không rõ nguyên nhân.

**Các file F4 liên quan:**
- `controller/student/AiController.java` — `POST /ai/explain`, role STUDENT
- `service/AiExplanationService.java` + `service/impl/AiExplanationServiceImpl.java` — dựng prompt, gọi LLM, cache, rate limit
- `service/llm/LlmClient.java` + `service/llm/OpenAiCompatibleLlmClient.java` — gọi router OpenAI-compatible
- `dto/request/AiExplainRequest.java`, `dto/response/AiExplainResponse.java`
- `exception/GlobalExceptionHandler.java`, `exception/AppException.java`

**Config (file local đã .gitignore `VSAT/vsat-compass-api/.env.local`, KHÔNG commit, KHÔNG đổi giá trị, KHÔNG in key ra log):**
- `LLM_BASE_URL=http://localhost:20128/v1`
- `LLM_MODEL=gemini/gemini-2.5-flash-lite`
- `LLM_API_KEY=...` (router OpenAI-compatible chạy local cổng 20128)

## LỖ HỔNG ĐÃ XÁC ĐỊNH

Trong `AiExplanationServiceImpl` (chỗ gọi `llmClient.complete(...)`) **không có try/catch**. Trong `OpenAiCompatibleLlmClient.complete()`, `RestClient...retrieve().body()` sẽ **ném exception khi router trả non-2xx** (sai model, sai key, router không chạy, timeout). Exception đó **không phải `AppException`** → `GlobalExceptionHandler` không map → rơi vào fallback → app nhận **500 trống**, giấu mất nguyên nhân thật.

## YÊU CẦU

### Bước 1 — Chẩn đoán (làm trước, ghi lại kết quả)
1. Xác nhận router sống và model hợp lệ bằng cách gọi trực tiếp (KHÔNG in key ra report, thay bằng `$LLM_API_KEY`):
   ```bash
   curl -i http://localhost:20128/v1/chat/completions \
     -H "Authorization: Bearer $LLM_API_KEY" -H "Content-Type: application/json" \
     -d '{"model":"gemini/gemini-2.5-flash-lite","messages":[{"role":"user","content":"ping"}]}'
   ```
   - Nếu **connection refused** → router chưa chạy / sai cổng.
   - Nếu **400/404** kèm body báo model → chuỗi model sai; lấy danh sách model hợp lệ qua `curl http://localhost:20128/v1/models -H "Authorization: Bearer $LLM_API_KEY"`.
   - Nếu **401** → key sai/hết hạn.
   - Nếu **200** → router OK, lỗi nằm ở phía backend (URL/parse/headers).
2. Boot backend dev (nạp `.env.local`), gọi thật `POST /api/v1/ai/explain` bằng token STUDENT với 1 `questionId` có thật, **đọc nguyên văn dòng log/exception** ở console/`bootrun.log`. Dán nguyên văn vào report.

### Bước 2 — Fix robustness (làm bất kể nguyên nhân)
Trong `OpenAiCompatibleLlmClient` (hoặc service):
1. Bọc lời gọi RestClient bằng try/catch:
   - Bắt `RestClientResponseException` (non-2xx) → **log `WARN` nguyên văn HTTP status + response body từ router** (KHÔNG log key/Authorization header) → ném `AppException.serviceUnavailable("AI provider lỗi: <status>")`.
   - Bắt `ResourceAccessException` (connection refused/timeout) → log rõ → `AppException.serviceUnavailable("Không kết nối được AI provider")`.
   - Các lỗi khác → log + `serviceUnavailable` message rõ ràng.
2. Đặt connect/read timeout hợp lý cho RestClient (vd 5s/30s) để không treo request.
3. Đảm bảo các `AppException.serviceUnavailable(...)` này được `GlobalExceptionHandler` trả về JSON envelope chuẩn với HTTP 503 + message rõ (không còn 500 trống).
4. (Tùy chẩn đoán) nếu model string sai: KHÔNG hard-code id mới vào source — chỉ cập nhật `LLM_MODEL` trong `.env.local` (đã ignore) cho đúng id router chấp nhận, và ghi lại id đúng trong report.

### Bước 3 — Verify
- Gọi lại `POST /ai/explain`:
  - Khi config đúng + router sống → trả giải thích thật (200).
  - Khi tắt router (mô phỏng lỗi) → app/endpoint nhận **503 + message rõ ràng**, log có status/body thật, **không 500 trống, không crash**.
- `./gradlew test` còn xanh (nếu có test cho LLM client, dùng mock — không gọi mạng thật trong test).

## RÀNG BUỘC
- KHÔNG commit / tag / deploy. Chỉ sửa + để onii-chan review.
- KHÔNG in API key / Authorization header ra log hay report; che bằng `***`.
- KHÔNG commit `.env.local`, KHÔNG ghi key vào source/`application.yml`/file tracked.
- KHÔNG revert/commit file dirty cũ; nếu commit sau này: **không co-author**.
- Không đổi `ddl-auto`. Không đổi hành vi feature khác.

## BÁO CÁO KHI XONG
- Kết quả Bước 1 (curl status + body, log backend nguyên văn) → **nguyên nhân thật**.
- File đã sửa + cách dịch lỗi.
- Kết quả verify (200 khi OK, 503 message rõ khi lỗi).
- Nếu phải đổi `LLM_MODEL`: ghi id đúng (chỉ trong `.env.local`).
