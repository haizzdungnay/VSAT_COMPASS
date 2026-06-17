package com.vsatcompass.api.controller.student;

import com.vsatcompass.api.dto.common.ApiResponse;
import com.vsatcompass.api.dto.response.TopicStatsResponse;
import com.vsatcompass.api.service.MyStatsService;
import com.vsatcompass.api.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/my-stats")
@RequiredArgsConstructor
@Tag(name = "My-Stats", description = "Thống kê học tập cá nhân")
public class MyStatsController {

    private final MyStatsService myStatsService;

    @GetMapping("/topics")
    @Operation(summary = "Topic-level accuracy stats from submitted session answers")
    public ResponseEntity<ApiResponse<List<TopicStatsResponse>>> getTopicStats() {
        Long userId = SecurityUtils.getCurrentUserId();
        List<TopicStatsResponse> stats = myStatsService.getTopicStats(userId);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/weak-topics")
    @Operation(summary = "Weakest topic stats for the current student")
    public ResponseEntity<ApiResponse<List<TopicStatsResponse>>> getWeakTopics(
            @RequestParam(defaultValue = "5") int limit
    ) {
        Long userId = SecurityUtils.getCurrentUserId();
        List<TopicStatsResponse> stats = myStatsService.getWeakTopics(userId, limit);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}
