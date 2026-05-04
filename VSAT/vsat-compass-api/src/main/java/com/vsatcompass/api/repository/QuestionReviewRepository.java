package com.vsatcompass.api.repository;

import com.vsatcompass.api.entity.QuestionReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionReviewRepository extends JpaRepository<QuestionReview, Long> {

    List<QuestionReview> findByQuestionIdOrderByCreatedAtDesc(Long questionId);
}
