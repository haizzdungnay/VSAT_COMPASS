package com.vsatcompass.api.controller.collaborator;

import com.vsatcompass.api.dto.common.ApiResponse;
import com.vsatcompass.api.dto.request.CreateQuestionRequest;
import com.vsatcompass.api.dto.request.UpdateQuestionRequest;
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
@RequestMapping("/collaborator/questions")
@RequiredArgsConstructor
@Tag(name = "Collaborator-Questions", description = "Tạo, sửa, gửi duyệt câu hỏi (collaborator)")
public class CollaboratorQuestionController {

    private final QuestionService questionService;

    @PostMapping
    @PreAuthorize("hasAnyRole('COLLABORATOR', 'CONTENT_ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Q-COL-01: Tạo câu hỏi mới (DRAFT)")
    public ResponseEntity<ApiResponse<QuestionResponse>> create(
            @Valid @RequestBody CreateQuestionRequest req) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        QuestionResponse result = questionService.create(currentUserId, req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(result, "Tạo câu hỏi thành công"));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('COLLABORATOR', 'CONTENT_ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Q-COL-02: Liệt kê câu hỏi của mình (paged, optional status filter)")
    public ResponseEntity<ApiResponse<Page<QuestionListItemResponse>>> list(
            @RequestParam(required = false) QuestionStatus status,
            Pageable pageable) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        Page<QuestionListItemResponse> result = questionService.listByCreator(currentUserId, status, pageable);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('COLLABORATOR', 'CONTENT_ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Q-COL-03: Chi tiết câu hỏi (own only for COLLABORATOR; any for CONTENT_ADMIN+)")
    public ResponseEntity<ApiResponse<QuestionResponse>> get(@PathVariable Long id) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        String currentUserRole = SecurityUtils.getCurrentUserRole();
        QuestionResponse result = questionService.getById(currentUserId, currentUserRole, id);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('COLLABORATOR', 'CONTENT_ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Q-COL-04: Sửa câu hỏi (DRAFT/NEEDS_REVISION + owner-check)")
    public ResponseEntity<ApiResponse<QuestionResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateQuestionRequest req) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        String currentUserRole = SecurityUtils.getCurrentUserRole();
        QuestionResponse result = questionService.update(currentUserId, currentUserRole, id, req);
        return ResponseEntity.ok(ApiResponse.success(result, "Cập nhật câu hỏi thành công"));
    }

    @PostMapping("/{id}/submit-for-review")
    @PreAuthorize("hasAnyRole('COLLABORATOR', 'CONTENT_ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Q-COL-05: Gửi câu hỏi để duyệt (DRAFT/NEEDS_REVISION → PENDING_REVIEW)")
    public ResponseEntity<ApiResponse<QuestionResponse>> submitForReview(@PathVariable Long id) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        QuestionResponse result = questionService.submitForReview(currentUserId, id);
        return ResponseEntity.ok(ApiResponse.success(result, "Đã gửi duyệt"));
    }
}
