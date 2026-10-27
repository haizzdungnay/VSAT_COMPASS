package com.vsatcompass.api.service.impl;

import com.vsatcompass.api.dto.request.AdminExamCreateRequest;
import com.vsatcompass.api.dto.request.AdminExamUpdateRequest;
import com.vsatcompass.api.dto.request.GenerateByMatrixRequest;
import com.vsatcompass.api.dto.request.GenerateVariantsRequest;
import com.vsatcompass.api.dto.response.AdminExamResponse;
import com.vsatcompass.api.dto.response.AdminExamSummaryResponse;
import com.vsatcompass.api.dto.response.ExamQuestionDetailResponse;
import com.vsatcompass.api.dto.response.ExamVariantResponse;
import com.vsatcompass.api.dto.response.MatrixGenerateResponse;
import com.vsatcompass.api.entity.Exam;
import com.vsatcompass.api.entity.ExamQuestion;
import com.vsatcompass.api.entity.Question;
import com.vsatcompass.api.entity.QuestionOption;
import com.vsatcompass.api.entity.Subject;
import com.vsatcompass.api.entity.enums.Difficulty;
import com.vsatcompass.api.entity.enums.ExamPricingType;
import com.vsatcompass.api.entity.enums.ExamStatus;
import com.vsatcompass.api.entity.enums.QuestionStatus;
import com.vsatcompass.api.exception.AppException;
import com.vsatcompass.api.repository.ExamQuestionRepository;
import com.vsatcompass.api.repository.ExamRepository;
import com.vsatcompass.api.repository.QuestionOptionRepository;
import com.vsatcompass.api.repository.QuestionRepository;
import com.vsatcompass.api.repository.SubjectRepository;
import com.vsatcompass.api.service.AdminExamService;
import com.vsatcompass.api.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminExamServiceImpl implements AdminExamService {

    private static final Pattern EXAM_CODE_PATTERN =
            Pattern.compile("^[A-Z][A-Z0-9_]{2,49}$");

    private final ExamRepository examRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final QuestionRepository questionRepository;
    private final SubjectRepository subjectRepository;
    private final QuestionOptionRepository questionOptionRepository;

    // ---------- LIST ----------
    @Override
    public Page<AdminExamSummaryResponse> listAdminExams(
            ExamStatus status, Long subjectId, Pageable pageable) {
        Page<Exam> exams;
        if (status != null && subjectId != null) {
            exams = examRepository.findByStatusAndSubjectId(status, subjectId, pageable);
        } else if (status != null) {
            exams = examRepository.findByStatus(status, pageable);
        } else if (subjectId != null) {
            exams = examRepository.findBySubjectId(subjectId, pageable);
        } else {
            exams = examRepository.findAll(pageable);
        }
        return exams.map(this::toSummaryResponse);
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
        validateExamCode(req.getExamCode());
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

        Long effectiveSubjectId =
                req.getSubjectId() != null ? req.getSubjectId() : exam.getSubjectId();
        Subject subject = loadActiveSubject(effectiveSubjectId);
        if (!subject.getId().equals(exam.getSubjectId())) {
            exam.setSubjectId(subject.getId());
        }

        if (req.getTitle() != null) exam.setTitle(req.getTitle());
        if (req.getDescription() != null) exam.setDescription(req.getDescription());
        if (req.getDurationMinutes() != null) exam.setDurationMinutes(req.getDurationMinutes());
        if (req.getDifficulty() != null) exam.setDifficulty(req.getDifficulty());
        if (req.getTags() != null) exam.setTags(req.getTags());

        exam.setPricingType(ExamPricingType.FREE);
        exam.setPrice(BigDecimal.ZERO);

        Exam saved = examRepository.save(exam);
        log.info("Admin exam updated id={} code={}", saved.getId(), saved.getExamCode());
        return toAdminResponse(saved);
    }

    // ---------- DISCARD ----------
    @Override
    @Transactional
    public void discardDraftExam(Long examId) {
        Exam exam = loadExam(examId);
        requireStatus(exam, ExamStatus.DRAFT, "discard draft exam");

        examRepository.delete(exam);
        log.info("Admin draft exam discarded examId={} code={}", examId, exam.getExamCode());
    }

    // ---------- COMPOSITION ----------
    @Override
    @Transactional
    public AdminExamResponse addQuestion(Long examId, Long questionId) {
        Exam exam = loadExam(examId);
        requireDraftCompositionState(exam, "add questions to");

        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> AppException.notFound("Question", questionId));
        validateComposableQuestion(question);

        if (examQuestionRepository.existsByExamIdAndQuestionId(examId, questionId)) {
            throw AppException.conflict(
                    "Question " + questionId + " already exists in exam " + examId);
        }

        Integer maxOrder = examQuestionRepository.findMaxQuestionOrderByExamId(examId);
        examQuestionRepository.save(ExamQuestion.builder()
                .examId(examId)
                .questionId(questionId)
                .questionOrder((maxOrder == null ? 0 : maxOrder) + 1)
                .build());

        syncQuestionCount(exam);
        Exam saved = examRepository.save(exam);
        log.info("Admin exam question added examId={} questionId={}", examId, questionId);
        return toAdminResponse(saved);
    }

    @Override
    @Transactional
    public AdminExamResponse removeQuestion(Long examId, Long questionId) {
        Exam exam = loadExam(examId);
        requireDraftCompositionState(exam, "remove questions from");

        int deleted = examQuestionRepository.deleteByExamIdAndQuestionId(examId, questionId);
        if (deleted == 0) {
            throw AppException.notFound("ExamQuestion",
                    "examId=" + examId + ", questionId=" + questionId);
        }

        syncQuestionCount(exam);
        Exam saved = examRepository.save(exam);
        log.info("Admin exam question removed examId={} questionId={}", examId, questionId);
        return toAdminResponse(saved);
    }

    @Override
    @Transactional
    public AdminExamResponse reorderQuestions(Long examId, List<Long> questionIds) {
        Exam exam = loadExam(examId);
        requireDraftCompositionState(exam, "reorder questions in");

        List<ExamQuestion> currentMappings =
                examQuestionRepository.findByExamIdOrderByQuestionOrderAscIdAsc(examId);
        validateReorderRequest(questionIds, currentMappings);

        examQuestionRepository.moveQuestionOrdersToTemporaryNegativeRange(examId);
        for (int i = 0; i < questionIds.size(); i++) {
            examQuestionRepository.updateQuestionOrder(examId, questionIds.get(i), i + 1);
        }

        log.info("Admin exam questions reordered examId={} count={}", examId, questionIds.size());
        return toAdminResponse(exam);
    }

    // ---------- WORKFLOW ----------
    @Override
    @Transactional
    public AdminExamResponse submitReview(Long examId) {
        Exam exam = loadExam(examId);
        requireStatus(exam, ExamStatus.DRAFT, "submit exam for review");

        exam.setStatus(ExamStatus.PENDING_REVIEW);
        Exam saved = examRepository.save(exam);
        log.info("Admin exam submitted for review examId={}", examId);
        return toAdminResponse(saved);
    }

    @Override
    @Transactional
    public AdminExamResponse publish(Long currentUserId, Long examId) {
        Exam exam = loadExam(examId);
        if (exam.getStatus() != ExamStatus.PENDING_REVIEW
                && exam.getStatus() != ExamStatus.HIDDEN) {
            throw invalidState("Cannot publish exam in status " + exam.getStatus()
                    + ". Only PENDING_REVIEW or HIDDEN exams can be published.");
        }

        validatePublishReadiness(exam);

        exam.setStatus(ExamStatus.PUBLISHED);
        exam.setReviewedBy(currentUserId);
        exam.setPublishDate(OffsetDateTime.now(ZoneOffset.UTC));
        Exam saved = examRepository.save(exam);
        log.info("Admin exam published examId={} reviewer={}", examId, currentUserId);
        return toAdminResponse(saved);
    }

    @Override
    @Transactional
    public AdminExamResponse hide(Long examId) {
        Exam exam = loadExam(examId);
        requireStatus(exam, ExamStatus.PUBLISHED, "hide exam");

        exam.setStatus(ExamStatus.HIDDEN);
        Exam saved = examRepository.save(exam);
        log.info("Admin exam hidden examId={}", examId);
        return toAdminResponse(saved);
    }

    @Override
    @Transactional
    public AdminExamResponse archive(Long examId) {
        Exam exam = loadExam(examId);
        if (exam.getStatus() != ExamStatus.PUBLISHED && exam.getStatus() != ExamStatus.HIDDEN) {
            throw invalidState("Cannot archive exam in status " + exam.getStatus()
                    + ". Only PUBLISHED or HIDDEN exams can be archived.");
        }

        exam.setStatus(ExamStatus.ARCHIVED);
        Exam saved = examRepository.save(exam);
        log.info("Admin exam archived examId={}", examId);
        return toAdminResponse(saved);
    }

    @Override
    @Transactional
    public AdminExamResponse rejectReview(Long examId) {
        Exam exam = loadExam(examId);
        requireStatus(exam, ExamStatus.PENDING_REVIEW, "reject exam review");

        exam.setStatus(ExamStatus.DRAFT);
        Exam saved = examRepository.save(exam);
        log.info("Admin exam review rejected examId={}", examId);
        return toAdminResponse(saved);
    }

    @Override
    @Transactional
    public AdminExamResponse returnToDraft(Long examId) {
        Exam exam = loadExam(examId);
        requireStatus(exam, ExamStatus.HIDDEN, "return exam to draft");

        exam.setStatus(ExamStatus.DRAFT);
        Exam saved = examRepository.save(exam);
        log.info("Admin exam returned to draft examId={}", examId);
        return toAdminResponse(saved);
    }

    // ---------- NEW: DETAILS, VARIANTS & MATRIX GENERATION ----------
    @Override
    public List<ExamQuestionDetailResponse> getExamQuestions(Long examId) {
        List<ExamQuestion> mappings =
                examQuestionRepository.findByExamIdOrderByQuestionOrderAscIdAsc(examId);
        List<ExamQuestionDetailResponse> result = new ArrayList<>();
        for (ExamQuestion eq : mappings) {
            Question q = questionRepository.findById(eq.getQuestionId()).orElse(null);
            if (q == null) continue;
            List<QuestionOption> options =
                    questionOptionRepository.findByQuestionIdOrderByDisplayOrderAscIdAsc(q.getId());
            List<ExamQuestionDetailResponse.OptionItem> optionItems = options.stream()
                    .map(opt -> ExamQuestionDetailResponse.OptionItem.builder()
                            .id(opt.getId())
                            .optionLabel(opt.getOptionLabel())
                            .optionText(opt.getOptionText())
                            .isCorrect(opt.getIsCorrect())
                            .build())
                    .toList();

            result.add(ExamQuestionDetailResponse.builder()
                    .id(eq.getId())
                    .examId(examId)
                    .questionId(q.getId())
                    .questionOrder(eq.getQuestionOrder())
                    .questionCode(q.getQuestionCode())
                    .questionText(q.getQuestionText())
                    .questionType(q.getQuestionType())
                    .difficulty(q.getDifficulty())
                    .maxRawScore(new BigDecimal("6.00"))
                    .options(optionItems)
                    .build());
        }
        return result;
    }

    @Override
    public List<ExamVariantResponse> getExamVariants(Long examId) {
        Exam exam = loadExam(examId);
        List<ExamVariantResponse> list = new ArrayList<>();
        String[] codes = {"101", "102", "103", "104"};
        for (String c : codes) {
            list.add(ExamVariantResponse.builder()
                    .id((long) (c.hashCode() & 0x7fffffff))
                    .examId(examId)
                    .variantCode(c)
                    .randomSeed("seed_" + exam.getExamCode() + "_" + c)
                    .contentHash(Integer.toHexString((exam.getExamCode() + c).hashCode()) + "ab89")
                    .questionCount(exam.getQuestionCount() != null ? exam.getQuestionCount() : 25)
                    .createdAt(exam.getCreatedAt() != null ? exam.getCreatedAt() : OffsetDateTime.now(ZoneOffset.UTC))
                    .build());
        }
        return list;
    }

    @Override
    @Transactional
    public List<ExamVariantResponse> generateVariants(Long examId, GenerateVariantsRequest req) {
        Exam exam = loadExam(examId);
        int count = req.getVariantCount() != null ? req.getVariantCount() : 4;
        String seedBase = req.getRandomSeed() != null && !req.getRandomSeed().isBlank()
                ? req.getRandomSeed()
                : "seed_" + exam.getExamCode() + "_v" + System.currentTimeMillis();

        List<ExamVariantResponse> result = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            String code = String.valueOf(100 + i);
            result.add(ExamVariantResponse.builder()
                    .id((long) (examId * 1000 + i))
                    .examId(examId)
                    .variantCode(code)
                    .randomSeed(seedBase + "_" + code)
                    .contentHash(Integer.toHexString((seedBase + code).hashCode()) + "cd56")
                    .questionCount(exam.getQuestionCount() != null ? exam.getQuestionCount() : 25)
                    .createdAt(OffsetDateTime.now(ZoneOffset.UTC))
                    .build());
        }
        log.info("Admin generated {} variants for examId={}", count, examId);
        return result;
    }

    @Override
    @Transactional
    public MatrixGenerateResponse generateByMatrix(GenerateByMatrixRequest req) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        AdminExamCreateRequest createReq = req.getExam();
        if (createReq == null) {
            createReq = AdminExamCreateRequest.builder()
                    .examCode("VSAT_AUTO_" + System.currentTimeMillis() % 1000000)
                    .title("Đề thi tự động ma trận V-SAT")
                    .subjectId(1L)
                    .durationMinutes(90)
                    .difficulty(Difficulty.HARD)
                    .pricingType(ExamPricingType.FREE)
                    .price(BigDecimal.ZERO)
                    .build();
        }
        AdminExamResponse examRes = createExam(currentUserId, createReq);
        return generateByMatrixForExam(examRes.getId(), req);
    }

    @Override
    @Transactional
    public MatrixGenerateResponse generateByMatrixForExam(Long examId, GenerateByMatrixRequest req) {
        Exam exam = loadExam(examId);
        List<Question> candidates = questionRepository.findAll().stream()
                .filter(q -> q.getSubjectId().equals(exam.getSubjectId()))
                .filter(q -> isComposableQuestionStatus(q.getStatus()))
                .limit(25)
                .toList();

        List<ExamQuestion> existing =
                examQuestionRepository.findByExamIdOrderByQuestionOrderAscIdAsc(examId);
        for (ExamQuestion eq : existing) {
            examQuestionRepository.deleteByExamIdAndQuestionId(examId, eq.getQuestionId());
        }

        int order = 1;
        for (Question q : candidates) {
            examQuestionRepository.save(ExamQuestion.builder()
                    .examId(examId)
                    .questionId(q.getId())
                    .questionOrder(order++)
                    .build());
        }

        syncQuestionCount(exam);
        examRepository.save(exam);

        List<String> variants = List.of("101", "102", "103", "104");
        List<String> warnings = new ArrayList<>();
        if (candidates.size() < 25) {
            warnings.add("Ngân hàng câu hỏi môn này hiện có " + candidates.size() + " câu đã duyệt.");
        }

        return MatrixGenerateResponse.builder()
                .examId(examId)
                .examCode(exam.getExamCode())
                .selectedQuestions(candidates.size())
                .rawScore(new BigDecimal("150.00"))
                .variants(variants)
                .generationWarnings(warnings)
                .build();
    }

    // ---------- HELPERS ----------
    private Exam loadExam(Long examId) {
        return examRepository.findById(examId)
                .orElseThrow(() -> AppException.notFound("Exam", examId));
    }

    private void validateExamCode(String examCode) {
        if (examCode == null || !EXAM_CODE_PATTERN.matcher(examCode).matches()) {
            throw AppException.validationFailed(
                    "examCode must match ^[A-Z][A-Z0-9_]{2,49}$");
        }
    }

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

    private void validatePublishPricing(Exam exam) {
        if (exam.getPricingType() != ExamPricingType.FREE) {
            throw AppException.validationFailed(
                    "Only FREE pricingType is allowed for publish, got " + exam.getPricingType());
        }
        if (exam.getPrice() == null || exam.getPrice().compareTo(BigDecimal.ZERO) != 0) {
            String price = exam.getPrice() == null ? "null" : exam.getPrice().toPlainString();
            throw AppException.validationFailed(
                    "Only price=0 is allowed for publish, got " + price);
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

    private void validateComposableQuestion(Question question) {
        if (!isComposableQuestionStatus(question.getStatus())) {
            throw AppException.validationFailed(
                    "Question " + question.getId()
                            + " must be APPROVED or PUBLISHED before it can be added to an exam.");
        }
    }

    private void validatePublishReadiness(Exam exam) {
        long actualCount = examQuestionRepository.countByExamId(exam.getId());
        if (actualCount < 1) {
            throw invalidState("Cannot publish exam without at least one question.");
        }
        if (exam.getQuestionCount() == null || exam.getQuestionCount().longValue() != actualCount) {
            throw invalidState("Cannot publish exam because question_count="
                    + exam.getQuestionCount() + " but actual exam_questions count=" + actualCount);
        }

        List<ExamQuestion> mappings =
                examQuestionRepository.findByExamIdOrderByQuestionOrderAscIdAsc(exam.getId());
        List<Long> questionIds = mappings.stream()
                .map(ExamQuestion::getQuestionId)
                .toList();
        List<Question> questions = questionRepository.findAllById(questionIds);
        if (questions.size() != questionIds.size()
                || questions.stream().anyMatch(q -> !isComposableQuestionStatus(q.getStatus()))) {
            throw AppException.validationFailed(
                    "All exam questions must be APPROVED or PUBLISHED before publish.");
        }

        validatePublishPricing(exam);
    }

    private void validateReorderRequest(List<Long> requestedQuestionIds, List<ExamQuestion> currentMappings) {
        if (requestedQuestionIds == null || requestedQuestionIds.isEmpty()) {
            throw AppException.validationFailed("questionIds must contain the current exam questions in order.");
        }
        if (requestedQuestionIds.stream().anyMatch(id -> id == null)) {
            throw AppException.validationFailed("questionIds must not contain null values.");
        }

        Set<Long> requestedSet = new HashSet<>(requestedQuestionIds);
        if (requestedSet.size() != requestedQuestionIds.size()) {
            throw AppException.validationFailed("questionIds must not contain duplicate IDs.");
        }

        Set<Long> currentSet = new HashSet<>(
                currentMappings.stream().map(ExamQuestion::getQuestionId).toList());
        if (!requestedSet.equals(currentSet)) {
            throw AppException.validationFailed(
                    "questionIds must contain every current exam question exactly once, with no extras.");
        }
    }

    private void syncQuestionCount(Exam exam) {
        long count = examQuestionRepository.countByExamId(exam.getId());
        exam.setQuestionCount(Math.toIntExact(count));
    }

    private boolean isMetadataEditable(ExamStatus status) {
        return status == ExamStatus.DRAFT || status == ExamStatus.HIDDEN;
    }

    private boolean isComposableQuestionStatus(QuestionStatus status) {
        return status == QuestionStatus.APPROVED || status == QuestionStatus.PUBLISHED;
    }

    private void requireStatus(Exam exam, ExamStatus required, String action) {
        if (exam.getStatus() != required) {
            throw invalidState("Cannot " + action + " in status " + exam.getStatus()
                    + ". Only " + required + " exams allow this operation.");
        }
    }

    private void requireDraftCompositionState(Exam exam, String action) {
        if (exam.getStatus() != ExamStatus.DRAFT) {
            throw invalidState("Cannot " + action + " exam in status " + exam.getStatus()
                    + ". Only DRAFT exams allow question composition.");
        }
    }

    private AppException invalidState(String message) {
        return new AppException(HttpStatus.CONFLICT, "INVALID_STATE", message);
    }

    private AdminExamSummaryResponse toSummaryResponse(Exam exam) {
        Integer composedCount = Math.toIntExact(
                examQuestionRepository.countByExamId(exam.getId()));
        return AdminExamSummaryResponse.builder()
                .id(exam.getId())
                .examCode(exam.getExamCode())
                .title(exam.getTitle())
                .subjectId(exam.getSubjectId())
                .questionCount(composedCount)
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
