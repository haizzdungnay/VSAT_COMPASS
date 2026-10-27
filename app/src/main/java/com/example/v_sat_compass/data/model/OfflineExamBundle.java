package com.example.v_sat_compass.data.model;

import java.util.List;

public class OfflineExamBundle {
    private Long id;
    private String examCode;
    private String title;
    private Long subjectId;
    private String description;
    private Integer questionCount;
    private Integer durationMinutes;
    private String difficulty;
    private String pricingType;
    private String tags;
    private Integer examVersion;
    private String updatedAt;
    private List<QuestionBundleItem> questions;

    public Long getId() { return id; }
    public String getExamCode() { return examCode; }
    public String getTitle() { return title; }
    public Long getSubjectId() { return subjectId; }
    public String getDescription() { return description; }
    public Integer getQuestionCount() { return questionCount; }
    public Integer getDurationMinutes() { return durationMinutes; }
    public String getDifficulty() { return difficulty; }
    public String getPricingType() { return pricingType; }
    public String getTags() { return tags; }
    public Integer getExamVersion() { return examVersion; }
    public String getUpdatedAt() { return updatedAt; }
    public List<QuestionBundleItem> getQuestions() { return questions; }

    public static class QuestionBundleItem {
        private Long id;
        private String questionCode;
        private Long subjectId;
        private Long topicId;
        private Long subtopicId;
        private String difficulty;
        private String questionType;
        private String questionText;
        private String questionHtml;
        private String imageUrl;
        private String explanation;
        private String explanationHtml;
        private Integer questionOrder;
        private String correctAnswer;
        private List<Long> correctOptionIds;
        private List<OptionBundleItem> options;

        public Long getId() { return id; }
        public String getQuestionCode() { return questionCode; }
        public String getQuestionText() { return questionText; }
        public String getQuestionType() { return questionType; }
        public String getExplanation() { return explanation; }
        public Integer getQuestionOrder() { return questionOrder; }
        public List<Long> getCorrectOptionIds() { return correctOptionIds; }
        public List<OptionBundleItem> getOptions() { return options; }
    }

    public static class OptionBundleItem {
        private Long id;
        private String optionLabel;
        private String optionText;
        private String optionHtml;
        private String imageUrl;
        private Boolean isCorrect;
        private Integer displayOrder;

        public Long getId() { return id; }
        public String getOptionLabel() { return optionLabel; }
        public String getOptionText() { return optionText; }
        public Boolean getIsCorrect() { return isCorrect; }
        public Integer getDisplayOrder() { return displayOrder; }
    }
}
