package com.vsatcompass.api.service.impl;

import com.vsatcompass.api.dto.response.SubjectResponse;
import com.vsatcompass.api.dto.response.TopicResponse;
import com.vsatcompass.api.entity.Subject;
import com.vsatcompass.api.entity.Topic;
import com.vsatcompass.api.exception.AppException;
import com.vsatcompass.api.repository.SubjectRepository;
import com.vsatcompass.api.repository.TopicRepository;
import com.vsatcompass.api.service.SubjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SubjectServiceImpl implements SubjectService {

    private final SubjectRepository subjectRepository;
    private final TopicRepository topicRepository;

    @Override
    public List<SubjectResponse> listActiveSubjects() {
        List<Subject> subjects = subjectRepository.findByIsActiveTrueOrderByDisplayOrderAscIdAsc();
        return subjects.stream().map(this::toSubjectResponse).toList();
    }

    @Override
    public List<TopicResponse> listActiveTopicsBySubjectId(Long subjectId) {
        if (!subjectRepository.existsById(subjectId)) {
            throw AppException.notFound("Subject", subjectId);
        }
        List<Topic> topics = topicRepository.findBySubjectIdAndIsActiveTrueOrderByDisplayOrderAscIdAsc(subjectId);
        return topics.stream().map(this::toTopicResponse).toList();
    }

    private SubjectResponse toSubjectResponse(Subject s) {
        return SubjectResponse.builder()
                .id(s.getId())
                .code(s.getCode())
                .name(s.getName())
                .description(s.getDescription())
                .iconUrl(s.getIconUrl())
                .displayOrder(s.getDisplayOrder())
                .build();
    }

    private TopicResponse toTopicResponse(Topic t) {
        return TopicResponse.builder()
                .id(t.getId())
                .subjectId(t.getSubjectId())
                .code(t.getCode())
                .name(t.getName())
                .description(t.getDescription())
                .displayOrder(t.getDisplayOrder())
                .build();
    }
}
