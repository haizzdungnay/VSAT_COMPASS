package com.example.v_sat_compass.data.model.admin;

import com.google.gson.annotations.SerializedName;

public class AdminReviewActionRequest {
    @SerializedName("comment")
    private String comment;

    public AdminReviewActionRequest() {
    }

    public AdminReviewActionRequest(String comment) {
        this.comment = comment;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
