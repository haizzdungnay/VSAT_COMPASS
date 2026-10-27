package com.vsatcompass.api.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateByMatrixRequest {
    private AdminExamCreateRequest exam;
    private Long blueprintId;
    @Builder.Default
    private Integer variantCount = 4;
    @Builder.Default
    private Boolean avoidPreviouslyUsedQuestions = true;
    @Builder.Default
    private Double maxExposureRate = 0.3;
}