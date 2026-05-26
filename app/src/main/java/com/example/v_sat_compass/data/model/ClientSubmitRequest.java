package com.example.v_sat_compass.data.model;

import java.util.List;

public class ClientSubmitRequest {
    private double score;
    private int correctCount;
    private int totalQuestions;
    private int timeSpentSeconds;
    private List<AnswerItem> answers;

    public static class AnswerItem {
        private long questionId;
        private Long selectedOptionId;
        private Integer questionOrder;
        private Boolean bookmarked;

        public AnswerItem(long questionId, Long selectedOptionId, Integer questionOrder, Boolean bookmarked) {
            this.questionId = questionId;
            this.selectedOptionId = selectedOptionId;
            this.questionOrder = questionOrder;
            this.bookmarked = bookmarked;
        }
    }

    public ClientSubmitRequest(
            double score,
            int correctCount,
            int totalQuestions,
            int timeSpentSeconds,
            List<AnswerItem> answers
    ) {
        this.score = score;
        this.correctCount = correctCount;
        this.totalQuestions = totalQuestions;
        this.timeSpentSeconds = timeSpentSeconds;
        this.answers = answers;
    }
}
