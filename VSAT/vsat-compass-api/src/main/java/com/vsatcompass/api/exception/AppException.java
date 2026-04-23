package com.vsatcompass.api.exception;

import org.springframework.http.HttpStatus;

public class AppException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public AppException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() { return status; }
    public String getCode() { return code; }

    // ---- Generic factory methods ----

    public static AppException badRequest(String message) {
        return new AppException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message);
    }

    public static AppException unauthorized(String message) {
        return new AppException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", message);
    }

    public static AppException forbidden(String message) {
        return new AppException(HttpStatus.FORBIDDEN, "FORBIDDEN", message);
    }

    public static AppException notFound(String entity, Object id) {
        return new AppException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND",
                entity + " không tồn tại với id: " + id);
    }

    public static AppException conflict(String message) {
        return new AppException(HttpStatus.CONFLICT, "DUPLICATE", message);
    }

    // ---- Auth-specific error codes ----

    public static AppException authEmailTaken() {
        return new AppException(HttpStatus.CONFLICT, "AUTH_EMAIL_TAKEN",
                "Email đã được sử dụng");
    }

    public static AppException authInvalidCredentials() {
        return new AppException(HttpStatus.UNAUTHORIZED, "AUTH_INVALID_CREDENTIALS",
                "Email hoặc mật khẩu không đúng");
    }

    public static AppException authUnauthorized() {
        return new AppException(HttpStatus.UNAUTHORIZED, "AUTH_UNAUTHORIZED",
                "Vui lòng đăng nhập");
    }

    public static AppException authRefreshInvalid() {
        return new AppException(HttpStatus.UNAUTHORIZED, "AUTH_REFRESH_INVALID",
                "Refresh token không hợp lệ hoặc đã hết hạn");
    }

    public static AppException authForbidden() {
        return new AppException(HttpStatus.FORBIDDEN, "AUTH_FORBIDDEN",
                "Bạn không có quyền thực hiện hành động này");
    }

    // ---- Session-specific error codes ----

    public static AppException sessionAlreadySubmitted() {
        return new AppException(HttpStatus.CONFLICT, "SESSION_ALREADY_SUBMITTED",
                "Phiên thi đã được nộp trước đó");
    }

    public static AppException sessionForbidden() {
        return new AppException(HttpStatus.FORBIDDEN, "SESSION_FORBIDDEN",
                "Bạn không có quyền truy cập phiên thi này");
    }

    // ---- Validation error code ----

    public static AppException validationFailed(String message) {
        return new AppException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", message);
    }

    // ---- Rate limiting ----

    public static AppException rateLimitExceeded() {
        return new AppException(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED",
                "Bạn đã gửi quá nhiều yêu cầu. Vui lòng thử lại sau.");
    }
}
