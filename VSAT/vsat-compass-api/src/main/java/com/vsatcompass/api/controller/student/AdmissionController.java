package com.vsatcompass.api.controller.student;

import com.vsatcompass.api.dto.common.ApiResponse;
import com.vsatcompass.api.dto.request.AdmissionPredictionRequest;
import com.vsatcompass.api.dto.response.AdmissionPredictionResponse;
import com.vsatcompass.api.dto.response.UniversityResponse;
import com.vsatcompass.api.service.AdmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admissions")
@RequiredArgsConstructor
@Tag(name = "Admission Matching", description = "Dự đoán và tra cứu điểm chuẩn tuyển sinh đại học")
public class AdmissionController {

    private final AdmissionService admissionService;

    @GetMapping("/universities")
    @Operation(summary = "P2-01: Lấy danh sách trường đại học")
    public ResponseEntity<ApiResponse<List<UniversityResponse>>> getUniversities() {
        List<UniversityResponse> result = admissionService.getUniversities();
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/subject-combos")
    @Operation(summary = "P2-02: Lấy danh sách tổ hợp xét tuyển (A00, A01, D01, B00, C00...)")
    public ResponseEntity<ApiResponse<List<String>>> getSubjectCombos() {
        List<String> result = admissionService.getSubjectCombos();
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/predict")
    @Operation(summary = "P2-03: Dự đoán xác suất trúng tuyển theo mức điểm (SAFE, FIT, RISK)")
    public ResponseEntity<ApiResponse<AdmissionPredictionResponse>> predictAdmission(
            @Valid @RequestBody AdmissionPredictionRequest request) {
        AdmissionPredictionResponse result = admissionService.predictAdmission(request);
        return ResponseEntity.ok(ApiResponse.success(result, "Dự đoán trúng tuyển thành công"));
    }
}
