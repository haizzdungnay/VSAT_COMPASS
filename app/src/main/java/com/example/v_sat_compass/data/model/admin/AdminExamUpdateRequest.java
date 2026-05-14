package com.example.v_sat_compass.data.model.admin;

import com.google.gson.annotations.SerializedName;

import java.math.BigDecimal;

public class AdminExamUpdateRequest {
    @SerializedName("title")
    private String title;

    @SerializedName("subjectId")
    private Long subjectId;

    @SerializedName("description")
    private String description;

    @SerializedName("durationMinutes")
    private Integer durationMinutes;

    @SerializedName("difficulty")
    private String difficulty;

    @SerializedName("pricingType")
    private String pricingType;

    @SerializedName("price")
    private BigDecimal price;

    @SerializedName("tags")
    private String tags;

    public AdminExamUpdateRequest() {
    }

    public AdminExamUpdateRequest(
            String title,
            Long subjectId,
            String description,
            Integer durationMinutes,
            String difficulty,
            String pricingType,
            BigDecimal price,
            String tags
    ) {
        this.title = title;
        this.subjectId = subjectId;
        this.description = description;
        this.durationMinutes = durationMinutes;
        this.difficulty = difficulty;
        this.pricingType = pricingType;
        this.price = price;
        this.tags = tags;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }
}
