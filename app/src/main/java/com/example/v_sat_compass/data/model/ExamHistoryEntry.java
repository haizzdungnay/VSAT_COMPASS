package com.example.v_sat_compass.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * Một bản ghi lịch sử bài thi. Mọi field có @SerializedName để Gson parse đúng
 * ngay cả khi code bị obfuscate hoặc field được đổi tên trong tương lai.
 * Thêm field mới? Chỉ cần khai báo với giá trị default — JSON cũ vẫn parse OK.
 */
public class ExamHistoryEntry {

    @SerializedName("id")
    private long id;            // timestamp ms khi tạo entry (unique key)

    @SerializedName("exam_id")
    private long examId;

    @SerializedName("exam_title")
    private String examTitle;

    @SerializedName("subject")
    private String subject;

    @SerializedName("total_questions")
    private int totalQuestions;

    @SerializedName("correct_count")
    private int correctCount;

    @SerializedName("score")
    private int score;          // V-SAT 1200 thang điểm

    @SerializedName("time_spent_seconds")
    private long timeSpentSeconds;

    @SerializedName("submitted_at_millis")
    private long submittedAtMillis;

    @SerializedName("selected_answers_json")
    private String selectedAnswersJson; // để xem lại review sau khi đóng app

    public ExamHistoryEntry() {}

    public ExamHistoryEntry(long examId, String examTitle, String subject,
                            int totalQuestions, int correctCount,
                            double scorePercent, long timeSpentSeconds,
                            String selectedAnswersJson) {
        this.id = System.currentTimeMillis();
        this.examId = examId;
        this.examTitle = examTitle != null ? examTitle : "Đề thi";
        this.subject = subject != null ? subject : "Chung";
        this.totalQuestions = totalQuestions;
        this.correctCount = correctCount;
        this.score = (int) (scorePercent * ScoreConstants.PERCENT_TO_VSAT);
        this.timeSpentSeconds = timeSpentSeconds;
        this.submittedAtMillis = System.currentTimeMillis();
        this.selectedAnswersJson = selectedAnswersJson != null ? selectedAnswersJson : "{}";
    }

    public long getId() { return id; }
    public long getExamId() { return examId; }
    public String getExamTitle() { return examTitle; }
    public String getSubject() { return subject; }
    public int getTotalQuestions() { return totalQuestions; }
    public int getCorrectCount() { return correctCount; }
    public int getScore() { return score; }
    public long getTimeSpentSeconds() { return timeSpentSeconds; }
    public long getSubmittedAtMillis() { return submittedAtMillis; }
    public String getSelectedAnswersJson() { return selectedAnswersJson; }

    /** Tỷ lệ đúng 0–100. */
    public double getAccuracyPercent() {
        if (totalQuestions == 0) return 0.0;
        return (correctCount * 100.0) / totalQuestions;
    }
}
