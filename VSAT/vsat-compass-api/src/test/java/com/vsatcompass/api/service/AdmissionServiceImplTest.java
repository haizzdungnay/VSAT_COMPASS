package com.vsatcompass.api.service;

import com.vsatcompass.api.dto.request.AdmissionPredictionRequest;
import com.vsatcompass.api.dto.response.AdmissionPredictionResponse;
import com.vsatcompass.api.dto.response.UniversityResponse;
import com.vsatcompass.api.entity.AdmissionBenchmark;
import com.vsatcompass.api.entity.University;
import com.vsatcompass.api.repository.AdmissionBenchmarkRepository;
import com.vsatcompass.api.repository.UniversityRepository;
import com.vsatcompass.api.service.impl.AdmissionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdmissionServiceImpl — Unit Tests for Admission Matching Engine")
class AdmissionServiceImplTest {

    @Mock
    private UniversityRepository universityRepository;

    @Mock
    private AdmissionBenchmarkRepository admissionBenchmarkRepository;

    @InjectMocks
    private AdmissionServiceImpl admissionService;

    private University sampleUniversity;

    @BeforeEach
    void setUp() {
        sampleUniversity = University.builder()
                .id(1L)
                .code("HUB")
                .name("Trường Đại học Ngân hàng TP.HCM")
                .region("Miền Nam")
                .website("https://hub.edu.vn")
                .build();
    }

