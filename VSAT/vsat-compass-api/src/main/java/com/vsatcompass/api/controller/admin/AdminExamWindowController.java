package com.vsatcompass.api.controller.admin;

import com.vsatcompass.api.dto.common.ApiResponse;
import com.vsatcompass.api.dto.request.AdminExamWindowRequest;
import com.vsatcompass.api.dto.response.AdminExamWindowResponse;
import com.vsatcompass.api.service.AdminExamWindowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/exam-windows")
@RequiredArgsConstructor
@Tag(name = "Admin-ExamWindows", description = "Admin exam window delivery schedule management")
public class AdminExamWindowController {

    private final AdminExamWindowService adminExamWindowService;

    @GetMapping
    @PreAuthorize("hasAnyRole('CONTENT_ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "List exam windows with optional filters")
    public ResponseEntity<ApiResponse<List<AdminExamWindowResponse>>> listWindows(
            @RequestParam(required = false) Long examId,
            @RequestParam(required = false) String status) {
        List<AdminExamWindowResponse> result = adminExamWindowService.listWindows(examId, status);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CONTENT_ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Get exam window detail by ID")
    public ResponseEntity<ApiResponse<AdminExamWindowResponse>> getWindow(@PathVariable Long id) {
        AdminExamWindowResponse result = adminExamWindowService.getWindow(id);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('CONTENT_ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Create exam window delivery schedule")
    public ResponseEntity<ApiResponse<AdminExamWindowResponse>> createWindow(
            @Valid @RequestBody AdminExamWindowRequest request) {
        AdminExamWindowResponse result = adminExamWindowService.createWindow(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(result, "T\u1ea1o ca thi th\u00e0nh c\u00f4ng"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('CONTENT_ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Update exam window delivery schedule")
    public ResponseEntity<ApiResponse<AdminExamWindowResponse>> updateWindow(
            @PathVariable Long id,
            @Valid @RequestBody AdminExamWindowRequest request) {
        AdminExamWindowResponse result = adminExamWindowService.updateWindow(id, request);
        return ResponseEntity.ok(ApiResponse.success(result, "C\u1eadp nh\u1eadt ca thi th\u00e0nh c\u00f4ng"));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('CONTENT_ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Quick update exam window status (UPCOMING, RUNNING, CLOSED)")
    public ResponseEntity<ApiResponse<AdminExamWindowResponse>> updateStatus(
            @PathVariable Long id,
            @RequestParam String status) {
        AdminExamWindowResponse result = adminExamWindowService.updateStatus(id, status);
        return ResponseEntity.ok(ApiResponse.success(result, "C\u1eadp nh\u1eadt tr\u1ea1ng th\u00e1i ca thi th\u00e0nh c\u00f4ng"));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('CONTENT_ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Delete exam window (only if no sessions exist)")
    public ResponseEntity<ApiResponse<Void>> deleteWindow(@PathVariable Long id) {
        adminExamWindowService.deleteWindow(id);
        return ResponseEntity.ok(ApiResponse.success(null, "X\u00f3a ca thi th\u00e0nh c\u00f4ng"));
    }
}
