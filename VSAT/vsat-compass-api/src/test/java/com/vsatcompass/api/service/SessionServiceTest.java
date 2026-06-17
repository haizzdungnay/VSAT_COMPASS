package com.vsatcompass.api.service;

import com.vsatcompass.api.dto.request.SessionRequest;
import com.vsatcompass.api.dto.response.QuestionOptionContentResponse;
import com.vsatcompass.api.dto.response.SessionAnswerKeysResponse;
import com.vsatcompass.api.dto.response.SessionQuestionContentResponse;
import com.vsatcompass.api.dto.response.SessionResponse;
import com.vsatcompass.api.entity.ExamQuestion;
import com.vsatcompass.api.entity.ExamSession;
import com.vsatcompass.api.entity.Question;
import com.vsatcompass.api.entity.QuestionOption;
import com.vsatcompass.api.entity.enums.Difficulty;
import com.vsatcompass.api.entity.enums.QuestionType;
import com.vsatcompass.api.entity.enums.SessionMode;
import com.vsatcompass.api.entity.enums.SessionStatus;
import com.vsatcompass.api.exception.AppException;
import com.vsatcompass.api.repository.ExamQuestionRepository;
import com.vsatcompass.api.repository.ExamSessionRepository;
import com.vsatcompass.api.repository.QuestionOptionRepository;
import com.vsatcompass.api.repository.QuestionRepository;
import com.vsatcompass.api.repository.SessionAnswerRepository;
import com.vsatcompass.api.entity.SessionAnswer;
import com.vsatcompass.api.service.impl.SessionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/*
 * Trust boundary note: SessionService.clientSubmit trusts the score computed by
 * the Android client (CLIENT_SIDE_EXAM_PROCESSING flag is true). These tests
 * verify the score is persisted as received, NOT re-derived server-side. That is
 * an architectural choice, not a bug.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SessionService — characterization tests (Spring Boot 3.2.5 baseline)")
class SessionServiceTest {

    @Mock ExamSessionRepository examSessionRepository;
    @Mock ExamQuestionRepository examQuestionRepository;
    @Mock QuestionRepository questionRepository;
    @Mock QuestionOptionRepository questionOptionRepository;
    @Mock SessionAnswerRepository sessionAnswerRepository;

    @InjectMocks SessionServiceImpl sessionService;

    private static final Long USER_ID = 7L;
    private static final Long OTHER_USER_ID = 99L;
    private static final Long EXAM_ID = 100L;
    private static final Long SESSION_ID = 555L;
    private static final Long QUESTION_ID = 200L;
    private static final Long SECOND_QUESTION_ID = 201L;

    private ExamSession inProgressSession;

    @BeforeEach
    void setUp() {
        inProgressSession = ExamSession.builder()
                .id(SESSION_ID)
                .userId(USER_ID)
                .examId(EXAM_ID)
                .mode(SessionMode.MOCK_EXAM)
                .status(SessionStatus.IN_PROGRESS)
                .totalQuestions(50)
                .answeredCount(0)
                .correctCount(0)
                .wrongCount(0)
                .skippedCount(0)
                .deviceType("ANDROID")
                .build();
    }

    private SessionRequest.StartSession startReq(Long examId, String mode, Integer totalQuestions) {
        SessionRequest.StartSession r = new SessionRequest.StartSession();
        r.setExamId(examId);
        r.setMode(mode);
        r.setTotalQuestions(totalQuestions);
        return r;
    }

    private SessionRequest.ClientSubmit submitReq(double score, int correctCount, int totalQuestions, int timeSpent) {
        SessionRequest.ClientSubmit r = new SessionRequest.ClientSubmit();
        r.setScore(score);
        r.setCorrectCount(correctCount);
        r.setTotalQuestions(totalQuestions);
        r.setTimeSpentSeconds(timeSpent);
        return r;
    }

    private void mockStartSessionSave() {
        when(examSessionRepository.save(any(ExamSession.class))).thenAnswer(inv -> {
            ExamSession s = inv.getArgument(0);
            s.setId(SESSION_ID);
            return s;
        });
    }

    private void mockOrderedExamQuestions(List<ExamQuestion> questions) {
        when(examQuestionRepository.findByExamIdOrderByQuestionOrderAscIdAsc(EXAM_ID))
                .thenReturn(questions);
    }

    // ===== startSession =====

    @Test
    @DisplayName("startSession: happy path persists IN_PROGRESS session with given mode")
    void startSession_happyPath_persistsSession() {
        mockStartSessionSave();
        mockOrderedExamQuestions(Collections.emptyList());

        SessionResponse.SessionInfo info = sessionService.startSession(
                USER_ID, startReq(EXAM_ID, "PRACTICE", 30));

        assertThat(info.getId()).isEqualTo(SESSION_ID);
        assertThat(info.getExamId()).isEqualTo(EXAM_ID);
        assertThat(info.getMode()).isEqualTo("PRACTICE");
        assertThat(info.getStatus()).isEqualTo("IN_PROGRESS");

        ArgumentCaptor<ExamSession> cap = ArgumentCaptor.forClass(ExamSession.class);
        verify(examSessionRepository).save(cap.capture());
        ExamSession saved = cap.getValue();
        assertThat(saved.getUserId()).isEqualTo(USER_ID);
        assertThat(saved.getMode()).isEqualTo(SessionMode.PRACTICE);
        assertThat(saved.getStatus()).isEqualTo(SessionStatus.IN_PROGRESS);
        assertThat(saved.getTotalQuestions()).isEqualTo(30);
        assertThat(saved.getDeviceType()).isEqualTo("ANDROID");
    }

    @Test
    @DisplayName("startSession: response includes orderedQuestionIds")
    void startSession_happyPath_returns_orderedQuestionIds() {
        mockStartSessionSave();
        mockOrderedExamQuestions(List.of(
                examQuestion(QUESTION_ID, 1),
                examQuestion(SECOND_QUESTION_ID, 2)
        ));

        SessionResponse.SessionInfo info = sessionService.startSession(
                USER_ID, startReq(EXAM_ID, "MOCK_EXAM", 2));

        assertThat(info.getOrderedQuestionIds())
                .containsExactly(QUESTION_ID, SECOND_QUESTION_ID);
    }

    @Test
    @DisplayName("startSession: empty exam returns empty orderedQuestionIds")
    void startSession_examHasNoQuestions_returns_emptyOrderedQuestionIds() {
        mockStartSessionSave();
        mockOrderedExamQuestions(Collections.emptyList());

        SessionResponse.SessionInfo info = sessionService.startSession(
                USER_ID, startReq(EXAM_ID, "MOCK_EXAM", 30));

        assertThat(info.getOrderedQuestionIds()).isNotNull();
        assertThat(info.getOrderedQuestionIds()).isEmpty();
    }

    @Test
    @DisplayName("startSession: orderedQuestionIds follow repository question order")
    void startSession_orderedQuestionIds_followQuestionOrder() {
        mockStartSessionSave();
        mockOrderedExamQuestions(List.of(
                examQuestion(SECOND_QUESTION_ID, 1),
                examQuestion(QUESTION_ID, 2)
        ));

        SessionResponse.SessionInfo info = sessionService.startSession(
                USER_ID, startReq(EXAM_ID, "PRACTICE", 2));

        assertThat(info.getOrderedQuestionIds())
                .containsExactly(SECOND_QUESTION_ID, QUESTION_ID);
    }

    @Test
    @DisplayName("startSession: invalid mode string throws BAD_REQUEST and does not save")
    void startSession_invalidMode_throwsBadRequest() {
        assertThatThrownBy(() -> sessionService.startSession(
                USER_ID, startReq(EXAM_ID, "NOT_A_MODE", 50)))
                .isInstanceOfSatisfying(AppException.class, ex -> {
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(ex.getCode()).isEqualTo("BAD_REQUEST");
                });

        verify(examSessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("startSession: null mode defaults to MOCK_EXAM")
    void startSession_modeNull_defaultsToMockExam() {
        mockStartSessionSave();
        mockOrderedExamQuestions(Collections.emptyList());

        SessionResponse.SessionInfo info = sessionService.startSession(
                USER_ID, startReq(EXAM_ID, null, 30));

        assertThat(info.getMode()).isEqualTo("MOCK_EXAM");

        ArgumentCaptor<ExamSession> cap = ArgumentCaptor.forClass(ExamSession.class);
        verify(examSessionRepository).save(cap.capture());
        assertThat(cap.getValue().getMode()).isEqualTo(SessionMode.MOCK_EXAM);
    }

    @Test
    @DisplayName("startSession: null totalQuestions defaults to 0 (client populates via client-submit)")
    void startSession_totalQuestionsNull_defaultsToZero() {
        mockStartSessionSave();
        mockOrderedExamQuestions(Collections.emptyList());

        SessionResponse.SessionInfo info = sessionService.startSession(
                USER_ID, startReq(EXAM_ID, "MOCK_EXAM", null));

        assertThat(info.getTotalQuestions()).isEqualTo(0);

        ArgumentCaptor<ExamSession> cap = ArgumentCaptor.forClass(ExamSession.class);
        verify(examSessionRepository).save(cap.capture());
        assertThat(cap.getValue().getTotalQuestions()).isEqualTo(0);
    }

    // ===== clientSubmit =====

    @Test
    @DisplayName("clientSubmit: happy path marks SUBMITTED and persists client-supplied score")
    void clientSubmit_happyPath_persistsClientScoreAndCounts() {
        when(examSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(inProgressSession));
        when(examSessionRepository.save(any(ExamSession.class))).thenAnswer(inv -> inv.getArgument(0));

        SessionResponse.SessionInfo info = sessionService.clientSubmit(
                USER_ID, SESSION_ID, submitReq(82.5, 41, 50, 1800));

        assertThat(info.getStatus()).isEqualTo("SUBMITTED");
        assertThat(info.getScore()).isEqualByComparingTo(BigDecimal.valueOf(82.5));
        assertThat(info.getCorrectCount()).isEqualTo(41);
        assertThat(info.getTotalQuestions()).isEqualTo(50);
        assertThat(info.getTimeSpentSeconds()).isEqualTo(1800);
        assertThat(info.getOrderedQuestionIds()).isEmpty();

        // Verify the saved entity reflects the client payload — server does NOT recompute
        ArgumentCaptor<ExamSession> cap = ArgumentCaptor.forClass(ExamSession.class);
        verify(examSessionRepository).save(cap.capture());
        ExamSession saved = cap.getValue();
        assertThat(saved.getStatus()).isEqualTo(SessionStatus.SUBMITTED);
        assertThat(saved.getSubmittedAt()).isNotNull();
        assertThat(saved.getScore()).isEqualByComparingTo(BigDecimal.valueOf(82.5));
        assertThat(saved.getScorePercentage()).isEqualByComparingTo(BigDecimal.valueOf(82.5));
        assertThat(saved.getCorrectCount()).isEqualTo(41);
        assertThat(saved.getWrongCount()).isEqualTo(9);
        assertThat(saved.getAnsweredCount()).isEqualTo(50);
        assertThat(saved.getSkippedCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("clientSubmit: persists optional session answers for topic stats")
    void clientSubmit_withAnswers_persistsSessionAnswers() {
        when(examSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(inProgressSession));
        when(examSessionRepository.save(any(ExamSession.class))).thenAnswer(inv -> inv.getArgument(0));
        when(examQuestionRepository.findByExamIdOrderByQuestionOrderAscIdAsc(EXAM_ID))
                .thenReturn(List.of(examQuestion(QUESTION_ID, 1)));
        when(questionOptionRepository.findById(301L))
                .thenReturn(Optional.of(QuestionOption.builder().id(301L).isCorrect(true).build()));

        SessionRequest.ClientSubmit request = submitReq(80.0, 1, 1, 120);
        SessionRequest.ClientSubmit.ClientSubmitAnswer answer = new SessionRequest.ClientSubmit.ClientSubmitAnswer();
        answer.setQuestionId(QUESTION_ID);
        answer.setSelectedOptionId(301L);
        request.setAnswers(List.of(answer));

        sessionService.clientSubmit(USER_ID, SESSION_ID, request);

        verify(sessionAnswerRepository).deleteBySessionId(SESSION_ID);
        ArgumentCaptor<List<SessionAnswer>> answersCaptor = ArgumentCaptor.forClass(List.class);
        verify(sessionAnswerRepository).saveAll(answersCaptor.capture());
        assertThat(answersCaptor.getValue()).hasSize(1);
        assertThat(answersCaptor.getValue().get(0).getIsCorrect()).isTrue();
    }

    @Test
    @DisplayName("clientSubmit: correctCount == totalQuestions persists all-answered, zero-wrong (boundary)")
    void clientSubmit_correctEqualsTotal_persistsAllAnsweredZeroWrong() {
        when(examSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(inProgressSession));
        when(examSessionRepository.save(any(ExamSession.class))).thenAnswer(inv -> inv.getArgument(0));

        SessionResponse.SessionInfo info = sessionService.clientSubmit(
                USER_ID, SESSION_ID, submitReq(100.0, 50, 50, 1800));

        assertThat(info.getStatus()).isEqualTo("SUBMITTED");
        assertThat(info.getCorrectCount()).isEqualTo(50);
        assertThat(info.getTotalQuestions()).isEqualTo(50);

        ArgumentCaptor<ExamSession> cap = ArgumentCaptor.forClass(ExamSession.class);
        verify(examSessionRepository).save(cap.capture());
        ExamSession saved = cap.getValue();
        assertThat(saved.getStatus()).isEqualTo(SessionStatus.SUBMITTED);
        assertThat(saved.getCorrectCount()).isEqualTo(50);
        assertThat(saved.getTotalQuestions()).isEqualTo(50);
        assertThat(saved.getWrongCount()).isEqualTo(0);
        assertThat(saved.getAnsweredCount()).isEqualTo(50);
        assertThat(saved.getSkippedCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("clientSubmit: unknown sessionId throws RESOURCE_NOT_FOUND with 404")
    void clientSubmit_unknownSessionId_throwsNotFound() {
        when(examSessionRepository.findById(SESSION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sessionService.clientSubmit(
                USER_ID, SESSION_ID, submitReq(80.0, 40, 50, 1800)))
                .isInstanceOfSatisfying(AppException.class, ex -> {
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(ex.getCode()).isEqualTo("RESOURCE_NOT_FOUND");
                });

        verify(examSessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("clientSubmit: session belongs to a different user throws SESSION_FORBIDDEN with 403")
    void clientSubmit_differentUser_throwsForbidden() {
        when(examSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(inProgressSession));

        assertThatThrownBy(() -> sessionService.clientSubmit(
                OTHER_USER_ID, SESSION_ID, submitReq(80.0, 40, 50, 1800)))
                .isInstanceOfSatisfying(AppException.class, ex -> {
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(ex.getCode()).isEqualTo("SESSION_FORBIDDEN");
                });

        verify(examSessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("clientSubmit: already SUBMITTED session throws SESSION_ALREADY_SUBMITTED with 409 (anti-replay)")
    void clientSubmit_alreadySubmitted_throwsConflict() {
        inProgressSession.setStatus(SessionStatus.SUBMITTED);
        when(examSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(inProgressSession));

        assertThatThrownBy(() -> sessionService.clientSubmit(
                USER_ID, SESSION_ID, submitReq(80.0, 40, 50, 1800)))
                .isInstanceOfSatisfying(AppException.class, ex -> {
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(ex.getCode()).isEqualTo("SESSION_ALREADY_SUBMITTED");
                });

        verify(examSessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("clientSubmit: session in non-IN_PROGRESS terminal state (e.g. ABANDONED) throws BAD_REQUEST")
    void clientSubmit_abandonedSession_throwsBadRequest() {
        inProgressSession.setStatus(SessionStatus.ABANDONED);
        when(examSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(inProgressSession));

        assertThatThrownBy(() -> sessionService.clientSubmit(
                USER_ID, SESSION_ID, submitReq(80.0, 40, 50, 1800)))
                .isInstanceOfSatisfying(AppException.class, ex -> {
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(ex.getCode()).isEqualTo("BAD_REQUEST");
                });

        verify(examSessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("clientSubmit: session in TIMED_OUT terminal state throws BAD_REQUEST")
    void clientSubmit_timedOutSession_throwsBadRequest() {
        inProgressSession.setStatus(SessionStatus.TIMED_OUT);
        when(examSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(inProgressSession));

        assertThatThrownBy(() -> sessionService.clientSubmit(
                USER_ID, SESSION_ID, submitReq(80.0, 40, 50, 1800)))
                .isInstanceOfSatisfying(AppException.class, ex -> {
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(ex.getCode()).isEqualTo("BAD_REQUEST");
                });

        verify(examSessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("clientSubmit: correctCount > totalQuestions throws VALIDATION_FAILED")
    void clientSubmit_correctGreaterThanTotal_throwsValidation() {
        when(examSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(inProgressSession));

        assertThatThrownBy(() -> sessionService.clientSubmit(
                USER_ID, SESSION_ID, submitReq(110.0, 60, 50, 1800)))
                .isInstanceOfSatisfying(AppException.class, ex -> {
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(ex.getCode()).isEqualTo("VALIDATION_FAILED");
                });

        verify(examSessionRepository, never()).save(any());
    }

    // ===== getQuestionForSession =====

    @Test
    @DisplayName("getQuestionForSession: happy path returns content and options without answer keys")
    void getQuestionForSession_happyPath_returnsQuestionContent() {
        when(examSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(inProgressSession));
        when(examQuestionRepository.findByExamIdOrderByQuestionOrderAscIdAsc(EXAM_ID))
                .thenReturn(List.of(examQuestion(QUESTION_ID, 3)));
        when(questionRepository.findById(QUESTION_ID)).thenReturn(Optional.of(question(QUESTION_ID, "Hidden explanation")));
        when(questionOptionRepository.findByQuestionIdOrderByDisplayOrderAscIdAsc(QUESTION_ID))
                .thenReturn(List.of(
                        option(301L, QUESTION_ID, true, 1),
                        option(302L, QUESTION_ID, false, 2)
                ));

        SessionQuestionContentResponse response =
                sessionService.getQuestionForSession(SESSION_ID, QUESTION_ID, USER_ID);

        assertThat(response.getId()).isEqualTo(QUESTION_ID);
        assertThat(response.getQuestionCode()).isEqualTo("Q-200");
        assertThat(response.getContent()).isEqualTo("Question text 200");
        assertThat(response.getQuestionType()).isEqualTo(QuestionType.SINGLE_CHOICE);
        assertThat(response.getDifficulty()).isEqualTo(Difficulty.MEDIUM);
        assertThat(response.getOrder()).isEqualTo(3);
        assertThat(response.getOptions()).hasSize(2);
        assertThat(response.getOptions().get(0).getId()).isEqualTo(301L);
        assertThat(response.getOptions().get(0).getContent()).isEqualTo("Option 1");
        assertThat(response.getOptions().get(0).getOrder()).isEqualTo(1);
    }

    @Test
    @DisplayName("getQuestionForSession: unknown session throws RESOURCE_NOT_FOUND with 404")
    void getQuestionForSession_unknownSession_throwsNotFound() {
        when(examSessionRepository.findById(SESSION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sessionService.getQuestionForSession(SESSION_ID, QUESTION_ID, USER_ID))
                .isInstanceOfSatisfying(AppException.class, ex -> {
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(ex.getCode()).isEqualTo("RESOURCE_NOT_FOUND");
                });

        verify(examQuestionRepository, never()).findByExamIdOrderByQuestionOrderAscIdAsc(any());
    }

    @Test
    @DisplayName("getQuestionForSession: different user throws SESSION_FORBIDDEN with 403")
    void getQuestionForSession_differentUser_throwsForbidden() {
        when(examSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(inProgressSession));

        assertThatThrownBy(() -> sessionService.getQuestionForSession(SESSION_ID, QUESTION_ID, OTHER_USER_ID))
                .isInstanceOfSatisfying(AppException.class, ex -> {
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(ex.getCode()).isEqualTo("SESSION_FORBIDDEN");
                });

        verify(examQuestionRepository, never()).findByExamIdOrderByQuestionOrderAscIdAsc(any());
    }

    @Test
    @DisplayName("getQuestionForSession: SUBMITTED session throws BAD_REQUEST")
    void getQuestionForSession_submittedSession_throwsBadRequest() {
        assertQuestionContentStatusRejected(SessionStatus.SUBMITTED);
    }

    @Test
    @DisplayName("getQuestionForSession: TIMED_OUT session throws BAD_REQUEST")
    void getQuestionForSession_timedOutSession_throwsBadRequest() {
        assertQuestionContentStatusRejected(SessionStatus.TIMED_OUT);
    }

    @Test
    @DisplayName("getQuestionForSession: ABANDONED session throws BAD_REQUEST")
    void getQuestionForSession_abandonedSession_throwsBadRequest() {
        assertQuestionContentStatusRejected(SessionStatus.ABANDONED);
    }

    @Test
    @DisplayName("getQuestionForSession: question outside session exam throws RESOURCE_NOT_FOUND")
    void getQuestionForSession_questionNotInExam_throwsNotFound() {
        when(examSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(inProgressSession));
        when(examQuestionRepository.findByExamIdOrderByQuestionOrderAscIdAsc(EXAM_ID))
                .thenReturn(List.of(examQuestion(SECOND_QUESTION_ID, 1)));

        assertThatThrownBy(() -> sessionService.getQuestionForSession(SESSION_ID, QUESTION_ID, USER_ID))
                .isInstanceOfSatisfying(AppException.class, ex -> {
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(ex.getCode()).isEqualTo("RESOURCE_NOT_FOUND");
                });

        verify(questionRepository, never()).findById(any());
        verify(questionOptionRepository, never()).findByQuestionIdOrderByDisplayOrderAscIdAsc(any());
    }

    @Test
    @DisplayName("getQuestionForSession DTOs do not expose answer key or explanation fields")
    void getQuestionForSession_contentDtos_doNotExposeAnswerKeys() {
        assertThat(Arrays.stream(SessionQuestionContentResponse.class.getDeclaredFields())
                .map(field -> field.getName()))
                .doesNotContain("isCorrect", "correctAnswer", "explanation");
        assertThat(Arrays.stream(QuestionOptionContentResponse.class.getDeclaredFields())
                .map(field -> field.getName()))
                .doesNotContain("isCorrect", "correctAnswer", "explanation");
    }

    // ===== getAnswerKeysForSession =====

    @Test
    @DisplayName("getAnswerKeysForSession: happy path returns correct option ids and explanations")
    void getAnswerKeysForSession_happyPath_returnsKeys() {
        inProgressSession.setStatus(SessionStatus.SUBMITTED);
        when(examSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(inProgressSession));
        when(examQuestionRepository.findByExamIdOrderByQuestionOrderAscIdAsc(EXAM_ID))
                .thenReturn(List.of(
                        examQuestion(QUESTION_ID, 1),
                        examQuestion(SECOND_QUESTION_ID, 2)
                ));
        when(questionRepository.findById(QUESTION_ID))
                .thenReturn(Optional.of(question(QUESTION_ID, "Because A")));
        when(questionRepository.findById(SECOND_QUESTION_ID))
                .thenReturn(Optional.of(question(SECOND_QUESTION_ID, "Because B and C")));
        when(questionOptionRepository.findByQuestionIdOrderByDisplayOrderAscIdAsc(QUESTION_ID))
                .thenReturn(List.of(
                        option(301L, QUESTION_ID, true, 1),
                        option(302L, QUESTION_ID, false, 2)
                ));
        when(questionOptionRepository.findByQuestionIdOrderByDisplayOrderAscIdAsc(SECOND_QUESTION_ID))
                .thenReturn(List.of(
                        option(401L, SECOND_QUESTION_ID, true, 1),
                        option(402L, SECOND_QUESTION_ID, true, 2),
                        option(403L, SECOND_QUESTION_ID, false, 3)
                ));

        SessionAnswerKeysResponse response =
                sessionService.getAnswerKeysForSession(SESSION_ID, USER_ID);

        assertThat(response.getSessionId()).isEqualTo(SESSION_ID);
        assertThat(response.getExamId()).isEqualTo(EXAM_ID);
        assertThat(response.getQuestions()).hasSize(2);
        assertThat(response.getQuestions().get(0).getQuestionId()).isEqualTo(QUESTION_ID);
        assertThat(response.getQuestions().get(0).getCorrectOptionIds()).containsExactly(301L);
        assertThat(response.getQuestions().get(0).getExplanation()).isEqualTo("Because A");
        assertThat(response.getQuestions().get(1).getCorrectOptionIds()).containsExactly(401L, 402L);
        assertThat(response.getQuestions().get(1).getExplanation()).isEqualTo("Because B and C");
    }

    @Test
    @DisplayName("getAnswerKeysForSession: unknown session throws RESOURCE_NOT_FOUND with 404")
    void getAnswerKeysForSession_unknownSession_throwsNotFound() {
        when(examSessionRepository.findById(SESSION_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sessionService.getAnswerKeysForSession(SESSION_ID, USER_ID))
                .isInstanceOfSatisfying(AppException.class, ex -> {
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(ex.getCode()).isEqualTo("RESOURCE_NOT_FOUND");
                });
    }

    @Test
    @DisplayName("getAnswerKeysForSession: different user throws SESSION_FORBIDDEN with 403")
    void getAnswerKeysForSession_differentUser_throwsForbidden() {
        inProgressSession.setStatus(SessionStatus.SUBMITTED);
        when(examSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(inProgressSession));

        assertThatThrownBy(() -> sessionService.getAnswerKeysForSession(SESSION_ID, OTHER_USER_ID))
                .isInstanceOfSatisfying(AppException.class, ex -> {
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(ex.getCode()).isEqualTo("SESSION_FORBIDDEN");
                });
    }

    @Test
    @DisplayName("getAnswerKeysForSession: IN_PROGRESS session throws BAD_REQUEST")
    void getAnswerKeysForSession_inProgressSession_throwsBadRequest() {
        assertAnswerKeysStatusRejected(SessionStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("getAnswerKeysForSession: TIMED_OUT session throws BAD_REQUEST")
    void getAnswerKeysForSession_timedOutSession_throwsBadRequest() {
        assertAnswerKeysStatusRejected(SessionStatus.TIMED_OUT);
    }

    @Test
    @DisplayName("getAnswerKeysForSession: empty exam question list returns empty questions")
    void getAnswerKeysForSession_emptyExamQuestions_returnsEmptyList() {
        inProgressSession.setStatus(SessionStatus.SUBMITTED);
        when(examSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(inProgressSession));
        when(examQuestionRepository.findByExamIdOrderByQuestionOrderAscIdAsc(EXAM_ID))
                .thenReturn(Collections.emptyList());

        SessionAnswerKeysResponse response =
                sessionService.getAnswerKeysForSession(SESSION_ID, USER_ID);

        assertThat(response.getSessionId()).isEqualTo(SESSION_ID);
        assertThat(response.getExamId()).isEqualTo(EXAM_ID);
        assertThat(response.getQuestions()).isEmpty();
    }

    // ===== submitAnswer (server-side scoring) =====

    @Test
    @DisplayName("submitAnswer: happy path persists a single answer row and updates answeredCount")
    void submitAnswer_happyPath_persistsRow() {
        when(examSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(inProgressSession));
        when(examQuestionRepository.findByExamIdOrderByQuestionOrderAscIdAsc(EXAM_ID))
                .thenReturn(List.of(examQuestion(QUESTION_ID, 1)));
        when(questionOptionRepository.findByQuestionIdOrderByDisplayOrderAscIdAsc(QUESTION_ID))
                .thenReturn(List.of(option(301L, QUESTION_ID, true, 1)));
        when(questionOptionRepository.findById(301L))
                .thenReturn(Optional.of(option(301L, QUESTION_ID, true, 1)));
        when(sessionAnswerRepository.findBySessionIdAndQuestionId(SESSION_ID, QUESTION_ID))
                .thenReturn(Optional.empty());
        when(sessionAnswerRepository.countBySessionId(SESSION_ID)).thenReturn(1L);
        when(examSessionRepository.save(any(ExamSession.class))).thenAnswer(inv -> inv.getArgument(0));

        SessionRequest.SubmitAnswer req = new SessionRequest.SubmitAnswer();
        req.setQuestionId(QUESTION_ID);
        req.setSelectedOptionId(301L);
        req.setBookmarked(false);
        req.setTimeSpentSeconds(12);

        sessionService.submitAnswer(USER_ID, SESSION_ID, req);

        ArgumentCaptor<SessionAnswer> cap = ArgumentCaptor.forClass(SessionAnswer.class);
        verify(sessionAnswerRepository).save(cap.capture());
        SessionAnswer saved = cap.getValue();
        assertThat(saved.getSessionId()).isEqualTo(SESSION_ID);
        assertThat(saved.getQuestionId()).isEqualTo(QUESTION_ID);
        assertThat(saved.getQuestionOrder()).isEqualTo(1);
        assertThat(saved.getSelectedOptionId()).isEqualTo(301L);
        assertThat(saved.getIsCorrect()).isTrue();
        assertThat(saved.getIsBookmarked()).isFalse();
        assertThat(saved.getTimeSpentSeconds()).isEqualTo(12);
        assertThat(saved.getAnsweredAt()).isNotNull();
    }

    @Test
    @DisplayName("submitAnswer: second submission for the same question replaces the previous row (idempotent)")
    void submitAnswer_idempotent_replacesExistingRow() {
        when(examSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(inProgressSession));
        when(examQuestionRepository.findByExamIdOrderByQuestionOrderAscIdAsc(EXAM_ID))
                .thenReturn(List.of(examQuestion(QUESTION_ID, 1)));
        when(questionOptionRepository.findByQuestionIdOrderByDisplayOrderAscIdAsc(QUESTION_ID))
                .thenReturn(List.of(
                        option(301L, QUESTION_ID, true, 1),
                        option(302L, QUESTION_ID, false, 2)
                ));
        when(questionOptionRepository.findById(302L))
                .thenReturn(Optional.of(option(302L, QUESTION_ID, false, 2)));
        SessionAnswer previous = SessionAnswer.builder()
                .id(9001L)
                .sessionId(SESSION_ID)
                .questionId(QUESTION_ID)
                .questionOrder(1)
                .selectedOptionId(301L)
                .isCorrect(true)
                .isBookmarked(false)
                .build();
        when(sessionAnswerRepository.findBySessionIdAndQuestionId(SESSION_ID, QUESTION_ID))
                .thenReturn(Optional.of(previous));
        when(sessionAnswerRepository.countBySessionId(SESSION_ID)).thenReturn(1L);
        when(examSessionRepository.save(any(ExamSession.class))).thenAnswer(inv -> inv.getArgument(0));

        SessionRequest.SubmitAnswer req = new SessionRequest.SubmitAnswer();
        req.setQuestionId(QUESTION_ID);
        req.setSelectedOptionId(302L);

        sessionService.submitAnswer(USER_ID, SESSION_ID, req);

        ArgumentCaptor<SessionAnswer> cap = ArgumentCaptor.forClass(SessionAnswer.class);
        verify(sessionAnswerRepository).save(cap.capture());
        assertThat(cap.getValue().getId()).isEqualTo(previous.getId());
        assertThat(cap.getValue().getSelectedOptionId()).isEqualTo(302L);
        assertThat(cap.getValue().getIsCorrect()).isFalse();
    }

    @Test
    @DisplayName("submitAnswer: option not belonging to the question throws BAD_REQUEST")
    void submitAnswer_optionOutOfScope_throwsBadRequest() {
        when(examSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(inProgressSession));
        when(examQuestionRepository.findByExamIdOrderByQuestionOrderAscIdAsc(EXAM_ID))
                .thenReturn(List.of(examQuestion(QUESTION_ID, 1)));
        when(questionOptionRepository.findByQuestionIdOrderByDisplayOrderAscIdAsc(QUESTION_ID))
                .thenReturn(List.of(option(301L, QUESTION_ID, true, 1)));

        SessionRequest.SubmitAnswer req = new SessionRequest.SubmitAnswer();
        req.setQuestionId(QUESTION_ID);
        req.setSelectedOptionId(999L);

        assertThatThrownBy(() -> sessionService.submitAnswer(USER_ID, SESSION_ID, req))
                .isInstanceOfSatisfying(AppException.class, ex -> {
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(ex.getCode()).isEqualTo("BAD_REQUEST");
                });

        verify(sessionAnswerRepository, never()).save(any(SessionAnswer.class));
    }

    @Test
    @DisplayName("submitAnswer: already SUBMITTED session throws SESSION_ALREADY_SUBMITTED (anti-replay)")
    void submitAnswer_alreadySubmitted_throwsConflict() {
        inProgressSession.setStatus(SessionStatus.SUBMITTED);
        when(examSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(inProgressSession));

        SessionRequest.SubmitAnswer req = new SessionRequest.SubmitAnswer();
        req.setQuestionId(QUESTION_ID);
        req.setSelectedOptionId(301L);

        assertThatThrownBy(() -> sessionService.submitAnswer(USER_ID, SESSION_ID, req))
                .isInstanceOfSatisfying(AppException.class, ex -> {
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(ex.getCode()).isEqualTo("SESSION_ALREADY_SUBMITTED");
                });

        verify(sessionAnswerRepository, never()).save(any(SessionAnswer.class));
    }

    // ===== serverSubmit (server-side scoring) =====

    @Test
    @DisplayName("serverSubmit: happy path marks SUBMITTED and computes score from stored answers")
    void serverSubmit_happyPath_marksSubmittedAndScoresFromStored() {
        SessionAnswer a1 = SessionAnswer.builder()
                .id(1L).sessionId(SESSION_ID).questionId(QUESTION_ID).questionOrder(1)
                .selectedOptionId(301L).isCorrect(true).isBookmarked(false).build();
        SessionAnswer a2 = SessionAnswer.builder()
                .id(2L).sessionId(SESSION_ID).questionId(SECOND_QUESTION_ID).questionOrder(2)
                .selectedOptionId(402L).isCorrect(false).isBookmarked(false).build();
        when(examSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(inProgressSession));
        when(sessionAnswerRepository.findBySessionId(SESSION_ID)).thenReturn(List.of(a1, a2));
        when(examQuestionRepository.findByExamIdOrderByQuestionOrderAscIdAsc(EXAM_ID))
                .thenReturn(List.of(
                        examQuestion(QUESTION_ID, 1),
                        examQuestion(SECOND_QUESTION_ID, 2)
                ));
        when(examSessionRepository.save(any(ExamSession.class))).thenAnswer(inv -> inv.getArgument(0));

        SessionResponse.SessionInfo info = sessionService.serverSubmit(USER_ID, SESSION_ID);

        assertThat(info.getStatus()).isEqualTo("SUBMITTED");
        assertThat(info.getCorrectCount()).isEqualTo(1);
        assertThat(info.getTotalQuestions()).isEqualTo(2);
        assertThat(info.getScorePercentage()).isEqualByComparingTo(BigDecimal.valueOf(50.00));
        assertThat(info.getSubmittedAt()).isNotNull();

        ArgumentCaptor<ExamSession> cap = ArgumentCaptor.forClass(ExamSession.class);
        verify(examSessionRepository).save(cap.capture());
        ExamSession saved = cap.getValue();
        assertThat(saved.getStatus()).isEqualTo(SessionStatus.SUBMITTED);
        assertThat(saved.getAnsweredCount()).isEqualTo(2);
        assertThat(saved.getCorrectCount()).isEqualTo(1);
        assertThat(saved.getWrongCount()).isEqualTo(1);
        assertThat(saved.getSkippedCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("serverSubmit: partially answered session sets skippedCount = total - answered")
    void serverSubmit_partialAnswer_setsSkippedCount() {
        SessionAnswer a1 = SessionAnswer.builder()
                .id(1L).sessionId(SESSION_ID).questionId(QUESTION_ID).questionOrder(1)
                .selectedOptionId(301L).isCorrect(true).isBookmarked(false).build();
        when(examSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(inProgressSession));
        when(sessionAnswerRepository.findBySessionId(SESSION_ID)).thenReturn(List.of(a1));
        when(examQuestionRepository.findByExamIdOrderByQuestionOrderAscIdAsc(EXAM_ID))
                .thenReturn(List.of(
                        examQuestion(QUESTION_ID, 1),
                        examQuestion(SECOND_QUESTION_ID, 2)
                ));
        when(examSessionRepository.save(any(ExamSession.class))).thenAnswer(inv -> inv.getArgument(0));

        SessionResponse.SessionInfo info = sessionService.serverSubmit(USER_ID, SESSION_ID);

        assertThat(info.getTotalQuestions()).isEqualTo(2);
        assertThat(info.getCorrectCount()).isEqualTo(1);
        assertThat(info.getScorePercentage()).isEqualByComparingTo(BigDecimal.valueOf(50.00));

        ArgumentCaptor<ExamSession> cap = ArgumentCaptor.forClass(ExamSession.class);
        verify(examSessionRepository).save(cap.capture());
        ExamSession saved = cap.getValue();
        assertThat(saved.getAnsweredCount()).isEqualTo(1);
        assertThat(saved.getSkippedCount()).isEqualTo(1);
        assertThat(saved.getWrongCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("serverSubmit: exam with no questions throws BAD_REQUEST")
    void serverSubmit_emptyExam_throwsBadRequest() {
        when(examSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(inProgressSession));
        when(examQuestionRepository.findByExamIdOrderByQuestionOrderAscIdAsc(EXAM_ID))
                .thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> sessionService.serverSubmit(USER_ID, SESSION_ID))
                .isInstanceOfSatisfying(AppException.class, ex -> {
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(ex.getCode()).isEqualTo("BAD_REQUEST");
                });

        verify(examSessionRepository, never()).save(any());
    }

    @Test
    @DisplayName("serverSubmit: different user throws SESSION_FORBIDDEN")
    void serverSubmit_differentUser_throwsForbidden() {
        when(examSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(inProgressSession));

        assertThatThrownBy(() -> sessionService.serverSubmit(OTHER_USER_ID, SESSION_ID))
                .isInstanceOfSatisfying(AppException.class, ex -> {
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
                    assertThat(ex.getCode()).isEqualTo("SESSION_FORBIDDEN");
                });
    }

    @Test
    @DisplayName("serverSubmit: already submitted session throws SESSION_ALREADY_SUBMITTED (anti-replay)")
    void serverSubmit_alreadySubmitted_throwsConflict() {
        inProgressSession.setStatus(SessionStatus.SUBMITTED);
        when(examSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(inProgressSession));

        assertThatThrownBy(() -> sessionService.serverSubmit(USER_ID, SESSION_ID))
                .isInstanceOfSatisfying(AppException.class, ex -> {
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(ex.getCode()).isEqualTo("SESSION_ALREADY_SUBMITTED");
                });
    }

    private void assertQuestionContentStatusRejected(SessionStatus status) {
        inProgressSession.setStatus(status);
        when(examSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(inProgressSession));

        assertThatThrownBy(() -> sessionService.getQuestionForSession(SESSION_ID, QUESTION_ID, USER_ID))
                .isInstanceOfSatisfying(AppException.class, ex -> {
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(ex.getCode()).isEqualTo("BAD_REQUEST");
                });

        verify(examQuestionRepository, never()).findByExamIdOrderByQuestionOrderAscIdAsc(any());
    }

    private void assertAnswerKeysStatusRejected(SessionStatus status) {
        inProgressSession.setStatus(status);
        when(examSessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(inProgressSession));

        assertThatThrownBy(() -> sessionService.getAnswerKeysForSession(SESSION_ID, USER_ID))
                .isInstanceOfSatisfying(AppException.class, ex -> {
                    assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(ex.getCode()).isEqualTo("BAD_REQUEST");
                });

        verify(examQuestionRepository, never()).findByExamIdOrderByQuestionOrderAscIdAsc(any());
    }

    private ExamQuestion examQuestion(Long questionId, Integer questionOrder) {
        return ExamQuestion.builder()
                .id(questionId + 1000)
                .examId(EXAM_ID)
                .questionId(questionId)
                .questionOrder(questionOrder)
                .build();
    }

    private Question question(Long questionId, String explanation) {
        return Question.builder()
                .id(questionId)
                .questionCode("Q-" + questionId)
                .subjectId(1L)
                .topicId(2L)
                .difficulty(Difficulty.MEDIUM)
                .questionType(QuestionType.SINGLE_CHOICE)
                .questionText("Question text " + questionId)
                .explanation(explanation)
                .status(com.vsatcompass.api.entity.enums.QuestionStatus.APPROVED)
                .version(1)
                .createdBy(3L)
                .build();
    }

    private QuestionOption option(Long id, Long questionId, boolean correct, Integer displayOrder) {
        return QuestionOption.builder()
                .id(id)
                .questionId(questionId)
                .optionLabel(String.valueOf(displayOrder))
                .optionText("Option " + displayOrder)
                .isCorrect(correct)
                .displayOrder(displayOrder)
                .build();
    }
}
