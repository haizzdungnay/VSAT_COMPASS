package com.example.v_sat_compass.data.model.question;

import com.example.v_sat_compass.data.model.enums.Difficulty;
import com.example.v_sat_compass.data.model.enums.QuestionStatus;
import com.example.v_sat_compass.data.model.enums.QuestionType;
import com.google.gson.annotations.SerializedName;

public class QuestionListItemResponse {
    @SerializedName("id")
    private Long id;

    @SerializedName("questionCode")
    private String questionCode;

    @SerializedName("subjectId")
    private Long subjectId;

    @SerializedName("topicId")
    private Long topicId;

    @SerializedName("difficulty")
    private Difficulty difficulty;

    @SerializedName("questionType")
    private QuestionType questionType;

    @SerializedName("status")
    private QuestionStatus status;

    @SerializedName("version")
    private Integer version;

    @SerializedName("createdBy")
    private Long createdBy;

    @SerializedName("updatedAt")
    private String updatedAt;

    public QuestionListItemResponse() {
    }

    public QuestionListItemResponse(
            Long id,
            String questionCode,
            Long subjectId,
            Long topicId,
            Difficulty difficulty,
            QuestionType questionType,
            QuestionStatus status,
            Integer version,
            Long createdBy,
            String updatedAt
    ) {
        this.id = id;
        this.questionCode = questionCode;
        this.subjectId = subjectId;
        this.topicId = topicId;
        this.difficulty = difficulty;
        this.questionType = questionType;
        this.status = status;
        this.version = version;
        this.createdBy = createdBy;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getQuestionCode() {
        return questionCode;
    }

    public void setQuestionCode(String questionCode) {
        this.questionCode = questionCode;
    }

    public Long getSubjectId() {
        return subjectId;
    }

    public void setSubjectId(Long subjectId) {
        this.subjectId = subjectId;
    }

    public Long getTopicId() {
        return topicId;
    }

    public void setTopicId(Long topicId) {
        this.topicId = topicId;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
    }

    public QuestionType getQuestionType() {
        return questionType;
    }

    public void setQuestionType(QuestionType questionType) {
        this.questionType = questionType;
    }

    public QuestionStatus getStatus() {
        return status;
    }

    public void setStatus(QuestionStatus status) {
        this.status = status;
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

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
}
