package com.vsatcompass.api.controller.student;

import com.vsatcompass.api.dto.common.ApiResponse;
import com.vsatcompass.api.dto.request.TicketCreateRequest;
import com.vsatcompass.api.dto.response.TicketResponse;
import com.vsatcompass.api.service.TicketService;
import com.vsatcompass.api.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @PostMapping
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<ApiResponse<TicketResponse>> create(
            @Valid @RequestBody TicketCreateRequest request) {
        TicketResponse result = ticketService.create(SecurityUtils.getCurrentUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(result, "Da gui bao loi"));
    }
}
