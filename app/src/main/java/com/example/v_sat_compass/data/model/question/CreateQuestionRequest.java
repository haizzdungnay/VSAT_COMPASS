package com.example.v_sat_compass.data.model.question;

import com.example.v_sat_compass.data.model.enums.Difficulty;
import com.example.v_sat_compass.data.model.enums.QuestionType;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class CreateQuestionRequest {
    @SerializedName("subjectId")
    private Long subjectId;

    @SerializedName("topicId")
    private Long topicId;

    @SerializedName("subtopicId")
    private Long subtopicId;

    @SerializedName("difficulty")
    private Difficulty difficulty;

    @SerializedName("questionType")
    private QuestionType questionType;

    @SerializedName("questionText")
    private String questionText;

    @SerializedName("questionHtml")
    private String questionHtml;

    @SerializedName("imageUrl")
    private String imageUrl;

    @SerializedName("explanation")
    private String explanation;

    @SerializedName("explanationHtml")
    private String explanationHtml;

    @SerializedName("source")
    private String source;

    @SerializedName("tags")
    private String tags;

    @SerializedName("options")
    private List<QuestionOptionInput> options;

    public CreateQuestionRequest() {
    }

    public CreateQuestionRequest(
            Long subjectId,
            Long topicId,
            Long subtopicId,
            Difficulty difficulty,
            QuestionType questionType,
            String questionText,
            String questionHtml,
            String imageUrl,
            String explanation,
            String explanationHtml,
            String source,
            String tags,
            List<QuestionOptionInput> options
    ) {
        this.subjectId = subjectId;
        this.topicId = topicId;
        this.subtopicId = subtopicId;
        this.difficulty = difficulty;
        this.questionType = questionType;
        this.questionText = questionText;
        this.questionHtml = questionHtml;
        this.imageUrl = imageUrl;
        this.explanation = explanation;
        this.explanationHtml = explanationHtml;
        this.source = source;
        this.tags = tags;
        this.options = options;
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

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public String getQuestionHtml() {
        return questionHtml;
    }

    public void setQuestionHtml(String questionHtml) {
        this.questionHtml = questionHtml;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public String getExplanationHtml() {
        return explanationHtml;
    }

    public void setExplanationHtml(String explanationHtml) {
        this.explanationHtml = explanationHtml;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public List<QuestionOptionInput> getOptions() {
        return options;
    }

    public void setOptions(List<QuestionOptionInput> options) {
        this.options = options;
    }
}
