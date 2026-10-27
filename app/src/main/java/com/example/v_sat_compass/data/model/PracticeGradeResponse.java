package com.example.v_sat_compass.data.model;

import com.google.gson.annotations.SerializedName;

import java.util.Collections;
import java.util.List;

public class PracticeGradeResponse {
    @SerializedName("questionId")
    private long questionId;

    @SerializedName("selectedOptionId")
    private long selectedOptionId;

    @SerializedName("correct")
    private boolean correct;

    @SerializedName("correctOptionIds")
    private List<Long> correctOptionIds;

    @SerializedName("explanation")
    private String explanation;

    public long getQuestionId() { return questionId; }
    public long getSelectedOptionId() { return selectedOptionId; }
    public boolean isCorrect() { return correct; }
    public List<Long> getCorrectOptionIds() {
        return correctOptionIds != null ? correctOptionIds : Collections.emptyList();
    }
    public String getExplanation() { return explanation; }
}
