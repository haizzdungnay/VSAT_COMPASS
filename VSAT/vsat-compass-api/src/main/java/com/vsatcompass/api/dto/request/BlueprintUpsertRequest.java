package com.vsatcompass.api.dto.request;

import com.vsatcompass.api.dto.response.BlueprintResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BlueprintUpsertRequest {

    @NotBlank(message = "code is required")
    @Size(max = 50)
    private String code;

    @NotBlank(message = "name is required")
    @Size(max = 200)
    private String name;

    @NotNull(message = "subjectId is required")
    private Long subjectId;

    private Long scoringProfileId;

    private Integer totalQuestions;

    private BigDecimal maxRawScore;

    private Integer version;

    private List<BlueprintResponse.BlueprintCellResponse> cells;
}
