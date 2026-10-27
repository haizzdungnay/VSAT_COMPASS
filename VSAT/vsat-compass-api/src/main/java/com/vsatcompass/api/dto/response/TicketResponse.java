package com.vsatcompass.api.dto.response;

import com.vsatcompass.api.entity.enums.TicketStatus;
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
public class TicketResponse {
    private Long id;
    private Long questionId;
    private Long reporterUserId;
    private String message;
    private TicketStatus status;
    private String resolverNote;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
