package com.vsatcompass.api.service;

import com.vsatcompass.api.dto.request.AdminExamCreateRequest;
import com.vsatcompass.api.dto.request.AdminExamUpdateRequest;
import com.vsatcompass.api.dto.response.AdminExamResponse;
import com.vsatcompass.api.dto.response.AdminExamSummaryResponse;
import com.vsatcompass.api.entity.Exam;
import com.vsatcompass.api.entity.ExamQuestion;
import com.vsatcompass.api.entity.Question;
import com.vsatcompass.api.entity.Subject;
import com.vsatcompass.api.entity.enums.Difficulty;
import com.vsatcompass.api.entity.enums.ExamPricingType;
import com.vsatcompass.api.entity.enums.ExamStatus;
import com.vsatcompass.api.entity.enums.QuestionStatus;
import com.vsatcompass.api.exception.AppException;
import com.vsatcompass.api.repository.ExamQuestionRepository;
import com.vsatcompass.api.repository.ExamRepository;
import com.vsatcompass.api.repository.QuestionRepository;
import com.vsatcompass.api.repository.SubjectRepository;
import com.vsatcompass.api.service.impl.AdminExamServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminExamService — Phase C1.2b-1 Admin Exam CRUD foundation")
class AdminExamServiceTest {

    @Mock ExamRepository examRepository;
    @Mock ExamQuestionRepository examQuestionRepository;
    @Mock QuestionRepository questionRepository;
    @Mock SubjectRepository subjectRepository;

    @InjectMocks AdminExamServiceImpl adminExamService;

    private static final Long ADMIN_USER_ID = 901L;
    private static final Long SUBJECT_ACTIVE_ID = 10L;
    private static final Long SUBJECT_INACTIVE_ID = 11L;
    private static final String SUBJECT_ACTIVE_CODE = "MATH";
    private static final String EXAM_CODE_VALID = "ADM_C1_2B_001";
    private static final OffsetDateTime FIXED_TIME =
            OffsetDateTime.parse("2026-05-05T00:00:00Z");

    private Subject activeSubject;
    private Subject inactiveSubject;

    @BeforeEach
    void setUp() {
        activeSubject = Subject.builder()
                .id(SUBJECT_ACTIVE_ID)
                .code(SUBJECT_ACTIVE_CODE)
                .name("Toán")
                .isActive(true)
                .displayOrder(1)
                .build();
        inactiveSubject = Subject.builder()
                .id(SUBJECT_INACTIVE_ID)
                .code("OLD")
                .name("Old Subject")
                .isActive(false)
                .displayOrder(99)
                .build();
    }

    // =========================================================
    // CREATE
    // =========================================================

    @Test
    @DisplayName("create: draft exam — sets server-controlled fields and persists FREE/0 pricing")
    void create_draftExam_success_setsServerControlledFields() {
        AdminExamCreateRequest req = baseCreateRequest();

        when(subjectRepository.findById(SUBJECT_ACTIVE_ID)).thenReturn(Optional.of(activeSubject));
        when(examRepository.existsByExamCode(EXAM_CODE_VALID)).thenReturn(false);
        when(examRepository.save(any(Exam.class))).thenAnswer(inv -> {
            Exam toSave = inv.getArgument(0);
            toSave.setId(42L);
            toSave.setCreatedAt(FIXED_TIME);
            toSave.setUpdatedAt(FIXED_TIME);
            return toSave;
        });
        when(examQuestionRepository.countByExamId(42L)).thenReturn(0L);

        AdminExamResponse result = adminExamService.createExam(ADMIN_USER_ID, req);

        ArgumentCaptor<Exam> examCaptor = ArgumentCaptor.forClass(Exam.class);
        verify(examRepository).save(examCaptor.capture());
        Exam saved = examCaptor.getValue();
        assertThat(saved.getStatus()).isEqualTo(ExamStatus.DRAFT);
        assertThat(saved.getQuestionCount()).isZero();
        assertThat(saved.getPricingType()).isEqualTo(ExamPricingType.FREE);
        assertThat(saved.getPrice()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(saved.getVersion()).isEqualTo(1);
        assertThat(saved.getTotalAttempts()).isZero();
        assertThat(saved.getAvgScore()).isNull();
        assertThat(saved.getCreatedBy()).isEqualTo(ADMIN_USER_ID);
        assertThat(saved.getReviewedBy()).isNull();
        assertThat(saved.getPublishDate()).isNull();
        assertThat(saved.getExamCode()).isEqualTo(EXAM_CODE_VALID);

        assertThat(result.getId()).isEqualTo(42L);
        assertThat(result.getStatus()).isEqualTo(ExamStatus.DRAFT);
        assertThat(result.getSubjectCode()).isEqualTo(SUBJECT_ACTIVE_CODE);
    }

    @Test
    @DisplayName("create: duplicate examCode → 409 DUPLICATE")
    void create_duplicateExamCode_throwsConflict() {
        AdminExamCreateRequest req = baseCreateRequest();
        when(subjectRepository.findById(SUBJECT_ACTIVE_ID)).thenReturn(Optional.of(activeSubject));
        when(examRepository.existsByExamCode(EXAM_CODE_VALID)).thenReturn(true);

        assertThatThrownBy(() -> adminExamService.createExam(ADMIN_USER_ID, req))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException ae = (AppException) ex;
                    assertThat(ae.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(ae.getCode()).isEqualTo("DUPLICATE");
                });

        verify(examRepository, never()).save(any(Exam.class));
    }

    @Test
    @DisplayName("create: invalid examCode -> 400 VALIDATION_FAILED")
    void create_invalidExamCode_throwsValidationOrBadRequest() {
        AdminExamCreateRequest req = baseCreateRequest();
        req.setExamCode("bad-code");

        assertThatThrownBy(() -> adminExamService.createExam(ADMIN_USER_ID, req))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException ae = (AppException) ex;
                    assertThat(ae.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(ae.getCode()).isEqualTo("VALIDATION_FAILED");
                    assertThat(ae.getMessage()).contains("examCode");
                });

