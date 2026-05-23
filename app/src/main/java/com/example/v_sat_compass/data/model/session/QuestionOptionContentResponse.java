package com.example.v_sat_compass.data.model.session;

import com.google.gson.annotations.SerializedName;

public class QuestionOptionContentResponse {
    @SerializedName("id")
    private long id;

    @SerializedName("content")
    private String content;

    @SerializedName("order")
    private int order;

    public long getId() { return id; }
    public String getContent() { return content; }
    public int getOrder() { return order; }
}
