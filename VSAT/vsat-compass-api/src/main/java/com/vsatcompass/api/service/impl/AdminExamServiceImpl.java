package com.vsatcompass.api.service.impl;

import com.vsatcompass.api.dto.request.AdminExamCreateRequest;
import com.vsatcompass.api.dto.request.AdminExamUpdateRequest;
import com.vsatcompass.api.dto.response.AdminExamResponse;
import com.vsatcompass.api.dto.response.AdminExamSummaryResponse;
import com.vsatcompass.api.entity.Exam;
import com.vsatcompass.api.entity.Subject;
import com.vsatcompass.api.entity.enums.ExamPricingType;
import com.vsatcompass.api.entity.enums.ExamStatus;
import com.vsatcompass.api.exception.AppException;
import com.vsatcompass.api.repository.ExamQuestionRepository;
import com.vsatcompass.api.repository.ExamRepository;
import com.vsatcompass.api.repository.SubjectRepository;
import com.vsatcompass.api.service.AdminExamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminExamServiceImpl implements AdminExamService {

    private final ExamRepository examRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final SubjectRepository subjectRepository;

    // ---------- LIST ----------
    @Override
    public Page<AdminExamSummaryResponse> listAdminExams(
            ExamStatus status, Long subjectId, Pageable pageable) {
        return examRepository.findAdminList(status, subjectId, pageable)
                .map(this::toSummaryResponse);
    }

    // ---------- GET ----------
    @Override
    public AdminExamResponse getAdminExam(Long id) {
        Exam exam = examRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Exam", id));
        return toAdminResponse(exam);
    }

    // ---------- CREATE ----------
    @Override
    @Transactional
    public AdminExamResponse createExam(Long currentUserId, AdminExamCreateRequest req) {
        validateFreeOnlyPricing(req.getPricingType(), req.getPrice());
        Subject subject = loadActiveSubject(req.getSubjectId());

        if (examRepository.existsByExamCode(req.getExamCode())) {
            throw AppException.conflict("Exam code already exists: " + req.getExamCode());
        }

        Exam exam = Exam.builder()
                .examCode(req.getExamCode())
                .title(req.getTitle())
                .subjectId(subject.getId())
                .description(req.getDescription())
                .questionCount(0)
                .durationMinutes(req.getDurationMinutes())
                .difficulty(req.getDifficulty())
                .pricingType(ExamPricingType.FREE)
                .price(BigDecimal.ZERO)
                .status(ExamStatus.DRAFT)
                .tags(req.getTags())
                .publishDate(null)
                .createdBy(currentUserId)
                .reviewedBy(null)
                .version(1)
                .totalAttempts(0)
                .avgScore(null)
                .build();

        Exam saved = examRepository.save(exam);
        log.info("Admin exam created id={} code={} by user={}",
                saved.getId(), saved.getExamCode(), currentUserId);
        return toAdminResponse(saved);
    }

    // ---------- UPDATE ----------
    @Override
    @Transactional
    public AdminExamResponse updateExam(Long id, AdminExamUpdateRequest req) {
        Exam exam = examRepository.findById(id)
                .orElseThrow(() -> AppException.notFound("Exam", id));

        if (!isMetadataEditable(exam.getStatus())) {
            throw new AppException(HttpStatus.CONFLICT, "INVALID_STATE",
                    "Cannot edit exam metadata in status " + exam.getStatus()
                            + ". Only DRAFT or HIDDEN exams allow metadata edits in this batch.");
        }

        if (req.getPricingType() != null || req.getPrice() != null) {
            validateFreeOnlyPricing(
                    req.getPricingType() != null ? req.getPricingType() : exam.getPricingType(),
                    req.getPrice() != null ? req.getPrice() : exam.getPrice());
        }

        if (req.getSubjectId() != null && !req.getSubjectId().equals(exam.getSubjectId())) {
            Subject subject = loadActiveSubject(req.getSubjectId());
            exam.setSubjectId(subject.getId());
        }

        if (req.getTitle() != null) exam.setTitle(req.getTitle());
        if (req.getDescription() != null) exam.setDescription(req.getDescription());
        if (req.getDurationMinutes() != null) exam.setDurationMinutes(req.getDurationMinutes());
        if (req.getDifficulty() != null) exam.setDifficulty(req.getDifficulty());
        if (req.getTags() != null) exam.setTags(req.getTags());

        // pricingType / price stay FREE / 0 in this MVP — re-assert in case client sent them
        exam.setPricingType(ExamPricingType.FREE);
        exam.setPrice(BigDecimal.ZERO);

        // questionCount, status, examCode, createdBy, reviewedBy, version, totalAttempts,
        // avgScore, publishDate intentionally NOT touched here.
        // Composition + publish workflow + version bump rules belong to C1.2b-2.

        Exam saved = examRepository.save(exam);
        log.info("Admin exam updated id={} code={}", saved.getId(), saved.getExamCode());
        return toAdminResponse(saved);
    }

    // ---------- HELPERS ----------
    private void validateFreeOnlyPricing(ExamPricingType pricingType, BigDecimal price) {
        if (pricingType != null && pricingType != ExamPricingType.FREE) {
            throw AppException.validationFailed(
                    "Only FREE pricingType is allowed in this MVP, got " + pricingType);
        }
        if (price != null && price.compareTo(BigDecimal.ZERO) != 0) {
            throw AppException.validationFailed(
                    "Only price=0 is allowed in this MVP, got " + price.toPlainString());
        }
    }

    private Subject loadActiveSubject(Long subjectId) {
        Subject subject = subjectRepository.findById(subjectId)
                .orElseThrow(() -> AppException.notFound("Subject", subjectId));
        if (!Boolean.TRUE.equals(subject.getIsActive())) {
            throw AppException.validationFailed(
                    "Subject " + subjectId + " is not active");
        }
        return subject;
    }

    private static boolean isMetadataEditable(ExamStatus status) {
        return status == ExamStatus.DRAFT || status == ExamStatus.HIDDEN;
    }

    // ---------- MAPPERS ----------
    private AdminExamSummaryResponse toSummaryResponse(Exam exam) {
        return AdminExamSummaryResponse.builder()
                .id(exam.getId())
                .examCode(exam.getExamCode())
                .title(exam.getTitle())
                .subjectId(exam.getSubjectId())
                .questionCount(exam.getQuestionCount())
                .durationMinutes(exam.getDurationMinutes())
                .difficulty(exam.getDifficulty())
                .pricingType(exam.getPricingType())
                .price(exam.getPrice())
                .status(exam.getStatus())
                .version(exam.getVersion())
                .updatedAt(exam.getUpdatedAt())
                .build();
    }

    private AdminExamResponse toAdminResponse(Exam exam) {
        String subjectCode = subjectRepository.findById(exam.getSubjectId())
                .map(Subject::getCode)
                .orElse(null);
        Integer composedCount = Math.toIntExact(
                examQuestionRepository.countByExamId(exam.getId()));
        return AdminExamResponse.builder()
                .id(exam.getId())
                .examCode(exam.getExamCode())
                .title(exam.getTitle())
                .subjectId(exam.getSubjectId())
                .subjectCode(subjectCode)
                .description(exam.getDescription())
                .questionCount(composedCount)
                .durationMinutes(exam.getDurationMinutes())
                .difficulty(exam.getDifficulty())
                .pricingType(exam.getPricingType())
                .price(exam.getPrice())
                .status(exam.getStatus())
                .tags(exam.getTags())
                .publishDate(exam.getPublishDate())
                .version(exam.getVersion())
                .createdBy(exam.getCreatedBy())
                .reviewedBy(exam.getReviewedBy())
                .createdAt(exam.getCreatedAt())
                .updatedAt(exam.getUpdatedAt())
                .build();
    }
}
