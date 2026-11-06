package com.vsatcompass.api.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdmissionPredictionResponse {

    private BigDecimal totalScore;
    private String subjectCombo;
    private Integer year;
    private String region;
    private int safeCount;
    private int fitCount;
    private int riskCount;
    private int totalEvaluated;
    private List<PredictionItem> predictions;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PredictionItem {
        private Long benchmarkId;
        private Long universityId;
        private String universityCode;
        private String universityName;
        private String region;
        private String majorCode;
        private String majorName;
        private String subjectCombo;
        private Integer year;
        private BigDecimal cutoffScore;
        private BigDecimal delta;
        private String classification; // SAFE, FIT, RISK
        private String notes;
    }
}
