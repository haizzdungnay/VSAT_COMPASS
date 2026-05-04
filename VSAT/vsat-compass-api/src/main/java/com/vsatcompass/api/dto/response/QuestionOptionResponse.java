package com.vsatcompass.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionOptionResponse {

    private Long id;
    private String optionLabel;
    private String optionText;
    private String optionHtml;
    private String imageUrl;
    private Boolean isCorrect;
    private Integer displayOrder;
}
