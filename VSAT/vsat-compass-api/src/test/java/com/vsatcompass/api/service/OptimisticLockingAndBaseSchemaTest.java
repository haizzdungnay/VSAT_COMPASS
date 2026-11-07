package com.vsatcompass.api.service;

import com.vsatcompass.api.dto.request.AdminExamUpdateRequest;
import com.vsatcompass.api.dto.request.UpdateQuestionRequest;
import com.vsatcompass.api.dto.response.AdminExamResponse;
import com.vsatcompass.api.dto.response.QuestionResponse;
import com.vsatcompass.api.entity.Exam;
import com.vsatcompass.api.entity.Question;
import com.vsatcompass.api.entity.QuestionGroup;
import com.vsatcompass.api.entity.Subject;
import com.vsatcompass.api.entity.enums.Difficulty;
import com.vsatcompass.api.entity.enums.ExamPricingType;
import com.vsatcompass.api.entity.enums.ExamStatus;
import com.vsatcompass.api.entity.enums.QuestionStatus;
import com.vsatcompass.api.entity.enums.QuestionType;
import com.vsatcompass.api.entity.enums.UserRole;
import com.vsatcompass.api.repository.ExamQuestionRepository;
import com.vsatcompass.api.repository.ExamRepository;
import com.vsatcompass.api.repository.QuestionGroupRepository;
import com.vsatcompass.api.repository.QuestionOptionRepository;
import com.vsatcompass.api.repository.QuestionRepository;
import com.vsatcompass.api.repository.QuestionReviewRepository;
import com.vsatcompass.api.repository.SubjectRepository;
import com.vsatcompass.api.repository.SubtopicRepository;
import com.vsatcompass.api.repository.TopicRepository;
import com.vsatcompass.api.service.impl.AdminExamServiceImpl;
import com.vsatcompass.api.service.impl.QuestionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ClassPathResource;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OptimisticLockingAndBaseSchemaTest {

    @Mock private ExamRepository examRepository;
    @Mock private ExamQuestionRepository examQuestionRepository;
    @Mock private QuestionRepository questionRepository;
    @Mock private SubjectRepository subjectRepository;
    @Mock private QuestionOptionRepository questionOptionRepository;
    @Mock private QuestionReviewRepository questionReviewRepository;
    @Mock private TopicRepository topicRepository;
    @Mock private SubtopicRepository subtopicRepository;
    @Mock private QuestionGroupRepository questionGroupRepository;

    @InjectMocks private AdminExamServiceImpl adminExamService;
    @InjectMocks private QuestionServiceImpl questionService;

    private Exam testExam;
    private Question testQuestion;
    private Subject testSubject;
    private QuestionGroup testGroup;

    @BeforeEach
    void setUp() {
        testSubject = Subject.builder()
                .id(1L)
                .code("MATH")
                .name("Toan")
                .isActive(true)
                .build();

        testExam = Exam.builder()
                .id(10L)
                .examCode("MATH_EXAM_01")
                .title("Math Exam")
                .subjectId(1L)
                .status(ExamStatus.DRAFT)
                .durationMinutes(60)
                .difficulty(Difficulty.MEDIUM)
                .pricingType(ExamPricingType.FREE)
                .price(BigDecimal.ZERO)
                .version(1)
                .lockVersion(2)
                .build();

        testQuestion = Question.builder()
                .id(20L)
                .questionCode("Q_001")
                .subjectId(1L)
                .topicId(2L)
                .subtopicId(3L)
                .questionGroupId(30L)
                .difficulty(Difficulty.EASY)
                .questionType(QuestionType.SINGLE_CHOICE)
                .questionText("Sample Question")
                .status(QuestionStatus.DRAFT)
                .version(1)
                .lockVersion(5)
                .createdBy(100L)
                .build();

        testGroup = QuestionGroup.builder()
                .id(30L)
                .subjectId(1L)
                .code("GRP_01")
                .maxPlays(2)
                .build();
    }

    // ---------- Issue #20: V0__base_schema.sql exists in classpath ----------
    @Test
    @DisplayName("V0__base_schema.sql exists and is non-empty in classpath")
    void v0BaseSchema_existsInClasspath() throws Exception {
        ClassPathResource resource = new ClassPathResource("db/migration/V0__base_schema.sql");
        assertThat(resource.exists()).isTrue();
        try (InputStream is = resource.getInputStream()) {
            assertThat(is.available()).isGreaterThan(1000);
        }
    }

    // ---------- Issue #19: AdminExamServiceImpl Optimistic Locking ----------
    @Test
    @DisplayName("updateExam throws ObjectOptimisticLockingFailureException when lockVersion differs")
    void updateExam_lockVersionMismatch_throwsOptimisticLockingException() {
        when(examRepository.findById(10L)).thenReturn(Optional.of(testExam));

        AdminExamUpdateRequest req = AdminExamUpdateRequest.builder()
                .title("New Title")
                .lockVersion(1) // expected 2
                .build();

        assertThatThrownBy(() -> adminExamService.updateExam(10L, req))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }

    @Test
    @DisplayName("updateExam succeeds when lockVersion matches")
    void updateExam_lockVersionMatches_succeeds() {
        when(examRepository.findById(10L)).thenReturn(Optional.of(testExam));
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(testSubject));
        when(examRepository.save(any(Exam.class))).thenAnswer(inv -> inv.getArgument(0));
        when(examQuestionRepository.countByExamId(10L)).thenReturn(0L);

        AdminExamUpdateRequest req = AdminExamUpdateRequest.builder()
                .title("Updated Title")
                .lockVersion(2)
                .build();

        AdminExamResponse res = adminExamService.updateExam(10L, req);
        assertThat(res.getTitle()).isEqualTo("Updated Title");
        assertThat(res.getLockVersion()).isEqualTo(2);
    }

    @Test
    @DisplayName("updateExam succeeds when lockVersion is null (backward compatible)")
    void updateExam_lockVersionNull_succeeds() {
        when(examRepository.findById(10L)).thenReturn(Optional.of(testExam));
        when(subjectRepository.findById(1L)).thenReturn(Optional.of(testSubject));
        when(examRepository.save(any(Exam.class))).thenAnswer(inv -> inv.getArgument(0));
        when(examQuestionRepository.countByExamId(10L)).thenReturn(0L);

        AdminExamUpdateRequest req = AdminExamUpdateRequest.builder()
                .title("Updated Title")
                .lockVersion(null)
                .build();

        AdminExamResponse res = adminExamService.updateExam(10L, req);
        assertThat(res.getTitle()).isEqualTo("Updated Title");
    }

    // ---------- Issue #19: QuestionServiceImpl Optimistic Locking ----------
    @Test
    @DisplayName("updateQuestion throws ObjectOptimisticLockingFailureException when lockVersion differs")
    void updateQuestion_lockVersionMismatch_throwsOptimisticLockingException() {
        when(questionRepository.findById(20L)).thenReturn(Optional.of(testQuestion));

        UpdateQuestionRequest req = UpdateQuestionRequest.builder()
                .questionText("New text")
                .lockVersion(4) // expected 5
                .build();

        assertThatThrownBy(() -> questionService.update(100L, UserRole.CONTENT_ADMIN.name(), 20L, req))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
    }

    @Test
    @DisplayName("updateQuestion succeeds when lockVersion matches")
    void updateQuestion_lockVersionMatches_succeeds() {
        when(questionRepository.findById(20L)).thenReturn(Optional.of(testQuestion));
        when(questionRepository.save(any(Question.class))).thenAnswer(inv -> inv.getArgument(0));
        when(questionGroupRepository.findById(30L)).thenReturn(Optional.of(testGroup));
        when(questionOptionRepository.findByQuestionIdOrderByDisplayOrderAscIdAsc(20L)).thenReturn(Collections.emptyList());
        when(questionReviewRepository.findByQuestionIdOrderByCreatedAtDesc(20L)).thenReturn(Collections.emptyList());

        UpdateQuestionRequest req = UpdateQuestionRequest.builder()
                .questionText("Updated Question Text")
                .lockVersion(5)
                .build();

        QuestionResponse res = questionService.update(100L, UserRole.CONTENT_ADMIN.name(), 20L, req);
        assertThat(res.getQuestionText()).isEqualTo("Updated Question Text");
        assertThat(res.getLockVersion()).isEqualTo(5);
    }

    @Test
    @DisplayName("updateQuestion succeeds when lockVersion is null (backward compatible)")
    void updateQuestion_lockVersionNull_succeeds() {
        when(questionRepository.findById(20L)).thenReturn(Optional.of(testQuestion));
        when(questionRepository.save(any(Question.class))).thenAnswer(inv -> inv.getArgument(0));
        when(questionGroupRepository.findById(30L)).thenReturn(Optional.of(testGroup));
        when(questionOptionRepository.findByQuestionIdOrderByDisplayOrderAscIdAsc(20L)).thenReturn(Collections.emptyList());
        when(questionReviewRepository.findByQuestionIdOrderByCreatedAtDesc(20L)).thenReturn(Collections.emptyList());

        UpdateQuestionRequest req = UpdateQuestionRequest.builder()
                .questionText("Updated Question Text")
                .lockVersion(null)
                .build();

        QuestionResponse res = questionService.update(100L, UserRole.CONTENT_ADMIN.name(), 20L, req);
        assertThat(res.getQuestionText()).isEqualTo("Updated Question Text");
    }

    // ---------- Issue #19: QuestionGroup audioUrl / maxPlays update ----------
    @Test
    @DisplayName("updateQuestion updates QuestionGroup audioUrl, audioDurationSec, maxPlays")
    void updateQuestion_updatesGroupAudioFields() {
        when(questionRepository.findById(20L)).thenReturn(Optional.of(testQuestion));
        when(questionRepository.save(any(Question.class))).thenAnswer(inv -> inv.getArgument(0));
        when(questionGroupRepository.findById(30L)).thenReturn(Optional.of(testGroup));
        when(questionOptionRepository.findByQuestionIdOrderByDisplayOrderAscIdAsc(20L)).thenReturn(Collections.emptyList());
        when(questionReviewRepository.findByQuestionIdOrderByCreatedAtDesc(20L)).thenReturn(Collections.emptyList());

        UpdateQuestionRequest req = UpdateQuestionRequest.builder()
                .audioUrl("https://example.com/audio.mp3")
                .audioDurationSec(45)
                .maxPlays(3)
                .build();

        questionService.update(100L, UserRole.CONTENT_ADMIN.name(), 20L, req);

        assertThat(testGroup.getAudioUrl()).isEqualTo("https://example.com/audio.mp3");
        assertThat(testGroup.getAudioDurationSec()).isEqualTo(45);
        assertThat(testGroup.getMaxPlays()).isEqualTo(3);
        verify(questionGroupRepository).save(testGroup);
    }
}
