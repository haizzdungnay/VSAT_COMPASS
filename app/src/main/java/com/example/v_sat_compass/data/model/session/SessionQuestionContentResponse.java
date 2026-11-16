package com.example.v_sat_compass.data.model.session;

import com.example.v_sat_compass.data.model.QuestionGroup;
import com.google.gson.annotations.SerializedName;

import java.util.Collections;
import java.util.List;

public class SessionQuestionContentResponse {
    @SerializedName("id")
    private long id;

    @SerializedName("questionCode")
    private String questionCode;

    @SerializedName("questionGroupId")
    private Long questionGroupId;

    @SerializedName("questionGroup")
    private QuestionGroup questionGroup;

    @SerializedName("content")
    private String content;

    @SerializedName("questionType")
    private String questionType;

    @SerializedName("difficulty")
    private String difficulty;

    @SerializedName("order")
    private int order;

    @SerializedName("options")
    private List<QuestionOptionContentResponse> options;

    @SerializedName("matchingChoices")
    private List<MatchingChoiceDto> matchingChoices;

    public long getId() { return id; }
    public String getQuestionCode() { return questionCode; }
    public Long getQuestionGroupId() { return questionGroupId; }
    public QuestionGroup getQuestionGroup() { return questionGroup; }
    public String getContent() { return content; }
    public String getQuestionType() { return questionType; }
    public String getDifficulty() { return difficulty; }
    public int getOrder() { return order; }
    public List<QuestionOptionContentResponse> getOptions() {
        return options != null ? options : Collections.emptyList();
    }
    public List<MatchingChoiceDto> getMatchingChoices() {
        return matchingChoices != null ? matchingChoices : Collections.emptyList();
    }

    public static class MatchingChoiceDto {
        @SerializedName("key")
        private String key;

        @SerializedName("text")
        private String text;

        public String getKey() { return key; }
        public String getText() { return text; }
    }
}
