package com.vsatcompass.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.vsatcompass.api.entity.enums.Difficulty;
import com.vsatcompass.api.entity.enums.QuestionStatus;
import com.vsatcompass.api.entity.enums.QuestionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QuestionPickerItemResponse {

    private Long id;
    private String questionCode;
    private String questionTextSnippet;
    private Long subjectId;
    private Long topicId;
    private Long subtopicId;
    private QuestionType questionType;
    private Difficulty difficulty;
    private QuestionStatus status;
    private Integer version;
    private OffsetDateTime updatedAt;
    private String imageUrl;
}
