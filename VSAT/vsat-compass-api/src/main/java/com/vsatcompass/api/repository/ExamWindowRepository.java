package com.vsatcompass.api.repository;

import com.vsatcompass.api.entity.ExamWindow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExamWindowRepository extends JpaRepository<ExamWindow, Long> {

    List<ExamWindow> findAllByOrderByCreatedAtDesc();

    List<ExamWindow> findByExamIdOrderByCreatedAtDesc(Long examId);

    Optional<ExamWindow> findByExamIdAndWindowCode(Long examId, String windowCode);

    boolean existsByExamIdAndWindowCode(Long examId, String windowCode);

    boolean existsByExamIdAndWindowCodeAndIdNot(Long examId, String windowCode, Long id);
}

