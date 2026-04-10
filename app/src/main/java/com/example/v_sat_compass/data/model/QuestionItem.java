package com.example.v_sat_compass.data.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class QuestionItem {

    private Long id;

    @SerializedName("question_code")
    private String questionCode;

    private String content;

    @SerializedName("subject_name")
    private String subjectName;

    @SerializedName("topic_name")
    private String topicName;

    @SerializedName("question_type")
    private String questionType; // MULTIPLE_CHOICE, SHORT_ANSWER

    private String difficulty; // EASY, MEDIUM, HARD

    private String status; // DRAFT, PENDING, APPROVED, PUBLISHED, NEEDS_REVISION

    @SerializedName("creator_name")
    private String creatorName;

    @SerializedName("created_at")
    private String createdAt;

    @SerializedName("view_count")
    private int viewCount;

    @SerializedName("flag_count")
    private int flagCount;

    private List<Option> options;

    @SerializedName("correct_answer")
    private String correctAnswer;

    private String explanation;

    @SerializedName("admin_comment")
    private String adminComment;

    public static class Option {
        private String label;   // A, B, C, D
        private String content;
        public String getLabel()   { return label; }
        public String getContent() { return content; }
    }

    public Long getId()             { return id; }
    public String getQuestionCode() { return questionCode; }
    public String getContent()      { return content; }
    public String getSubjectName()  { return subjectName; }
    public String getTopicName()    { return topicName; }
    public String getQuestionType() { return questionType; }
    public String getDifficulty()   { return difficulty; }
    public String getStatus()       { return status; }
    public String getCreatorName()  { return creatorName; }
    public String getCreatedAt()    { return createdAt; }
    public int getViewCount()       { return viewCount; }
    public int getFlagCount()       { return flagCount; }
    public List<Option> getOptions(){ return options; }
    public String getCorrectAnswer(){ return correctAnswer; }
    public String getExplanation()  { return explanation; }
    public String getAdminComment() { return adminComment; }

    public void setStatus(String status) { this.status = status; }

    /** Nhãn trạng thái tiếng Việt */
    public String getStatusLabel() {
        if (status == null) return "Nháp";
        switch (status) {
            case "PENDING":        return "Chờ duyệt";
            case "APPROVED":       return "Đã duyệt";
            case "PUBLISHED":      return "Đã xuất bản";
            case "NEEDS_REVISION": return "Cần sửa";
            default:               return "Nháp";
        }
    }

    /** Màu badge trạng thái */
    public int getStatusColor() {
        if (status == null) return 0xFF9E9E9E;
        switch (status) {
            case "PENDING":        return 0xFFF39C12;
            case "APPROVED":       return 0xFF27AE60;
            case "PUBLISHED":      return 0xFF4A3ABA;
            case "NEEDS_REVISION": return 0xFFE74C3C;
            default:               return 0xFF9E9E9E;
        }
    }

    /** Nhãn độ khó tiếng Việt */
    public String getDifficultyLabel() {
        if (difficulty == null) return "Trung bình";
        switch (difficulty) {
            case "EASY":   return "Dễ";
            case "HARD":   return "Khó";
            default:       return "Trung bình";
        }
    }

    /** Nhãn loại câu hỏi tiếng Việt */
    public String getQuestionTypeLabel() {
        if (questionType == null) return "Trắc nghiệm";
        return "SHORT_ANSWER".equals(questionType) ? "Tự luận" : "Trắc nghiệm";
    }
}
