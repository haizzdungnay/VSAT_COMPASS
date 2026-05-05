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
}
