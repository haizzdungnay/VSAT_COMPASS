package com.vsatcompass.api.dto.response;

import com.vsatcompass.api.entity.enums.Difficulty;
import com.vsatcompass.api.entity.enums.QuestionStatus;
import com.vsatcompass.api.entity.enums.QuestionType;
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
public class QuestionListItemResponse {

    private Long id;
    private String questionCode;
    private Long subjectId;
    private Long topicId;
    private Difficulty difficulty;
    private QuestionType questionType;
    private QuestionStatus status;
    private Integer version;
    private Long createdBy;
    private OffsetDateTime updatedAt;
}
