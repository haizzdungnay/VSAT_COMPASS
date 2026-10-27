package com.example.v_sat_compass.data.model;

import com.google.gson.annotations.SerializedName;

public class PracticeGradeRequest {
    @SerializedName("questionId")
    private final long questionId;

    @SerializedName("selectedOptionId")
    private final long selectedOptionId;

    public PracticeGradeRequest(long questionId, long selectedOptionId) {
        this.questionId = questionId;
        this.selectedOptionId = selectedOptionId;
    }
}
