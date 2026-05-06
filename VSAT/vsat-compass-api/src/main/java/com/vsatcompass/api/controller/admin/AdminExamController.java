package com.vsatcompass.api.controller.admin;

import com.vsatcompass.api.dto.common.ApiResponse;
import com.vsatcompass.api.dto.request.AdminExamAddQuestionRequest;
import com.vsatcompass.api.dto.request.AdminExamCreateRequest;
import com.vsatcompass.api.dto.request.AdminExamReorderQuestionsRequest;
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
import org.springframework.web.bind.annotation.DeleteMapping;
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
@Tag(name = "Admin-Exams", description = "Admin exam metadata, composition, and workflow")
public class AdminExamController {

    private final AdminExamService adminExamService;

    @GetMapping
    @PreAuthorize("hasAnyRole('CONTENT_ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "EX-ADM-01: List exams (admin) - optional status / subjectId filter")
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
                .body(ApiResponse.success(result, "T\u1ea1o \u0111\u1ec1 thi th\u00e0nh c\u00f4ng"));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('CONTENT_ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "EX-ADM-04: Update exam metadata (DRAFT/HIDDEN only)")
    public ResponseEntity<ApiResponse<AdminExamResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody AdminExamUpdateRequest req) {
        AdminExamResponse result = adminExamService.updateExam(id, req);
        return ResponseEntity.ok(ApiResponse.success(result, "C\u1eadp nh\u1eadt \u0111\u1ec1 thi th\u00e0nh c\u00f4ng"));
    }

    @PostMapping("/{examId}/questions")
    @PreAuthorize("hasAnyRole('CONTENT_ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "EX-ADM-05: Add question to DRAFT exam")
    public ResponseEntity<ApiResponse<AdminExamResponse>> addQuestion(
            @PathVariable Long examId,
            @Valid @RequestBody AdminExamAddQuestionRequest req) {
        AdminExamResponse result = adminExamService.addQuestion(examId, req.getQuestionId());
        return ResponseEntity.ok(ApiResponse.success(result, "Question added to exam"));
    }

    @DeleteMapping("/{examId}/questions/{questionId}")
    @PreAuthorize("hasAnyRole('CONTENT_ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "EX-ADM-06: Remove question from DRAFT exam")
    public ResponseEntity<ApiResponse<AdminExamResponse>> removeQuestion(
            @PathVariable Long examId,
            @PathVariable Long questionId) {
        AdminExamResponse result = adminExamService.removeQuestion(examId, questionId);
        return ResponseEntity.ok(ApiResponse.success(result, "Question removed from exam"));
    }

    @PutMapping("/{examId}/questions/reorder")
    @PreAuthorize("hasAnyRole('CONTENT_ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "EX-ADM-07: Reorder DRAFT exam questions")
    public ResponseEntity<ApiResponse<AdminExamResponse>> reorderQuestions(
            @PathVariable Long examId,
            @Valid @RequestBody AdminExamReorderQuestionsRequest req) {
        AdminExamResponse result = adminExamService.reorderQuestions(examId, req.getQuestionIds());
        return ResponseEntity.ok(ApiResponse.success(result, "Exam questions reordered"));
    }

    @PostMapping("/{examId}/submit-review")
    @PreAuthorize("hasAnyRole('CONTENT_ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "EX-ADM-08: Submit DRAFT exam for review")
    public ResponseEntity<ApiResponse<AdminExamResponse>> submitReview(@PathVariable Long examId) {
        AdminExamResponse result = adminExamService.submitReview(examId);
        return ResponseEntity.ok(ApiResponse.success(result, "Exam submitted for review"));
    }

    @PostMapping("/{examId}/publish")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @Operation(summary = "EX-ADM-09: Publish reviewed or hidden exam")
    public ResponseEntity<ApiResponse<AdminExamResponse>> publish(@PathVariable Long examId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        AdminExamResponse result = adminExamService.publish(currentUserId, examId);
        return ResponseEntity.ok(ApiResponse.success(result, "Exam published"));
    }

    @PostMapping("/{examId}/hide")
    @PreAuthorize("hasAnyRole('CONTENT_ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "EX-ADM-10: Hide published exam")
    public ResponseEntity<ApiResponse<AdminExamResponse>> hide(@PathVariable Long examId) {
        AdminExamResponse result = adminExamService.hide(examId);
        return ResponseEntity.ok(ApiResponse.success(result, "Exam hidden"));
    }

    @PostMapping("/{examId}/archive")
    @PreAuthorize("hasAnyRole('CONTENT_ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "EX-ADM-11: Archive published or hidden exam")
    public ResponseEntity<ApiResponse<AdminExamResponse>> archive(@PathVariable Long examId) {
        AdminExamResponse result = adminExamService.archive(examId);
        return ResponseEntity.ok(ApiResponse.success(result, "Exam archived"));
    }

    @PostMapping("/{examId}/reject-review")
    @PreAuthorize("hasAnyRole('CONTENT_ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "EX-ADM-12: Reject review without request body")
    public ResponseEntity<ApiResponse<AdminExamResponse>> rejectReview(@PathVariable Long examId) {
        AdminExamResponse result = adminExamService.rejectReview(examId);
        return ResponseEntity.ok(ApiResponse.success(result, "Exam review rejected"));
    }

    @PostMapping("/{examId}/return-to-draft")
    @PreAuthorize("hasAnyRole('CONTENT_ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "EX-ADM-13: Return hidden exam to draft")
    public ResponseEntity<ApiResponse<AdminExamResponse>> returnToDraft(@PathVariable Long examId) {
        AdminExamResponse result = adminExamService.returnToDraft(examId);
        return ResponseEntity.ok(ApiResponse.success(result, "Exam returned to draft"));
    }
}
