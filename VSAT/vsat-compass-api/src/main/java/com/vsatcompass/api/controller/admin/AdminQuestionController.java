package com.vsatcompass.api.controller.admin;

import com.vsatcompass.api.dto.common.ApiResponse;
import com.vsatcompass.api.dto.request.ReviewActionRequest;
import com.vsatcompass.api.dto.response.QuestionImportResultResponse;
import com.vsatcompass.api.dto.response.QuestionListItemResponse;
import com.vsatcompass.api.dto.response.QuestionPickerItemResponse;
import com.vsatcompass.api.dto.response.QuestionResponse;
import com.vsatcompass.api.entity.enums.QuestionStatus;
import com.vsatcompass.api.entity.enums.QuestionType;
import com.vsatcompass.api.service.QuestionImportService;
import com.vsatcompass.api.service.QuestionService;
import com.vsatcompass.api.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/admin/questions")
@RequiredArgsConstructor
@Tag(name = "Admin-Questions", description = "Duyệt, từ chối, yêu cầu sửa, import câu hỏi (admin)")
public class AdminQuestionController {

    private final QuestionService questionService;
    private final QuestionImportService questionImportService;

    @GetMapping
    @PreAuthorize("hasAnyRole('CONTENT_ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Q-ADM-01: Hàng đợi duyệt (filter by status)")
    public ResponseEntity<ApiResponse<Page<QuestionListItemResponse>>> queue(
            @RequestParam(required = false) QuestionStatus status,
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<QuestionListItemResponse> result = questionService.listByStatus(status, capPageSize(pageable));
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/picker")
    @PreAuthorize("hasAnyRole('CONTENT_ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Q-ADM-05: Question picker for exam composition (GET /admin/questions/picker)")
    public ResponseEntity<ApiResponse<Page<QuestionPickerItemResponse>>> getQuestionPicker(
            @RequestParam(required = false) QuestionStatus status,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Long topicId,
            @RequestParam(required = false) QuestionType questionType,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        QuestionStatus effectiveStatus = status != null ? status : QuestionStatus.APPROVED;
        Pageable effectivePageable = capPageSize(pageable);
        Page<QuestionPickerItemResponse> result = questionService.findForPicker(
                effectiveStatus, subjectId, topicId, questionType, q, effectivePageable);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CONTENT_ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Q-ADM-06: Chi tiết câu hỏi (admin)")
    public ResponseEntity<ApiResponse<QuestionResponse>> getQuestionDetail(@PathVariable Long id) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        String currentUserRole = SecurityUtils.getCurrentUserRole();
        QuestionResponse result = questionService.getById(currentUserId, currentUserRole, id);
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

    @GetMapping(value = "/import/template", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    @PreAuthorize("hasAnyRole('CONTENT_ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Q-ADM-07: Tải file template Excel mẫu")
    public ResponseEntity<byte[]> downloadTemplate() {
        byte[] bytes = questionImportService.generateTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"vsat_question_import_template.xlsx\"")
                .header(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                .body(bytes);
    }

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('CONTENT_ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Q-ADM-08: Import câu hỏi từ Excel (preview khi dryRun=true, lưu khi dryRun=false)")
    public ResponseEntity<ApiResponse<QuestionImportResultResponse>> importQuestions(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "dryRun", defaultValue = "true") boolean dryRun) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        QuestionImportResultResponse result = questionImportService.importQuestions(file, dryRun, currentUserId);
        String msg = dryRun ? "Kiểm tra dữ liệu hoàn tất" : "Import câu hỏi thành công (" + result.getImportedQuestions() + " câu)";
        return ResponseEntity.ok(ApiResponse.success(result, msg));
    }

    private Pageable capPageSize(Pageable pageable) {
        if (pageable.getPageSize() <= 100) {
            return pageable;
        }
        return PageRequest.of(
                pageable.getPageNumber(),
                100,
                pageable.getSortOr(Sort.by(Sort.Direction.DESC, "updatedAt"))
        );
    }
}
