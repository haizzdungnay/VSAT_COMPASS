package com.vsatcompass.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.vsatcompass.api.dto.response.ExamDetailResponse;
import com.vsatcompass.api.dto.response.ExamSummaryResponse;
import com.vsatcompass.api.entity.Exam;
import com.vsatcompass.api.entity.enums.Difficulty;
import com.vsatcompass.api.entity.enums.ExamPricingType;
import com.vsatcompass.api.entity.enums.ExamStatus;
import com.vsatcompass.api.exception.AppException;
import com.vsatcompass.api.repository.ExamQuestionRepository;
import com.vsatcompass.api.repository.ExamRepository;
import com.vsatcompass.api.service.impl.ExamServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExamService - Phase C1.2a read-only public API")
class ExamServiceTest {

    @Mock ExamRepository examRepository;
    @Mock ExamQuestionRepository examQuestionRepository;

    @InjectMocks ExamServiceImpl examService;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private Exam algebraExam;
    private Exam geometryExam;

    @BeforeEach
    void setUp() {
        OffsetDateTime now = OffsetDateTime.parse("2026-05-05T00:00:00Z");
        algebraExam = exam(1L, "SMOKE_C1_2A_001", "Algebra Smoke Exam",
                Difficulty.MEDIUM, ExamStatus.PUBLISHED, ExamPricingType.FREE,
                "algebra,smoke", now);
        geometryExam = exam(2L, "SMOKE_C1_2A_002", "Geometry Smoke Exam",
                Difficulty.HARD, ExamStatus.PUBLISHED, ExamPricingType.FREE,
                "geometry,smoke", now.plusDays(1));
    }

    @Test
    @DisplayName("listPublishedFreeExams: maps published free exams to summary DTOs")
    void listPublishedFreeExams_happyPath_returnsMappedPage() {
        Pageable pageable = PageRequest.of(0, 20);
        when(examRepository.findByStatusAndPricingType(
                ExamStatus.PUBLISHED, ExamPricingType.FREE, pageable))
                .thenReturn(new PageImpl<>(List.of(algebraExam, geometryExam), pageable, 2));
        when(examQuestionRepository.countByExamId(1L)).thenReturn(12L);
        when(examQuestionRepository.countByExamId(2L)).thenReturn(20L);

        Page<ExamSummaryResponse> result = examService.listPublishedFreeExams(pageable);

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(2);
        ExamSummaryResponse first = result.getContent().get(0);
        assertThat(first.getId()).isEqualTo(1L);
        assertThat(first.getExamCode()).isEqualTo("SMOKE_C1_2A_001");
        assertThat(first.getTitle()).isEqualTo("Algebra Smoke Exam");
        assertThat(first.getSubjectId()).isEqualTo(10L);
        assertThat(first.getDifficulty()).isEqualTo(Difficulty.MEDIUM);
        assertThat(first.getPricingType()).isEqualTo(ExamPricingType.FREE);
        assertThat(first.getQuestionCount()).isEqualTo(12);
    }

    @Test
    @DisplayName("listPublishedFreeExams: empty repository page returns empty page")
    void listPublishedFreeExams_empty_returnsEmptyPage() {
        Pageable pageable = PageRequest.of(0, 20);
        when(examRepository.findByStatusAndPricingType(
                ExamStatus.PUBLISHED, ExamPricingType.FREE, pageable))
                .thenReturn(new PageImpl<>(Collections.emptyList(), pageable, 0));

        Page<ExamSummaryResponse> result = examService.listPublishedFreeExams(pageable);

        assertThat(result).isNotNull();
        assertThat(result.getTotalElements()).isZero();
        assertThat(result.getContent()).isEmpty();
    }

    @Test
    @DisplayName("listPublishedFreeExams: filters by PUBLISHED + FREE and propagates pageable")
    void listPublishedFreeExams_callsRepositoryWithPublishedFreeFilter() {
        Pageable pageable = PageRequest.of(1, 5);
        when(examRepository.findByStatusAndPricingType(
                ExamStatus.PUBLISHED, ExamPricingType.FREE, pageable))
                .thenReturn(new PageImpl<>(Collections.emptyList(), pageable, 0));

        examService.listPublishedFreeExams(pageable);

        verify(examRepository).findByStatusAndPricingType(
                ExamStatus.PUBLISHED, ExamPricingType.FREE, pageable);
    }

