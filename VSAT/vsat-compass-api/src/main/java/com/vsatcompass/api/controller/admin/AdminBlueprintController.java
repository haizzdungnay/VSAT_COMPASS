package com.vsatcompass.api.controller.admin;

import com.vsatcompass.api.dto.common.ApiResponse;
import com.vsatcompass.api.dto.response.BlueprintResponse;
import com.vsatcompass.api.service.AdminBlueprintService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/blueprints")
@RequiredArgsConstructor
@Tag(name = "Admin-Blueprints", description = "V-SAT Exam Matrix Blueprints")
public class AdminBlueprintController {

    private final AdminBlueprintService blueprintService;

    @GetMapping
    @PreAuthorize("hasAnyRole('CONTENT_ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "BP-ADM-01: List V-SAT blueprints")
    public ResponseEntity<ApiResponse<List<BlueprintResponse>>> list(
            @RequestParam(required = false) Long subjectId) {
        List<BlueprintResponse> result = blueprintService.listBlueprints(subjectId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CONTENT_ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "BP-ADM-02: Get blueprint detail with cells")
    public ResponseEntity<ApiResponse<BlueprintResponse>> get(@PathVariable Long id) {
        BlueprintResponse result = blueprintService.getBlueprint(id);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}