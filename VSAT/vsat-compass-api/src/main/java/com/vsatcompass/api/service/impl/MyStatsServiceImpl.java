package com.vsatcompass.api.service.impl;

import com.vsatcompass.api.dto.response.TopicStatsResponse;
import com.vsatcompass.api.repository.SessionAnswerRepository;
import com.vsatcompass.api.repository.projection.TopicStatsProjection;
import com.vsatcompass.api.service.MyStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MyStatsServiceImpl implements MyStatsService {

    private final SessionAnswerRepository sessionAnswerRepository;

    @Override
    @Transactional(readOnly = true)
    public List<TopicStatsResponse> getTopicStats(Long userId) {
        return sessionAnswerRepository.aggregateTopicStatsByUserId(userId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TopicStatsResponse> getWeakTopics(Long userId, int limit) {
        int boundedLimit = Math.max(1, Math.min(limit, 20));
        return getTopicStats(userId).stream()
                .filter(topic -> topic.getTotal() > 0)
                .sorted(Comparator
                        .comparingInt(TopicStatsResponse::getPercentage)
                        .thenComparing(TopicStatsResponse::getTopicName,
                                Comparator.nullsLast(String::compareToIgnoreCase)))
                .limit(boundedLimit)
                .toList();
    }

    private TopicStatsResponse toResponse(TopicStatsProjection projection) {
        int total = projection.getTotalAttempts() != null
                ? projection.getTotalAttempts().intValue() : 0;
        int correct = projection.getCorrectCount() != null
                ? projection.getCorrectCount().intValue() : 0;
        int percentage = total > 0 ? (int) Math.round(correct * 100.0 / total) : 0;
        return TopicStatsResponse.builder()
                .topicId(projection.getTopicId())
                .topicName(projection.getTopicName())
                .correct(correct)
                .total(total)
                .percentage(percentage)
                .build();
    }
}
