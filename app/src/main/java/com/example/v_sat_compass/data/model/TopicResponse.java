package com.example.v_sat_compass.data.model;

import com.google.gson.annotations.SerializedName;

public class TopicResponse {
    @SerializedName("id")
    private Long id;

    @SerializedName("subjectId")
    private Long subjectId;

    @SerializedName("code")
    private String code;

    @SerializedName("name")
    private String name;

    @SerializedName("description")
    private String description;

    @SerializedName("displayOrder")
    private Integer displayOrder;

    public TopicResponse() {
    }

    public TopicResponse(
            Long id,
            Long subjectId,
            String code,
            String name,
            String description,
            Integer displayOrder
    ) {
        this.id = id;
        this.subjectId = subjectId;
        this.code = code;
        this.name = name;
        this.description = description;
        this.displayOrder = displayOrder;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(Long subjectId) {
        this.subjectId = subjectId;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }
}
