package com.vsatcompass.api.service.impl;

import com.vsatcompass.api.dto.request.TicketCreateRequest;
import com.vsatcompass.api.dto.request.TicketUpdateRequest;
import com.vsatcompass.api.dto.response.TicketResponse;
import com.vsatcompass.api.entity.Ticket;
import com.vsatcompass.api.entity.enums.TicketStatus;
import com.vsatcompass.api.exception.AppException;
import com.vsatcompass.api.repository.TicketRepository;
import com.vsatcompass.api.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;

    @Override
    @Transactional
    public TicketResponse create(Long reporterUserId, TicketCreateRequest request) {
        if (reporterUserId == null) {
            throw AppException.authUnauthorized();
        }
        String message = request != null ? request.getMessage() : null;
        if (message == null || message.trim().isEmpty()) {
            throw AppException.badRequest("Noi dung bao loi khong duoc de trong");
        }
        Ticket ticket = Ticket.builder()
                .questionId(request.getQuestionId())
                .reporterUserId(reporterUserId)
                .message(message.trim())
                .status(TicketStatus.OPEN)
                .build();
        return toResponse(ticketRepository.save(ticket));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<TicketResponse> list(TicketStatus status, Pageable pageable) {
        Page<Ticket> tickets = status != null
                ? ticketRepository.findByStatusOrderByCreatedAtDesc(status, pageable)
                : ticketRepository.findAllByOrderByCreatedAtDesc(pageable);
        return tickets.map(this::toResponse);
    }

    @Override
    @Transactional
    public TicketResponse update(Long id, TicketUpdateRequest request) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Ticket", id));
        TicketStatus next = request.getStatus();
        if (!isValidTransition(ticket.getStatus(), next)) {
            throw AppException.invalidState("Trang thai ticket khong hop le: "
                    + ticket.getStatus() + " -> " + next);
        }
        ticket.setStatus(next);
        ticket.setResolverNote(request.getResolverNote());
        return toResponse(ticketRepository.save(ticket));
    }

    private boolean isValidTransition(TicketStatus current, TicketStatus next) {
        if (current == null || next == null) return false;
        if (current == next) return true;
        if (current == TicketStatus.CLOSED) return false;
        if (current == TicketStatus.OPEN) {
            return next == TicketStatus.IN_PROGRESS || next == TicketStatus.CLOSED;
        }
        return current == TicketStatus.IN_PROGRESS && next == TicketStatus.CLOSED;
    }

    private TicketResponse toResponse(Ticket ticket) {
        return TicketResponse.builder()
                .id(ticket.getId())
                .questionId(ticket.getQuestionId())
                .reporterUserId(ticket.getReporterUserId())
                .message(ticket.getMessage())
                .status(ticket.getStatus())
                .resolverNote(ticket.getResolverNote())
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .build();
    }
}
