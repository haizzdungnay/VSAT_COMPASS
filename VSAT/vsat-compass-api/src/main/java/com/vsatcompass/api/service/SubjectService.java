package com.vsatcompass.api.service;

import com.vsatcompass.api.dto.response.SubjectResponse;
import com.vsatcompass.api.dto.response.TopicResponse;

import java.util.List;

public interface SubjectService {

    List<SubjectResponse> listActiveSubjects();

    List<TopicResponse> listActiveTopicsBySubjectId(Long subjectId);
}
