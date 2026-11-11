package com.vsatcompass.api.controller.student;

import com.vsatcompass.api.dto.common.ApiResponse;
import com.vsatcompass.api.dto.request.SessionRequest;
import com.vsatcompass.api.dto.response.AudioPlayResponse;
import com.vsatcompass.api.dto.response.SessionAnswerKeysResponse;
import com.vsatcompass.api.dto.response.SessionAnswerResponse;
import com.vsatcompass.api.dto.response.SessionQuestionContentResponse;
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

import java.util.List;
import java.util.Map;

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

    @PostMapping("/{sessionId}/answers")
    @Operation(summary = "ES-05: Lưu một câu trả lời trong phiên thi (server-side scoring)")
    public ResponseEntity<ApiResponse<Void>> submitAnswer(
            @PathVariable Long sessionId,
            @Valid @RequestBody SessionRequest.SubmitAnswer request) {
        Long userId = SecurityUtils.getCurrentUserId();
        sessionService.submitAnswer(userId, sessionId, request);
        return ResponseEntity.ok(ApiResponse.success(null, "Đã lưu câu trả lời"));
    }

    @PostMapping("/{sessionId}/submit")
    @Operation(summary = "ES-06: Nộp bài — backend tự chấm từ các câu trả lời đã lưu")
    public ResponseEntity<ApiResponse<SessionResponse.SessionInfo>> submit(
            @PathVariable Long sessionId) {
        Long userId = SecurityUtils.getCurrentUserId();
        SessionResponse.SessionInfo result = sessionService.serverSubmit(userId, sessionId);
        return ResponseEntity.ok(ApiResponse.success(result, "Nộp bài thành công"));
    }

    @GetMapping("/{sessionId}/questions/{questionId}")
    @Operation(summary = "ES-03: Get in-session question content")
    public ResponseEntity<ApiResponse<SessionQuestionContentResponse>> getQuestionForSession(
            @PathVariable Long sessionId,
            @PathVariable Long questionId) {
        Long userId = SecurityUtils.getCurrentUserId();
        SessionQuestionContentResponse result =
                sessionService.getQuestionForSession(sessionId, questionId, userId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{sessionId}/answer-keys")
    @Operation(summary = "ES-04: Get answer keys after session submission")
    public ResponseEntity<ApiResponse<SessionAnswerKeysResponse>> getAnswerKeysForSession(
            @PathVariable Long sessionId) {
        Long userId = SecurityUtils.getCurrentUserId();
        SessionAnswerKeysResponse result =
                sessionService.getAnswerKeysForSession(sessionId, userId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/{sessionId}/violations")
    @Operation(summary = "P4-01: Ghi nhận vi phạm giám sát thi (Anti-cheat proctoring violation)")
    public ResponseEntity<ApiResponse<SessionResponse.ViolationResult>> recordViolation(
            @PathVariable Long sessionId,
            @Valid @RequestBody SessionRequest.RecordViolation request) {
        Long userId = SecurityUtils.getCurrentUserId();
        SessionResponse.ViolationResult result = sessionService.recordViolation(userId, sessionId, request);
        return ResponseEntity.ok(ApiResponse.success(result, "Ghi nhận vi phạm thành công"));
    }

    @GetMapping("/{sessionId}/answers")
    @Operation(summary = "ES-07: Lấy danh sách câu trả lời trong phiên thi (sync client)")
    public ResponseEntity<ApiResponse<List<SessionAnswerResponse>>> getSessionAnswers(
            @PathVariable Long sessionId) {
        Long userId = SecurityUtils.getCurrentUserId();
        List<SessionAnswerResponse> result = sessionService.getSessionAnswers(sessionId, userId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{sessionId}/audio-plays")
    @Operation(summary = "ES-08: Lấy số lượt đã nghe audio trong phiên thi")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> getAudioPlayCounts(
            @PathVariable Long sessionId) {
        Long userId = SecurityUtils.getCurrentUserId();
        Map<String, Integer> result = sessionService.getAudioPlayCounts(sessionId, userId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @PostMapping("/{sessionId}/audio-plays")
    @Operation(summary = "ES-09: Ghi nhận một lượt nghe audio")
    public ResponseEntity<ApiResponse<AudioPlayResponse>> recordAudioPlay(
            @PathVariable Long sessionId,
            @Valid @RequestBody SessionRequest.RecordAudioPlay request) {
        Long userId = SecurityUtils.getCurrentUserId();
        AudioPlayResponse result = sessionService.recordAudioPlay(sessionId, userId, request);
        return ResponseEntity.ok(ApiResponse.success(result, "Ghi nhận lượt nghe thành công"));
    }
}
