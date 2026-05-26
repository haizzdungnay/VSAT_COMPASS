package com.example.v_sat_compass.data.model;

import com.google.gson.annotations.SerializedName;

public class PublicExamSummaryResponse {
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

    public void setId(Long id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setExamCode(String examCode) { this.examCode = examCode; }
    public void setSubjectId(Long subjectId) { this.subjectId = subjectId; }
    public void setQuestionCount(int questionCount) { this.questionCount = questionCount; }
    public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }
    public void setPricingType(String pricingType) { this.pricingType = pricingType; }
}
