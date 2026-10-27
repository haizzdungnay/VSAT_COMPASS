package com.vsatcompass.api.service;

import com.vsatcompass.api.dto.request.PracticeGradeRequest;
import com.vsatcompass.api.dto.response.PracticeGradeResponse;
import com.vsatcompass.api.dto.response.SessionQuestionContentResponse;
import com.vsatcompass.api.entity.enums.Difficulty;

import java.util.List;

public interface PracticeService {

    List<SessionQuestionContentResponse> getQuestions(Long subtopicId, Long topicId, Difficulty difficulty, int limit);

    PracticeGradeResponse grade(PracticeGradeRequest request);
}
