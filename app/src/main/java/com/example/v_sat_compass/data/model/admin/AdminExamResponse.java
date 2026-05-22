package com.example.v_sat_compass.data.model.admin;

import com.example.v_sat_compass.data.model.ExamStructureQuestion;
import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;
import java.util.List;

public class AdminExamResponse {
    @SerializedName("id")
    private Long id;

    @SerializedName("examCode")
    private String examCode;

    @SerializedName("title")
    private String title;

    @SerializedName("subjectId")
    private Long subjectId;

    @SerializedName("subjectCode")
    private String subjectCode;

    @SerializedName("description")
    private String description;

    @SerializedName("questionCount")
    private Integer questionCount;

    @SerializedName("questions")
    private List<ExamStructureQuestion> questions;

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

    @SerializedName("tags")
    private String tags;

    @SerializedName("publishDate")
    private String publishDate;

    @SerializedName("version")
    private Integer version;

    @SerializedName("createdBy")
    private Long createdBy;

    @SerializedName("reviewedBy")
    private Long reviewedBy;

    @SerializedName("createdAt")
    private String createdAt;

    @SerializedName("updatedAt")
    private String updatedAt;

    public AdminExamResponse() {
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

    public String getSubjectCode() {
        return subjectCode;
    }

    public void setSubjectCode(String subjectCode) {
        this.subjectCode = subjectCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getQuestionCount() {
        return questionCount;
    }

    public void setQuestionCount(Integer questionCount) {
        this.questionCount = questionCount;
    }

    public List<ExamStructureQuestion> getQuestions() {
        return questions;
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

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getPublishDate() {
        return publishDate;
    }

    public void setPublishDate(String publishDate) {
        this.publishDate = publishDate;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public Long getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Long createdBy) {
        this.createdBy = createdBy;
    }

    public Long getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(Long reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
