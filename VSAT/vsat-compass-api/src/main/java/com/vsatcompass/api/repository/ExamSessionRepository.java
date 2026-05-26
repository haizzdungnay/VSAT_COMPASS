package com.vsatcompass.api.repository;

import com.vsatcompass.api.entity.ExamSession;
import com.vsatcompass.api.entity.enums.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
public interface ExamSessionRepository extends JpaRepository<ExamSession, Long> {

    Optional<ExamSession> findByIdAndUserId(Long id, Long userId);

    long countByStatusAndSubmittedAtBetween(
            SessionStatus status,
            OffsetDateTime start,
            OffsetDateTime end
    );

    @Query("""
            SELECT COUNT(s) FROM ExamSession s
            WHERE s.status = :status
              AND s.submittedAt >= :dayStart
              AND s.submittedAt < :dayEnd
            """)
    long countSubmittedOnDay(
            @Param("status") SessionStatus status,
            @Param("dayStart") OffsetDateTime dayStart,
            @Param("dayEnd") OffsetDateTime dayEnd
    );
}
