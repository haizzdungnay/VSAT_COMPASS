package com.vsatcompass.api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "admission_benchmarks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdmissionBenchmark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "university_id", nullable = false)
    private University university;

    @Column(name = "major_code", nullable = false, length = 50)
    private String majorCode;

    @Column(name = "major_name", nullable = false, length = 255)
    private String majorName;

    @Column(name = "subject_combo", nullable = false, length = 50)
    private String subjectCombo;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "cutoff_score", nullable = false, precision = 6, scale = 2)
    private BigDecimal cutoffScore;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
}
