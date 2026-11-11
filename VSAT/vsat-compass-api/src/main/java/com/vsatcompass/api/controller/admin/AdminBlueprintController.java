package com.vsatcompass.api.controller.admin;

import com.vsatcompass.api.dto.common.ApiResponse;
import com.vsatcompass.api.dto.request.BlueprintUpsertRequest;
import com.vsatcompass.api.dto.response.BlueprintResponse;
import com.vsatcompass.api.service.AdminBlueprintService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

    @PostMapping
    @PreAuthorize("hasAnyRole('CONTENT_ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "BP-ADM-03: Create exam blueprint")
    public ResponseEntity<ApiResponse<BlueprintResponse>> create(
            @Valid @RequestBody BlueprintUpsertRequest request) {
        BlueprintResponse result = blueprintService.createBlueprint(request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('CONTENT_ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "BP-ADM-04: Update exam blueprint")
    public ResponseEntity<ApiResponse<BlueprintResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody BlueprintUpsertRequest request) {
        BlueprintResponse result = blueprintService.updateBlueprint(id, request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('CONTENT_ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "BP-ADM-05: Delete exam blueprint")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        blueprintService.deleteBlueprint(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
