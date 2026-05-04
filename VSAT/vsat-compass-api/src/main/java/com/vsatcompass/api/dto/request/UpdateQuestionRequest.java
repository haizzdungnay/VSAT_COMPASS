package com.vsatcompass.api.dto.request;

import com.vsatcompass.api.entity.enums.Difficulty;
import com.vsatcompass.api.entity.enums.QuestionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Selective patch for an existing question.
 * Null/missing field = leave as-is. Non-null = replace.
 * When {@code options} is non-null, ALL options are replaced atomically.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateQuestionRequest {

    private Long subjectId;
    private Long topicId;
    private Long subtopicId;
    private Difficulty difficulty;
    private QuestionType questionType;

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

    @Size(min = 2, max = 10, message = "options must have 2 to 10 entries when provided")
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
