package com.vsatcompass.api.service;

import com.vsatcompass.api.dto.request.SessionRequest;
import com.vsatcompass.api.dto.response.SessionAnswerKeysResponse;
import com.vsatcompass.api.dto.response.SessionQuestionContentResponse;
import com.vsatcompass.api.dto.response.SessionResponse;

public interface SessionService {

    SessionResponse.SessionInfo startSession(Long userId, SessionRequest.StartSession request);

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
}
