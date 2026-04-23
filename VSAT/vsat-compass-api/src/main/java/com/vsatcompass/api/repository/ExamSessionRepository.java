package com.vsatcompass.api.repository;

import com.vsatcompass.api.entity.ExamSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExamSessionRepository extends JpaRepository<ExamSession, Long> {

    Optional<ExamSession> findByIdAndUserId(Long id, Long userId);
}
