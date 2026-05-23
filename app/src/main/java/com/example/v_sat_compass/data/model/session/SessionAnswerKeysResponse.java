package com.example.v_sat_compass.data.model.session;

import com.google.gson.annotations.SerializedName;

import java.util.Collections;
import java.util.List;

public class SessionAnswerKeysResponse {
    @SerializedName("sessionId")
    private long sessionId;

    @SerializedName("examId")
    private long examId;

    @SerializedName("questions")
    private List<QuestionAnswerKeyResponse> questions;

    public long getSessionId() { return sessionId; }
    public long getExamId() { return examId; }
    public List<QuestionAnswerKeyResponse> getQuestions() {
        return questions != null ? questions : Collections.emptyList();
    }
}
