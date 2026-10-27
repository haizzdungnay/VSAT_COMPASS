package com.vsatcompass.api.dto.request;

import com.vsatcompass.api.entity.enums.TicketStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TicketUpdateRequest {
    @NotNull(message = "status khong duoc de trong")
    private TicketStatus status;

    @Size(max = 5000, message = "resolverNote khong duoc vuot qua 5000 ky tu")
    private String resolverNote;
}
