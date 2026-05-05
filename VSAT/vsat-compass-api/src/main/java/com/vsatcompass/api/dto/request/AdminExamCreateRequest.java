package com.vsatcompass.api.dto.request;

import com.vsatcompass.api.entity.enums.Difficulty;
import com.vsatcompass.api.entity.enums.ExamPricingType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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
public class AdminExamCreateRequest {

    @NotBlank(message = "examCode is required")
    @Pattern(regexp = "^[A-Z][A-Z0-9_]{2,49}$",
            message = "examCode must match ^[A-Z][A-Z0-9_]{2,49}$")
    private String examCode;

    @NotBlank(message = "title is required")
    @Size(max = 300)
    private String title;

    @NotNull(message = "subjectId is required")
    private Long subjectId;

    @Size(max = 5000)
    private String description;

    @NotNull(message = "durationMinutes is required")
    @Positive(message = "durationMinutes must be positive")
    private Integer durationMinutes;

    @NotNull(message = "difficulty is required")
    private Difficulty difficulty;

    @NotNull(message = "pricingType is required")
    private ExamPricingType pricingType;

    @DecimalMin(value = "0.0", inclusive = true, message = "price must be >= 0")
    private BigDecimal price;

    @Size(max = 500)
    private String tags;
}
