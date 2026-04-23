package com.vsatcompass.api.controller.student;

import com.vsatcompass.api.dto.common.ApiResponse;
import com.vsatcompass.api.dto.request.SessionRequest;
import com.vsatcompass.api.dto.response.SessionResponse;
import com.vsatcompass.api.service.SessionService;
import com.vsatcompass.api.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/sessions")
@RequiredArgsConstructor
@Tag(name = "Session", description = "Quản lý phiên thi")
public class SessionController {

    private final SessionService sessionService;

    @PostMapping("/start")
    @Operation(summary = "ES-01: Bắt đầu phiên thi mới")
    public ResponseEntity<ApiResponse<SessionResponse.SessionInfo>> startSession(
            @Valid @RequestBody SessionRequest.StartSession request) {
        Long userId = SecurityUtils.getCurrentUserId();
        SessionResponse.SessionInfo result = sessionService.startSession(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(result, "Phiên thi đã được tạo"));
    }

    @PostMapping("/{sessionId}/client-submit")
    @Operation(summary = "ES-02: Nộp kết quả thi từ client (client-side scoring)")
    public ResponseEntity<ApiResponse<SessionResponse.SessionInfo>> clientSubmit(
            @PathVariable Long sessionId,
            @Valid @RequestBody SessionRequest.ClientSubmit request) {
        Long userId = SecurityUtils.getCurrentUserId();
        SessionResponse.SessionInfo result = sessionService.clientSubmit(userId, sessionId, request);
        return ResponseEntity.ok(ApiResponse.success(result, "Nộp bài thành công"));
    }
}
