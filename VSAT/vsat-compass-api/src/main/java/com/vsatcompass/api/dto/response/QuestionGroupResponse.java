package com.vsatcompass.api.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QuestionGroupResponse {
    private Long id;
    private String code;
    private Long subjectId;
    private String title;
    private String passageText;
    private String passageHtml;
    private String imageUrl;
    private String audioUrl;
    private Integer audioDurationSec;
    private Integer maxPlays;
}
