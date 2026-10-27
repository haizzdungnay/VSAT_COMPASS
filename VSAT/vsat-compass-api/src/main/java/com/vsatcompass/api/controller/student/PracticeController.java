package com.vsatcompass.api.controller.student;

import com.vsatcompass.api.dto.common.ApiResponse;
import com.vsatcompass.api.dto.request.PracticeGradeRequest;
import com.vsatcompass.api.dto.response.PracticeGradeResponse;
import com.vsatcompass.api.dto.response.SessionQuestionContentResponse;
import com.vsatcompass.api.entity.enums.Difficulty;
import com.vsatcompass.api.service.PracticeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/practice")
@RequiredArgsConstructor
public class PracticeController {

    private final PracticeService practiceService;

    @GetMapping("/questions")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<List<SessionQuestionContentResponse>>> getQuestions(
            @RequestParam(required = false) Long subtopicId,
            @RequestParam(required = false) Long topicId,
            @RequestParam(defaultValue = "MEDIUM") Difficulty difficulty,
            @RequestParam(defaultValue = "1") int limit
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                practiceService.getQuestions(subtopicId, topicId, difficulty, limit)));
    }

    @PostMapping("/grade")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<PracticeGradeResponse>> grade(
            @Valid @RequestBody PracticeGradeRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(practiceService.grade(request)));
    }
}
