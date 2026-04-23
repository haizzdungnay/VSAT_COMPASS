package com.vsatcompass.api.service;

import com.vsatcompass.api.dto.request.SessionRequest;
import com.vsatcompass.api.dto.response.SessionResponse;

public interface SessionService {

    SessionResponse.SessionInfo startSession(Long userId, SessionRequest.StartSession request);

    SessionResponse.SessionInfo clientSubmit(Long userId, Long sessionId, SessionRequest.ClientSubmit request);
}
