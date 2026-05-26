package com.vsatcompass.api.controller.admin;

import com.vsatcompass.api.dto.common.ApiResponse;
import com.vsatcompass.api.dto.request.UpdateUserRoleRequest;
import com.vsatcompass.api.dto.response.UserSummaryResponse;
import com.vsatcompass.api.entity.enums.UserRole;
import com.vsatcompass.api.entity.enums.UserStatus;
import com.vsatcompass.api.service.AdminUserService;
import com.vsatcompass.api.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
@Tag(name = "Admin-Users", description = "Quản lý người dùng (Super Admin)")
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "List users with optional role/status/keyword filters")
    public ResponseEntity<ApiResponse<Page<UserSummaryResponse>>> list(
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 50) Pageable pageable
    ) {
        Page<UserSummaryResponse> result = adminUserService.listUsers(role, status, keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Update user role")
    public ResponseEntity<ApiResponse<UserSummaryResponse>> updateRole(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRoleRequest request
    ) {
        Long actorId = SecurityUtils.getCurrentUserId();
        UserSummaryResponse updated = adminUserService.updateRole(id, request, actorId);
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    @PatchMapping("/{id}/lock")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Lock user account")
    public ResponseEntity<ApiResponse<Void>> lock(@PathVariable Long id) {
        adminUserService.lockUser(id, SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(null, "Đã khoá tài khoản"));
    }

    @PatchMapping("/{id}/unlock")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "Unlock user account")
    public ResponseEntity<ApiResponse<Void>> unlock(@PathVariable Long id) {
        adminUserService.unlockUser(id, SecurityUtils.getCurrentUserId());
        return ResponseEntity.ok(ApiResponse.success(null, "Đã mở khoá tài khoản"));
    }
}
