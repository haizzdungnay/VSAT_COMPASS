package com.vsatcompass.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.vsatcompass.api.entity.enums.Difficulty;
import com.vsatcompass.api.entity.enums.QuestionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SessionQuestionContentResponse {

    private Long id;
    private String questionCode;
    private String content;
    private QuestionType questionType;
    private Difficulty difficulty;
    private Integer order;
    private List<QuestionOptionContentResponse> options;
}
