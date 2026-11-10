package com.vsatcompass.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QuestionAnswerKeyResponse {

    private Long questionId;
    private List<Long> correctOptionIds;
    private String explanation;
    private Map<String, Boolean> correctTrueFalse;
    private String correctShortAnswer;
    private Map<String, String> correctMatching;
}
