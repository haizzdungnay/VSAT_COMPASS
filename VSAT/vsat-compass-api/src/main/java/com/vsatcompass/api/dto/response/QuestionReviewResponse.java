package com.vsatcompass.api.dto.response;

import com.vsatcompass.api.entity.enums.ReviewAction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionReviewResponse {

    private Long id;
    private Long reviewerId;
    private ReviewAction action;
    private String comment;
    private Integer versionReviewed;
    private OffsetDateTime createdAt;
}
