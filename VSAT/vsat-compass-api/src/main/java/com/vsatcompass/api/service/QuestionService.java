package com.vsatcompass.api.service;

import com.vsatcompass.api.dto.request.CreateQuestionRequest;
import com.vsatcompass.api.dto.request.ReviewActionRequest;
import com.vsatcompass.api.dto.request.UpdateQuestionRequest;
import com.vsatcompass.api.dto.response.QuestionListItemResponse;
import com.vsatcompass.api.dto.response.QuestionResponse;
import com.vsatcompass.api.entity.enums.QuestionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface QuestionService {

    QuestionResponse create(Long currentUserId, CreateQuestionRequest req);

    QuestionResponse getById(Long currentUserId, String currentUserRole, Long questionId);

    QuestionResponse update(Long currentUserId, String currentUserRole, Long questionId, UpdateQuestionRequest req);

    Page<QuestionListItemResponse> listByCreator(Long currentUserId, QuestionStatus statusFilter, Pageable pageable);

    Page<QuestionListItemResponse> listByStatus(QuestionStatus status, Pageable pageable);

    QuestionResponse submitForReview(Long currentUserId, Long questionId);

    QuestionResponse approve(Long currentUserId, Long questionId, ReviewActionRequest req);

    QuestionResponse requestRevision(Long currentUserId, Long questionId, ReviewActionRequest req);

    QuestionResponse reject(Long currentUserId, Long questionId, ReviewActionRequest req);
}