    @Test
    @DisplayName("getPublicExam: happy path returns detail DTO with whitelisted fields")
    void getPublicExam_happyPath_returnsDetail() {
        when(examRepository.findByIdAndStatusAndPricingType(
                1L, ExamStatus.PUBLISHED, ExamPricingType.FREE))
                .thenReturn(Optional.of(algebraExam));
        when(examQuestionRepository.countByExamId(1L)).thenReturn(12L);

        ExamDetailResponse result = examService.getPublicExam(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getExamCode()).isEqualTo("SMOKE_C1_2A_001");
        assertThat(result.getTitle()).isEqualTo("Algebra Smoke Exam");
        assertThat(result.getSubjectId()).isEqualTo(10L);
        assertThat(result.getDescription()).isEqualTo("Published free smoke exam");
        assertThat(result.getQuestionCount()).isEqualTo(12);
        assertThat(result.getDurationMinutes()).isEqualTo(90);
        assertThat(result.getDifficulty()).isEqualTo(Difficulty.MEDIUM);
        assertThat(result.getPricingType()).isEqualTo(ExamPricingType.FREE);
        assertThat(result.getTags()).isEqualTo("algebra,smoke");
        assertThat(result.getPublishDate()).isEqualTo(algebraExam.getPublishDate());
    }

    @Test
    @DisplayName("getPublicExam: missing exam throws RESOURCE_NOT_FOUND")
    void getPublicExam_notFound_throwsResourceNotFound() {
        when(examRepository.findByIdAndStatusAndPricingType(
                404L, ExamStatus.PUBLISHED, ExamPricingType.FREE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> examService.getPublicExam(404L))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException ae = (AppException) ex;
                    assertThat(ae.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(ae.getCode()).isEqualTo("RESOURCE_NOT_FOUND");
                    assertThat(ae.getMessage()).contains("Exam").contains("404");
                });
    }

    @Test
    @DisplayName("getPublicExam: anti-leak lookup filters by PUBLISHED + FREE")
    void getPublicExam_callsFilteredRepositoryNotPlainFindById() {
        when(examRepository.findByIdAndStatusAndPricingType(
                1L, ExamStatus.PUBLISHED, ExamPricingType.FREE))
                .thenReturn(Optional.of(algebraExam));
        when(examQuestionRepository.countByExamId(1L)).thenReturn(12L);

        examService.getPublicExam(1L);

        verify(examRepository).findByIdAndStatusAndPricingType(
                1L, ExamStatus.PUBLISHED, ExamPricingType.FREE);
        verify(examRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("ExamSummaryResponse JSON uses camelCase whitelist only")
    void summaryDto_serializedJson_excludesForbiddenFields() throws Exception {
        ExamSummaryResponse dto = ExamSummaryResponse.builder()
                .id(1L)
                .examCode("SMOKE_C1_2A_001")
                .title("Algebra Smoke Exam")
                .subjectId(10L)
                .description("Published free smoke exam")
                .questionCount(12)
                .durationMinutes(90)
                .difficulty(Difficulty.MEDIUM)
                .pricingType(ExamPricingType.FREE)
                .tags("algebra,smoke")
                .build();

        String json = objectMapper.writeValueAsString(dto);

        assertThat(json).contains("\"examCode\"", "\"subjectId\"", "\"questionCount\"",
                "\"durationMinutes\"", "\"pricingType\":\"FREE\"");
        assertThat(json).doesNotContain("\"status\"", "\"price\"", "\"publishDate\"",
                "\"createdBy\"", "\"reviewedBy\"", "\"version\"", "\"totalAttempts\"",
                "\"avgScore\"", "\"createdAt\"", "\"updatedAt\"");
        assertThat(ExamSummaryResponse.class.getDeclaredFields())
                .extracting(java.lang.reflect.Field::getName)
                .containsExactlyInAnyOrder("id", "examCode", "title", "subjectId",
                        "description", "questionCount", "durationMinutes", "difficulty",
                        "pricingType", "tags");
    }

    @Test
    @DisplayName("ExamDetailResponse JSON uses camelCase whitelist, allows publishDate, excludes questions")
    void detailDto_serializedJson_excludesForbiddenFields() throws Exception {
        ExamDetailResponse dto = ExamDetailResponse.builder()
                .id(1L)
                .examCode("SMOKE_C1_2A_001")
                .title("Algebra Smoke Exam")
                .subjectId(10L)
                .description("Published free smoke exam")
                .questionCount(12)
                .durationMinutes(90)
                .difficulty(Difficulty.MEDIUM)
                .pricingType(ExamPricingType.FREE)
                .tags("algebra,smoke")
                .publishDate(OffsetDateTime.parse("2026-05-05T00:00:00Z"))
                .build();

        String json = objectMapper.writeValueAsString(dto);

        assertThat(json).contains("\"examCode\"", "\"subjectId\"", "\"questionCount\"",
                "\"durationMinutes\"", "\"pricingType\":\"FREE\"", "\"publishDate\"");
        assertThat(json).doesNotContain("\"questions\"", "\"status\"", "\"price\"",
                "\"createdBy\"", "\"reviewedBy\"", "\"version\"", "\"totalAttempts\"",
                "\"avgScore\"", "\"createdAt\"", "\"updatedAt\"");
        assertThat(ExamDetailResponse.class.getDeclaredFields())
                .extracting(java.lang.reflect.Field::getName)
                .containsExactlyInAnyOrder("id", "examCode", "title", "subjectId",
                        "description", "questionCount", "durationMinutes", "difficulty",
                        "pricingType", "tags", "publishDate");
    }

    @Test
    @DisplayName("getPublicExam: hidden exam is invisible and returns same 404")
    void getPublicExam_hiddenExam_returns404() {
        when(examRepository.findByIdAndStatusAndPricingType(
                99L, ExamStatus.PUBLISHED, ExamPricingType.FREE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> examService.getPublicExam(99L))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException ae = (AppException) ex;
                    assertThat(ae.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(ae.getCode()).isEqualTo("RESOURCE_NOT_FOUND");
                });

        verify(examRepository, never()).findById(any());
    }

    @Test
    @DisplayName("getPublicExam: paid exam is invisible and returns same 404")
    void getPublicExam_paidExam_returns404() {
        when(examRepository.findByIdAndStatusAndPricingType(
                100L, ExamStatus.PUBLISHED, ExamPricingType.FREE))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> examService.getPublicExam(100L))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException ae = (AppException) ex;
                    assertThat(ae.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(ae.getCode()).isEqualTo("RESOURCE_NOT_FOUND");
                });

        verify(examRepository, never()).findById(any());
    }

