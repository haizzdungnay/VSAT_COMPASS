package com.vsatcompass.api.service.impl;

import com.vsatcompass.api.dto.request.SessionRequest;
import com.vsatcompass.api.dto.response.AudioPlayResponse;
import com.vsatcompass.api.dto.response.QuestionAnswerKeyResponse;
import com.vsatcompass.api.dto.response.QuestionGroupResponse;
import com.vsatcompass.api.dto.response.QuestionOptionContentResponse;
import com.vsatcompass.api.dto.response.SessionAnswerKeysResponse;
import com.vsatcompass.api.dto.response.SessionAnswerResponse;
import com.vsatcompass.api.dto.response.SessionQuestionContentResponse;
import com.vsatcompass.api.dto.response.SessionResponse;
import com.vsatcompass.api.entity.ExamQuestion;
import com.vsatcompass.api.entity.ExamSession;
import com.vsatcompass.api.entity.Question;
import com.vsatcompass.api.entity.QuestionGroup;
import com.vsatcompass.api.entity.QuestionMatchingChoice;
import com.vsatcompass.api.entity.QuestionOption;
import com.vsatcompass.api.entity.SessionAnswer;
import com.vsatcompass.api.entity.enums.QuestionType;
import com.vsatcompass.api.entity.enums.SessionMode;
import com.vsatcompass.api.entity.enums.SessionStatus;
import com.vsatcompass.api.exception.AppException;
import com.vsatcompass.api.repository.ExamQuestionRepository;
import com.vsatcompass.api.repository.ExamSessionRepository;
import com.vsatcompass.api.repository.QuestionGroupRepository;
import com.vsatcompass.api.entity.ExamWindow;
import com.vsatcompass.api.entity.enums.ExamDeliveryMode;
import com.vsatcompass.api.repository.ExamWindowRepository;
import com.vsatcompass.api.repository.QuestionMatchingChoiceRepository;
import com.vsatcompass.api.repository.QuestionOptionRepository;
import com.vsatcompass.api.repository.QuestionRepository;
import com.vsatcompass.api.repository.SessionAnswerRepository;
import com.vsatcompass.api.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionServiceImpl implements SessionService {

    private final ExamSessionRepository examSessionRepository;
    private final ExamQuestionRepository examQuestionRepository;
    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository questionOptionRepository;
    private final SessionAnswerRepository sessionAnswerRepository;
    private final QuestionGroupRepository questionGroupRepository;
    private final ExamWindowRepository examWindowRepository;

    @Autowired(required = false)
    private QuestionMatchingChoiceRepository questionMatchingChoiceRepository;

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

        Long examWindowId = request.getExamWindowId();
        if (examWindowId != null) {
            ExamWindow window = examWindowRepository.findById(examWindowId)
                    .orElseThrow(() -> AppException.notFound("ExamWindow", examWindowId));
            if (!Objects.equals(window.getExamId(), request.getExamId())) {
                throw AppException.badRequest("Ca thi khong thuoc ve de thi nay");
            }
            if (window.getMode() == ExamDeliveryMode.SCHEDULED_MOCK) {
                OffsetDateTime now = OffsetDateTime.now();
                if (window.getStartAt() != null && now.isBefore(window.getStartAt())) {
                    throw AppException.badRequest("Ca thi chua bat dau");
                }
                OffsetDateTime hardEnd = window.getEndAt() != null
                        ? window.getEndAt().plusMinutes(window.getGracePeriodMinutes() != null ? window.getGracePeriodMinutes() : 0)
                        : null;
                if (hardEnd != null && now.isAfter(hardEnd)) {
                    throw AppException.badRequest("Ca thi da ket thuc");
                }
            }
            if (window.getMaxAttempts() != null && window.getMaxAttempts() > 0) {
                long userAttempts = examSessionRepository.countByUserIdAndExamWindowId(userId, examWindowId);
                if (userAttempts >= window.getMaxAttempts()) {
                    throw AppException.badRequest("Ban da vuot qua so lan thi cho phep (" + window.getMaxAttempts() + " lan)");
                }
            }
        }

        ExamSession session = ExamSession.builder()
                .userId(userId)
                .examId(request.getExamId())
                .examWindowId(examWindowId)
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
        if (session.getStatus() != SessionStatus.IN_PROGRESS && session.getStatus() != SessionStatus.SUBMITTED) {
            throw AppException.badRequest(
                    "Session must be IN_PROGRESS or SUBMITTED (current: " + session.getStatus() + ")");
        }

        ExamQuestion examQuestion = findExamQuestion(session.getExamId(), questionId);
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> AppException.notFound("Question", questionId));
        List<QuestionOptionContentResponse> options = questionOptionRepository
                .findByQuestionIdOrderByDisplayOrderAscIdAsc(questionId)
                .stream()
                .map(opt -> toOptionContent(opt, question.getQuestionType()))
                .toList();

        List<SessionQuestionContentResponse.MatchingChoiceDto> matchingChoices = null;
        if (question.getQuestionType() == QuestionType.MATCHING && questionMatchingChoiceRepository != null) {
            List<QuestionMatchingChoice> foundChoices = questionMatchingChoiceRepository
                    .findByQuestionIdOrderByDisplayOrderAsc(questionId);
            if (foundChoices != null && !foundChoices.isEmpty()) {
                matchingChoices = foundChoices.stream()
                        .map(choice -> SessionQuestionContentResponse.MatchingChoiceDto.builder()
                                .key(choice.getChoiceKey())
                                .text(choice.getChoiceText())
                                .build())
                        .toList();
            }
        }

        QuestionGroupResponse group = null;
        if (question.getQuestionGroupId() != null) {
            group = questionGroupRepository.findById(question.getQuestionGroupId())
                    .map(this::toQuestionGroupResponse)
                    .orElse(null);
        }
        return SessionQuestionContentResponse.builder()
                .id(question.getId())
                .questionCode(question.getQuestionCode())
                .questionGroupId(question.getQuestionGroupId())
                .questionGroup(group)
                .content(question.getQuestionText())
                .questionType(question.getQuestionType())
                .difficulty(question.getDifficulty())
                .order(examQuestion.getQuestionOrder())
                .options(options)
                .matchingChoices(matchingChoices)
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
    public SessionResponse.ViolationResult recordViolation(
            Long userId,
            Long sessionId,
            SessionRequest.RecordViolation request
    ) {
        ExamSession session = loadOwnedSession(sessionId, userId);

        int newCount = (session.getViolationCount() != null ? session.getViolationCount() : 0) + 1;
        session.setViolationCount(newCount);

        Map<String, Object> logEntry = new HashMap<>();
        logEntry.put("violationType", request.getViolationType());
        if (request.getDetails() != null) {
            logEntry.put("details", request.getDetails());
        }
        logEntry.put("timestamp", request.getTimestamp() != null ? request.getTimestamp() : OffsetDateTime.now().toString());
        logEntry.put("recordedAt", OffsetDateTime.now().toString());

        List<Map<String, Object>> logs = session.getProctoringLogs();
        if (logs == null) {
            logs = new ArrayList<>();
        } else {
            logs = new ArrayList<>(logs);
        }
        logs.add(logEntry);
        session.setProctoringLogs(logs);

        session = examSessionRepository.save(session);

        log.warn("Proctoring violation recorded: session={} user={} type={} count={}",
                sessionId, userId, request.getViolationType(), newCount);

        if (newCount >= 3 && session.getStatus() == SessionStatus.IN_PROGRESS) {
            log.warn("Session {} reached violation limit ({} >= 3). Auto-submitting...", sessionId, newCount);
            SessionResponse.SessionInfo submitted = serverSubmit(userId, sessionId);
            return SessionResponse.ViolationResult.builder()
                    .sessionId(sessionId)
                    .violationCount(newCount)
                    .maxViolations(3)
                    .autoSubmitted(true)
                    .sessionStatus(submitted.getStatus())
                    .score(submitted.getScore())
                    .build();
        }

        return SessionResponse.ViolationResult.builder()
                .sessionId(sessionId)
                .violationCount(newCount)
                .maxViolations(3)
                .autoSubmitted(false)
                .sessionStatus(session.getStatus().name())
                .score(session.getScore())
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
        if (questionId == null) {
            throw AppException.badRequest("questionId và câu trả lời không được để trống");
        }

        // Confirm the question belongs to the session's exam
        ExamQuestion examQuestion = findExamQuestion(session.getExamId(), questionId);

        Long selectedOptionId = request.getSelectedOptionId();
        Map<String, Object> payload = request.getAnswerPayload();

        if (selectedOptionId == null && (payload == null || payload.isEmpty())) {
            // ponytail: deleting row also clears bookmark if any
            sessionAnswerRepository.findBySessionIdAndQuestionId(sessionId, questionId)
                    .ifPresent(existing -> {
                        sessionAnswerRepository.delete(existing);
                        session.setAnsweredCount((int) sessionAnswerRepository.countBySessionId(sessionId));
                        examSessionRepository.save(session);
                    });
            return;
        }

        Question question = questionRepository.findById(questionId).orElse(null);
        List<QuestionOption> options = questionOptionRepository
                .findByQuestionIdOrderByDisplayOrderAscIdAsc(questionId);

        Boolean isCorrect = false;
        Integer correctSubitems = null;
        Integer totalSubitems = null;
        BigDecimal rawPoints = BigDecimal.ZERO;
        Long effectiveOptionId = selectedOptionId;

        if (payload != null && !payload.isEmpty()) {
            QuestionType qType = question != null ? question.getQuestionType() : QuestionType.SINGLE_CHOICE;
            if (qType == null) qType = QuestionType.SINGLE_CHOICE;

            switch (qType) {
                case TRUE_FALSE -> {
                    // ponytail: scoring profile hardcoded to draft spec 150 rules; upgrade to dynamic scoring profile lookup when multi-spec enabled
                    int matched = 0;
                    int total = options.isEmpty() ? payload.size() : options.size();
                    if (total == 0) total = 4;

                    for (int i = 0; i < options.size(); i++) {
                        QuestionOption opt = options.get(i);
                        String key = opt.getOptionLabel() != null
                                ? opt.getOptionLabel().toLowerCase().trim()
                                : String.valueOf((char) ('a' + i));
                        Object userVal = payload.get(key);
                        if (userVal == null) {
                            userVal = payload.get(key.toUpperCase());
                        }
                        if (userVal != null) {
                            boolean userBool = parseBooleanValue(userVal);
                            boolean expectedBool = Boolean.TRUE.equals(opt.getIsCorrect());
                            if (userBool == expectedBool) {
                                matched++;
                            }
                        }
                    }
                    correctSubitems = matched;
                    totalSubitems = total;
                    isCorrect = (matched == total && total > 0);
                    double pts = switch (matched) {
                        case 1 -> 1.0;
                        case 2 -> 2.0;
                        case 3 -> 3.0;
                        case 4 -> 6.0;
                        default -> matched > 4 ? 6.0 : 0.0;
                    };
                    rawPoints = BigDecimal.valueOf(pts);
                }
                case MATCHING -> {
                    if (options == null || options.isEmpty()) {
                        correctSubitems = 0;
                        totalSubitems = 0;
                        isCorrect = false;
                        rawPoints = BigDecimal.ZERO;
                    } else {
                        int matchedCount = 0;
                        int total = options.size();

                        for (QuestionOption opt : options) {
                            String key = opt.getOptionLabel() != null ? opt.getOptionLabel().trim() : "";
                            Object userVal = payload.get(key);
                            if (userVal != null && opt.getOptionText() != null) {
                                String userStr = String.valueOf(userVal).trim();
                                if (isMatchingChoiceEqual(opt.getOptionText(), userStr)) {
                                    matchedCount++;
                                }
                            }
                        }
                        if (matchedCount > total) {
                            matchedCount = total;
                        }
                        correctSubitems = matchedCount;
                        totalSubitems = total;
                        isCorrect = (matchedCount == total && total > 0);
                        rawPoints = BigDecimal.valueOf(matchedCount * 1.5);
                    }
                }
                case SHORT_ANSWER -> {
                    Object val = payload.get("value");
                    if (val == null) val = payload.get("answer");
                    if (val == null) val = payload.get("text");
                    if (val == null && !payload.isEmpty()) {
                        val = payload.values().iterator().next();
                    }
                    String userStr = val != null ? String.valueOf(val).trim() : "";

                    String expected = "";
                    for (QuestionOption opt : options) {
                        if (Boolean.TRUE.equals(opt.getIsCorrect()) || options.size() == 1) {
                            expected = opt.getOptionText() != null ? opt.getOptionText().trim() : "";
                            break;
                        }
                    }
                    boolean matches = isShortAnswerEqual(userStr, expected);
                    correctSubitems = matches ? 1 : 0;
                    totalSubitems = 1;
                    isCorrect = matches;
                    rawPoints = matches ? BigDecimal.valueOf(6.0) : BigDecimal.ZERO;
                }
                default -> {
                    if (effectiveOptionId != null) {
                        final Long optId = effectiveOptionId;
                        QuestionOption chosen = options.stream()
                                .filter(o -> Objects.equals(o.getId(), optId))
                                .findFirst()
                                .orElseThrow(() -> AppException.badRequest("Option " + optId + " không thuộc câu hỏi " + questionId));
                        isCorrect = Boolean.TRUE.equals(chosen.getIsCorrect());
                    } else if (payload.containsKey("selectedOptionId")) {
                        Long optId = parseLongValue(payload.get("selectedOptionId"));
                        QuestionOption chosen = options.stream()
                                .filter(o -> Objects.equals(o.getId(), optId))
                                .findFirst()
                                .orElseThrow(() -> AppException.badRequest("Option " + optId + " không thuộc câu hỏi " + questionId));
                        effectiveOptionId = optId;
                        isCorrect = Boolean.TRUE.equals(chosen.getIsCorrect());
                    }
                    correctSubitems = isCorrect ? 1 : 0;
                    totalSubitems = 1;
                    rawPoints = isCorrect ? BigDecimal.valueOf(6.0) : BigDecimal.ZERO;
                }
            }
        } else {
            // Traditional selectedOptionId flow
            final Long optId = effectiveOptionId;
            boolean optionBelongs = options.stream()
                    .anyMatch(option -> Objects.equals(option.getId(), optId));
            if (!optionBelongs) {
                throw AppException.badRequest("Option " + optId + " không thuộc câu hỏi " + questionId);
            }

            isCorrect = questionOptionRepository.findById(optId)
                    .map(option -> Boolean.TRUE.equals(option.getIsCorrect()))
                    .orElse(false);
            correctSubitems = isCorrect ? 1 : 0;
            totalSubitems = 1;
            rawPoints = isCorrect ? BigDecimal.valueOf(6.0) : BigDecimal.ZERO;
        }

        OffsetDateTime answeredAt = OffsetDateTime.now();

        // Idempotent: if an answer for (session, question) already exists, update it in-place
        SessionAnswer row = sessionAnswerRepository
                .findBySessionIdAndQuestionId(sessionId, questionId)
                .orElseGet(() -> SessionAnswer.builder()
                        .sessionId(sessionId)
                        .questionId(questionId)
                        .questionOrder(examQuestion.getQuestionOrder())
                        .build());
        row.setQuestionOrder(examQuestion.getQuestionOrder());
        row.setSelectedOptionId(effectiveOptionId);
        row.setAnswerPayload(payload);
        row.setIsCorrect(isCorrect);
        row.setCorrectSubitems(correctSubitems);
        row.setTotalSubitems(totalSubitems);
        row.setRawPointsAwarded(rawPoints);
        row.setIsBookmarked(Boolean.TRUE.equals(request.getBookmarked()));
        row.setTimeSpentSeconds(request.getTimeSpentSeconds() != null ? request.getTimeSpentSeconds() : 0);
        row.setAnsweredAt(answeredAt);
        sessionAnswerRepository.save(row);

        // Keep session.answeredCount fresh so clients can show a live progress bar.
        long answeredCount = sessionAnswerRepository.countBySessionId(sessionId);
        session.setAnsweredCount((int) answeredCount);
        examSessionRepository.save(session);

        log.debug("Session {} answer saved: question={} option={} payload={} correct={}",
                sessionId, questionId, effectiveOptionId, payload, isCorrect);
    }

    @Override
    @Transactional
    @CacheEvict(value = "leaderboard", allEntries = true)
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

        BigDecimal rawScore = rows.stream()
                .map(r -> r.getRawPointsAwarded() != null
                        ? r.getRawPointsAwarded()
                        : (Boolean.TRUE.equals(r.getIsCorrect()) ? BigDecimal.valueOf(6.0) : BigDecimal.ZERO))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal maxRawScore = BigDecimal.valueOf(totalQuestions * 6.0);

        BigDecimal scorePercentage;
        if (maxRawScore.compareTo(BigDecimal.ZERO) > 0) {
            scorePercentage = rawScore.multiply(BigDecimal.valueOf(100.0))
                    .divide(maxRawScore, 2, RoundingMode.HALF_UP);
        } else {
            scorePercentage = BigDecimal.ZERO;
        }

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
        session.setRawScore(rawScore);
        session.setMaxRawScore(maxRawScore);
        session.setTimeSpentSeconds(timeSpentSeconds != null ? timeSpentSeconds : 0);

        session = examSessionRepository.save(session);

        log.info("Session {} server-submitted by user {}: {}/{} correct ({}%, raw {}/{})",
                session.getId(), userId, correctCount, totalQuestions, scorePercentage, rawScore, maxRawScore);

        return toSessionInfo(session);
    }

    @Override
    @Transactional
    @CacheEvict(value = "leaderboard", allEntries = true)
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

    @Override
    @Transactional(readOnly = true)
    public List<SessionAnswerResponse> getSessionAnswers(Long sessionId, Long userId) {
        ExamSession session = loadOwnedSession(sessionId, userId);
        if (session.getStatus() != SessionStatus.IN_PROGRESS && session.getStatus() != SessionStatus.SUBMITTED) {
            throw AppException.badRequest(
                    "Session must be IN_PROGRESS or SUBMITTED (current: " + session.getStatus() + ")");
        }

        List<SessionAnswer> answers = sessionAnswerRepository.findBySessionId(sessionId);
        return answers.stream()
                .map(a -> SessionAnswerResponse.builder()
                        .questionId(a.getQuestionId())
                        .questionOrder(a.getQuestionOrder())
                        .selectedOptionId(a.getSelectedOptionId())
                        .answerPayload(a.getAnswerPayload())
                        .bookmarked(a.getIsBookmarked())
                        .timeSpentSeconds(a.getTimeSpentSeconds())
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Integer> getAudioPlayCounts(Long sessionId, Long userId) {
        ExamSession session = loadOwnedSession(sessionId, userId);
        if (session.getStatus() != SessionStatus.IN_PROGRESS && session.getStatus() != SessionStatus.SUBMITTED) {
            throw AppException.badRequest(
                    "Session must be IN_PROGRESS or SUBMITTED (current: " + session.getStatus() + ")");
        }
        Map<String, Integer> counts = session.getAudioPlayCounts();
        return counts != null ? counts : Map.of();
    }

    @Override
    @Transactional
    public AudioPlayResponse recordAudioPlay(Long sessionId, Long userId, SessionRequest.RecordAudioPlay request) {
        Optional<ExamSession> sessionOpt = examSessionRepository.findByIdForUpdate(sessionId);
        if (sessionOpt == null || sessionOpt.isEmpty()) {
            sessionOpt = examSessionRepository.findById(sessionId);
        }
        ExamSession session = (sessionOpt != null ? sessionOpt : Optional.<ExamSession>empty())
                .orElseThrow(() -> AppException.notFound("ExamSession", sessionId));

        if (!session.getUserId().equals(userId)) {
            throw AppException.sessionForbidden();
        }
        if (session.getStatus() != SessionStatus.IN_PROGRESS) {
            throw AppException.badRequest("Phiên thi không ở trạng thái IN_PROGRESS (hiện tại: " + session.getStatus() + ")");
        }

        String audioKey = request != null ? request.getAudioKey() : null;
        if (audioKey == null || !audioKey.matches("^[gq]:\\d+$")) {
            throw AppException.badRequest("audioKey phải có định dạng g:{id} hoặc q:{id}");
        }

        String prefix = audioKey.substring(0, 1);
        long targetId;
        try {
            targetId = Long.parseLong(audioKey.substring(2));
        } catch (NumberFormatException e) {
            throw AppException.badRequest("audioKey không hợp lệ");
        }

        List<ExamQuestion> examQuestions = examQuestionRepository
                .findByExamIdOrderByQuestionOrderAscIdAsc(session.getExamId());

        int maxPlays = 2;

        if ("g".equals(prefix)) {
            boolean groupInExam = false;
            for (ExamQuestion eq : examQuestions) {
                Question q = questionRepository.findById(eq.getQuestionId()).orElse(null);
                if (q != null && Objects.equals(q.getQuestionGroupId(), targetId)) {
                    groupInExam = true;
                    break;
                }
            }
            if (!groupInExam) {
                throw new AppException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND",
                        "Nhóm câu hỏi nghe không thuộc đề thi này");
            }

            QuestionGroup qg = questionGroupRepository.findById(targetId).orElse(null);
            if (qg != null && qg.getMaxPlays() != null && qg.getMaxPlays() > 0) {
                maxPlays = qg.getMaxPlays();
            }
        } else {
            boolean questionInExam = examQuestions.stream()
                    .anyMatch(eq -> Objects.equals(eq.getQuestionId(), targetId));
            if (!questionInExam) {
                throw new AppException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND",
                        "Câu hỏi nghe không thuộc đề thi này");
            }

            Question q = questionRepository.findById(targetId).orElse(null);
            if (q != null && q.getQuestionGroupId() != null) {
                QuestionGroup qg = questionGroupRepository.findById(q.getQuestionGroupId()).orElse(null);
                if (qg != null && qg.getMaxPlays() != null && qg.getMaxPlays() > 0) {
                    maxPlays = qg.getMaxPlays();
                }
            }
        }

        Map<String, Integer> counts = session.getAudioPlayCounts();
        if (counts == null) {
            counts = new HashMap<>();
        } else {
            counts = new HashMap<>(counts);
        }

        int current = counts.getOrDefault(audioKey, 0);
        if (current >= maxPlays) {
            throw AppException.conflict("Đã hết số lượt nghe tối đa (" + maxPlays + " lượt) cho phần nghe này");
        }

        int updated = current + 1;
        counts.put(audioKey, updated);
        session.setAudioPlayCounts(counts);
        examSessionRepository.save(session);

        return AudioPlayResponse.builder()
                .audioKey(audioKey)
                .playCount(updated)
                .maxPlays(maxPlays)
                .allowed(true)
                .build();
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
                    .answerPayload(answer.getAnswerPayload())
                    .isCorrect(isCorrect)
                    .isBookmarked(Boolean.TRUE.equals(answer.getBookmarked()))
                    .answeredAt(answeredAt)
                    .build());
        }

        if (!rows.isEmpty()) {
            sessionAnswerRepository.saveAll(rows);
        }
    }

    private boolean parseBooleanValue(Object val) {
        if (val instanceof Boolean b) return b;
        if (val instanceof Number n) return n.intValue() == 1;
        String s = String.valueOf(val).trim().toLowerCase();
        return "true".equals(s) || "1".equals(s) || "t".equals(s) || "d".equals(s) || "dung".equals(s);
    }

    private boolean isMatchingChoiceEqual(String optionText, String userChoice) {
        if (optionText == null || userChoice == null) return false;
        String rawExpected = optionText.trim();
        if (rawExpected.contains("⇄")) {
            rawExpected = rawExpected.substring(rawExpected.indexOf("⇄") + 1).trim();
        } else if (rawExpected.contains("->")) {
            rawExpected = rawExpected.substring(rawExpected.indexOf("->") + 2).trim();
        }

        if (rawExpected.contains(":")) {
            rawExpected = rawExpected.substring(rawExpected.lastIndexOf(":") + 1).trim();
        }

        return rawExpected.equalsIgnoreCase(userChoice.trim());
    }

    private String extractMatchingChoiceKey(String optionText) {
        if (optionText == null) return "";
        String raw = optionText.trim();
        if (raw.contains("⇄")) {
            raw = raw.substring(raw.indexOf("⇄") + 1).trim();
        } else if (raw.contains("->")) {
            raw = raw.substring(raw.indexOf("->") + 2).trim();
        }
        if (raw.contains(":")) {
            raw = raw.substring(raw.lastIndexOf(":") + 1).trim();
        }
        return raw;
    }

    private QuestionOptionContentResponse toOptionContent(QuestionOption option, QuestionType questionType) {
        String content = option.getOptionText();
        if (questionType == QuestionType.MATCHING && content != null && content.contains("⇄")) {
            int idx = content.indexOf("⇄");
            content = content.substring(0, idx).trim();
        }

        return QuestionOptionContentResponse.builder()
                .id(option.getId())
                .optionLabel(option.getOptionLabel())
                .content(content)
                .order(option.getDisplayOrder())
                .build();
    }

    private QuestionAnswerKeyResponse toAnswerKey(ExamQuestion examQuestion) {
        Long questionId = examQuestion.getQuestionId();
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> AppException.notFound("Question", questionId));
        List<QuestionOption> options = questionOptionRepository
                .findByQuestionIdOrderByDisplayOrderAscIdAsc(questionId);

        List<Long> correctOptionIds = options.stream()
                .filter(option -> Boolean.TRUE.equals(option.getIsCorrect()))
                .map(QuestionOption::getId)
                .toList();

        Map<String, Boolean> correctTrueFalse = null;
        String correctShortAnswer = null;
        Map<String, String> correctMatching = null;

        if (question.getQuestionType() != null) {
            switch (question.getQuestionType()) {
                case TRUE_FALSE -> {
                    correctTrueFalse = new LinkedHashMap<>();
                    for (int i = 0; i < options.size(); i++) {
                        QuestionOption opt = options.get(i);
                        String label = opt.getOptionLabel() != null && !opt.getOptionLabel().isBlank()
                                ? opt.getOptionLabel().trim()
                                : String.valueOf((char) ('a' + i));
                        correctTrueFalse.put(label, Boolean.TRUE.equals(opt.getIsCorrect()));
                    }
                }
                case SHORT_ANSWER -> {
                    for (QuestionOption opt : options) {
                        if (Boolean.TRUE.equals(opt.getIsCorrect()) || options.size() == 1) {
                            correctShortAnswer = opt.getOptionText() != null ? opt.getOptionText().trim() : "";
                            break;
                        }
                    }
                }
                case MATCHING -> {
                    correctMatching = new LinkedHashMap<>();
                    for (QuestionOption opt : options) {
                        String label = opt.getOptionLabel() != null ? opt.getOptionLabel().trim() : "";
                        String choiceKey = extractMatchingChoiceKey(opt.getOptionText());
                        correctMatching.put(label, choiceKey);
                    }
                }
                default -> {
                    // SINGLE_CHOICE: covered by correctOptionIds
                }
            }
        }

        return QuestionAnswerKeyResponse.builder()
                .questionId(questionId)
                .correctOptionIds(correctOptionIds)
                .explanation(question.getExplanation())
                .correctTrueFalse(correctTrueFalse)
                .correctShortAnswer(correctShortAnswer)
                .correctMatching(correctMatching)
                .build();
    }

    private boolean isShortAnswerEqual(String userStr, String expectedStr) {
        if (userStr == null || expectedStr == null) return false;
        String u = userStr.trim();
        String e = expectedStr.trim();
        if (u.equalsIgnoreCase(e)) return true;
        try {
            double d1 = Double.parseDouble(u);
            double d2 = Double.parseDouble(e);
            return Math.abs(d1 - d2) < 0.0001;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private Long parseLongValue(Object obj) {
        if (obj instanceof Number n) return n.longValue();
        return Long.parseLong(String.valueOf(obj).trim());
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
                .rawScore(session.getRawScore())
                .maxRawScore(session.getMaxRawScore())
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


    private QuestionGroupResponse toQuestionGroupResponse(QuestionGroup g) {
        if (g == null) return null;
        return QuestionGroupResponse.builder()
                .id(g.getId())
                .code(g.getCode())
                .subjectId(g.getSubjectId())
                .title(g.getTitle())
                .passageText(g.getPassageText())
                .passageHtml(g.getPassageHtml())
                .imageUrl(g.getImageUrl())
                .audioUrl(g.getAudioUrl())
                .audioDurationSec(g.getAudioDurationSec())
                .maxPlays(g.getMaxPlays() != null ? g.getMaxPlays() : 2)
                .build();
    }
}
