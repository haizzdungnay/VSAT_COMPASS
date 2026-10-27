package com.example.v_sat_compass.data.model;

import com.google.gson.annotations.SerializedName;

public class AiExplainRequest {
    @SerializedName("questionId")
    private final long questionId;

    @SerializedName("selectedOptionId")
    private final Long selectedOptionId;

    public AiExplainRequest(long questionId, Long selectedOptionId) {
        this.questionId = questionId;
        this.selectedOptionId = selectedOptionId;
    }
}
