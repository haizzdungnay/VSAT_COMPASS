package com.vsatcompass.api.service.impl;

import com.vsatcompass.api.dto.request.AiExplainRequest;
import com.vsatcompass.api.dto.response.AiExplainResponse;
import com.vsatcompass.api.entity.Question;
import com.vsatcompass.api.entity.QuestionOption;
import com.vsatcompass.api.entity.enums.QuestionStatus;
import com.vsatcompass.api.exception.AppException;
import com.vsatcompass.api.repository.QuestionOptionRepository;
import com.vsatcompass.api.repository.QuestionRepository;
import com.vsatcompass.api.service.AiExplanationService;
import com.vsatcompass.api.service.llm.LlmClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiExplanationServiceImpl implements AiExplanationService {

    private static final int MAX_REQUESTS_PER_MINUTE = 10;
    private static final List<QuestionStatus> VISIBLE_STATUSES = List.of(
            QuestionStatus.APPROVED,
            QuestionStatus.PUBLISHED
    );

    private final QuestionRepository questionRepository;
    private final QuestionOptionRepository questionOptionRepository;
    private final LlmClient llmClient;
    private final Map<String, String> cache = new ConcurrentHashMap<>();
    private final Map<Long, Deque<Instant>> requestTimesByUser = new ConcurrentHashMap<>();

    @Override
    @Transactional(readOnly = true)
    public AiExplainResponse explain(Long userId, AiExplainRequest request) {
        enforceRateLimit(userId);

        String cacheKey = buildCacheKey(request.getQuestionId(), request.getSelectedOptionId());
        String cached = cache.get(cacheKey);
        if (cached != null) {
            return AiExplainResponse.builder()
                    .questionId(request.getQuestionId())
                    .explanation(cached)
                    .cached(true)
                    .build();
        }

        Question question = questionRepository.findById(request.getQuestionId())
                .orElseThrow(() -> AppException.notFound("Question", request.getQuestionId()));
        if (!VISIBLE_STATUSES.contains(question.getStatus())) {
            throw AppException.notFound("Question", request.getQuestionId());
        }
        List<QuestionOption> options = questionOptionRepository
                .findByQuestionIdOrderByDisplayOrderAscIdAsc(question.getId());

        String prompt = buildPrompt(question, options, request.getSelectedOptionId());
        String explanation = llmClient.complete(prompt);
        cache.put(cacheKey, explanation);

        return AiExplainResponse.builder()
                .questionId(question.getId())
                .explanation(explanation)
                .cached(false)
                .build();
    }

    private static String buildCacheKey(Long questionId, Long selectedOptionId) {
        return questionId + ":" + (selectedOptionId != null ? selectedOptionId : "none");
    }

    private void enforceRateLimit(Long userId) {
        if (userId == null) return;
        Instant now = Instant.now();
        Deque<Instant> times = requestTimesByUser.computeIfAbsent(userId, ignored -> new ArrayDeque<>());
        synchronized (times) {
            while (!times.isEmpty() && Duration.between(times.peekFirst(), now).toSeconds() >= 60) {
                times.removeFirst();
            }
            if (times.size() >= MAX_REQUESTS_PER_MINUTE) {
                throw AppException.rateLimitExceeded();
            }
            times.addLast(now);
        }
    }

    private String buildPrompt(Question question, List<QuestionOption> options, Long selectedOptionId) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Hay giai thich loi giai cau hoi V-SAT sau bang tieng Viet.\n\n");
        prompt.append("Cau hoi:\n").append(question.getQuestionText()).append("\n\n");
        prompt.append("Cac lua chon:\n");
        for (QuestionOption option : options) {
            prompt.append(option.getOptionLabel()).append(". ").append(option.getOptionText()).append("\n");
        }
        prompt.append("\nDap an dung:\n");
        options.stream()
                .filter(option -> Boolean.TRUE.equals(option.getIsCorrect()))
                .findFirst()
                .ifPresent(option -> prompt.append(option.getOptionLabel()).append(". ").append(option.getOptionText()).append("\n"));
        if (selectedOptionId != null) {
            options.stream()
                    .filter(option -> Objects.equals(option.getId(), selectedOptionId))
                    .findFirst()
                    .ifPresent(option -> prompt.append("\nHoc sinh da chon: ").append(option.getOptionLabel()).append(". ").append(option.getOptionText()).append("\n"));
        }
        if (question.getExplanation() != null && !question.getExplanation().isBlank()) {
            prompt.append("\nGiai thich goc:\n").append(question.getExplanation()).append("\n");
        }
        return prompt.toString();
    }
}
