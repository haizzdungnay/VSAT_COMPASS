package com.example.v_sat_compass.data.model.admin;

import com.google.gson.annotations.SerializedName;

public class AdminExamAddQuestionRequest {
    @SerializedName("questionId")
    private Long questionId;

    public AdminExamAddQuestionRequest() {
    }

    public AdminExamAddQuestionRequest(Long questionId) {
        this.questionId = questionId;
    }

    public Long getQuestionId() {
        return questionId;
    }

    public void setQuestionId(Long questionId) {
        this.questionId = questionId;
    }
}
