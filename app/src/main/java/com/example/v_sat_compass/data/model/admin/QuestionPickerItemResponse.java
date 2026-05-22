package com.example.v_sat_compass.data.model.admin;

import com.example.v_sat_compass.data.model.enums.Difficulty;
import com.example.v_sat_compass.data.model.enums.QuestionStatus;
import com.example.v_sat_compass.data.model.enums.QuestionType;
import com.google.gson.annotations.SerializedName;

public class QuestionPickerItemResponse {

    @SerializedName("id")
    private Long id;

    @SerializedName("questionCode")
    private String questionCode;

    @SerializedName("questionTextSnippet")
    private String questionTextSnippet;

    @SerializedName("subjectId")
    private Long subjectId;

    @SerializedName("topicId")
    private Long topicId;

    @SerializedName("subtopicId")
    private Long subtopicId;

    @SerializedName("questionType")
    private QuestionType questionType;

    @SerializedName("difficulty")
    private Difficulty difficulty;

    @SerializedName("status")
    private QuestionStatus status;

    @SerializedName("version")
    private Integer version;

    @SerializedName("updatedAt")
    private String updatedAt;

    @SerializedName("imageUrl")
    private String imageUrl;

    public QuestionPickerItemResponse() {
    }

    public QuestionPickerItemResponse(
            Long id,
            String questionCode,
            String questionTextSnippet,
            Long subjectId,
            Long topicId,
            Long subtopicId,
            QuestionType questionType,
            Difficulty difficulty,
            QuestionStatus status,
            Integer version,
            String updatedAt,
            String imageUrl
    ) {
        this.id = id;
        this.questionCode = questionCode;
        this.questionTextSnippet = questionTextSnippet;
        this.subjectId = subjectId;
        this.topicId = topicId;
        this.subtopicId = subtopicId;
        this.questionType = questionType;
        this.difficulty = difficulty;
        this.status = status;
        this.version = version;
        this.updatedAt = updatedAt;
        this.imageUrl = imageUrl;
    }

    public static Builder builder() {
        return new Builder();
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

    public String getQuestionTextSnippet() {
        return questionTextSnippet;
    }

    public void setQuestionTextSnippet(String questionTextSnippet) {
        this.questionTextSnippet = questionTextSnippet;
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

    public Long getSubtopicId() {
        return subtopicId;
    }

    public void setSubtopicId(Long subtopicId) {
        this.subtopicId = subtopicId;
    }

    public QuestionType getQuestionType() {
        return questionType;
    }

    public void setQuestionType(QuestionType questionType) {
        this.questionType = questionType;
    }

    public Difficulty getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(Difficulty difficulty) {
        this.difficulty = difficulty;
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

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public static class Builder {
        private Long id;
        private String questionCode;
        private String questionTextSnippet;
        private Long subjectId;
        private Long topicId;
        private Long subtopicId;
        private QuestionType questionType;
        private Difficulty difficulty;
        private QuestionStatus status;
        private Integer version;
        private String updatedAt;
        private String imageUrl;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder questionCode(String questionCode) {
            this.questionCode = questionCode;
            return this;
        }

        public Builder questionTextSnippet(String questionTextSnippet) {
            this.questionTextSnippet = questionTextSnippet;
            return this;
        }

        public Builder subjectId(Long subjectId) {
            this.subjectId = subjectId;
            return this;
        }

        public Builder topicId(Long topicId) {
            this.topicId = topicId;
            return this;
        }

        public Builder subtopicId(Long subtopicId) {
            this.subtopicId = subtopicId;
            return this;
        }

        public Builder questionType(QuestionType questionType) {
            this.questionType = questionType;
            return this;
        }

        public Builder difficulty(Difficulty difficulty) {
            this.difficulty = difficulty;
            return this;
        }

        public Builder status(QuestionStatus status) {
            this.status = status;
            return this;
        }

        public Builder version(Integer version) {
            this.version = version;
            return this;
        }

        public Builder updatedAt(String updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Builder imageUrl(String imageUrl) {
            this.imageUrl = imageUrl;
            return this;
        }

        public QuestionPickerItemResponse build() {
            return new QuestionPickerItemResponse(
                    id,
                    questionCode,
                    questionTextSnippet,
                    subjectId,
                    topicId,
                    subtopicId,
                    questionType,
                    difficulty,
                    status,
                    version,
                    updatedAt,
                    imageUrl
            );
        }
    }
}
