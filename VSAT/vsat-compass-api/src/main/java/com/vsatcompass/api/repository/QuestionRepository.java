package com.vsatcompass.api.repository;

import com.vsatcompass.api.entity.Question;
import com.vsatcompass.api.entity.enums.Difficulty;
import com.vsatcompass.api.entity.enums.QuestionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long>,
        org.springframework.data.jpa.repository.JpaSpecificationExecutor<Question> {

    Optional<Question> findByQuestionCode(String questionCode);

    Page<Question> findByCreatedByOrderByUpdatedAtDesc(Long createdBy, Pageable pageable);

    Page<Question> findByCreatedByAndStatusOrderByUpdatedAtDesc(Long createdBy, QuestionStatus status, Pageable pageable);

    Page<Question> findAllByOrderByUpdatedAtDesc(Pageable pageable);

    Page<Question> findByStatusOrderByUpdatedAtDesc(QuestionStatus status, Pageable pageable);

    long countByStatus(QuestionStatus status);

    @Query("""
            select q from Question q
            where q.status in :statuses
              and q.difficulty = :difficulty
              and (:subtopicId is null or q.subtopicId = :subtopicId)
              and (:topicId is null or q.topicId = :topicId)
            order by q.updatedAt desc, q.id desc
            """)
    List<Question> findPracticeQuestions(
            @Param("subtopicId") Long subtopicId,
            @Param("topicId") Long topicId,
            @Param("difficulty") Difficulty difficulty,
            @Param("statuses") Collection<QuestionStatus> statuses,
            Pageable pageable);
}
