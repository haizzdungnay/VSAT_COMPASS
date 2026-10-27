package com.vsatcompass.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamVariantResponse {
    private Long id;
    private Long examId;
    private String variantCode;
    private String randomSeed;
    private String contentHash;
    private Integer questionCount;
    private OffsetDateTime createdAt;
}