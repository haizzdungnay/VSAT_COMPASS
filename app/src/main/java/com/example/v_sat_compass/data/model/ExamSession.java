package com.example.v_sat_compass.data.model;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class ExamSession {
    private Long id;

    @SerializedName("exam_id")
    private Long examId;

    @SerializedName("exam_title")
    private String examTitle;

    private String status;

    @SerializedName("total_questions")
    private int totalQuestions;

    @SerializedName("correct_answers")
    private int correctAnswers;

    @SerializedName("score_percentage")
    private double scorePercentage;

    @SerializedName("time_spent_seconds")
    private int timeSpentSeconds;

    private List<SessionAnswer> answers;

    public Long getId() { return id; }
    public Long getExamId() { return examId; }
    public String getExamTitle() { return examTitle; }
    public String getStatus() { return status; }
    public int getTotalQuestions() { return totalQuestions; }
    public int getCorrectAnswers() { return correctAnswers; }
    public double getScorePercentage() { return scorePercentage; }
    public int getTimeSpentSeconds() { return timeSpentSeconds; }
    public List<SessionAnswer> getAnswers() { return answers; }

    public static class SessionAnswer {
        @SerializedName("question_id")
        private Long questionId;

        @SerializedName("selected_option_id")
        private Long selectedOptionId;

        @SerializedName("is_correct")
        private boolean isCorrect;

        @SerializedName("is_bookmarked")
        private boolean isBookmarked;

        @SerializedName("time_spent_seconds")
        private int timeSpentSeconds;

        public Long getQuestionId() { return questionId; }
        public Long getSelectedOptionId() { return selectedOptionId; }
        public boolean isCorrect() { return isCorrect; }
        public boolean isBookmarked() { return isBookmarked; }
        public int getTimeSpentSeconds() { return timeSpentSeconds; }
    }
}
