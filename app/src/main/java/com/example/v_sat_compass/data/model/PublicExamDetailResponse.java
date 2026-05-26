package com.example.v_sat_compass.data.model;

import com.google.gson.annotations.SerializedName;

public class PublicExamDetailResponse {
    private Long id;
    private String title;
    private String description;

    @SerializedName("examCode")
    private String examCode;

    @SerializedName("subjectId")
    private Long subjectId;

    @SerializedName("questionCount")
    private int questionCount;

    @SerializedName("durationMinutes")
    private int durationMinutes;

    @SerializedName("pricingType")
    private String pricingType;

    private String difficulty;
    private String tags;

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getExamCode() { return examCode; }
    public Long getSubjectId() { return subjectId; }
    public int getQuestionCount() { return questionCount; }
    public int getDurationMinutes() { return durationMinutes; }
    public String getPricingType() { return pricingType; }
    public String getDifficulty() { return difficulty; }
    public String getTags() { return tags; }
}
