package com.vsatcompass.api.controller.exam;

import com.vsatcompass.api.dto.common.ApiResponse;
import com.vsatcompass.api.dto.response.ExamDetailResponse;
import com.vsatcompass.api.dto.response.ExamSummaryResponse;
import com.vsatcompass.api.service.ExamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/exams")
@RequiredArgsConstructor
@Tag(name = "Exams", description = "Published free exams (public read-only API)")
public class ExamPublicController {

    private final ExamService examService;

    @GetMapping
    @Operation(summary = "EX-01: List published free exams")
    public ResponseEntity<ApiResponse<Page<ExamSummaryResponse>>> list(Pageable pageable) {
        Page<ExamSummaryResponse> result = examService.listPublishedFreeExams(pageable);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{id}")
    @Operation(summary = "EX-02: Get published free exam detail")
    public ResponseEntity<ApiResponse<ExamDetailResponse>> get(@PathVariable Long id) {
        ExamDetailResponse result = examService.getPublicExam(id);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
