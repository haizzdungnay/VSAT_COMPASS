package com.vsatcompass.api.controller;

import com.vsatcompass.api.dto.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "API Info", description = "Public API entry point")
public class ApiInfoController {

    @GetMapping("/")
    @Operation(summary = "Public API entry point")
    public ResponseEntity<ApiResponse<ApiInfoResponse>> info() {
        ApiInfoResponse result = new ApiInfoResponse(
                "V-SAT Compass API",
                "UP",
                "/api/v1/swagger-ui.html",
                "/api/v1/actuator/health",
                List.of("/api/v1/auth/login", "/api/v1/exams", "/api/v1/subjects")
        );

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    public record ApiInfoResponse(
            String name,
            String status,
            String swaggerUrl,
            String healthUrl,
            List<String> publicEndpoints) {
    }
}
