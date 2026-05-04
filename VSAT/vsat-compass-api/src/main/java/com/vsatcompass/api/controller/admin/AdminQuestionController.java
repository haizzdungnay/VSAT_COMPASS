package com.vsatcompass.api.controller.admin;

import com.vsatcompass.api.dto.common.ApiResponse;
import com.vsatcompass.api.dto.request.ReviewActionRequest;
import com.vsatcompass.api.dto.response.QuestionListItemResponse;
import com.vsatcompass.api.dto.response.QuestionResponse;
import com.vsatcompass.api.entity.enums.QuestionStatus;
import com.vsatcompass.api.service.QuestionService;
import com.vsatcompass.api.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/questions")
@RequiredArgsConstructor
@Tag(name = "Admin-Questions", description = "Duyệt, từ chối, yêu cầu sửa câu hỏi (admin)")
public class AdminQuestionController {

    private final QuestionService questionService;

    @GetMapping
    @PreAuthorize("hasAnyRole('CONTENT_ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Q-ADM-01: Hàng đợi duyệt (filter by status)")
    public ResponseEntity<ApiResponse<Page<QuestionListItemResponse>>> queue(
            @RequestParam(defaultValue = "PENDING_REVIEW") QuestionStatus status,
            Pageable pageable) {
        Page<QuestionListItemResponse> result = questionService.listByStatus(status, pageable);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('CONTENT_ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Q-ADM-02: Duyệt (PENDING_REVIEW → APPROVED)")
    public ResponseEntity<ApiResponse<QuestionResponse>> approve(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) ReviewActionRequest req) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        ReviewActionRequest body = req != null ? req : new ReviewActionRequest();
        QuestionResponse result = questionService.approve(currentUserId, id, body);
        return ResponseEntity.ok(ApiResponse.success(result, "Đã duyệt"));
    }

    @PostMapping("/{id}/request-revision")
    @PreAuthorize("hasAnyRole('CONTENT_ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Q-ADM-03: Yêu cầu sửa (PENDING_REVIEW → NEEDS_REVISION). Comment bắt buộc.")
    public ResponseEntity<ApiResponse<QuestionResponse>> requestRevision(
            @PathVariable Long id,
            @Valid @RequestBody ReviewActionRequest req) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        QuestionResponse result = questionService.requestRevision(currentUserId, id, req);
        return ResponseEntity.ok(ApiResponse.success(result, "Đã yêu cầu sửa"));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('CONTENT_ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Q-ADM-04: Từ chối (PENDING_REVIEW → ARCHIVED). Comment bắt buộc.")
    public ResponseEntity<ApiResponse<QuestionResponse>> reject(
            @PathVariable Long id,
            @Valid @RequestBody ReviewActionRequest req) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        QuestionResponse result = questionService.reject(currentUserId, id, req);
        return ResponseEntity.ok(ApiResponse.success(result, "Đã từ chối"));
    }
}
