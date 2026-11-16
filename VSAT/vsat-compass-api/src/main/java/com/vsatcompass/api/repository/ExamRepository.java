package com.vsatcompass.api.repository;

import com.vsatcompass.api.entity.Exam;
import com.vsatcompass.api.entity.enums.ExamPricingType;
import com.vsatcompass.api.entity.enums.ExamStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExamRepository extends JpaRepository<Exam, Long> {

    Page<Exam> findByStatusAndPricingType(
            ExamStatus status,
            ExamPricingType pricingType,
            Pageable pageable);

    Optional<Exam> findByIdAndStatusAndPricingType(
            Long id,
            ExamStatus status,
            ExamPricingType pricingType);

    boolean existsByExamCode(String examCode);

    Page<Exam> findByStatus(ExamStatus status, Pageable pageable);

    Page<Exam> findBySubjectId(Long subjectId, Pageable pageable);

    Page<Exam> findByStatusAndSubjectId(
            ExamStatus status,
            Long subjectId,
            Pageable pageable);

    @org.springframework.data.jpa.repository.Query(
            value = "SELECT id FROM scoring_profiles WHERE code = :code LIMIT 1",
            nativeQuery = true)
    Long findScoringProfileIdByCode(@org.springframework.data.repository.query.Param("code") String code);

    @org.springframework.data.jpa.repository.Query(
            value = "SELECT EXISTS(SELECT 1 FROM scoring_profiles WHERE id = :id)",
            nativeQuery = true)
    boolean existsScoringProfileById(@org.springframework.data.repository.query.Param("id") Long id);
}
