package com.vsatcompass.api.repository;

import com.vsatcompass.api.entity.Ticket;
import com.vsatcompass.api.entity.enums.TicketStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {
    Page<Ticket> findByStatusOrderByCreatedAtDesc(TicketStatus status, Pageable pageable);
    Page<Ticket> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
