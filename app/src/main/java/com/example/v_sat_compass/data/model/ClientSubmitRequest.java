package com.example.v_sat_compass.data.model;

import java.util.List;
import java.util.Map;

public class ClientSubmitRequest {
    private Double score;
    private Integer correctCount;
    private int totalQuestions;
    private int timeSpentSeconds;
    private List<AnswerItem> answers;

    public static class AnswerItem {
        private long questionId;
        private Long selectedOptionId;
        private Integer questionOrder;
        private Boolean bookmarked;
        private Map<String, Object> answerPayload;

        public AnswerItem(long questionId, Long selectedOptionId, Integer questionOrder, Boolean bookmarked) {
            this(questionId, selectedOptionId, questionOrder, bookmarked, null);
        }

        public AnswerItem(
                long questionId,
                Long selectedOptionId,
                Integer questionOrder,
                Boolean bookmarked,
                Map<String, Object> answerPayload
        ) {
            this.questionId = questionId;
            this.selectedOptionId = selectedOptionId;
            this.questionOrder = questionOrder;
            this.bookmarked = bookmarked;
            this.answerPayload = answerPayload;
        }
    }

    public ClientSubmitRequest(
            Double score,
            Integer correctCount,
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
