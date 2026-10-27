package com.vsatcompass.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PracticeGradeResponse {

    private Long questionId;
    private Long selectedOptionId;
    private boolean correct;
    private List<Long> correctOptionIds;
    private String explanation;
}
