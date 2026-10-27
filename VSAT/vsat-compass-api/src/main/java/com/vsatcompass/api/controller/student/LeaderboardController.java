package com.vsatcompass.api.controller.student;

import com.vsatcompass.api.dto.common.ApiResponse;
import com.vsatcompass.api.dto.response.LeaderboardEntryResponse;
import com.vsatcompass.api.service.LeaderboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/leaderboard")
@RequiredArgsConstructor
@Tag(name = "Leaderboard", description = "Bảng xếp hạng học viên (F9)")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    @GetMapping
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "F9.2: Top học viên opt-in, sắp xếp theo tổng điểm bài thi")
    public ResponseEntity<ApiResponse<List<LeaderboardEntryResponse>>> top(
            @RequestParam(defaultValue = "20") int limit
    ) {
        return ResponseEntity.ok(ApiResponse.success(leaderboardService.getTop(limit)));
    }
}
