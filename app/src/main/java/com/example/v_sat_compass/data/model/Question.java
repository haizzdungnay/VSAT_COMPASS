package com.example.v_sat_compass.data.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Question {
    private Long id;

    @SerializedName("question_code")
    private String questionCode;

    @SerializedName(value = "questionGroupId", alternate = {"question_group_id"})
    private Long questionGroupId;

    @SerializedName(value = "questionGroup", alternate = {"question_group"})
    private QuestionGroup questionGroup;

    @SerializedName("question_text")
    private String questionText;

    @SerializedName("question_type")
    private String questionType;

    @SerializedName("explanation")
    private String explanation;

    @SerializedName("subject_name")
    private String subjectName;

    @SerializedName("topic_name")
    private String topicName;

    @SerializedName(value = "maxRawScore", alternate = {"max_raw_score"})
    private Double maxRawScore;

    private List<Option> options;

    @SerializedName(value = "matchingChoices", alternate = {"matching_choices"})
    private List<MatchingChoice> matchingChoices;

    public Long getId() { return id; }
    public String getQuestionCode() { return questionCode; }
    public Long getQuestionGroupId() { return questionGroupId; }
    public void setQuestionGroupId(Long questionGroupId) { this.questionGroupId = questionGroupId; }
    public QuestionGroup getQuestionGroup() { return questionGroup; }
    public void setQuestionGroup(QuestionGroup questionGroup) { this.questionGroup = questionGroup; }
    public String getQuestionText() { return questionText; }
    public String getQuestionType() { return questionType; }
    public void setQuestionType(String questionType) { this.questionType = questionType; }
    public String getExplanation() { return explanation; }
    public String getSubjectName() { return subjectName; }
    public String getTopicName() { return topicName; }
    public Double getMaxRawScore() { return maxRawScore; }
    public List<Option> getOptions() { return options; }
    public List<MatchingChoice> getMatchingChoices() { return matchingChoices; }

    public static class MatchingChoice {
        private String key;
        private String text;

        public String getKey() { return key; }
        public String getText() { return text; }
    }

    public static class Option {
        private Long id;

        @SerializedName("option_label")
        private String optionLabel;

        @SerializedName("option_text")
        private String optionText;

        @SerializedName("display_order")
        private int displayOrder;

        public Long getId() { return id; }
        public String getOptionLabel() { return optionLabel; }
        public String getOptionText() { return optionText; }
        public int getDisplayOrder() { return displayOrder; }
    }
}
