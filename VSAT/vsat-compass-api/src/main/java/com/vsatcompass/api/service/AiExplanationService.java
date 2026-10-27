package com.vsatcompass.api.service;

import com.vsatcompass.api.dto.request.AiExplainRequest;
import com.vsatcompass.api.dto.response.AiExplainResponse;

public interface AiExplanationService {

    AiExplainResponse explain(Long userId, AiExplainRequest request);
}
