package com.vsatcompass.api.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PracticeGradeRequest {

    @NotNull
    private Long questionId;

    @NotNull
    private Long selectedOptionId;
}
