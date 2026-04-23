package com.vsatcompass.api.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

public class SessionRequest {

    @Data
    public static class StartSession {
        @NotNull(message = "examId không được để trống")
        private Long examId;

        private String mode; // MOCK_EXAM, PRACTICE — defaults to MOCK_EXAM if null

        @Min(value = 1, message = "totalQuestions phải ≥ 1")
        private Integer totalQuestions;
    }

    @Data
    public static class ClientSubmit {
        @NotNull(message = "score không được để trống")
        @DecimalMin(value = "0.0", message = "score phải ≥ 0")
        @DecimalMax(value = "100.0", message = "score phải ≤ 100")
        private Double score;

        @NotNull(message = "correctCount không được để trống")
        @Min(value = 0, message = "correctCount phải ≥ 0")
        private Integer correctCount;

        @NotNull(message = "totalQuestions không được để trống")
        @Min(value = 1, message = "totalQuestions phải ≥ 1")
        private Integer totalQuestions;

        @NotNull(message = "timeSpentSeconds không được để trống")
        @Min(value = 0, message = "timeSpentSeconds phải ≥ 0")
        @Max(value = 86400, message = "timeSpentSeconds phải ≤ 86400")
        private Integer timeSpentSeconds;
    }
}
