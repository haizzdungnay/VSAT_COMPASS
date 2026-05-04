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
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionResponse {

    private Long id;
    private String questionCode;
    private Long subjectId;
    private Long topicId;
    private Long subtopicId;
    private Difficulty difficulty;
    private QuestionType questionType;
    private String questionText;
    private String questionHtml;
    private String imageUrl;
    private String explanation;
    private String explanationHtml;
    private String source;
    private String tags;
    private QuestionStatus status;
    private Integer version;
    private Long createdBy;
    private Long reviewedBy;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private List<QuestionOptionResponse> options;
    private List<QuestionReviewResponse> reviewHistory;
}
