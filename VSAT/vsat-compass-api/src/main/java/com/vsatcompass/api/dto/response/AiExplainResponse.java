package com.vsatcompass.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiExplainResponse {

    private Long questionId;
    private String explanation;
    private boolean cached;
}
