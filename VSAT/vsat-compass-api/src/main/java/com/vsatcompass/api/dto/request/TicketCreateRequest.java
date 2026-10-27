package com.vsatcompass.api.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TicketCreateRequest {
    private Long questionId;

    @Size(max = 5000, message = "message khong duoc vuot qua 5000 ky tu")
    private String message;
}
