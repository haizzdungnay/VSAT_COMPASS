package com.vsatcompass.api.dto.request;

import com.vsatcompass.api.entity.enums.Difficulty;
import com.vsatcompass.api.entity.enums.QuestionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateQuestionRequest {

    @NotNull(message = "subjectId is required")
    private Long subjectId;

    @NotNull(message = "topicId is required")
    private Long topicId;

    private Long subtopicId;

    @NotNull(message = "difficulty is required")
    private Difficulty difficulty;

    @NotNull(message = "questionType is required")
    private QuestionType questionType;

    @NotBlank(message = "questionText is required")
    @Size(max = 5000)
    private String questionText;

    @Size(max = 5000)
    private String questionHtml;

    @Size(max = 500)
    private String imageUrl;

    @Size(max = 5000)
    private String explanation;

    @Size(max = 5000)
    private String explanationHtml;

    @Size(max = 200)
    private String source;

    @Size(max = 500)
    private String tags;

    @NotEmpty(message = "options required (>=2 for SINGLE_CHOICE/MULTIPLE_CHOICE/TRUE_FALSE)")
    @Size(min = 2, max = 10, message = "options must have 2 to 10 entries")
    @Valid
    private List<OptionInput> options;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OptionInput {

        @NotBlank(message = "optionLabel required")
        @Size(max = 5)
        private String optionLabel;

        @NotBlank(message = "optionText required")
        @Size(max = 2000)
        private String optionText;

        @Size(max = 2000)
        private String optionHtml;

        @Size(max = 500)
        private String imageUrl;

        @NotNull(message = "isCorrect required")
        private Boolean isCorrect;

        @NotNull(message = "displayOrder required")
        private Integer displayOrder;
    }
}
