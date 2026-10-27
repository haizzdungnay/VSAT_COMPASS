package com.vsatcompass.api.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiExplainRequest {

    @NotNull
    private Long questionId;

    private Long selectedOptionId;
}
