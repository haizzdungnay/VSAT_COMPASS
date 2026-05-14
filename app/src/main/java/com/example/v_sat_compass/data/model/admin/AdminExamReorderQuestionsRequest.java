package com.example.v_sat_compass.data.model.admin;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class AdminExamReorderQuestionsRequest {
    @SerializedName("questionIds")
    private List<Long> questionIds;

    public AdminExamReorderQuestionsRequest() {
    }

    public AdminExamReorderQuestionsRequest(List<Long> questionIds) {
        this.questionIds = questionIds;
    }

    public List<Long> getQuestionIds() {
        return questionIds;
    }

    public void setQuestionIds(List<Long> questionIds) {
        this.questionIds = questionIds;
    }
}
