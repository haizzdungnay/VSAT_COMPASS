package com.vsatcompass.api.repository;

import com.vsatcompass.api.entity.ExamBlueprint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExamBlueprintRepository extends JpaRepository<ExamBlueprint, Long> {

    Optional<ExamBlueprint> findByCode(String code);

    List<ExamBlueprint> findBySubjectId(Long subjectId);

    boolean existsByCode(String code);
}
