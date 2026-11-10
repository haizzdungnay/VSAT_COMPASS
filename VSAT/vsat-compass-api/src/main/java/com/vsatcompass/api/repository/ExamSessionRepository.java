package com.vsatcompass.api.repository;

import com.vsatcompass.api.entity.ExamSession;
import com.vsatcompass.api.entity.enums.SessionStatus;
import org.springframework.data.domain.Pageable;
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

    long countByExamWindowIdAndStatus(Long examWindowId, SessionStatus status);

    boolean existsByExamWindowId(Long examWindowId);

    long countByUserIdAndExamWindowId(Long userId, Long examWindowId);

    @Query("SELECT s.examWindowId AS windowId, s.status AS status, COUNT(s) AS count FROM ExamSession s WHERE s.examWindowId IN :windowIds GROUP BY s.examWindowId, s.status")
    List<WindowSessionCount> countSessionsByWindowIds(@Param("windowIds") List<Long> windowIds);

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

    @Query(value = """
            SELECT es.user_id AS "userId",
                   COALESCE(SUM(es.score), 0) AS "totalScore",
                   COUNT(es.id) AS "examCount"
            FROM exam_sessions es
            JOIN exams e ON e.id = es.exam_id
            WHERE es.status = 'SUBMITTED'
              AND es.score IS NOT NULL
              AND e.subject_id = :subjectId
            GROUP BY es.user_id
            """, nativeQuery = true)
    List<UserScoreAggregate> aggregateScoresByUserAndSubject(
            @Param("subjectId") Long subjectId,
            Pageable pageable
    );
}
