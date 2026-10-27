package com.vsatcompass.api.service;

import com.vsatcompass.api.dto.request.TicketCreateRequest;
import com.vsatcompass.api.dto.request.TicketUpdateRequest;
import com.vsatcompass.api.dto.response.TicketResponse;
import com.vsatcompass.api.entity.Ticket;
import com.vsatcompass.api.entity.enums.TicketStatus;
import com.vsatcompass.api.exception.AppException;
import com.vsatcompass.api.repository.TicketRepository;
import com.vsatcompass.api.service.impl.TicketServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TicketService - F5 question reports")
class TicketServiceTest {

    @Mock TicketRepository ticketRepository;
    @InjectMocks TicketServiceImpl ticketService;

    @Test
    @DisplayName("create: student report creates OPEN ticket")
    void create_validRequest_returnsOpenTicket() {
        TicketCreateRequest request = new TicketCreateRequest();
        request.setQuestionId(11L);
        request.setMessage(" Sai dap an ");
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket ticket = invocation.getArgument(0);
            ticket.setId(1L);
            ticket.setCreatedAt(OffsetDateTime.parse("2026-06-18T00:00:00Z"));
            ticket.setUpdatedAt(ticket.getCreatedAt());
            return ticket;
        });

        TicketResponse result = ticketService.create(99L, request);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getQuestionId()).isEqualTo(11L);
        assertThat(result.getReporterUserId()).isEqualTo(99L);
        assertThat(result.getMessage()).isEqualTo("Sai dap an");
        assertThat(result.getStatus()).isEqualTo(TicketStatus.OPEN);
    }

    @Test
    @DisplayName("create: blank message is BAD_REQUEST")
    void create_blankMessage_throwsBadRequest() {
        TicketCreateRequest request = new TicketCreateRequest();
        request.setMessage("  ");

        assertThatThrownBy(() -> ticketService.create(99L, request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> assertThat(((AppException) ex).getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("list: status filter calls filtered repository")
    void list_withStatus_usesFilter() {
        PageRequest pageable = PageRequest.of(0, 20);
        when(ticketRepository.findByStatusOrderByCreatedAtDesc(TicketStatus.OPEN, pageable))
                .thenReturn(new PageImpl<>(List.of(ticket(1L, TicketStatus.OPEN)), pageable, 1));

        Page<TicketResponse> result = ticketService.list(TicketStatus.OPEN, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(TicketStatus.OPEN);
        verify(ticketRepository).findByStatusOrderByCreatedAtDesc(TicketStatus.OPEN, pageable);
    }

    @Test
    @DisplayName("update: OPEN to IN_PROGRESS succeeds")
    void update_openToInProgress_succeeds() {
        Ticket ticket = ticket(1L, TicketStatus.OPEN);
        TicketUpdateRequest request = new TicketUpdateRequest();
        request.setStatus(TicketStatus.IN_PROGRESS);
        request.setResolverNote("Dang kiem tra");
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TicketResponse result = ticketService.update(1L, request);

        assertThat(result.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
        assertThat(result.getResolverNote()).isEqualTo("Dang kiem tra");
    }

    @Test
    @DisplayName("update: CLOSED terminal transition is rejected")
    void update_closedToOpen_rejected() {
        TicketUpdateRequest request = new TicketUpdateRequest();
        request.setStatus(TicketStatus.OPEN);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket(1L, TicketStatus.CLOSED)));

        assertThatThrownBy(() -> ticketService.update(1L, request))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException appException = (AppException) ex;
                    assertThat(appException.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(appException.getCode()).isEqualTo("INVALID_STATE");
                });
    }

    private Ticket ticket(Long id, TicketStatus status) {
        OffsetDateTime now = OffsetDateTime.parse("2026-06-18T00:00:00Z");
        return Ticket.builder()
                .id(id)
                .questionId(11L)
                .reporterUserId(99L)
                .message("Sai dap an")
                .status(status)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}
