package com.vsatcompass.api.service.impl;

import com.vsatcompass.api.dto.response.SubtopicResponse;
import com.vsatcompass.api.entity.Subtopic;
import com.vsatcompass.api.entity.Topic;
import com.vsatcompass.api.exception.AppException;
import com.vsatcompass.api.repository.SubjectRepository;
import com.vsatcompass.api.repository.SubtopicRepository;
import com.vsatcompass.api.repository.TopicRepository;
import com.vsatcompass.api.service.SubtopicService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubtopicServiceImpl implements SubtopicService {

    private final SubjectRepository subjectRepository;
    private final TopicRepository topicRepository;
    private final SubtopicRepository subtopicRepository;

    @Override
    public List<SubtopicResponse> listActiveSubtopicsBySubjectAndTopic(Long subjectId, Long topicId) {
        if (!subjectRepository.existsById(subjectId)) {
            throw AppException.notFound("Subject", subjectId);
        }
        Topic topic = topicRepository.findById(topicId)
                .orElseThrow(() -> AppException.notFound("Topic", topicId));

        if (!topic.getSubjectId().equals(subjectId)) {
            throw AppException.validationFailed(
                    "Topic " + topicId + " does not belong to subject " + subjectId);
        }

        List<Subtopic> subtopics = subtopicRepository
                .findByTopicIdAndIsActiveTrueOrderByDisplayOrderAscIdAsc(topicId);
        return subtopics.stream().map(this::toSubtopicResponse).toList();
    }

    private SubtopicResponse toSubtopicResponse(Subtopic s) {
        return SubtopicResponse.builder()
                .id(s.getId())
                .topicId(s.getTopicId())
                .code(s.getCode())
                .name(s.getName())
                .description(s.getDescription())
                .displayOrder(s.getDisplayOrder())
                .build();
    }
}
