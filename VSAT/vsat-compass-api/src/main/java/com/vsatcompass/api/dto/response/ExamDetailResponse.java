package com.vsatcompass.api.dto.response;

import com.vsatcompass.api.entity.enums.Difficulty;
import com.vsatcompass.api.entity.enums.ExamPricingType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamDetailResponse {

    private Long id;
    private String examCode;
    private String title;
    private Long subjectId;
    private String description;
    private Integer questionCount;
    private Integer durationMinutes;
    private Difficulty difficulty;
    private ExamPricingType pricingType;
    private String tags;
    private OffsetDateTime publishDate;
}
