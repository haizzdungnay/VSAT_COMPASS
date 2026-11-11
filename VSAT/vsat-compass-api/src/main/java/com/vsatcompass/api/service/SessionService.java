package com.vsatcompass.api.service;

import com.vsatcompass.api.dto.request.SessionRequest;
import com.vsatcompass.api.dto.response.AudioPlayResponse;
import com.vsatcompass.api.dto.response.SessionAnswerKeysResponse;
import com.vsatcompass.api.dto.response.SessionAnswerResponse;
import com.vsatcompass.api.dto.response.SessionQuestionContentResponse;
import com.vsatcompass.api.dto.response.SessionResponse;

import java.util.List;
import java.util.Map;

public interface SessionService {

    SessionResponse.SessionInfo startSession(Long userId, SessionRequest.StartSession request);

    /**
     * Persist a single answer for an in-progress session. Idempotent by (session, question):
     * the previous row is replaced instead of duplicated.
     */
    void submitAnswer(Long userId, Long sessionId, SessionRequest.SubmitAnswer request);

    /**
     * Server-side scoring: aggregate the stored answers for this session, mark it SUBMITTED
     * and return the resulting SessionInfo. Used when the app does NOT compute the score itself.
     */
    SessionResponse.SessionInfo serverSubmit(Long userId, Long sessionId);

    SessionResponse.SessionInfo clientSubmit(Long userId, Long sessionId, SessionRequest.ClientSubmit request);

    SessionQuestionContentResponse getQuestionForSession(
            Long sessionId,
            Long questionId,
            Long currentUserId
    );

    SessionAnswerKeysResponse getAnswerKeysForSession(
            Long sessionId,
            Long currentUserId
    );

    SessionResponse.ViolationResult recordViolation(
            Long userId,
            Long sessionId,
            SessionRequest.RecordViolation request
    );

    List<SessionAnswerResponse> getSessionAnswers(Long sessionId, Long userId);

    Map<String, Integer> getAudioPlayCounts(Long sessionId, Long userId);

    AudioPlayResponse recordAudioPlay(Long sessionId, Long userId, SessionRequest.RecordAudioPlay request);
}
