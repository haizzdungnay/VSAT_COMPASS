package com.vsatcompass.api.controller.subject;

import com.vsatcompass.api.dto.common.ApiResponse;
import com.vsatcompass.api.dto.response.SubjectResponse;
import com.vsatcompass.api.dto.response.TopicResponse;
import com.vsatcompass.api.service.SubjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/subjects")
@RequiredArgsConstructor
@Tag(name = "Subjects", description = "Danh sách môn học và chủ đề (public)")
public class SubjectController {

    private final SubjectService subjectService;

    @GetMapping
    @Operation(summary = "SB-01: Danh sách môn học đang hoạt động")
    public ResponseEntity<ApiResponse<List<SubjectResponse>>> list() {
        List<SubjectResponse> result = subjectService.listActiveSubjects();
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{subjectId}/topics")
    @Operation(summary = "SB-02: Danh sách chủ đề thuộc một môn")
    public ResponseEntity<ApiResponse<List<TopicResponse>>> listTopics(@PathVariable Long subjectId) {
        List<TopicResponse> result = subjectService.listActiveTopicsBySubjectId(subjectId);
        return ResponseEntity.ok(ApiResponse.success(result));
    }
}
