package com.vsatcompass.api.controller.student;

import com.vsatcompass.api.dto.common.ApiResponse;
import com.vsatcompass.api.dto.request.AiExplainRequest;
import com.vsatcompass.api.dto.response.AiExplainResponse;
import com.vsatcompass.api.service.AiExplanationService;
import com.vsatcompass.api.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiExplanationService aiExplanationService;

    @PostMapping("/explain")
    @PreAuthorize("hasAnyRole('STUDENT','CONTENT_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<AiExplainResponse>> explain(
            @Valid @RequestBody AiExplainRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                aiExplanationService.explain(SecurityUtils.getCurrentUserId(), request)));
    }
}
