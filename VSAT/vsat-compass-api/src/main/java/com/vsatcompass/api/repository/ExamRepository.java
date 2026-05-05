package com.vsatcompass.api.repository;

import com.vsatcompass.api.entity.Exam;
import com.vsatcompass.api.entity.enums.ExamPricingType;
import com.vsatcompass.api.entity.enums.ExamStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    @Query("SELECT e FROM Exam e "
            + "WHERE (:status IS NULL OR e.status = :status) "
            + "AND (:subjectId IS NULL OR e.subjectId = :subjectId)")
    Page<Exam> findAdminList(
            @Param("status") ExamStatus status,
            @Param("subjectId") Long subjectId,
            Pageable pageable);
}
