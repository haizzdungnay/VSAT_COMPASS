package com.vsatcompass.api.repository;

import com.vsatcompass.api.entity.ExamSession;
import com.vsatcompass.api.entity.enums.SessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
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

    /**
     * F9 — per-user aggregate over submitted sessions.
     * Returns {@code [userId, totalScore, examCount]} for everyone who has
     * submitted at least one session. {@code totalScore} sums {@code score}
     * (V-SAT points; null rows are skipped). {@code examCount} counts the
     * sessions that contributed to the sum.
     *
     * The leaderboard service filters by {@code leaderboard_opt_in = true}
     * (kept on the users table to keep this query simple + indexable).
     */
    @Query(value = """
            SELECT user_id AS "userId",
                   COALESCE(SUM(score), 0) AS "totalScore",
                   COUNT(id) AS "examCount"
            FROM exam_sessions
            WHERE status = 'SUBMITTED'
              AND score IS NOT NULL
            GROUP BY user_id
            """, nativeQuery = true)
    List<UserScoreAggregate> aggregateScoresByUser();
}
