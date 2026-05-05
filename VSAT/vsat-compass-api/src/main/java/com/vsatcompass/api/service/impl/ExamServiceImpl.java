package com.vsatcompass.api.service.impl;

import com.vsatcompass.api.dto.response.ExamDetailResponse;
import com.vsatcompass.api.dto.response.ExamSummaryResponse;
import com.vsatcompass.api.entity.Exam;
import com.vsatcompass.api.entity.enums.ExamPricingType;
import com.vsatcompass.api.entity.enums.ExamStatus;
import com.vsatcompass.api.exception.AppException;
import com.vsatcompass.api.repository.ExamQuestionRepository;
import com.vsatcompass.api.repository.ExamRepository;
import com.vsatcompass.api.service.ExamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ExamServiceImpl implements ExamService {

    private final ExamRepository examRepository;
    private final ExamQuestionRepository examQuestionRepository;

    @Override
    public Page<ExamSummaryResponse> listPublishedFreeExams(Pageable pageable) {
        return examRepository
                .findByStatusAndPricingType(ExamStatus.PUBLISHED, ExamPricingType.FREE, pageable)
                .map(this::toSummaryResponse);
    }

    @Override
    public ExamDetailResponse getPublicExam(Long id) {
        Exam exam = examRepository
                .findByIdAndStatusAndPricingType(id, ExamStatus.PUBLISHED, ExamPricingType.FREE)
                .orElseThrow(() -> AppException.notFound("Exam", id));
        return toDetailResponse(exam);
    }

    private ExamSummaryResponse toSummaryResponse(Exam exam) {
        return ExamSummaryResponse.builder()
                .id(exam.getId())
                .examCode(exam.getExamCode())
                .title(exam.getTitle())
                .subjectId(exam.getSubjectId())
                .description(exam.getDescription())
                .questionCount(computedQuestionCount(exam))
                .durationMinutes(exam.getDurationMinutes())
                .difficulty(exam.getDifficulty())
                .pricingType(exam.getPricingType())
                .tags(exam.getTags())
                .build();
    }

    private ExamDetailResponse toDetailResponse(Exam exam) {
        return ExamDetailResponse.builder()
                .id(exam.getId())
                .examCode(exam.getExamCode())
                .title(exam.getTitle())
                .subjectId(exam.getSubjectId())
                .description(exam.getDescription())
                .questionCount(computedQuestionCount(exam))
                .durationMinutes(exam.getDurationMinutes())
                .difficulty(exam.getDifficulty())
                .pricingType(exam.getPricingType())
                .tags(exam.getTags())
                .publishDate(exam.getPublishDate())
                .build();
    }

    private Integer computedQuestionCount(Exam exam) {
        return Math.toIntExact(examQuestionRepository.countByExamId(exam.getId()));
    }
}
