package com.vsatcompass.api.dto.response;

import com.vsatcompass.api.entity.enums.Difficulty;
import com.vsatcompass.api.entity.enums.ExamPricingType;
import com.vsatcompass.api.entity.enums.ExamStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminExamSummaryResponse {

    private Long id;
    private String examCode;
    private String title;
    private Long subjectId;
    private Integer questionCount;
    private Integer durationMinutes;
    private Difficulty difficulty;
    private ExamPricingType pricingType;
    private BigDecimal price;
    private ExamStatus status;
    private Integer version;
    private OffsetDateTime updatedAt;
}
