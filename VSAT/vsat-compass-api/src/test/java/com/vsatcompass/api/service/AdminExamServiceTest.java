package com.vsatcompass.api.service;

import com.vsatcompass.api.dto.request.AdminExamCreateRequest;
import com.vsatcompass.api.dto.request.AdminExamUpdateRequest;
import com.vsatcompass.api.dto.response.AdminExamResponse;
import com.vsatcompass.api.dto.response.AdminExamSummaryResponse;
import com.vsatcompass.api.entity.Exam;
import com.vsatcompass.api.entity.Subject;
import com.vsatcompass.api.entity.enums.Difficulty;
import com.vsatcompass.api.entity.enums.ExamPricingType;
import com.vsatcompass.api.entity.enums.ExamStatus;
import com.vsatcompass.api.exception.AppException;
import com.vsatcompass.api.repository.ExamQuestionRepository;
import com.vsatcompass.api.repository.ExamRepository;
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
    // LIST / GET
    // =========================================================

    @Test
    @DisplayName("list: returns paged metadata DTOs (no questions)")
    void listAdminExams_returnsPagedMetadata() {
        Pageable pageable = PageRequest.of(0, 20);
        Exam draft = baseExam(1L, ExamStatus.DRAFT);
        Exam hidden = baseExam(2L, ExamStatus.HIDDEN);
        when(examRepository.findAdminList(null, null, pageable))
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
    }

    @Test
    @DisplayName("list: filterByStatus propagates status arg to repository")
    void listAdminExams_filterByStatus() {
        Pageable pageable = PageRequest.of(0, 20);
        Exam draft = baseExam(3L, ExamStatus.DRAFT);
        when(examRepository.findAdminList(eq(ExamStatus.DRAFT), eq(null), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(draft), pageable, 1));

        Page<AdminExamSummaryResponse> result =
                adminExamService.listAdminExams(ExamStatus.DRAFT, null, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getStatus()).isEqualTo(ExamStatus.DRAFT);
        verify(examRepository).findAdminList(ExamStatus.DRAFT, null, pageable);
    }

    @Test
    @DisplayName("list: filterBySubjectId propagates subject arg to repository")
    void listAdminExams_filterBySubjectId() {
        Pageable pageable = PageRequest.of(0, 20);
        when(examRepository.findAdminList(eq(null), eq(SUBJECT_ACTIVE_ID), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        adminExamService.listAdminExams(null, SUBJECT_ACTIVE_ID, pageable);

        verify(examRepository).findAdminList(null, SUBJECT_ACTIVE_ID, pageable);
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
