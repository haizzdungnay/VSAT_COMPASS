package com.vsatcompass.api.repository;

import com.vsatcompass.api.entity.ExamQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExamQuestionRepository extends JpaRepository<ExamQuestion, Long> {

    long countByExamId(Long examId);
}
