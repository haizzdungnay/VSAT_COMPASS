package com.vsatcompass.api.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdmissionPredictionRequest {

    @NotNull(message = "totalScore không được để trống")
    private BigDecimal totalScore;

    private String subjectCombo;

    private Integer year;

    private String region;
}
