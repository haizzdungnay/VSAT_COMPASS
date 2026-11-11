package com.vsatcompass.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionAnswerResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long questionId;
    private Integer questionOrder;
    private Long selectedOptionId;
    private Map<String, Object> answerPayload;
    private Boolean bookmarked;
    private Integer timeSpentSeconds;
}
