package com.vsatcompass.api.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GenerateVariantsRequest {
    @Builder.Default
    private Integer variantCount = 4;
    @Builder.Default
    private Boolean shuffleQuestions = true;
    @Builder.Default
    private Boolean shuffleMcq = true;
    @Builder.Default
    private Boolean shuffleMatching = true;
    private String randomSeed;
}