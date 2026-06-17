package com.vsatcompass.api.service.impl;

import com.vsatcompass.api.dto.request.SessionRequest;
import com.vsatcompass.api.dto.response.QuestionAnswerKeyResponse;
import com.vsatcompass.api.dto.response.QuestionOptionContentResponse;
import com.vsatcompass.api.dto.response.SessionAnswerKeysResponse;
import com.vsatcompass.api.dto.response.SessionQuestionContentResponse;
import com.vsatcompass.api.dto.response.SessionResponse;
import com.vsatcompass.api.entity.ExamQuestion;
import com.vsatcompass.api.entity.ExamSession;
import com.vsatcompass.api.entity.Question;
import com.vsatcompass.api.entity.QuestionOption;
import com.vsatcompass.api.entity.SessionAnswer;
import com.vsatcompass.api.entity.enums.SessionMode;
import com.vsatcompass.api.entity.enums.SessionStatus;
import com.vsatcompass.api.exception.AppException;
import com.vsatcompass.api.repository.ExamQuestionRepository;
import com.vsatcompass.api.repository.ExamSessionRepository;
import com.vsatcompass.api.repository.QuestionOptionRepository;
import com.vsatcompass.api.repository.QuestionRepository;
import com.vsatcompass.api.repository.SessionAnswerRepository;
import com.vsatcompass.api.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionServiceImpl implements SessionService {

    private final ExamSessionRepository examSessionRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository questionOptionRepository;
    private final SessionAnswerRepository sessionAnswerRepository;

    @Override
    @Transactional
    public SessionResponse.SessionInfo startSession(Long userId, SessionRequest.StartSession request) {
        // Resolve session mode (default MOCK_EXAM)
        SessionMode mode = SessionMode.MOCK_EXAM;
        if (request.getMode() != null) {
            try {
                mode = SessionMode.valueOf(request.getMode());
            } catch (IllegalArgumentException e) {
                throw AppException.badRequest("Mode không hợp lệ. Chọn: MOCK_EXAM, PRACTICE");
            }
        }

        // Default totalQuestions to 0 if not provided (client will update via client-submit)
        int totalQuestions = request.getTotalQuestions() != null ? request.getTotalQuestions() : 0;

        ExamSession session = ExamSession.builder()
                .userId(userId)
                .examId(request.getExamId())
                .mode(mode)
                .status(SessionStatus.IN_PROGRESS)
                .totalQuestions(totalQuestions)
                .deviceType("ANDROID")
                .build();

        session = examSessionRepository.save(session);

        log.info("Session {} started by user {} for exam {} (mode={})",
                session.getId(), userId, request.getExamId(), mode);

        return toSessionInfo(session, orderedQuestionIdsForExam(session.getExamId()));
    }

    @Override
    @Transactional(readOnly = true)
    public SessionQuestionContentResponse getQuestionForSession(
            Long sessionId,
            Long questionId,
            Long currentUserId
    ) {
        ExamSession session = loadOwnedSession(sessionId, currentUserId);
        requireStatus(session, SessionStatus.IN_PROGRESS);

        ExamQuestion examQuestion = findExamQuestion(session.getExamId(), questionId);
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> AppException.notFound("Question", questionId));
        List<QuestionOptionContentResponse> options = questionOptionRepository
                .findByQuestionIdOrderByDisplayOrderAscIdAsc(questionId)
                .stream()
                .map(this::toOptionContent)
                .toList();

        return SessionQuestionContentResponse.builder()
                .id(question.getId())
                .questionCode(question.getQuestionCode())
                .content(question.getQuestionText())
                .questionType(question.getQuestionType())
                .difficulty(question.getDifficulty())
                .order(examQuestion.getQuestionOrder())
                .options(options)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public SessionAnswerKeysResponse getAnswerKeysForSession(
            Long sessionId,
            Long currentUserId
    ) {
        ExamSession session = loadOwnedSession(sessionId, currentUserId);
        requireStatus(session, SessionStatus.SUBMITTED);

        List<QuestionAnswerKeyResponse> questions = examQuestionRepository
                .findByExamIdOrderByQuestionOrderAscIdAsc(session.getExamId())
                .stream()
                .map(this::toAnswerKey)
                .toList();

        return SessionAnswerKeysResponse.builder()
                .sessionId(session.getId())
                .examId(session.getExamId())
                .questions(questions)
                .build();
    }

    @Override
    @Transactional
    public void submitAnswer(Long userId, Long sessionId, SessionRequest.SubmitAnswer request) {
        ExamSession session = examSessionRepository.findById(sessionId)
                .orElseThrow(() -> AppException.notFound("ExamSession", sessionId));
        if (!session.getUserId().equals(userId)) {
            throw AppException.sessionForbidden();
        }
        if (session.getStatus() == SessionStatus.SUBMITTED) {
            throw AppException.sessionAlreadySubmitted();
        }
        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            throw AppException.badRequest("Phiên thi không ở trạng thái IN_PROGRESS (hiện tại: " + session.getStatus() + ")");
        }

        Long questionId = request.getQuestionId();
        Long selectedOptionId = request.getSelectedOptionId();
        if (questionId == null || selectedOptionId == null) {
            throw AppException.badRequest("questionId và selectedOptionId không được để trống");
        }

        // Confirm the question belongs to the session's exam
        ExamQuestion examQuestion = findExamQuestion(session.getExamId(), questionId);

        // Verify the option belongs to the question (defense against random ids)
        boolean optionBelongs = questionOptionRepository
                .findByQuestionIdOrderByDisplayOrderAscIdAsc(questionId)
                .stream()
                .anyMatch(option -> Objects.equals(option.getId(), selectedOptionId));
        if (!optionBelongs) {
            throw AppException.badRequest("Option " + selectedOptionId + " không thuộc câu hỏi " + questionId);
        }

        Boolean isCorrect = questionOptionRepository.findById(selectedOptionId)
                .map(option -> Boolean.TRUE.equals(option.getIsCorrect()))
                .orElse(false);

        // Idempotent: if an answer for (session, question) already exists, replace it.
        OffsetDateTime answeredAt = OffsetDateTime.now();

        // Idempotent: if an answer for (session, question) already exists, update it in-place
        // (delete+insert would violate the (session_id, question_id) unique constraint under
        // JPA's deferred flush).
        SessionAnswer row = sessionAnswerRepository
                .findBySessionIdAndQuestionId(sessionId, questionId)
                .orElseGet(() -> SessionAnswer.builder()
                        .sessionId(sessionId)
                        .questionId(questionId)
                        .questionOrder(examQuestion.getQuestionOrder())
                        .build());
        row.setQuestionOrder(examQuestion.getQuestionOrder());
        row.setSelectedOptionId(selectedOptionId);
        row.setIsCorrect(isCorrect);
        row.setIsBookmarked(Boolean.TRUE.equals(request.getBookmarked()));
        row.setTimeSpentSeconds(request.getTimeSpentSeconds() != null ? request.getTimeSpentSeconds() : 0);
        row.setAnsweredAt(answeredAt);
        sessionAnswerRepository.save(row);

        // Keep session.answeredCount fresh so clients can show a live progress bar.
        long answeredCount = sessionAnswerRepository.countBySessionId(sessionId);
        session.setAnsweredCount((int) answeredCount);
        examSessionRepository.save(session);

        log.debug("Session {} answer saved: question={} option={} correct={}",
                sessionId, questionId, selectedOptionId, isCorrect);
    }

    @Override
    @Transactional
    public SessionResponse.SessionInfo serverSubmit(Long userId, Long sessionId) {
        ExamSession session = examSessionRepository.findById(sessionId)
                .orElseThrow(() -> AppException.notFound("ExamSession", sessionId));
        if (!session.getUserId().equals(userId)) {
            throw AppException.sessionForbidden();
        }
        if (session.getStatus() == SessionStatus.SUBMITTED) {
            throw AppException.sessionAlreadySubmitted();
        }
        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            throw AppException.badRequest("Phiên thi không ở trạng thái IN_PROGRESS (hiện tại: " + session.getStatus() + ")");
        }

        // Count stored answers + score
        List<SessionAnswer> rows = sessionAnswerRepository.findBySessionId(sessionId);

        int totalQuestions = examQuestionRepository
                .findByExamIdOrderByQuestionOrderAscIdAsc(session.getExamId())
                .size();
        if (totalQuestions <= 0) {
            throw AppException.badRequest("Đề thi không có câu hỏi nào, không thể nộp bài");
        }

        int correctCount = (int) rows.stream()
                .filter(r -> Boolean.TRUE.equals(r.getIsCorrect()))
                .count();
        int answeredCount = rows.size();
        int wrongCount = Math.max(0, answeredCount - correctCount);
        int skippedCount = Math.max(0, totalQuestions - answeredCount);

        BigDecimal scorePercentage = BigDecimal.valueOf(correctCount * 100.0 / totalQuestions)
                .setScale(2, java.math.RoundingMode.HALF_UP);

        Integer timeSpentSeconds = session.getTimeSpentSeconds();
        if (timeSpentSeconds == null && session.getStartedAt() != null) {
            long seconds = java.time.Duration.between(session.getStartedAt(), OffsetDateTime.now()).getSeconds();
            timeSpentSeconds = (int) Math.max(0, seconds);
        }

        session.setStatus(SessionStatus.SUBMITTED);
        session.setSubmittedAt(OffsetDateTime.now());
        session.setTotalQuestions(totalQuestions);
        session.setAnsweredCount(answeredCount);
        session.setCorrectCount(correctCount);
        session.setWrongCount(wrongCount);
        session.setSkippedCount(skippedCount);
        session.setScore(scorePercentage);
        session.setScorePercentage(scorePercentage);
        session.setTimeSpentSeconds(timeSpentSeconds != null ? timeSpentSeconds : 0);

        session = examSessionRepository.save(session);

        log.info("Session {} server-submitted by user {}: {}/{} correct ({}%)",
                session.getId(), userId, correctCount, totalQuestions, scorePercentage);

        return toSessionInfo(session);
    }

    @Override
    @Transactional
    public SessionResponse.SessionInfo clientSubmit(Long userId, Long sessionId, SessionRequest.ClientSubmit request) {
        // Find session — must exist
        ExamSession session = examSessionRepository.findById(sessionId)
                .orElseThrow(() -> AppException.notFound("ExamSession", sessionId));

        // Owner check — the session must belong to this user
        if (!session.getUserId().equals(userId)) {
            throw AppException.sessionForbidden();
        }

        // Anti-replay: session must be IN_PROGRESS
        if (session.getStatus() == SessionStatus.SUBMITTED) {
            throw AppException.sessionAlreadySubmitted();
        }
        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            throw AppException.badRequest("Phiên thi không ở trạng thái IN_PROGRESS (hiện tại: " + session.getStatus() + ")");
        }

        // Validate correctCount ≤ totalQuestions
        if (request.getCorrectCount() > request.getTotalQuestions()) {
            throw AppException.validationFailed("correctCount không được lớn hơn totalQuestions");
        }

        // Update session with client-computed results
        BigDecimal score = BigDecimal.valueOf(request.getScore());
        BigDecimal scorePercentage = BigDecimal.valueOf(request.getScore());
        int wrongCount = request.getTotalQuestions() - request.getCorrectCount();

        session.setStatus(SessionStatus.SUBMITTED);
        session.setSubmittedAt(OffsetDateTime.now());
        session.setScore(score);
        session.setScorePercentage(scorePercentage);
        session.setCorrectCount(request.getCorrectCount());
        session.setTotalQuestions(request.getTotalQuestions());
        session.setAnsweredCount(request.getTotalQuestions()); // client-side: assume all answered
        session.setWrongCount(wrongCount);
        session.setSkippedCount(0);
        session.setTimeSpentSeconds(request.getTimeSpentSeconds());

        session = examSessionRepository.save(session);
        persistClientAnswers(session, request);

        log.info("Session {} submitted by user {}: score={}, correct={}/{}",
                session.getId(), userId, request.getScore(),
                request.getCorrectCount(), request.getTotalQuestions());

        return toSessionInfo(session);
    }

    private void persistClientAnswers(ExamSession session, SessionRequest.ClientSubmit request) {
        if (request.getAnswers() == null || request.getAnswers().isEmpty()) {
            return;
        }

        Map<Long, Integer> orderByQuestionId = new HashMap<>();
        for (ExamQuestion examQuestion : examQuestionRepository
                .findByExamIdOrderByQuestionOrderAscIdAsc(session.getExamId())) {
            orderByQuestionId.put(examQuestion.getQuestionId(), examQuestion.getQuestionOrder());
        }

        sessionAnswerRepository.deleteBySessionId(session.getId());
        OffsetDateTime answeredAt = OffsetDateTime.now();
        List<SessionAnswer> rows = new ArrayList<>();

        for (SessionRequest.ClientSubmit.ClientSubmitAnswer answer : request.getAnswers()) {
            if (answer.getQuestionId() == null) {
                continue;
            }
            Long selectedOptionId = answer.getSelectedOptionId();
            Boolean isCorrect = null;
            if (selectedOptionId != null) {
                isCorrect = questionOptionRepository.findById(selectedOptionId)
                        .map(option -> Boolean.TRUE.equals(option.getIsCorrect()))
                        .orElse(false);
            }
            int questionOrder = answer.getQuestionOrder() != null
                    ? answer.getQuestionOrder()
                    : orderByQuestionId.getOrDefault(answer.getQuestionId(), rows.size() + 1);

            rows.add(SessionAnswer.builder()
                    .sessionId(session.getId())
                    .questionId(answer.getQuestionId())
                    .questionOrder(questionOrder)
                    .selectedOptionId(selectedOptionId)
                    .isCorrect(isCorrect)
                    .isBookmarked(Boolean.TRUE.equals(answer.getBookmarked()))
                    .answeredAt(answeredAt)
                    .build());
        }

        if (!rows.isEmpty()) {
            sessionAnswerRepository.saveAll(rows);
        }
    }

    private SessionResponse.SessionInfo toSessionInfo(ExamSession session) {
        return toSessionInfo(session, List.of());
    }

    private SessionResponse.SessionInfo toSessionInfo(
            ExamSession session,
            List<Long> orderedQuestionIds
    ) {
        return SessionResponse.SessionInfo.builder()
                .id(session.getId())
                .examId(session.getExamId())
                .mode(session.getMode().name())
                .status(session.getStatus().name())
                .score(session.getScore())
                .scorePercentage(session.getScorePercentage())
                .correctCount(session.getCorrectCount())
                .totalQuestions(session.getTotalQuestions())
                .timeSpentSeconds(session.getTimeSpentSeconds())
                .startedAt(session.getStartedAt())
                .submittedAt(session.getSubmittedAt())
                .orderedQuestionIds(orderedQuestionIds != null ? orderedQuestionIds : List.of())
                .build();
    }

    private List<Long> orderedQuestionIdsForExam(Long examId) {
        return examQuestionRepository.findByExamIdOrderByQuestionOrderAscIdAsc(examId)
                .stream()
                .map(ExamQuestion::getQuestionId)
                .toList();
    }

    private ExamSession loadOwnedSession(Long sessionId, Long currentUserId) {
        ExamSession session = examSessionRepository.findById(sessionId)
                .orElseThrow(() -> AppException.notFound("ExamSession", sessionId));
        if (!session.getUserId().equals(currentUserId)) {
            throw AppException.sessionForbidden();
        }
        return session;
    }

    private void requireStatus(ExamSession session, SessionStatus requiredStatus) {
        if (session.getStatus() != requiredStatus) {
            throw AppException.badRequest(
                    "Session must be " + requiredStatus + " (current: " + session.getStatus() + ")");
        }
    }

    private ExamQuestion findExamQuestion(Long examId, Long questionId) {
        return examQuestionRepository.findByExamIdOrderByQuestionOrderAscIdAsc(examId)
                .stream()
                .filter(examQuestion -> Objects.equals(examQuestion.getQuestionId(), questionId))
                .findFirst()
                .orElseThrow(() -> AppException.notFound("ExamQuestion", questionId));
    }

    private QuestionOptionContentResponse toOptionContent(QuestionOption option) {
        return QuestionOptionContentResponse.builder()
                .id(option.getId())
                .content(option.getOptionText())
                .order(option.getDisplayOrder())
                .build();
    }

    private QuestionAnswerKeyResponse toAnswerKey(ExamQuestion examQuestion) {
        Long questionId = examQuestion.getQuestionId();
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> AppException.notFound("Question", questionId));
        List<Long> correctOptionIds = questionOptionRepository
                .findByQuestionIdOrderByDisplayOrderAscIdAsc(questionId)
                .stream()
                .filter(option -> Boolean.TRUE.equals(option.getIsCorrect()))
                .map(QuestionOption::getId)
                .toList();

        return QuestionAnswerKeyResponse.builder()
                .questionId(questionId)
                .correctOptionIds(correctOptionIds)
                .explanation(question.getExplanation())
                .build();
    }
}
