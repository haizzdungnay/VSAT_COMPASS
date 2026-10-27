package com.vsatcompass.api.controller.admin;

import com.vsatcompass.api.dto.common.ApiResponse;
import com.vsatcompass.api.dto.request.TicketUpdateRequest;
import com.vsatcompass.api.dto.response.TicketResponse;
import com.vsatcompass.api.entity.enums.TicketStatus;
import com.vsatcompass.api.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/tickets")
@RequiredArgsConstructor
public class AdminTicketController {

    private final TicketService ticketService;

    @GetMapping
    @PreAuthorize("hasAnyRole('CONTENT_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Page<TicketResponse>>> list(
            @RequestParam(required = false) TicketStatus status,
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.success(ticketService.list(status, pageable)));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('CONTENT_ADMIN','SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<TicketResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody TicketUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(ticketService.update(id, request)));
    }
}
