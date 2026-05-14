package com.example.v_sat_compass.data.model.admin;

import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;

public class AdminExamSummaryResponse {
    @SerializedName("id")
    private Long id;

    @SerializedName("examCode")
    private String examCode;

    @SerializedName("title")
    private String title;

    @SerializedName("subjectId")
    private Long subjectId;

    @SerializedName("questionCount")
    private Integer questionCount;

    @SerializedName("durationMinutes")
    private Integer durationMinutes;

    @SerializedName("difficulty")
    private String difficulty;

    @SerializedName("pricingType")
    private String pricingType;

    @SerializedName("price")
    private BigDecimal price;

    @SerializedName("status")
    private String status;

    @SerializedName("version")
    private Integer version;

    @SerializedName("updatedAt")
    private String updatedAt;

    public AdminExamSummaryResponse() {
    }

    public AdminExamSummaryResponse(
            Long id,
            String examCode,
            String title,
            Long subjectId,
            Integer questionCount,
            Integer durationMinutes,
            String difficulty,
            String pricingType,
            BigDecimal price,
            String status,
            Integer version,
            String updatedAt
    ) {
        this.id = id;
        this.examCode = examCode;
        this.title = title;
        this.subjectId = subjectId;
        this.questionCount = questionCount;
        this.durationMinutes = durationMinutes;
        this.difficulty = difficulty;
        this.pricingType = pricingType;
        this.price = price;
        this.status = status;
        this.version = version;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getExamCode() {
        return examCode;
    }

    public void setExamCode(String examCode) {
        this.examCode = examCode;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Long getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(Long subjectId) {
        this.subjectId = subjectId;
    }

    public Integer getQuestionCount() {
        return questionCount;
    }

    public void setQuestionCount(Integer questionCount) {
        this.questionCount = questionCount;
    }

    public Integer getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Integer durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public String getPricingType() {
        return pricingType;
    }

    public void setPricingType(String pricingType) {
        this.pricingType = pricingType;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
