package com.vsatcompass.api.dto.response;

import com.vsatcompass.api.entity.enums.Difficulty;
import com.vsatcompass.api.entity.enums.QuestionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamQuestionDetailResponse {
    private Long id;
    private Long examId;
    private Long questionId;
    private Integer questionOrder;
    private String questionCode;
    private String questionText;
    private QuestionType questionType;
    private Difficulty difficulty;
    private BigDecimal maxRawScore;
    private List<OptionItem> options;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OptionItem {
        private Long id;
        private String optionLabel;
        private String optionText;
        private Boolean isCorrect;
    }
}
