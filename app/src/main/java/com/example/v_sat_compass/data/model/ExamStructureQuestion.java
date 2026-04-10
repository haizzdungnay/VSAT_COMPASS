package com.example.v_sat_compass.data.model;

import com.google.gson.annotations.SerializedName;

/**
 * Một dòng trong cấu trúc đề thi khi tạo đề (AdminCreateExamActivity).
 */
public class ExamStructureQuestion {

    @SerializedName("question_id")
    private Long questionId;

    @SerializedName("question_code")
    private String questionCode;

    @SerializedName("preview_content")
    private String previewContent;

    @SerializedName("question_type")
    private String questionType;

    @SerializedName("question_order")
    private int questionOrder;

    private int points; // điểm cho câu này

    public ExamStructureQuestion() {}

    public ExamStructureQuestion(Long questionId, String questionCode,
                                  String previewContent, String questionType,
                                  int questionOrder, int points) {
        this.questionId     = questionId;
        this.questionCode   = questionCode;
        this.previewContent = previewContent;
        this.questionType   = questionType;
        this.questionOrder  = questionOrder;
        this.points         = points;
    }

    public Long getQuestionId()        { return questionId; }
    public String getQuestionCode()    { return questionCode; }
    public String getPreviewContent()  { return previewContent; }
    public String getQuestionType()    { return questionType; }
    public int getQuestionOrder()      { return questionOrder; }
    public int getPoints()             { return points; }

    public void setQuestionOrder(int order) { this.questionOrder = order; }
    public void setPoints(int points)       { this.points = points; }

    public String getQuestionTypeLabel() {
        if (questionType == null) return "Trắc nghiệm";
        return "SHORT_ANSWER".equals(questionType) ? "Tự luận" : "Trắc nghiệm";
    }
}
