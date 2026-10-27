package com.vsatcompass.api.service;

import com.vsatcompass.api.dto.request.TicketCreateRequest;
import com.vsatcompass.api.dto.request.TicketUpdateRequest;
import com.vsatcompass.api.dto.response.TicketResponse;
import com.vsatcompass.api.entity.enums.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TicketService {
    TicketResponse create(Long reporterUserId, TicketCreateRequest request);
    Page<TicketResponse> list(TicketStatus status, Pageable pageable);
    TicketResponse update(Long id, TicketUpdateRequest request);
}
