package com.vsatcompass.api.service;

import com.vsatcompass.api.dto.request.SessionRequest;
import com.vsatcompass.api.dto.response.QuestionGroupResponse;
import com.vsatcompass.api.dto.response.SessionResponse;
import com.vsatcompass.api.entity.ExamQuestion;
import com.vsatcompass.api.entity.ExamSession;
import com.vsatcompass.api.entity.Question;
import com.vsatcompass.api.entity.QuestionGroup;
import com.vsatcompass.api.entity.enums.Difficulty;
import com.vsatcompass.api.entity.enums.QuestionType;
import com.vsatcompass.api.entity.enums.SessionMode;
import com.vsatcompass.api.entity.enums.SessionStatus;
import com.vsatcompass.api.exception.AppException;
import com.vsatcompass.api.repository.ExamQuestionRepository;
import com.vsatcompass.api.repository.ExamSessionRepository;
import com.vsatcompass.api.repository.QuestionGroupRepository;
import com.vsatcompass.api.repository.QuestionOptionRepository;
import com.vsatcompass.api.repository.QuestionRepository;
import com.vsatcompass.api.repository.SessionAnswerRepository;
import com.vsatcompass.api.service.impl.SessionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.InputStream;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProctoringAndAudioTest — Unit tests for Anti-cheat Proctoring & Audio Support")
class ProctoringAndAudioTest {

    @Mock
    private ExamSessionRepository examSessionRepository;

    @Mock
    private ExamQuestionRepository examQuestionRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private QuestionOptionRepository questionOptionRepository;

    @Mock
    private QuestionGroupRepository questionGroupRepository;

    @Mock
    private SessionAnswerRepository sessionAnswerRepository;

    @InjectMocks
    private SessionServiceImpl sessionService;

    private ExamSession inProgressSession;

    @BeforeEach
    void setUp() {
        inProgressSession = ExamSession.builder()
                .id(100L)
                .userId(1L)
                .examId(10L)
                .mode(SessionMode.MOCK_EXAM)
                .status(SessionStatus.IN_PROGRESS)
                .violationCount(0)
                .startedAt(OffsetDateTime.now())
                .totalQuestions(1)
                .build();
    }

    @Test
    @DisplayName("recordViolation increments count and appends log when count < 3")
    void recordViolation_underThreshold_doesNotAutoSubmit() {
        when(examSessionRepository.findById(100L)).thenReturn(Optional.of(inProgressSession));
        when(examSessionRepository.save(any(ExamSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SessionRequest.RecordViolation req = new SessionRequest.RecordViolation();
        req.setViolationType("TAB_SWITCH");
        req.setDetails("User switched tab away from exam");

        SessionResponse.ViolationResult result = sessionService.recordViolation(1L, 100L, req);

        assertThat(result.getSessionId()).isEqualTo(100L);
        assertThat(result.getViolationCount()).isEqualTo(1);
        assertThat(result.getMaxViolations()).isEqualTo(3);
        assertThat(result.getAutoSubmitted()).isFalse();
        assertThat(result.getSessionStatus()).isEqualTo("IN_PROGRESS");
        assertThat(inProgressSession.getProctoringLogs()).hasSize(1);
        assertThat(inProgressSession.getProctoringLogs().get(0).get("violationType")).isEqualTo("TAB_SWITCH");
    }

    @Test
    @DisplayName("recordViolation triggers autoSubmit when violationCount reaches 3")
    void recordViolation_reaches3_triggersAutoSubmit() {
        inProgressSession.setViolationCount(2);

        when(examSessionRepository.findById(100L)).thenReturn(Optional.of(inProgressSession));
        when(examSessionRepository.save(any(ExamSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Prepare mocks for serverSubmit
        ExamQuestion eq = ExamQuestion.builder()
                .id(1L)
                .examId(10L)
                .questionId(50L)
                .questionOrder(1)
                .build();
        when(examQuestionRepository.findByExamIdOrderByQuestionOrderAscIdAsc(10L))
                .thenReturn(List.of(eq));
        when(sessionAnswerRepository.findBySessionId(100L))
                .thenReturn(Collections.emptyList());

        SessionRequest.RecordViolation req = new SessionRequest.RecordViolation();
        req.setViolationType("FULLSCREEN_EXITED");
        req.setDetails("User exited full screen window");

        SessionResponse.ViolationResult result = sessionService.recordViolation(1L, 100L, req);

        assertThat(result.getViolationCount()).isEqualTo(3);
        assertThat(result.getAutoSubmitted()).isTrue();
        assertThat(result.getSessionStatus()).isEqualTo("SUBMITTED");
        assertThat(inProgressSession.getStatus()).isEqualTo(SessionStatus.SUBMITTED);
        assertThat(inProgressSession.getProctoringLogs()).hasSize(1);
        assertThat(inProgressSession.getProctoringLogs().get(0).get("violationType")).isEqualTo("FULLSCREEN_EXITED");
    }

    @Test
    @DisplayName("recordViolation throws forbidden if user is not session owner")
    void recordViolation_wrongUser_throwsForbidden() {
        when(examSessionRepository.findById(100L)).thenReturn(Optional.of(inProgressSession));

        SessionRequest.RecordViolation req = new SessionRequest.RecordViolation();
        req.setViolationType("FOCUS_LOST");

        assertThatThrownBy(() -> sessionService.recordViolation(999L, 100L, req))
                .isInstanceOf(AppException.class);
    }

    @Test
    @DisplayName("QuestionGroup entity and response contain audioUrl, audioDurationSec, and maxPlays")
    void questionGroup_audioFields() {
        QuestionGroup group = QuestionGroup.builder()
                .id(5L)
                .code("GRP-ENG-01")
                .subjectId(2L)
                .title("Listening Part 1")
                .passageText("Listen to conversation")
                .audioUrl("https://cdn.vsat.vn/audio/eng_part1.mp3")
                .audioDurationSec(180)
                .maxPlays(3)
                .build();

        assertThat(group.getAudioUrl()).isEqualTo("https://cdn.vsat.vn/audio/eng_part1.mp3");
        assertThat(group.getAudioDurationSec()).isEqualTo(180);
        assertThat(group.getMaxPlays()).isEqualTo(3);

        // Default maxPlays
        QuestionGroup defaultGroup = QuestionGroup.builder().build();
        assertThat(defaultGroup.getMaxPlays()).isEqualTo(2);

        QuestionGroupResponse response = QuestionGroupResponse.builder()
                .id(group.getId())
                .code(group.getCode())
                .audioUrl(group.getAudioUrl())
                .audioDurationSec(group.getAudioDurationSec())
                .maxPlays(group.getMaxPlays())
                .build();

        assertThat(response.getAudioUrl()).isEqualTo("https://cdn.vsat.vn/audio/eng_part1.mp3");
        assertThat(response.getAudioDurationSec()).isEqualTo(180);
        assertThat(response.getMaxPlays()).isEqualTo(3);
    }

    @Test
    @DisplayName("Flyway migrations V13, V14, V15 exist on classpath and are non-empty")
    void flywayMigrations_existAndNonEmpty() {
        String[] migrations = {
                "/db/migration/V13__create_admission_benchmarks.sql",
                "/db/migration/V14__add_question_group_audio.sql",
                "/db/migration/V15__add_proctoring_columns.sql"
        };

        for (String path : migrations) {
            InputStream is = getClass().getResourceAsStream(path);
            assertThat(is)
                    .withFailMessage("Flyway migration not found: " + path)
                    .isNotNull();
        }
    }
}
