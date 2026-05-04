package com.vsatcompass.api.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Comment is required (non-null, non-blank) for REQUEST_REVISION and REJECT.
 * Optional for APPROVE.
 * The required-comment rule is enforced in the service layer because it depends on the action.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewActionRequest {

    @Size(max = 2000)
    private String comment;
}
