package com.vsatcompass.api.controller.student;

import com.vsatcompass.api.dto.common.ApiResponse;
import com.vsatcompass.api.dto.response.ExamDownloadBundleResponse;
import com.vsatcompass.api.service.ExamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/exams")
@RequiredArgsConstructor
@Tag(name = "Exam Download", description = "Student offline exam bundles")
public class ExamDownloadController {

    private final ExamService examService;

    @GetMapping("/{id}/download-bundle")
    @Operation(summary = "EX-F1: Download published free exam bundle for offline use")
    public ResponseEntity<ApiResponse<ExamDownloadBundleResponse>> downloadBundle(
            @PathVariable Long id) {
        ExamDownloadBundleResponse result = examService.getDownloadBundle(id);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
