# V-SAT Compass API — Error Codes

> Phiên bản: v0.8.0 | Cập nhật: 2026-04-23

---

## Response Envelope

Tất cả response từ API đều tuân theo cấu trúc sau:

### Success Response

```json
{
  "data": { ... },
  "message": "Success",
  "error": null,
  "timestamp": "2026-04-23T10:00:00.000+07:00"
}
```

### Error Response

```json
{
  "data": null,
  "message": null,
  "error": {
    "code": "AUTH_INVALID_CREDENTIALS",
    "message": "Email hoặc mật khẩu không đúng",
    "details": null
  },
  "timestamp": "2026-04-23T10:00:00.000+07:00"
}
```

### Validation Error Response

```json
{
  "data": {
    "email": "Email không hợp lệ",
    "password": "Mật khẩu phải có ít nhất 8 ký tự, bao gồm chữ và số"
  },
  "message": null,
  "error": {
    "code": "VALIDATION_FAILED",
    "message": "Dữ liệu không hợp lệ",
    "details": null
  },
  "timestamp": "2026-04-23T10:00:00.000+07:00"
}
```

---

## Error Code Catalog

### Authentication Errors (4xx)

| Code | HTTP Status | Ý nghĩa | Khi nào xảy ra |
|------|------------|----------|-----------------|
| `AUTH_INVALID_CREDENTIALS` | 401 | Sai thông tin đăng nhập | Email không tồn tại hoặc mật khẩu sai |
| `AUTH_UNAUTHORIZED` | 401 | Chưa đăng nhập | Request thiếu hoặc sai Bearer token |
| `AUTH_REFRESH_INVALID` | 401 | Refresh token không hợp lệ | Token đã bị revoke, hết hạn, hoặc không tồn tại |
| `AUTH_EMAIL_TAKEN` | 409 | Email đã được sử dụng | Đăng ký với email đã có trong hệ thống |
| `AUTH_FORBIDDEN` | 403 | Không có quyền | Truy cập endpoint admin khi không có role phù hợp |

### Session Errors

| Code | HTTP Status | Ý nghĩa | Khi nào xảy ra |
|------|------------|----------|-----------------|
| `SESSION_ALREADY_SUBMITTED` | 409 | Phiên thi đã nộp | Gọi client-submit lần thứ 2 trên cùng session (anti-replay) |
| `SESSION_FORBIDDEN` | 403 | Không phải chủ phiên thi | Gọi client-submit với session thuộc user khác |

### Validation Errors

| Code | HTTP Status | Ý nghĩa | Khi nào xảy ra |
|------|------------|----------|-----------------|
| `VALIDATION_FAILED` | 400 | Dữ liệu đầu vào không hợp lệ | Thiếu field bắt buộc, vi phạm ràng buộc (min/max/pattern) |
| `BAD_REQUEST` | 400 | Yêu cầu không hợp lệ | Logic lỗi (ví dụ: mode không hợp lệ, correctCount > totalQuestions) |

### Resource Errors

| Code | HTTP Status | Ý nghĩa | Khi nào xảy ra |
|------|------------|----------|-----------------|
| `RESOURCE_NOT_FOUND` | 404 | Không tìm thấy | Entity không tồn tại với ID đã cho |

### Rate Limiting

| Code | HTTP Status | Ý nghĩa | Khi nào xảy ra |
|------|------------|----------|-----------------|
| `RATE_LIMIT_EXCEEDED` | 429 | Quá nhiều yêu cầu | Vượt giới hạn: login 10/phút, register 5/giờ, refresh 30/phút |

### System Errors

| Code | HTTP Status | Ý nghĩa | Khi nào xảy ra |
|------|------------|----------|-----------------|
| `INTERNAL_ERROR` | 500 | Lỗi hệ thống | Lỗi không xác định — kiểm tra server logs |

---

## Rate Limits

| Endpoint | Giới hạn | Đơn vị |
|----------|----------|--------|
| `POST /auth/login` | 10 requests | per minute per IP |
| `POST /auth/register` | 5 requests | per hour per IP |
| `POST /auth/refresh` | 30 requests | per minute per IP |

Khi vượt giới hạn, API trả về `429 Too Many Requests` với error code `RATE_LIMIT_EXCEEDED`.

---

## Android Client Handling

### Mapping error code → hành vi

```java
switch (errorCode) {
    case "AUTH_INVALID_CREDENTIALS":
        // Hiện thông báo "Sai email hoặc mật khẩu"
        break;
    case "AUTH_REFRESH_INVALID":
        // Clear stored tokens, redirect → LoginActivity
        break;
    case "AUTH_EMAIL_TAKEN":
        // Hiện thông báo "Email đã được sử dụng"
        break;
    case "SESSION_ALREADY_SUBMITTED":
        // Bỏ qua — bài đã nộp rồi, navigate → ResultActivity
        break;
    case "RATE_LIMIT_EXCEEDED":
        // Hiện thông báo "Vui lòng thử lại sau"
        break;
    case "VALIDATION_FAILED":
        // Parse data field → highlight field lỗi
        break;
}
```
