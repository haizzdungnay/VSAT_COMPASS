package com.vsatcompass.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatrixGenerateResponse {
    private Long examId;
    private String examCode;
    private Integer selectedQuestions;
    private BigDecimal rawScore;
    private List<String> variants;
    private List<String> generationWarnings;
}