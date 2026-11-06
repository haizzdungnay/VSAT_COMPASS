package com.vsatcompass.api.service.impl;

import com.vsatcompass.api.dto.request.AdmissionPredictionRequest;
import com.vsatcompass.api.dto.response.AdmissionPredictionResponse;
import com.vsatcompass.api.dto.response.UniversityResponse;
import com.vsatcompass.api.entity.AdmissionBenchmark;
import com.vsatcompass.api.repository.AdmissionBenchmarkRepository;
import com.vsatcompass.api.repository.UniversityRepository;
import com.vsatcompass.api.service.AdmissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdmissionServiceImpl implements AdmissionService {

    private static final BigDecimal SAFE_THRESHOLD = BigDecimal.valueOf(15.0);
    private static final BigDecimal RISK_THRESHOLD = BigDecimal.valueOf(-10.0);

    private final UniversityRepository universityRepository;
    private final AdmissionBenchmarkRepository admissionBenchmarkRepository;

    @Override
    @Transactional(readOnly = true)
    public List<UniversityResponse> getUniversities() {
        return universityRepository.findAllByOrderByCodeAsc().stream()
                .map(u -> UniversityResponse.builder()
                        .id(u.getId())
                        .code(u.getCode())
                        .name(u.getName())
                        .region(u.getRegion())
                        .website(u.getWebsite())
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getSubjectCombos() {
        return admissionBenchmarkRepository.findDistinctSubjectCombos();
    }

    @Override
    @Transactional(readOnly = true)
    public AdmissionPredictionResponse predictAdmission(AdmissionPredictionRequest request) {
        if (request.getTotalScore() == null) {
            throw new IllegalArgumentException("totalScore không được để trống");
        }

        BigDecimal totalScore = request.getTotalScore();
        String subjectCombo = (request.getSubjectCombo() != null && !request.getSubjectCombo().isBlank())
                ? request.getSubjectCombo().trim().toUpperCase()
                : null;
        String region = (request.getRegion() != null && !request.getRegion().isBlank())
                ? request.getRegion().trim()
                : null;
        Integer year = request.getYear();

        List<AdmissionBenchmark> benchmarks = admissionBenchmarkRepository.findBenchmarks(subjectCombo, year, region);

        List<AdmissionPredictionResponse.PredictionItem> predictions = new ArrayList<>();
        int safeCount = 0;
        int fitCount = 0;
        int riskCount = 0;

        for (AdmissionBenchmark b : benchmarks) {
            BigDecimal cutoff = b.getCutoffScore();
            BigDecimal delta = totalScore.subtract(cutoff).setScale(2, RoundingMode.HALF_UP);

            String classification;
            if (delta.compareTo(SAFE_THRESHOLD) >= 0) {
                classification = "SAFE";
                safeCount++;
            } else if (delta.compareTo(RISK_THRESHOLD) >= 0) {
                classification = "FIT";
                fitCount++;
            } else {
                classification = "RISK";
                riskCount++;
            }

            predictions.add(AdmissionPredictionResponse.PredictionItem.builder()
                    .benchmarkId(b.getId())
                    .universityId(b.getUniversity() != null ? b.getUniversity().getId() : null)
                    .universityCode(b.getUniversity() != null ? b.getUniversity().getCode() : null)
                    .universityName(b.getUniversity() != null ? b.getUniversity().getName() : null)
                    .region(b.getUniversity() != null ? b.getUniversity().getRegion() : null)
                    .majorCode(b.getMajorCode())
                    .majorName(b.getMajorName())
                    .subjectCombo(b.getSubjectCombo())
                    .year(b.getYear())
                    .cutoffScore(cutoff)
                    .delta(delta)
                    .classification(classification)
                    .notes(b.getNotes())
                    .build());
        }

        return AdmissionPredictionResponse.builder()
                .totalScore(totalScore)
                .subjectCombo(subjectCombo)
                .year(year)
                .region(region)
                .safeCount(safeCount)
                .fitCount(fitCount)
                .riskCount(riskCount)
                .totalEvaluated(benchmarks.size())
                .predictions(predictions)
                .build();
    }
}
