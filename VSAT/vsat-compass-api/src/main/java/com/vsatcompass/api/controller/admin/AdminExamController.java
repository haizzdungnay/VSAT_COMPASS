package com.vsatcompass.api.controller.admin;

import com.vsatcompass.api.dto.common.ApiResponse;
import com.vsatcompass.api.dto.request.AdminExamCreateRequest;
import com.vsatcompass.api.dto.request.AdminExamUpdateRequest;
import com.vsatcompass.api.dto.response.AdminExamResponse;
import com.vsatcompass.api.dto.response.AdminExamSummaryResponse;
import com.vsatcompass.api.entity.enums.ExamStatus;
import com.vsatcompass.api.service.AdminExamService;
import com.vsatcompass.api.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/exams")
@RequiredArgsConstructor
@Tag(name = "Admin-Exams", description = "Admin exam metadata CRUD (Phase C1.2b-1)")
public class AdminExamController {

    private final AdminExamService adminExamService;

    @GetMapping
    @PreAuthorize("hasAnyRole('CONTENT_ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "EX-ADM-01: List exams (admin) — optional status / subjectId filter")
    public ResponseEntity<ApiResponse<Page<AdminExamSummaryResponse>>> list(
            @RequestParam(required = false) ExamStatus status,
            @RequestParam(required = false) Long subjectId,
            Pageable pageable) {
        Page<AdminExamSummaryResponse> result =
                adminExamService.listAdminExams(status, subjectId, pageable);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CONTENT_ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "EX-ADM-02: Get exam metadata (admin)")
    public ResponseEntity<ApiResponse<AdminExamResponse>> get(@PathVariable Long id) {
        AdminExamResponse result = adminExamService.getAdminExam(id);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('CONTENT_ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "EX-ADM-03: Create draft exam (admin)")
    public ResponseEntity<ApiResponse<AdminExamResponse>> create(
            @Valid @RequestBody AdminExamCreateRequest req) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        AdminExamResponse result = adminExamService.createExam(currentUserId, req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(result, "Tạo đề thi thành công"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('CONTENT_ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "EX-ADM-04: Update exam metadata (DRAFT/HIDDEN only)")
    public ResponseEntity<ApiResponse<AdminExamResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody AdminExamUpdateRequest req) {
        AdminExamResponse result = adminExamService.updateExam(id, req);
        return ResponseEntity.ok(ApiResponse.success(result, "Cập nhật đề thi thành công"));
    }
}
