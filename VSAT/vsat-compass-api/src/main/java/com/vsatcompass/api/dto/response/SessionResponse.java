package com.vsatcompass.api.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

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
        private Integer correctCount;
        private Integer totalQuestions;
        private Integer timeSpentSeconds;
        private OffsetDateTime startedAt;
        private OffsetDateTime submittedAt;
    }
}
