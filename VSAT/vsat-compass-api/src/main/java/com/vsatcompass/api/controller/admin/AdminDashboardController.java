package com.vsatcompass.api.controller.admin;

import com.vsatcompass.api.dto.common.ApiResponse;
import com.vsatcompass.api.dto.response.AdminStatsResponse;
import com.vsatcompass.api.service.AdminDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/stats")
@RequiredArgsConstructor
@Tag(name = "Admin-Dashboard", description = "Thống kê tổng quan quản trị")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    @GetMapping
    @PreAuthorize("hasAnyRole('CONTENT_ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Dashboard stats: pending questions, sessions, users")
    public ResponseEntity<ApiResponse<AdminStatsResponse>> getStats() {
        return ResponseEntity.ok(ApiResponse.success(adminDashboardService.getStats()));
    }
}