        verify(subjectRepository, never()).findById(any());
        verify(examRepository, never()).existsByExamCode(any());
        verify(examRepository, never()).save(any(Exam.class));
    }

    @Test
    @DisplayName("create: PAID pricingType → 400 VALIDATION_FAILED")
    void create_nonFreePricing_throwsValidation() {
        AdminExamCreateRequest req = baseCreateRequest();
        req.setPricingType(ExamPricingType.PAID);

        assertThatThrownBy(() -> adminExamService.createExam(ADMIN_USER_ID, req))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException ae = (AppException) ex;
                    assertThat(ae.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(ae.getCode()).isEqualTo("VALIDATION_FAILED");
                });

        verify(examRepository, never()).save(any(Exam.class));
    }

    @Test
    @DisplayName("create: non-zero price → 400 VALIDATION_FAILED")
    void create_nonZeroPrice_throwsValidation() {
        AdminExamCreateRequest req = baseCreateRequest();
        req.setPrice(new BigDecimal("9.99"));

        assertThatThrownBy(() -> adminExamService.createExam(ADMIN_USER_ID, req))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException ae = (AppException) ex;
                    assertThat(ae.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(ae.getCode()).isEqualTo("VALIDATION_FAILED");
                });

        verify(examRepository, never()).save(any(Exam.class));
    }

    @Test
    @DisplayName("create: subject not found → 404 RESOURCE_NOT_FOUND")
    void create_subjectNotFound_throws404() {
        AdminExamCreateRequest req = baseCreateRequest();
        when(subjectRepository.findById(SUBJECT_ACTIVE_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminExamService.createExam(ADMIN_USER_ID, req))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException ae = (AppException) ex;
                    assertThat(ae.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(ae.getCode()).isEqualTo("RESOURCE_NOT_FOUND");
                    assertThat(ae.getMessage()).contains("Subject");
                });

        verify(examRepository, never()).save(any(Exam.class));
    }

    @Test
    @DisplayName("create: inactive subject → 400 VALIDATION_FAILED")
    void create_inactiveSubject_throwsValidation() {
        AdminExamCreateRequest req = baseCreateRequest();
        req.setSubjectId(SUBJECT_INACTIVE_ID);
        when(subjectRepository.findById(SUBJECT_INACTIVE_ID))
                .thenReturn(Optional.of(inactiveSubject));

        assertThatThrownBy(() -> adminExamService.createExam(ADMIN_USER_ID, req))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException ae = (AppException) ex;
                    assertThat(ae.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(ae.getCode()).isEqualTo("VALIDATION_FAILED");
                    assertThat(ae.getMessage()).contains("not active");
                });

        verify(examRepository, never()).save(any(Exam.class));
    }

    // =========================================================
    // UPDATE
    // =========================================================

    @Test
    @DisplayName("update: DRAFT exam — updates only allowed metadata fields")
    void update_draftExam_success_updatesMetadataOnly() {
        Exam existing = baseExam(7L, ExamStatus.DRAFT);
        AdminExamUpdateRequest req = AdminExamUpdateRequest.builder()
                .title("New Title")
                .description("New Description")
                .durationMinutes(120)
                .difficulty(Difficulty.HARD)
                .tags("new,tags")
                .build();

        when(examRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(examRepository.save(any(Exam.class))).thenAnswer(inv -> inv.getArgument(0));
        when(subjectRepository.findById(SUBJECT_ACTIVE_ID)).thenReturn(Optional.of(activeSubject));
        when(examQuestionRepository.countByExamId(7L)).thenReturn(0L);

        AdminExamResponse result = adminExamService.updateExam(7L, req);

        assertThat(existing.getTitle()).isEqualTo("New Title");
        assertThat(existing.getDescription()).isEqualTo("New Description");
        assertThat(existing.getDurationMinutes()).isEqualTo(120);
        assertThat(existing.getDifficulty()).isEqualTo(Difficulty.HARD);
        assertThat(existing.getTags()).isEqualTo("new,tags");
        assertThat(result.getStatus()).isEqualTo(ExamStatus.DRAFT);
    }

    @Test
    @DisplayName("update: HIDDEN exam — also allowed for metadata edit")
    void update_hiddenExam_success_updatesMetadataOnly() {
        Exam existing = baseExam(8L, ExamStatus.HIDDEN);
        AdminExamUpdateRequest req = AdminExamUpdateRequest.builder()
                .title("Hidden Edit")
                .build();

        when(examRepository.findById(8L)).thenReturn(Optional.of(existing));
        when(examRepository.save(any(Exam.class))).thenAnswer(inv -> inv.getArgument(0));
        when(subjectRepository.findById(SUBJECT_ACTIVE_ID)).thenReturn(Optional.of(activeSubject));
        when(examQuestionRepository.countByExamId(8L)).thenReturn(0L);

        AdminExamResponse result = adminExamService.updateExam(8L, req);

        assertThat(existing.getTitle()).isEqualTo("Hidden Edit");
        assertThat(existing.getStatus()).isEqualTo(ExamStatus.HIDDEN);
        assertThat(result.getStatus()).isEqualTo(ExamStatus.HIDDEN);
    }

    @Test
    @DisplayName("update: PUBLISHED exam — 409 INVALID_STATE")
    void update_publishedExam_throwsInvalidState() {
        Exam existing = baseExam(9L, ExamStatus.PUBLISHED);
        when(examRepository.findById(9L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> adminExamService.updateExam(9L,
                AdminExamUpdateRequest.builder().title("nope").build()))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException ae = (AppException) ex;
                    assertThat(ae.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(ae.getCode()).isEqualTo("INVALID_STATE");
                });

        verify(examRepository, never()).save(any(Exam.class));
    }

    @Test
    @DisplayName("update: ARCHIVED exam — 409 INVALID_STATE")
    void update_archivedExam_throwsInvalidState() {
        Exam existing = baseExam(10L, ExamStatus.ARCHIVED);
        when(examRepository.findById(10L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> adminExamService.updateExam(10L,
                AdminExamUpdateRequest.builder().title("nope").build()))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException ae = (AppException) ex;
                    assertThat(ae.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(ae.getCode()).isEqualTo("INVALID_STATE");
                });

        verify(examRepository, never()).save(any(Exam.class));
    }

    @Test
    @DisplayName("update: PAID pricing in body → 400 VALIDATION_FAILED, no save")
    void update_nonFreePricing_throwsValidation() {
        Exam existing = baseExam(11L, ExamStatus.DRAFT);
        when(examRepository.findById(11L)).thenReturn(Optional.of(existing));

        AdminExamUpdateRequest req = AdminExamUpdateRequest.builder()
                .pricingType(ExamPricingType.PAID)
                .build();

        assertThatThrownBy(() -> adminExamService.updateExam(11L, req))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException ae = (AppException) ex;
                    assertThat(ae.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(ae.getCode()).isEqualTo("VALIDATION_FAILED");
                });

        verify(examRepository, never()).save(any(Exam.class));
    }

    @Test
    @DisplayName("update: non-zero price → 400 VALIDATION_FAILED, no save")
    void update_nonZeroPrice_throwsValidation() {
        Exam existing = baseExam(12L, ExamStatus.DRAFT);
        when(examRepository.findById(12L)).thenReturn(Optional.of(existing));

        AdminExamUpdateRequest req = AdminExamUpdateRequest.builder()
                .price(new BigDecimal("19.99"))
                .build();

        assertThatThrownBy(() -> adminExamService.updateExam(12L, req))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException ae = (AppException) ex;
                    assertThat(ae.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(ae.getCode()).isEqualTo("VALIDATION_FAILED");
                });

        verify(examRepository, never()).save(any(Exam.class));
    }

    @Test
    @DisplayName("update: does NOT change questionCount, status, examCode, createdBy, version")
    void update_doesNotChangeQuestionCountOrStatusOrCreatedBy() {
        Exam existing = baseExam(13L, ExamStatus.DRAFT);
        existing.setQuestionCount(7);
        existing.setVersion(3);
        existing.setCreatedBy(555L);
        existing.setExamCode("KEEP_ME_001");

        AdminExamUpdateRequest req = AdminExamUpdateRequest.builder()
                .title("Renamed")
                .build();

        when(examRepository.findById(13L)).thenReturn(Optional.of(existing));
        when(examRepository.save(any(Exam.class))).thenAnswer(inv -> inv.getArgument(0));
        when(subjectRepository.findById(SUBJECT_ACTIVE_ID)).thenReturn(Optional.of(activeSubject));
        when(examQuestionRepository.countByExamId(13L)).thenReturn(7L);

        adminExamService.updateExam(13L, req);

        assertThat(existing.getQuestionCount()).isEqualTo(7);
        assertThat(existing.getStatus()).isEqualTo(ExamStatus.DRAFT);
        assertThat(existing.getExamCode()).isEqualTo("KEEP_ME_001");
        assertThat(existing.getCreatedBy()).isEqualTo(555L);
        assertThat(existing.getVersion()).isEqualTo(3);
    }

    @Test
    @DisplayName("update: exam not found → 404 RESOURCE_NOT_FOUND")
    void update_examNotFound_throws404() {
        when(examRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminExamService.updateExam(404L,
                AdminExamUpdateRequest.builder().title("x").build()))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException ae = (AppException) ex;
                    assertThat(ae.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(ae.getCode()).isEqualTo("RESOURCE_NOT_FOUND");
                    assertThat(ae.getMessage()).contains("Exam").contains("404");
                });

        verify(examRepository, never()).save(any(Exam.class));
    }

    @Test
    @DisplayName("update: subject id change to inactive subject → 400 VALIDATION_FAILED")
    void update_subjectIdToInactive_throwsValidation() {
        Exam existing = baseExam(14L, ExamStatus.DRAFT);
        AdminExamUpdateRequest req = AdminExamUpdateRequest.builder()
                .subjectId(SUBJECT_INACTIVE_ID)
                .build();
        when(examRepository.findById(14L)).thenReturn(Optional.of(existing));
        when(subjectRepository.findById(SUBJECT_INACTIVE_ID))
                .thenReturn(Optional.of(inactiveSubject));

        assertThatThrownBy(() -> adminExamService.updateExam(14L, req))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException ae = (AppException) ex;
                    assertThat(ae.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(ae.getCode()).isEqualTo("VALIDATION_FAILED");
                });

        verify(examRepository, never()).save(any(Exam.class));
    }

    @Test
    @DisplayName("update: omitted subjectId still validates current subject is active")
    void update_currentSubjectInactive_throwsValidationOrBadRequest() {
        Exam existing = baseExam(15L, ExamStatus.DRAFT);
        Subject inactiveCurrentSubject = Subject.builder()
                .id(SUBJECT_ACTIVE_ID)
                .code(SUBJECT_ACTIVE_CODE)
                .name("Inactive Math")
                .isActive(false)
                .displayOrder(1)
                .build();
        AdminExamUpdateRequest req = AdminExamUpdateRequest.builder()
                .title("Should Not Save")
                .build();

        when(examRepository.findById(15L)).thenReturn(Optional.of(existing));
        when(subjectRepository.findById(SUBJECT_ACTIVE_ID))
                .thenReturn(Optional.of(inactiveCurrentSubject));

        assertThatThrownBy(() -> adminExamService.updateExam(15L, req))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException ae = (AppException) ex;
                    assertThat(ae.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(ae.getCode()).isEqualTo("VALIDATION_FAILED");
                    assertThat(ae.getMessage()).contains("not active");
                });

        assertThat(existing.getTitle()).isEqualTo("Existing exam 15");
        verify(examRepository, never()).save(any(Exam.class));
    }

    // =========================================================
    // COMPOSITION - Phase C1.2b-2
    // =========================================================

    @Test
    @DisplayName("Comp-01 add APPROVED question to DRAFT exam -> 200, mapping created, question_count incremented")
    void comp01_addApprovedQuestionToDraft_success() {
        Exam exam = baseExam(20L, ExamStatus.DRAFT);
        Question question = question(101L, QuestionStatus.APPROVED);
        when(examRepository.findById(20L)).thenReturn(Optional.of(exam));
        when(questionRepository.findById(101L)).thenReturn(Optional.of(question));
        when(examQuestionRepository.existsByExamIdAndQuestionId(20L, 101L)).thenReturn(false);
        when(examQuestionRepository.findMaxQuestionOrderByExamId(20L)).thenReturn(0);
        when(examRepository.save(any(Exam.class))).thenAnswer(inv -> inv.getArgument(0));
        stubAdminResponseDependencies(20L, 1L);

        AdminExamResponse result = adminExamService.addQuestion(20L, 101L);

        ArgumentCaptor<ExamQuestion> mappingCaptor = ArgumentCaptor.forClass(ExamQuestion.class);
        verify(examQuestionRepository).save(mappingCaptor.capture());
        assertThat(mappingCaptor.getValue().getExamId()).isEqualTo(20L);
        assertThat(mappingCaptor.getValue().getQuestionId()).isEqualTo(101L);
        assertThat(mappingCaptor.getValue().getQuestionOrder()).isEqualTo(1);
        assertThat(exam.getQuestionCount()).isEqualTo(1);
        assertThat(result.getQuestionCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Comp-02 add PUBLISHED question to DRAFT exam -> 200")
    void comp02_addPublishedQuestionToDraft_success() {
        Exam exam = baseExam(21L, ExamStatus.DRAFT);
        when(examRepository.findById(21L)).thenReturn(Optional.of(exam));
        when(questionRepository.findById(102L)).thenReturn(Optional.of(question(102L, QuestionStatus.PUBLISHED)));
        when(examQuestionRepository.existsByExamIdAndQuestionId(21L, 102L)).thenReturn(false);
        when(examQuestionRepository.findMaxQuestionOrderByExamId(21L)).thenReturn(3);
        when(examRepository.save(any(Exam.class))).thenAnswer(inv -> inv.getArgument(0));
        stubAdminResponseDependencies(21L, 4L);

        AdminExamResponse result = adminExamService.addQuestion(21L, 102L);

        ArgumentCaptor<ExamQuestion> mappingCaptor = ArgumentCaptor.forClass(ExamQuestion.class);
        verify(examQuestionRepository).save(mappingCaptor.capture());
        assertThat(mappingCaptor.getValue().getQuestionOrder()).isEqualTo(4);
        assertThat(result.getQuestionCount()).isEqualTo(4);
    }

    @Test
    @DisplayName("Comp-03 add DRAFT-status question -> 400 VALIDATION_FAILED")
    void comp03_addDraftQuestion_rejectedValidation() {
        Exam exam = baseExam(22L, ExamStatus.DRAFT);
        when(examRepository.findById(22L)).thenReturn(Optional.of(exam));
        when(questionRepository.findById(103L)).thenReturn(Optional.of(question(103L, QuestionStatus.DRAFT)));

        assertAppException(
                () -> adminExamService.addQuestion(22L, 103L),
                HttpStatus.BAD_REQUEST,
                "VALIDATION_FAILED");

        verify(examQuestionRepository, never()).save(any(ExamQuestion.class));
        verify(examRepository, never()).save(any(Exam.class));
    }

    @Test
    @DisplayName("Comp-04 add ARCHIVED-status question -> 400 VALIDATION_FAILED")
    void comp04_addArchivedQuestion_rejectedValidation() {
        Exam exam = baseExam(23L, ExamStatus.DRAFT);
        when(examRepository.findById(23L)).thenReturn(Optional.of(exam));
        when(questionRepository.findById(104L)).thenReturn(Optional.of(question(104L, QuestionStatus.ARCHIVED)));

        assertAppException(
                () -> adminExamService.addQuestion(23L, 104L),
                HttpStatus.BAD_REQUEST,
                "VALIDATION_FAILED");

        verify(examQuestionRepository, never()).save(any(ExamQuestion.class));
        verify(examRepository, never()).save(any(Exam.class));
    }

    @Test
    @DisplayName("Comp-05 add duplicate question -> 409 DUPLICATE")
    void comp05_addDuplicateQuestion_rejectedConflict() {
        Exam exam = baseExam(24L, ExamStatus.DRAFT);
        when(examRepository.findById(24L)).thenReturn(Optional.of(exam));
        when(questionRepository.findById(105L)).thenReturn(Optional.of(question(105L, QuestionStatus.APPROVED)));
        when(examQuestionRepository.existsByExamIdAndQuestionId(24L, 105L)).thenReturn(true);

        assertAppException(
                () -> adminExamService.addQuestion(24L, 105L),
                HttpStatus.CONFLICT,
                "DUPLICATE");

        verify(examQuestionRepository, never()).save(any(ExamQuestion.class));
        verify(examRepository, never()).save(any(Exam.class));
    }

    @Test
    @DisplayName("Comp-06 add to PENDING_REVIEW exam -> 409 INVALID_STATE")
    void comp06_addToPendingReview_rejectedInvalidState() {
        assertAddRejectedForExamStatus(25L, ExamStatus.PENDING_REVIEW);
    }

    @Test
    @DisplayName("Comp-07 add to PUBLISHED exam -> 409 INVALID_STATE")
    void comp07_addToPublished_rejectedInvalidState() {
        assertAddRejectedForExamStatus(26L, ExamStatus.PUBLISHED);
    }

    @Test
    @DisplayName("Comp-08 add to HIDDEN exam -> 409 INVALID_STATE")
    void comp08_addToHidden_rejectedInvalidState() {
        assertAddRejectedForExamStatus(27L, ExamStatus.HIDDEN);
    }

    @Test
    @DisplayName("Comp-09 add to ARCHIVED exam -> 409 INVALID_STATE")
    void comp09_addToArchived_rejectedInvalidState() {
        assertAddRejectedForExamStatus(28L, ExamStatus.ARCHIVED);
    }

    @Test
    @DisplayName("Comp-10 remove question from DRAFT -> 200, mapping deleted, count decremented, Question preserved")
    void comp10_removeQuestionFromDraft_successMappingOnly() {
        Exam exam = baseExam(29L, ExamStatus.DRAFT);
        exam.setQuestionCount(2);
        when(examRepository.findById(29L)).thenReturn(Optional.of(exam));
        when(examQuestionRepository.deleteByExamIdAndQuestionId(29L, 106L)).thenReturn(1);
        when(examRepository.save(any(Exam.class))).thenAnswer(inv -> inv.getArgument(0));
        stubAdminResponseDependencies(29L, 1L);

        AdminExamResponse result = adminExamService.removeQuestion(29L, 106L);

        assertThat(exam.getQuestionCount()).isEqualTo(1);
        assertThat(result.getQuestionCount()).isEqualTo(1);
        verify(examQuestionRepository).deleteByExamIdAndQuestionId(29L, 106L);
        verify(questionRepository, never()).delete(any(Question.class));
    }

    @Test
    @DisplayName("Comp-11 remove from non-DRAFT -> 409 INVALID_STATE")
    void comp11_removeFromNonDraft_rejectedInvalidState() {
        Exam exam = baseExam(30L, ExamStatus.PUBLISHED);
        when(examRepository.findById(30L)).thenReturn(Optional.of(exam));

        assertAppException(
                () -> adminExamService.removeQuestion(30L, 106L),
                HttpStatus.CONFLICT,
                "INVALID_STATE");

        verify(examQuestionRepository, never()).deleteByExamIdAndQuestionId(any(), any());
    }

    @Test
    @DisplayName("Comp-12 remove a question not in the exam -> 404 RESOURCE_NOT_FOUND")
    void comp12_removeQuestionNotInExam_rejectedNotFound() {
        Exam exam = baseExam(31L, ExamStatus.DRAFT);
        when(examRepository.findById(31L)).thenReturn(Optional.of(exam));
        when(examQuestionRepository.deleteByExamIdAndQuestionId(31L, 404L)).thenReturn(0);

        assertAppException(
                () -> adminExamService.removeQuestion(31L, 404L),
                HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND");

        verify(examRepository, never()).save(any(Exam.class));
    }

    @Test
    @DisplayName("Comp-13 reorder simple ([1,2,3] -> [2,1,3]) -> 200")
    void comp13_reorderSimple_success() {
        Exam exam = baseExam(32L, ExamStatus.DRAFT);
        when(examRepository.findById(32L)).thenReturn(Optional.of(exam));
        when(examQuestionRepository.findByExamIdOrderByQuestionOrderAscIdAsc(32L))
                .thenReturn(mappings(32L, 1L, 2L, 3L));
        stubAdminResponseDependencies(32L, 3L);

        AdminExamResponse result = adminExamService.reorderQuestions(32L, List.of(2L, 1L, 3L));

        assertThat(result.getQuestionCount()).isEqualTo(3);
        verify(examQuestionRepository).moveQuestionOrdersToTemporaryNegativeRange(32L);
        verify(examQuestionRepository).updateQuestionOrder(32L, 2L, 1);
        verify(examQuestionRepository).updateQuestionOrder(32L, 1L, 2);
        verify(examQuestionRepository).updateQuestionOrder(32L, 3L, 3);
    }

    @Test
    @DisplayName("Comp-14 reorder full reverse ([1,2,3,4] -> [4,3,2,1]) -> 200 via two-phase update")
    void comp14_reorderFullReverse_successTwoPhase() {
        Exam exam = baseExam(33L, ExamStatus.DRAFT);
        when(examRepository.findById(33L)).thenReturn(Optional.of(exam));
        when(examQuestionRepository.findByExamIdOrderByQuestionOrderAscIdAsc(33L))
                .thenReturn(mappings(33L, 1L, 2L, 3L, 4L));
        stubAdminResponseDependencies(33L, 4L);

        AdminExamResponse result = adminExamService.reorderQuestions(33L, List.of(4L, 3L, 2L, 1L));

        assertThat(result.getQuestionCount()).isEqualTo(4);
        verify(examQuestionRepository).moveQuestionOrdersToTemporaryNegativeRange(33L);
        verify(examQuestionRepository).updateQuestionOrder(33L, 4L, 1);
        verify(examQuestionRepository).updateQuestionOrder(33L, 3L, 2);
        verify(examQuestionRepository).updateQuestionOrder(33L, 2L, 3);
        verify(examQuestionRepository).updateQuestionOrder(33L, 1L, 4);
    }

    @Test
    @DisplayName("Comp-15 reorder with duplicate IDs -> 400 VALIDATION_FAILED")
    void comp15_reorderDuplicateIds_rejectedValidation() {
        assertInvalidReorder(List.of(1L, 1L, 3L));
    }

    @Test
    @DisplayName("Comp-16 reorder with missing IDs -> 400 VALIDATION_FAILED")
    void comp16_reorderMissingIds_rejectedValidation() {
        assertInvalidReorder(List.of(1L, 2L));
    }

    @Test
    @DisplayName("Comp-17 reorder with extra IDs -> 400 VALIDATION_FAILED")
    void comp17_reorderExtraIds_rejectedValidation() {
        assertInvalidReorder(List.of(1L, 2L, 3L, 99L));
    }

    @Test
    @DisplayName("Comp-18 reorder of non-DRAFT exam -> 409 INVALID_STATE")
    void comp18_reorderNonDraft_rejectedInvalidState() {
        Exam exam = baseExam(35L, ExamStatus.PUBLISHED);
        when(examRepository.findById(35L)).thenReturn(Optional.of(exam));

        assertAppException(
                () -> adminExamService.reorderQuestions(35L, List.of(1L, 2L, 3L)),
                HttpStatus.CONFLICT,
                "INVALID_STATE");

        verify(examQuestionRepository, never()).moveQuestionOrdersToTemporaryNegativeRange(any());
    }

    @Test
    @DisplayName("Comp-19 reorder empty exam with empty list -> 400 VALIDATION_FAILED")
    void comp19_reorderEmptyExamWithEmptyList_rejectedValidation() {
        Exam exam = baseExam(36L, ExamStatus.DRAFT);
        when(examRepository.findById(36L)).thenReturn(Optional.of(exam));
        when(examQuestionRepository.findByExamIdOrderByQuestionOrderAscIdAsc(36L))
                .thenReturn(List.of());

        assertAppException(
                () -> adminExamService.reorderQuestions(36L, List.of()),
                HttpStatus.BAD_REQUEST,
                "VALIDATION_FAILED");

        verify(examQuestionRepository, never()).moveQuestionOrdersToTemporaryNegativeRange(any());
    }

    // =========================================================
    // WORKFLOW - Phase C1.2b-2
    // =========================================================

    @Test
    @DisplayName("WF-01 DRAFT -> submit-review -> PENDING_REVIEW; reviewedBy & publishDate unchanged")
    void wf01_submitReviewFromDraft_successAuditUnchanged() {
        Exam exam = baseExam(40L, ExamStatus.DRAFT);
        OffsetDateTime oldPublishDate = FIXED_TIME.minusDays(1);
        exam.setReviewedBy(777L);
        exam.setPublishDate(oldPublishDate);
        when(examRepository.findById(40L)).thenReturn(Optional.of(exam));
        when(examRepository.save(any(Exam.class))).thenAnswer(inv -> inv.getArgument(0));
        stubAdminResponseDependencies(40L, 0L);

        AdminExamResponse result = adminExamService.submitReview(40L);

        assertThat(result.getStatus()).isEqualTo(ExamStatus.PENDING_REVIEW);
        assertThat(result.getReviewedBy()).isEqualTo(777L);
        assertThat(result.getPublishDate()).isEqualTo(oldPublishDate);
    }

    @Test
    @DisplayName("WF-02 submit-review on PUBLISHED -> 409 INVALID_STATE")
    void wf02_submitReviewOnPublished_rejectedInvalidState() {
        assertWorkflowInvalidState(41L, ExamStatus.PUBLISHED, () -> adminExamService.submitReview(41L));
    }

    @Test
    @DisplayName("WF-03 submit-review on HIDDEN -> 409 INVALID_STATE")
    void wf03_submitReviewOnHidden_rejectedInvalidState() {
        assertWorkflowInvalidState(42L, ExamStatus.HIDDEN, () -> adminExamService.submitReview(42L));
    }

    @Test
    @DisplayName("WF-04 submit-review on ARCHIVED -> 409 INVALID_STATE")
    void wf04_submitReviewOnArchived_rejectedInvalidState() {
        assertWorkflowInvalidState(43L, ExamStatus.ARCHIVED, () -> adminExamService.submitReview(43L));
    }

    @Test
    @DisplayName("WF-05 PENDING_REVIEW -> publish (SUPER_ADMIN) -> PUBLISHED; reviewedBy set, publishDate set")
    void wf05_publishPendingReviewAsSuperAdmin_successAuditSet() {
        Exam exam = publishableExam(44L, ExamStatus.PENDING_REVIEW, 1);
        when(examRepository.findById(44L)).thenReturn(Optional.of(exam));
        when(examQuestionRepository.findByExamIdOrderByQuestionOrderAscIdAsc(44L))
                .thenReturn(mappings(44L, 201L));
        when(questionRepository.findAllById(List.of(201L)))
                .thenReturn(List.of(question(201L, QuestionStatus.APPROVED)));
        when(examRepository.save(any(Exam.class))).thenAnswer(inv -> inv.getArgument(0));
        stubAdminResponseDependencies(44L, 1L);

        AdminExamResponse result = adminExamService.publish(902L, 44L);

        assertThat(result.getStatus()).isEqualTo(ExamStatus.PUBLISHED);
        assertThat(result.getReviewedBy()).isEqualTo(902L);
        assertThat(result.getPublishDate()).isNotNull();
    }

    @Test
    @DisplayName("WF-07 publish on DRAFT -> 409 INVALID_STATE")
    void wf07_publishOnDraft_rejectedInvalidState() {
        assertWorkflowInvalidState(45L, ExamStatus.DRAFT, () -> adminExamService.publish(902L, 45L));
    }

    @Test
    @DisplayName("WF-08 publish with zero questions -> 409 INVALID_STATE")
    void wf08_publishWithZeroQuestions_rejectedInvalidState() {
        Exam exam = publishableExam(46L, ExamStatus.PENDING_REVIEW, 0);
        when(examRepository.findById(46L)).thenReturn(Optional.of(exam));
        when(examQuestionRepository.countByExamId(46L)).thenReturn(0L);

        assertAppException(
                () -> adminExamService.publish(902L, 46L),
                HttpStatus.CONFLICT,
                "INVALID_STATE");

        verify(examRepository, never()).save(any(Exam.class));
    }

    @Test
    @DisplayName("WF-09 publish when one question is not APPROVED/PUBLISHED -> 400 VALIDATION_FAILED")
    void wf09_publishWithInvalidQuestionStatus_rejectedValidation() {
        Exam exam = publishableExam(47L, ExamStatus.PENDING_REVIEW, 1);
        when(examRepository.findById(47L)).thenReturn(Optional.of(exam));
        when(examQuestionRepository.countByExamId(47L)).thenReturn(1L);
        when(examQuestionRepository.findByExamIdOrderByQuestionOrderAscIdAsc(47L))
                .thenReturn(mappings(47L, 202L));
        when(questionRepository.findAllById(List.of(202L)))
                .thenReturn(List.of(question(202L, QuestionStatus.DRAFT)));

        assertAppException(
                () -> adminExamService.publish(902L, 47L),
                HttpStatus.BAD_REQUEST,
                "VALIDATION_FAILED");

        assertThat(exam.getStatus()).isEqualTo(ExamStatus.PENDING_REVIEW);
        verify(examRepository, never()).save(any(Exam.class));
    }

    @Test
    @DisplayName("WF-10 publish when pricingType != FREE -> 400 VALIDATION_FAILED")
    void wf10_publishWithPaidPricing_rejectedValidation() {
        Exam exam = publishableExam(48L, ExamStatus.PENDING_REVIEW, 1);
        exam.setPricingType(ExamPricingType.PAID);
        when(examRepository.findById(48L)).thenReturn(Optional.of(exam));
        when(examQuestionRepository.countByExamId(48L)).thenReturn(1L);
        when(examQuestionRepository.findByExamIdOrderByQuestionOrderAscIdAsc(48L))
                .thenReturn(mappings(48L, 203L));
        when(questionRepository.findAllById(List.of(203L)))
                .thenReturn(List.of(question(203L, QuestionStatus.APPROVED)));

        assertAppException(
                () -> adminExamService.publish(902L, 48L),
                HttpStatus.BAD_REQUEST,
                "VALIDATION_FAILED");

        assertThat(exam.getStatus()).isEqualTo(ExamStatus.PENDING_REVIEW);
        verify(examRepository, never()).save(any(Exam.class));
    }

    @Test
    @DisplayName("publish validation: question_count mismatch -> 409 INVALID_STATE")
    void publishQuestionCountMismatch_rejectedInvalidState() {
        Exam exam = publishableExam(49L, ExamStatus.PENDING_REVIEW, 2);
        when(examRepository.findById(49L)).thenReturn(Optional.of(exam));
        when(examQuestionRepository.countByExamId(49L)).thenReturn(1L);

        assertAppException(
                () -> adminExamService.publish(902L, 49L),
                HttpStatus.CONFLICT,
                "INVALID_STATE");

        verify(examRepository, never()).save(any(Exam.class));
    }

    @Test
    @DisplayName("publish validation: price != 0 -> 400 VALIDATION_FAILED")
    void publishNonZeroPrice_rejectedValidation() {
        Exam exam = publishableExam(50L, ExamStatus.PENDING_REVIEW, 1);
        exam.setPrice(new BigDecimal("10.00"));
        when(examRepository.findById(50L)).thenReturn(Optional.of(exam));
        when(examQuestionRepository.countByExamId(50L)).thenReturn(1L);
        when(examQuestionRepository.findByExamIdOrderByQuestionOrderAscIdAsc(50L))
                .thenReturn(mappings(50L, 204L));
        when(questionRepository.findAllById(List.of(204L)))
                .thenReturn(List.of(question(204L, QuestionStatus.APPROVED)));

        assertAppException(
                () -> adminExamService.publish(902L, 50L),
                HttpStatus.BAD_REQUEST,
                "VALIDATION_FAILED");

        verify(examRepository, never()).save(any(Exam.class));
    }

    @Test
    @DisplayName("WF-11 PENDING_REVIEW -> reject-review -> DRAFT; reviewedBy & publishDate unchanged")
    void wf11_rejectReviewFromPending_successAuditUnchanged() {
        Exam exam = baseExam(51L, ExamStatus.PENDING_REVIEW);
        OffsetDateTime oldPublishDate = FIXED_TIME.minusDays(1);
        exam.setReviewedBy(777L);
        exam.setPublishDate(oldPublishDate);
        when(examRepository.findById(51L)).thenReturn(Optional.of(exam));
        when(examRepository.save(any(Exam.class))).thenAnswer(inv -> inv.getArgument(0));
        stubAdminResponseDependencies(51L, 0L);

        AdminExamResponse result = adminExamService.rejectReview(51L);

        assertThat(result.getStatus()).isEqualTo(ExamStatus.DRAFT);
        assertThat(result.getReviewedBy()).isEqualTo(777L);
        assertThat(result.getPublishDate()).isEqualTo(oldPublishDate);
    }

    @Test
    @DisplayName("WF-13 reject-review on DRAFT -> 409 INVALID_STATE")
    void wf13_rejectReviewOnDraft_rejectedInvalidState() {
        assertWorkflowInvalidState(52L, ExamStatus.DRAFT, () -> adminExamService.rejectReview(52L));
    }

    @Test
    @DisplayName("WF-14 PUBLISHED -> hide -> HIDDEN; audit unchanged")
    void wf14_hidePublished_successAuditUnchanged() {
        Exam exam = baseExam(53L, ExamStatus.PUBLISHED);
        OffsetDateTime oldPublishDate = FIXED_TIME.minusDays(1);
        exam.setReviewedBy(777L);
        exam.setPublishDate(oldPublishDate);
        when(examRepository.findById(53L)).thenReturn(Optional.of(exam));
        when(examRepository.save(any(Exam.class))).thenAnswer(inv -> inv.getArgument(0));
        stubAdminResponseDependencies(53L, 0L);

        AdminExamResponse result = adminExamService.hide(53L);

        assertThat(result.getStatus()).isEqualTo(ExamStatus.HIDDEN);
        assertThat(result.getReviewedBy()).isEqualTo(777L);
        assertThat(result.getPublishDate()).isEqualTo(oldPublishDate);
    }

    @Test
    @DisplayName("WF-15 hide on DRAFT -> 409 INVALID_STATE")
    void wf15_hideOnDraft_rejectedInvalidState() {
        assertWorkflowInvalidState(54L, ExamStatus.DRAFT, () -> adminExamService.hide(54L));
    }

    @Test
    @DisplayName("WF-16 HIDDEN -> publish (SUPER_ADMIN) -> PUBLISHED; reviewedBy AND publishDate overwritten")
    void wf16_publishHidden_successAuditOverwritten() {
        Exam exam = publishableExam(55L, ExamStatus.HIDDEN, 1);
        OffsetDateTime oldPublishDate = FIXED_TIME.minusDays(2);
        exam.setReviewedBy(777L);
        exam.setPublishDate(oldPublishDate);
        when(examRepository.findById(55L)).thenReturn(Optional.of(exam));
        when(examQuestionRepository.findByExamIdOrderByQuestionOrderAscIdAsc(55L))
                .thenReturn(mappings(55L, 205L));
        when(questionRepository.findAllById(List.of(205L)))
                .thenReturn(List.of(question(205L, QuestionStatus.PUBLISHED)));
        when(examRepository.save(any(Exam.class))).thenAnswer(inv -> inv.getArgument(0));
        stubAdminResponseDependencies(55L, 1L);

        AdminExamResponse result = adminExamService.publish(902L, 55L);

        assertThat(result.getStatus()).isEqualTo(ExamStatus.PUBLISHED);
        assertThat(result.getReviewedBy()).isEqualTo(902L);
        assertThat(result.getPublishDate()).isNotNull();
        assertThat(result.getPublishDate()).isNotEqualTo(oldPublishDate);
    }

    @Test
    @DisplayName("WF-17 HIDDEN -> return-to-draft -> DRAFT; reviewedBy & publishDate unchanged")
    void wf17_returnHiddenToDraft_successAuditUnchanged() {
        Exam exam = baseExam(56L, ExamStatus.HIDDEN);
        OffsetDateTime oldPublishDate = FIXED_TIME.minusDays(1);
        exam.setReviewedBy(777L);
        exam.setPublishDate(oldPublishDate);
        when(examRepository.findById(56L)).thenReturn(Optional.of(exam));
        when(examRepository.save(any(Exam.class))).thenAnswer(inv -> inv.getArgument(0));
        stubAdminResponseDependencies(56L, 0L);

        AdminExamResponse result = adminExamService.returnToDraft(56L);

        assertThat(result.getStatus()).isEqualTo(ExamStatus.DRAFT);
        assertThat(result.getReviewedBy()).isEqualTo(777L);
        assertThat(result.getPublishDate()).isEqualTo(oldPublishDate);
    }

    @Test
    @DisplayName("WF-18 return-to-draft on PENDING_REVIEW -> 409 INVALID_STATE")
    void wf18_returnPendingReviewToDraft_rejectedInvalidState() {
        assertWorkflowInvalidState(57L, ExamStatus.PENDING_REVIEW, () -> adminExamService.returnToDraft(57L));
    }

    @Test
    @DisplayName("WF-19 return-to-draft on DRAFT -> 409 INVALID_STATE")
    void wf19_returnDraftToDraft_rejectedInvalidState() {
        assertWorkflowInvalidState(58L, ExamStatus.DRAFT, () -> adminExamService.returnToDraft(58L));
    }

    @Test
    @DisplayName("WF-20 PUBLISHED -> archive -> ARCHIVED")
    void wf20_archivePublished_success() {
        assertArchiveSuccess(59L, ExamStatus.PUBLISHED);
    }

    @Test
    @DisplayName("WF-21 HIDDEN -> archive -> ARCHIVED")
    void wf21_archiveHidden_success() {
        assertArchiveSuccess(60L, ExamStatus.HIDDEN);
    }

    @Test
    @DisplayName("WF-22 DRAFT -> archive -> 409 INVALID_STATE")
    void wf22_archiveDraft_rejectedInvalidState() {
        assertWorkflowInvalidState(61L, ExamStatus.DRAFT, () -> adminExamService.archive(61L));
    }

    @Test
    @DisplayName("WF-23 ARCHIVED -> any workflow transition -> 409 INVALID_STATE")
    void wf23_archivedRejectsAllWorkflowTransitions() {
        Exam archived = baseExam(62L, ExamStatus.ARCHIVED);
        when(examRepository.findById(62L)).thenReturn(Optional.of(archived));

        assertAppException(() -> adminExamService.submitReview(62L), HttpStatus.CONFLICT, "INVALID_STATE");
        assertAppException(() -> adminExamService.publish(902L, 62L), HttpStatus.CONFLICT, "INVALID_STATE");
        assertAppException(() -> adminExamService.hide(62L), HttpStatus.CONFLICT, "INVALID_STATE");
        assertAppException(() -> adminExamService.archive(62L), HttpStatus.CONFLICT, "INVALID_STATE");
        assertAppException(() -> adminExamService.rejectReview(62L), HttpStatus.CONFLICT, "INVALID_STATE");
        assertAppException(() -> adminExamService.returnToDraft(62L), HttpStatus.CONFLICT, "INVALID_STATE");
        verify(examRepository, never()).save(any(Exam.class));
    }

    // =========================================================
    // DISCARD
    // =========================================================

    @Test
    @DisplayName("discardDraft: DRAFT exam is hard-deleted")
    void discardDraft_deletesExam_whenStatusDraft() {
        Exam draft = baseExam(70L, ExamStatus.DRAFT);
        when(examRepository.findById(70L)).thenReturn(Optional.of(draft));

        adminExamService.discardDraftExam(70L);

        verify(examRepository).delete(draft);
        verify(examRepository, never()).save(any(Exam.class));
        verify(questionRepository, never()).delete(any(Question.class));
    }

    @Test
    @DisplayName("discardDraft: missing exam -> 404 RESOURCE_NOT_FOUND")
    void discardDraft_throws404_whenExamMissing() {
        when(examRepository.findById(404L)).thenReturn(Optional.empty());

        assertAppException(
                () -> adminExamService.discardDraftExam(404L),
                HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND");

        verify(examRepository, never()).delete(any(Exam.class));
    }

    @Test
    @DisplayName("discardDraft: COMPOSING exam -> 409 INVALID_STATE")
    void discardDraft_rejectsComposing_409() {
        assertDiscardRejectedForStatus(71L, ExamStatus.COMPOSING);
    }

    @Test
    @DisplayName("discardDraft: PENDING_REVIEW exam -> 409 INVALID_STATE")
    void discardDraft_rejectsPendingReview_409() {
        assertDiscardRejectedForStatus(72L, ExamStatus.PENDING_REVIEW);
    }

    @Test
    @DisplayName("discardDraft: PUBLISHED exam -> 409 INVALID_STATE")
    void discardDraft_rejectsPublished_409() {
        assertDiscardRejectedForStatus(73L, ExamStatus.PUBLISHED);
    }

    @Test
    @DisplayName("discardDraft: HIDDEN exam -> 409 INVALID_STATE")
    void discardDraft_rejectsHidden_409() {
        assertDiscardRejectedForStatus(74L, ExamStatus.HIDDEN);
    }

    @Test
    @DisplayName("discardDraft: ARCHIVED exam -> 409 INVALID_STATE")
    void discardDraft_rejectsArchived_409() {
        assertDiscardRejectedForStatus(75L, ExamStatus.ARCHIVED);
    }

    @Test
    @DisplayName("discardDraft: LOCKED exam -> 409 INVALID_STATE")
    void discardDraft_rejectsLocked_409() {
        assertDiscardRejectedForStatus(76L, ExamStatus.LOCKED);
    }

    @Test
    @DisplayName("discardDraft: service has no Question delete path")
    void discardDraft_doesNotDeleteQuestions_serviceInteraction() {
        Exam draft = baseExam(77L, ExamStatus.DRAFT);
        when(examRepository.findById(77L)).thenReturn(Optional.of(draft));

        adminExamService.discardDraftExam(77L);

        verify(examRepository).delete(draft);
        verify(questionRepository, never()).delete(any(Question.class));
    }

    // =========================================================
    // LIST / GET
    // =========================================================

    @Test
    @DisplayName("list: returns paged metadata DTOs (no questions)")
    void listAdminExams_returnsPagedMetadata() {
        Pageable pageable = PageRequest.of(0, 20);
        Exam draft = baseExam(1L, ExamStatus.DRAFT);
        Exam hidden = baseExam(2L, ExamStatus.HIDDEN);
        when(examRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(draft, hidden), pageable, 2));

        Page<AdminExamSummaryResponse> result =
                adminExamService.listAdminExams(null, null, pageable);

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(ExamStatus.DRAFT);
        assertThat(result.getContent().get(1).getStatus()).isEqualTo(ExamStatus.HIDDEN);
        // Summary DTO must not expose questions / publishDate / createdBy / etc.
        assertThat(AdminExamSummaryResponse.class.getDeclaredFields())
                .extracting(java.lang.reflect.Field::getName)
                .doesNotContain("questions", "publishDate", "createdBy",
                        "reviewedBy", "totalAttempts", "avgScore");
        verify(examRepository).findAll(pageable);
    }

    @Test
    @DisplayName("list: filterByStatus propagates status arg to repository")
    void listAdminExams_filterByStatus() {
        Pageable pageable = PageRequest.of(0, 20);
        Exam draft = baseExam(3L, ExamStatus.DRAFT);
        when(examRepository.findByStatus(eq(ExamStatus.DRAFT), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(draft), pageable, 1));

        Page<AdminExamSummaryResponse> result =
                adminExamService.listAdminExams(ExamStatus.DRAFT, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(ExamStatus.DRAFT);
        verify(examRepository).findByStatus(ExamStatus.DRAFT, pageable);
    }

    @Test
    @DisplayName("list: filterBySubjectId propagates subject arg to repository")
    void listAdminExams_filterBySubjectId() {
        Pageable pageable = PageRequest.of(0, 20);
        when(examRepository.findBySubjectId(eq(SUBJECT_ACTIVE_ID), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        adminExamService.listAdminExams(null, SUBJECT_ACTIVE_ID, pageable);

        verify(examRepository).findBySubjectId(SUBJECT_ACTIVE_ID, pageable);
    }

    @Test
    @DisplayName("list: filterByStatusAndSubjectId propagates both args to repository")
    void listAdminExams_filterByStatusAndSubjectId() {
        Pageable pageable = PageRequest.of(0, 20);
        Exam draft = baseExam(4L, ExamStatus.DRAFT);
        when(examRepository.findByStatusAndSubjectId(
                eq(ExamStatus.DRAFT), eq(SUBJECT_ACTIVE_ID), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(draft), pageable, 1));

        Page<AdminExamSummaryResponse> result =
                adminExamService.listAdminExams(
                        ExamStatus.DRAFT, SUBJECT_ACTIVE_ID, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getSubjectId()).isEqualTo(SUBJECT_ACTIVE_ID);
        verify(examRepository).findByStatusAndSubjectId(
                ExamStatus.DRAFT, SUBJECT_ACTIVE_ID, pageable);
    }

    @Test
    @DisplayName("get: found returns metadata, no question array, includes subjectCode")
    void getAdminExam_found_returnsMetadataNoQuestions() {
        Exam exam = baseExam(5L, ExamStatus.DRAFT);
        when(examRepository.findById(5L)).thenReturn(Optional.of(exam));
        when(subjectRepository.findById(SUBJECT_ACTIVE_ID)).thenReturn(Optional.of(activeSubject));
        when(examQuestionRepository.countByExamId(5L)).thenReturn(0L);

        AdminExamResponse result = adminExamService.getAdminExam(5L);

        assertThat(result.getId()).isEqualTo(5L);
        assertThat(result.getSubjectId()).isEqualTo(SUBJECT_ACTIVE_ID);
        assertThat(result.getSubjectCode()).isEqualTo(SUBJECT_ACTIVE_CODE);
        assertThat(result.getStatus()).isEqualTo(ExamStatus.DRAFT);
        // AdminExamResponse must NOT have a "questions" or "options" or "explanation" field.
        assertThat(AdminExamResponse.class.getDeclaredFields())
                .extracting(java.lang.reflect.Field::getName)
                .doesNotContain("questions", "options", "explanation",
                        "correctOptionId");
    }

    @Test
    @DisplayName("get: not found → 404 RESOURCE_NOT_FOUND")
    void getAdminExam_notFound_throws404() {
        when(examRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminExamService.getAdminExam(404L))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException ae = (AppException) ex;
                    assertThat(ae.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(ae.getCode()).isEqualTo("RESOURCE_NOT_FOUND");
                });
    }

    @Test
    @DisplayName("create response excludes content fields (no questions/options/explanation)")
    void createResponse_excludesContentFields() {
        // Static field-level whitelist check — fast.
        assertThat(AdminExamResponse.class.getDeclaredFields())
                .extracting(java.lang.reflect.Field::getName)
                .containsExactlyInAnyOrder(
                        "id", "examCode", "title", "subjectId", "subjectCode",
                        "description", "questionCount", "durationMinutes", "difficulty",
                        "pricingType", "price", "status", "tags", "publishDate",
                        "version", "createdBy", "reviewedBy", "createdAt", "updatedAt");
    }

    @Test
    @DisplayName("update DTO must not contain examCode (immutable in this batch)")
    void updateDto_doesNotExposeExamCode() {
        assertThat(AdminExamUpdateRequest.class.getDeclaredFields())
                .extracting(java.lang.reflect.Field::getName)
                .doesNotContain("examCode", "id", "status", "questionCount",
                        "createdBy", "reviewedBy", "publishDate", "version",
                        "totalAttempts", "avgScore");
    }

    // =========================================================
    // Helpers
    // =========================================================

    private void assertAddRejectedForExamStatus(Long examId, ExamStatus status) {
        Exam exam = baseExam(examId, status);
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        assertAppException(
                () -> adminExamService.addQuestion(examId, 999L),
                HttpStatus.CONFLICT,
                "INVALID_STATE");

        verify(questionRepository, never()).findById(any());
        verify(examQuestionRepository, never()).save(any(ExamQuestion.class));
        verify(examRepository, never()).save(any(Exam.class));
    }

    private void assertInvalidReorder(List<Long> requestedQuestionIds) {
        Exam exam = baseExam(34L, ExamStatus.DRAFT);
        when(examRepository.findById(34L)).thenReturn(Optional.of(exam));
        when(examQuestionRepository.findByExamIdOrderByQuestionOrderAscIdAsc(34L))
                .thenReturn(mappings(34L, 1L, 2L, 3L));

        assertAppException(
                () -> adminExamService.reorderQuestions(34L, requestedQuestionIds),
                HttpStatus.BAD_REQUEST,
                "VALIDATION_FAILED");

        verify(examQuestionRepository, never()).moveQuestionOrdersToTemporaryNegativeRange(any());
    }

    private void assertWorkflowInvalidState(Long examId, ExamStatus status, Runnable action) {
        Exam exam = baseExam(examId, status);
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        assertAppException(action, HttpStatus.CONFLICT, "INVALID_STATE");

        verify(examRepository, never()).save(any(Exam.class));
    }

    private void assertDiscardRejectedForStatus(Long examId, ExamStatus status) {
        Exam exam = baseExam(examId, status);
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));

        assertAppException(
                () -> adminExamService.discardDraftExam(examId),
                HttpStatus.CONFLICT,
                "INVALID_STATE");

        verify(examRepository, never()).delete(any(Exam.class));
        verify(examRepository, never()).save(any(Exam.class));
        verify(questionRepository, never()).delete(any(Question.class));
    }

    private void assertArchiveSuccess(Long examId, ExamStatus status) {
        Exam exam = baseExam(examId, status);
        when(examRepository.findById(examId)).thenReturn(Optional.of(exam));
        when(examRepository.save(any(Exam.class))).thenAnswer(inv -> inv.getArgument(0));
        stubAdminResponseDependencies(examId, 0L);

        AdminExamResponse result = adminExamService.archive(examId);

        assertThat(result.getStatus()).isEqualTo(ExamStatus.ARCHIVED);
    }

    private void assertAppException(Runnable action, HttpStatus expectedStatus, String expectedCode) {
        assertThatThrownBy(action::run)
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException ae = (AppException) ex;
                    assertThat(ae.getStatus()).isEqualTo(expectedStatus);
                    assertThat(ae.getCode()).isEqualTo(expectedCode);
                });
    }

    private void stubAdminResponseDependencies(Long examId, Long count) {
        when(subjectRepository.findById(SUBJECT_ACTIVE_ID)).thenReturn(Optional.of(activeSubject));
        when(examQuestionRepository.countByExamId(examId)).thenReturn(count);
    }

    private Question question(Long id, QuestionStatus status) {
        return Question.builder()
                .id(id)
                .status(status)
                .build();
    }

    private ExamQuestion mapping(Long examId, Long questionId, int order) {
        return ExamQuestion.builder()
                .id(questionId + 1000)
                .examId(examId)
                .questionId(questionId)
                .questionOrder(order)
                .build();
    }

    private List<ExamQuestion> mappings(Long examId, Long... questionIds) {
        java.util.ArrayList<ExamQuestion> mappings = new java.util.ArrayList<>();
        for (int i = 0; i < questionIds.length; i++) {
            mappings.add(mapping(examId, questionIds[i], i + 1));
        }
        return mappings;
    }

    private Exam publishableExam(Long id, ExamStatus status, int questionCount) {
        Exam exam = baseExam(id, status);
        exam.setQuestionCount(questionCount);
        exam.setPricingType(ExamPricingType.FREE);
        exam.setPrice(BigDecimal.ZERO);
        return exam;
    }

    private AdminExamCreateRequest baseCreateRequest() {
        return AdminExamCreateRequest.builder()
                .examCode(EXAM_CODE_VALID)
                .title("Admin Smoke Exam")
                .subjectId(SUBJECT_ACTIVE_ID)
                .description("Created via admin API")
                .durationMinutes(90)
                .difficulty(Difficulty.MEDIUM)
                .pricingType(ExamPricingType.FREE)
                .price(BigDecimal.ZERO)
                .tags("smoke,c1.2b-1")
                .build();
    }

    private Exam baseExam(Long id, ExamStatus status) {
        return Exam.builder()
                .id(id)
                .examCode("ADM_C1_2B_" + String.format("%03d", id))
                .title("Existing exam " + id)
                .subjectId(SUBJECT_ACTIVE_ID)
                .description("desc")
                .questionCount(0)
                .durationMinutes(60)
                .difficulty(Difficulty.MEDIUM)
                .pricingType(ExamPricingType.FREE)
                .price(BigDecimal.ZERO)
                .status(status)
                .tags("baseline")
                .publishDate(null)
                .createdBy(ADMIN_USER_ID)
                .reviewedBy(null)
                .version(1)
                .totalAttempts(0)
                .avgScore(null)
                .createdAt(FIXED_TIME)
                .updatedAt(FIXED_TIME)
                .build();
    }
}
