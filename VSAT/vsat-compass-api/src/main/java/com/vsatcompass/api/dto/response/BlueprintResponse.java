package com.vsatcompass.api.dto.response;

import com.vsatcompass.api.entity.enums.CognitiveLevel;
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
public class BlueprintResponse {
    private Long id;
    private String code;
    private String name;
    private Long subjectId;
    private String subjectCode;
    private String subjectName;
    private Integer totalQuestions;
    private BigDecimal maxRawScore;
    private Integer version;
    private List<BlueprintCellResponse> cells;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BlueprintCellResponse {
        private Long id;
        private Long topicId;
        private String topicName;
        private QuestionType questionType;
        private CognitiveLevel cognitiveLevel;
        private Difficulty difficulty;
        private Integer questionCount;
    }
}