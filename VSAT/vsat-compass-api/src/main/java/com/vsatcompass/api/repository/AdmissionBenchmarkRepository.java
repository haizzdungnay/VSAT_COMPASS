package com.vsatcompass.api.repository;

import com.vsatcompass.api.entity.AdmissionBenchmark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AdmissionBenchmarkRepository extends JpaRepository<AdmissionBenchmark, Long> {

    @Query("SELECT b FROM AdmissionBenchmark b JOIN FETCH b.university u WHERE " +
           "(:subjectCombo IS NULL OR b.subjectCombo = :subjectCombo) AND " +
           "(:year IS NULL OR b.year = :year) AND " +
           "(:region IS NULL OR u.region = :region) " +
           "ORDER BY b.cutoffScore DESC")
    List<AdmissionBenchmark> findBenchmarks(
            @Param("subjectCombo") String subjectCombo,
            @Param("year") Integer year,
            @Param("region") String region
    );

    @Query("SELECT DISTINCT b.subjectCombo FROM AdmissionBenchmark b ORDER BY b.subjectCombo ASC")
    List<String> findDistinctSubjectCombos();

    @Query("SELECT b FROM AdmissionBenchmark b JOIN FETCH b.university u ORDER BY b.year DESC, b.cutoffScore DESC")
    List<AdmissionBenchmark> findAllWithUniversity();
}
