package com.vsatcompass.api.dto.request;

import com.vsatcompass.api.entity.enums.Difficulty;
import com.vsatcompass.api.entity.enums.ExamPricingType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminExamUpdateRequest {

    @Size(max = 300)
    private String title;

    private Long subjectId;

    @Size(max = 5000)
    private String description;

    @Positive(message = "durationMinutes must be positive")
    private Integer durationMinutes;

    private Difficulty difficulty;

    private ExamPricingType pricingType;

    @DecimalMin(value = "0.0", inclusive = true, message = "price must be >= 0")
    private BigDecimal price;

    @Size(max = 500)
    private String tags;
}