    @Test
    @DisplayName("listPublishedFreeExams: difficulty enum round-trips through summary DTO")
    void listPublishedFreeExams_difficultyEnumPropagated() {
        Pageable pageable = PageRequest.of(0, 20);
        when(examRepository.findByStatusAndPricingType(
                ExamStatus.PUBLISHED, ExamPricingType.FREE, pageable))
                .thenReturn(new PageImpl<>(List.of(geometryExam), pageable, 1));
        when(examQuestionRepository.countByExamId(2L)).thenReturn(20L);

        Page<ExamSummaryResponse> result = examService.listPublishedFreeExams(pageable);

        assertThat(result.getContent().get(0).getDifficulty()).isEqualTo(Difficulty.HARD);
    }

    @Test
    @DisplayName("getPublicExam: tags string is propagated as-is")
    void getPublicExam_tagsPropagatedAsIs() {
        when(examRepository.findByIdAndStatusAndPricingType(
                1L, ExamStatus.PUBLISHED, ExamPricingType.FREE))
                .thenReturn(Optional.of(algebraExam));
        when(examQuestionRepository.countByExamId(1L)).thenReturn(12L);

        ExamDetailResponse result = examService.getPublicExam(1L);

        assertThat(result.getTags()).isEqualTo("algebra,smoke");
    }

    private Exam exam(
            Long id,
            String examCode,
            String title,
            Difficulty difficulty,
            ExamStatus status,
            ExamPricingType pricingType,
            String tags,
            OffsetDateTime publishDate) {
        OffsetDateTime now = OffsetDateTime.parse("2026-05-05T00:00:00Z");
        return Exam.builder()
                .id(id)
                .examCode(examCode)
                .title(title)
                .subjectId(10L)
                .description("Published free smoke exam")
                .questionCount(999)
                .durationMinutes(90)
                .difficulty(difficulty)
                .pricingType(pricingType)
                .price(BigDecimal.ZERO)
                .status(status)
                .tags(tags)
                .publishDate(publishDate)
                .createdBy(901L)
                .reviewedBy(902L)
                .version(3)
                .totalAttempts(123)
                .avgScore(new BigDecimal("81.25"))
                .createdAt(now.minusDays(1))
                .updatedAt(now)
                .build();
    }
}
