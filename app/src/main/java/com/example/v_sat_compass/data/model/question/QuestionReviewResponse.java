package com.example.v_sat_compass.data.model.question;

import com.example.v_sat_compass.data.model.enums.ReviewAction;
import com.google.gson.annotations.SerializedName;

public class QuestionReviewResponse {
    @SerializedName("id")
    private Long id;

    @SerializedName("reviewerId")
    private Long reviewerId;

    @SerializedName("action")
    private ReviewAction action;

    @SerializedName("comment")
    private String comment;

    @SerializedName("versionReviewed")
    private Integer versionReviewed;

    @SerializedName("createdAt")
    private String createdAt;

    public QuestionReviewResponse() {
    }

    public QuestionReviewResponse(
            Long id,
            Long reviewerId,
            ReviewAction action,
            String comment,
            Integer versionReviewed,
            String createdAt
    ) {
        this.id = id;
        this.reviewerId = reviewerId;
        this.action = action;
        this.comment = comment;
        this.versionReviewed = versionReviewed;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getReviewerId() {
        return reviewerId;
    }

    public void setReviewerId(Long reviewerId) {
        this.reviewerId = reviewerId;
    }

    public ReviewAction getAction() {
        return action;
    }

    public void setAction(ReviewAction action) {
        this.action = action;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public Integer getVersionReviewed() {
        return versionReviewed;
    }

    public void setVersionReviewed(Integer versionReviewed) {
        this.versionReviewed = versionReviewed;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
