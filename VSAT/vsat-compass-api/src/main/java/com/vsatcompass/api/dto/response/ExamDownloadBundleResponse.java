package com.vsatcompass.api.dto.response;

import com.vsatcompass.api.entity.enums.Difficulty;
import com.vsatcompass.api.entity.enums.ExamPricingType;
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
public class ExamDownloadBundleResponse {

    private Long id;
    private String examCode;
    private String title;
    private Long subjectId;
    private String description;
    private Integer questionCount;
    private Integer durationMinutes;
    private Difficulty difficulty;
    private ExamPricingType pricingType;
    private String tags;
    private OffsetDateTime publishDate;
    private Integer examVersion;
    private OffsetDateTime updatedAt;
    private List<QuestionBundleItem> questions;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class QuestionBundleItem {
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
        private Integer questionOrder;
        private String correctAnswer;
        private List<Long> correctOptionIds;
        private List<OptionBundleItem> options;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OptionBundleItem {
        private Long id;
        private String optionLabel;
        private String optionText;
        private String optionHtml;
        private String imageUrl;
        private Boolean isCorrect;
        private Integer displayOrder;
    }
}
