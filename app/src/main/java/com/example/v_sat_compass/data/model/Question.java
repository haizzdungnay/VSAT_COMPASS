package com.example.v_sat_compass.data.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Question {
    private Long id;

    @SerializedName("question_code")
    private String questionCode;

    @SerializedName("question_text")
    private String questionText;

    @SerializedName("explanation")
    private String explanation;

    @SerializedName("subject_name")
    private String subjectName;

    @SerializedName("topic_name")
    private String topicName;

    private List<Option> options;

    public Long getId() { return id; }
    public String getQuestionCode() { return questionCode; }
    public String getQuestionText() { return questionText; }
    public String getExplanation() { return explanation; }
    public String getSubjectName() { return subjectName; }
    public String getTopicName() { return topicName; }
    public List<Option> getOptions() { return options; }

    public static class Option {
        private Long id;

        @SerializedName("option_label")
        private String optionLabel;

        @SerializedName("option_text")
        private String optionText;

        @SerializedName("is_correct")
        private boolean isCorrect;

        @SerializedName("display_order")
        private int displayOrder;

        public Long getId() { return id; }
        public String getOptionLabel() { return optionLabel; }
        public String getOptionText() { return optionText; }
        public boolean isCorrect() { return isCorrect; }
        public int getDisplayOrder() { return displayOrder; }
    }
}
