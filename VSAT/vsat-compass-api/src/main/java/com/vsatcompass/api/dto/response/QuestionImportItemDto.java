package com.vsatcompass.api.dto.response;

import com.vsatcompass.api.entity.enums.Difficulty;
import com.vsatcompass.api.entity.enums.QuestionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionImportItemDto {
    private String sheetName;
    private int rowNumber;
    private String groupCode;
    private String groupTitle;
    private String passageText;
    private Long subjectId;
    private String subjectCode;
    private Long topicId;
    private Long subtopicId;
    private Difficulty difficulty;
    private QuestionType questionType;
    private String questionText;
    private List<String> options;
    private String correctAnswer;
    private String explanation;
    private String tags;
    private boolean valid;
    private String error;
}
