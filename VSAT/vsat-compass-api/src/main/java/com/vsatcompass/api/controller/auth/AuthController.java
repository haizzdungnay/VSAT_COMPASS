package com.vsatcompass.api.controller.auth;

import com.vsatcompass.api.dto.common.ApiResponse;
import com.vsatcompass.api.dto.request.AuthRequest;
import com.vsatcompass.api.dto.response.AuthResponse;
import com.vsatcompass.api.service.AuthService;
import com.vsatcompass.api.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Đăng ký, đăng nhập, quản lý token")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "AU-01: Đăng ký tài khoản mới")
    public ResponseEntity<ApiResponse<AuthResponse.TokenPair>> register(
            @Valid @RequestBody AuthRequest.Register request) {
        AuthResponse.TokenPair result = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(result, "Đăng ký thành công"));
    }

    @PostMapping("/login")
    @Operation(summary = "AU-02: Đăng nhập")
    public ResponseEntity<ApiResponse<AuthResponse.TokenPair>> login(
            @Valid @RequestBody AuthRequest.Login request) {
        AuthResponse.TokenPair result = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(result, "Đăng nhập thành công"));
    }

    @PostMapping("/refresh")
    @Operation(summary = "AU-03: Làm mới access token")
    public ResponseEntity<ApiResponse<AuthResponse.TokenPair>> refreshToken(
            @Valid @RequestBody AuthRequest.RefreshToken request) {
        AuthResponse.TokenPair result = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/logout")
    @Operation(summary = "AU-04: Đăng xuất")
    public ResponseEntity<ApiResponse<Void>> logout(
            @Valid @RequestBody AuthRequest.RefreshToken request) {
        authService.logout(request);
        return ResponseEntity.ok(ApiResponse.success(null, "Đăng xuất thành công"));
    }

    @GetMapping("/me")
    @Operation(summary = "AU-07: Lấy thông tin user đang đăng nhập")
    public ResponseEntity<ApiResponse<AuthResponse.UserInfo>> getMe() {
        Long userId = SecurityUtils.getCurrentUserId();
        AuthResponse.UserInfo result = authService.getMe(userId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PutMapping("/me")
    @Operation(summary = "AU-08: Cập nhật hồ sơ cá nhân")
    public ResponseEntity<ApiResponse<AuthResponse.UserInfo>> updateProfile(
            @Valid @RequestBody AuthRequest.UpdateProfile request) {
        Long userId = SecurityUtils.getCurrentUserId();
        AuthResponse.UserInfo result = authService.updateProfile(userId, request);
        return ResponseEntity.ok(ApiResponse.success(result, "Cập nhật hồ sơ thành công"));
    }

    @PutMapping("/me/password")
    @Operation(summary = "AU-09: Đổi mật khẩu")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody AuthRequest.ChangePassword request) {
        Long userId = SecurityUtils.getCurrentUserId();
        authService.changePassword(userId, request);
        return ResponseEntity.ok(ApiResponse.success(null, "Đổi mật khẩu thành công"));
    }
}
