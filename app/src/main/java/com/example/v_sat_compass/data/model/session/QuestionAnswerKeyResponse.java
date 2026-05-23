package com.example.v_sat_compass.data.model.session;

import com.google.gson.annotations.SerializedName;

import java.util.Collections;
import java.util.List;

public class QuestionAnswerKeyResponse {
    @SerializedName("questionId")
    private long questionId;

    @SerializedName("correctOptionIds")
    private List<Long> correctOptionIds;

    @SerializedName("explanation")
    private String explanation;

    public long getQuestionId() { return questionId; }
    public List<Long> getCorrectOptionIds() {
        return correctOptionIds != null ? correctOptionIds : Collections.emptyList();
    }
    public String getExplanation() { return explanation; }
}
