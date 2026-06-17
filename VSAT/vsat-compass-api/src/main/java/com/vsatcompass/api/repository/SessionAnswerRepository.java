package com.vsatcompass.api.repository;

import com.vsatcompass.api.entity.SessionAnswer;
import com.vsatcompass.api.repository.projection.TopicStatsProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SessionAnswerRepository extends JpaRepository<SessionAnswer, Long> {

    @Modifying
    void deleteBySessionId(Long sessionId);

    @org.springframework.data.jpa.repository.Query("""
            SELECT sa FROM SessionAnswer sa
            WHERE sa.sessionId = :sessionId
            """)
    List<SessionAnswer> findBySessionId(@Param("sessionId") Long sessionId);

    @org.springframework.data.jpa.repository.Query("""
            SELECT sa FROM SessionAnswer sa
            WHERE sa.sessionId = :sessionId AND sa.questionId = :questionId
            """)
    java.util.Optional<SessionAnswer> findBySessionIdAndQuestionId(
            @Param("sessionId") Long sessionId,
            @Param("questionId") Long questionId
    );

    @org.springframework.data.jpa.repository.Query("""
            SELECT COUNT(sa) FROM SessionAnswer sa
            WHERE sa.sessionId = :sessionId
            """)
    long countBySessionId(@Param("sessionId") Long sessionId);

    @Query("""
            SELECT t.id AS topicId,
                   t.name AS topicName,
                   COUNT(sa.id) AS totalAttempts,
                   SUM(CASE WHEN sa.isCorrect = true THEN 1 ELSE 0 END) AS correctCount
            FROM SessionAnswer sa
            JOIN Question q ON q.id = sa.questionId
            JOIN Topic t ON t.id = q.topicId
            JOIN ExamSession es ON es.id = sa.sessionId
            WHERE es.userId = :userId
            GROUP BY t.id, t.name
            ORDER BY t.displayOrder ASC, t.name ASC
            """)
    List<TopicStatsProjection> aggregateTopicStatsByUserId(@Param("userId") Long userId);
}
