package com.vsatcompass.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public class SessionResponse {

    @Data
    @Builder
    public static class SessionInfo {
        private Long id;
        private Long examId;
        private String mode;
        private String status;
        private BigDecimal score;
        private BigDecimal scorePercentage;
        private BigDecimal rawScore;
        private BigDecimal maxRawScore;
        private Integer correctCount;
        private Integer totalQuestions;
        private Integer timeSpentSeconds;
        private OffsetDateTime startedAt;
        private OffsetDateTime submittedAt;
        private List<Long> orderedQuestionIds;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ViolationResult {
        private Long sessionId;
        private Integer violationCount;
        @Builder.Default
        private Integer maxViolations = 3;
        private Boolean autoSubmitted;
        private String sessionStatus;
        private BigDecimal score;
    }
}
