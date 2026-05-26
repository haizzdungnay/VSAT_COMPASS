package com.vsatcompass.api.repository;

import com.vsatcompass.api.entity.Question;
import com.vsatcompass.api.entity.enums.QuestionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long>,
        org.springframework.data.jpa.repository.JpaSpecificationExecutor<Question> {

    Optional<Question> findByQuestionCode(String questionCode);

    Page<Question> findByCreatedByOrderByUpdatedAtDesc(Long createdBy, Pageable pageable);

    Page<Question> findByCreatedByAndStatusOrderByUpdatedAtDesc(Long createdBy, QuestionStatus status, Pageable pageable);

    Page<Question> findByStatusOrderByUpdatedAtDesc(QuestionStatus status, Pageable pageable);

    long countByStatus(QuestionStatus status);
}