    @Test
    @DisplayName("getUniversities returns mapped universities sorted by code")
    void getUniversities_returnsMappedList() {
        when(universityRepository.findAllByOrderByCodeAsc()).thenReturn(List.of(sampleUniversity));

        List<UniversityResponse> result = admissionService.getUniversities();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCode()).isEqualTo("HUB");
        assertThat(result.get(0).getName()).isEqualTo("Trường Đại học Ngân hàng TP.HCM");
        assertThat(result.get(0).getRegion()).isEqualTo("Miền Nam");
        assertThat(result.get(0).getWebsite()).isEqualTo("https://hub.edu.vn");
    }

    @Test
    @DisplayName("getSubjectCombos returns list of combinations from repository")
    void getSubjectCombos_returnsCombos() {
        when(admissionBenchmarkRepository.findDistinctSubjectCombos())
                .thenReturn(List.of("A00", "A01", "D01"));

        List<String> combos = admissionService.getSubjectCombos();

        assertThat(combos).containsExactly("A00", "A01", "D01");
    }

    @Test
    @DisplayName("predictAdmission classifies SAFE when score >= cutoff + 15.0")
    void predictAdmission_safeThreshold() {
        AdmissionBenchmark b1 = AdmissionBenchmark.builder()
                .id(101L)
                .university(sampleUniversity)
                .majorCode("7340201")
                .majorName("Tài chính - Ngân hàng")
                .subjectCombo("A00")
                .year(2024)
                .cutoffScore(BigDecimal.valueOf(300.00))
                .build();

        // Exactly cutoff + 15.0 (315.00) -> SAFE
        when(admissionBenchmarkRepository.findBenchmarks(eq("A00"), eq(2024), eq("Miền Nam")))
                .thenReturn(List.of(b1));

        AdmissionPredictionResponse respExact = admissionService.predictAdmission(
                AdmissionPredictionRequest.builder()
                        .totalScore(BigDecimal.valueOf(315.00))
                        .subjectCombo("A00")
                        .year(2024)
                        .region("Miền Nam")
                        .build()
        );

        assertThat(respExact.getSafeCount()).isEqualTo(1);
        assertThat(respExact.getFitCount()).isZero();
        assertThat(respExact.getRiskCount()).isZero();
        assertThat(respExact.getPredictions().get(0).getClassification()).isEqualTo("SAFE");
        assertThat(respExact.getPredictions().get(0).getDelta()).isEqualByComparingTo(BigDecimal.valueOf(15.00));

        // Score 320.00 (> 315.00) -> SAFE
        AdmissionPredictionResponse respHigher = admissionService.predictAdmission(
                AdmissionPredictionRequest.builder()
                        .totalScore(BigDecimal.valueOf(320.00))
                        .subjectCombo("A00")
                        .year(2024)
                        .region("Miền Nam")
                        .build()
        );
        assertThat(respHigher.getPredictions().get(0).getClassification()).isEqualTo("SAFE");
        assertThat(respHigher.getPredictions().get(0).getDelta()).isEqualByComparingTo(BigDecimal.valueOf(20.00));
    }

    @Test
    @DisplayName("predictAdmission classifies FIT on boundaries [cutoff - 10.0, cutoff + 15.0)")
    void predictAdmission_fitBoundaries() {
        AdmissionBenchmark b1 = AdmissionBenchmark.builder()
                .id(102L)
                .university(sampleUniversity)
                .majorCode("7340201")
                .majorName("Tài chính - Ngân hàng")
                .subjectCombo("A00")
                .year(2024)
                .cutoffScore(BigDecimal.valueOf(300.00))
                .build();

        when(admissionBenchmarkRepository.findBenchmarks(eq("A00"), eq(2024), eq(null)))
                .thenReturn(List.of(b1));

        // Upper FIT bound: 314.99 (delta +14.99) -> FIT
        AdmissionPredictionResponse respUpper = admissionService.predictAdmission(
                AdmissionPredictionRequest.builder()
                        .totalScore(BigDecimal.valueOf(314.99))
                        .subjectCombo("A00")
                        .year(2024)
                        .build()
        );
        assertThat(respUpper.getSafeCount()).isZero();
        assertThat(respUpper.getFitCount()).isEqualTo(1);
        assertThat(respUpper.getRiskCount()).isZero();
        assertThat(respUpper.getPredictions().get(0).getClassification()).isEqualTo("FIT");

        // Lower FIT bound: exactly 290.00 (delta -10.00) -> FIT
        AdmissionPredictionResponse respLower = admissionService.predictAdmission(
                AdmissionPredictionRequest.builder()
                        .totalScore(BigDecimal.valueOf(290.00))
                        .subjectCombo("A00")
                        .year(2024)
                        .build()
        );
        assertThat(respLower.getFitCount()).isEqualTo(1);
        assertThat(respLower.getPredictions().get(0).getClassification()).isEqualTo("FIT");
        assertThat(respLower.getPredictions().get(0).getDelta()).isEqualByComparingTo(BigDecimal.valueOf(-10.00));
    }

    @Test
    @DisplayName("predictAdmission classifies RISK when score < cutoff - 10.0")
    void predictAdmission_riskBoundary() {
        AdmissionBenchmark b1 = AdmissionBenchmark.builder()
                .id(103L)
                .university(sampleUniversity)
                .majorCode("7340201")
                .majorName("Tài chính - Ngân hàng")
                .subjectCombo("A00")
                .year(2024)
                .cutoffScore(BigDecimal.valueOf(300.00))
                .build();

        when(admissionBenchmarkRepository.findBenchmarks(eq("A00"), eq(null), eq(null)))
                .thenReturn(List.of(b1));

        // Just below lower FIT bound: 289.99 (delta -10.01) -> RISK
        AdmissionPredictionResponse respRisk = admissionService.predictAdmission(
                AdmissionPredictionRequest.builder()
                        .totalScore(BigDecimal.valueOf(289.99))
                        .subjectCombo("a00") // test lowercase trim
                        .build()
        );

        assertThat(respRisk.getSafeCount()).isZero();
        assertThat(respRisk.getFitCount()).isZero();
        assertThat(respRisk.getRiskCount()).isEqualTo(1);
        assertThat(respRisk.getPredictions().get(0).getClassification()).isEqualTo("RISK");
        assertThat(respRisk.getPredictions().get(0).getDelta()).isEqualByComparingTo(BigDecimal.valueOf(-10.01));
    }

    @Test
    @DisplayName("predictAdmission throws IllegalArgumentException when totalScore is null")
    void predictAdmission_nullScoreThrows() {
        assertThatThrownBy(() -> admissionService.predictAdmission(
                AdmissionPredictionRequest.builder().build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("totalScore không được để trống");
    }
}
