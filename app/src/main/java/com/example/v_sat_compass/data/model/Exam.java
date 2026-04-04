package com.example.v_sat_compass.data.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class Exam {
    private Long id;
    private String title;
    private String description;

    @SerializedName("exam_code")
    private String examCode;

    @SerializedName("subject_name")
    private String subjectName;

    @SerializedName("total_questions")
    private int totalQuestions;

    @SerializedName("duration_minutes")
    private int durationMinutes;

    @SerializedName("passing_score")
    private double passingScore;

    private String status;

    private List<ExamQuestion> questions;

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getExamCode() { return examCode; }
    public String getSubjectName() { return subjectName; }
    public int getTotalQuestions() { return totalQuestions; }
    public int getDurationMinutes() { return durationMinutes; }
    public double getPassingScore() { return passingScore; }
    public String getStatus() { return status; }
    public List<ExamQuestion> getQuestions() { return questions; }

    public static class ExamQuestion {
        @SerializedName("question_id")
        private Long questionId;

        @SerializedName("question_code")
        private String questionCode;

        @SerializedName("question_order")
        private int questionOrder;

        public Long getQuestionId() { return questionId; }
        public String getQuestionCode() { return questionCode; }
        public int getQuestionOrder() { return questionOrder; }
    }
}
