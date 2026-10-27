package com.example.v_sat_compass.data.model;

import com.google.gson.annotations.SerializedName;

public class AiExplainResponse {
    @SerializedName("questionId")
    private long questionId;

    @SerializedName("explanation")
    private String explanation;

    @SerializedName("cached")
    private boolean cached;

    public long getQuestionId() { return questionId; }
    public String getExplanation() { return explanation; }
    public boolean isCached() { return cached; }
}
