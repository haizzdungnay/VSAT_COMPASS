package com.vsatcompass.api.service;

import com.vsatcompass.api.dto.response.SubtopicResponse;

import java.util.List;

public interface SubtopicService {

    /**
     * Returns active subtopics for a topic, ordered by displayOrder then id.
     * Validates that subjectId and topicId both exist and that the topic belongs to the subject.
     * Throws AppException 404 on either missing, 400 VALIDATION_FAILED on subject/topic mismatch.
     */
    List<SubtopicResponse> listActiveSubtopicsBySubjectAndTopic(Long subjectId, Long topicId);
}
