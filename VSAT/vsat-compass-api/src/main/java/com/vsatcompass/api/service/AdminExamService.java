package com.vsatcompass.api.service;

import com.vsatcompass.api.dto.request.AdminExamCreateRequest;
import com.vsatcompass.api.dto.request.AdminExamUpdateRequest;
import com.vsatcompass.api.dto.response.AdminExamResponse;
import com.vsatcompass.api.dto.response.AdminExamSummaryResponse;
import com.vsatcompass.api.entity.enums.ExamStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface AdminExamService {

    Page<AdminExamSummaryResponse> listAdminExams(
            ExamStatus status,
            Long subjectId,
            Pageable pageable);

    AdminExamResponse getAdminExam(Long id);

    AdminExamResponse createExam(Long currentUserId, AdminExamCreateRequest request);

    AdminExamResponse updateExam(Long id, AdminExamUpdateRequest request);

    void discardDraftExam(Long examId);

    AdminExamResponse addQuestion(Long examId, Long questionId);

    AdminExamResponse removeQuestion(Long examId, Long questionId);

    AdminExamResponse reorderQuestions(Long examId, List<Long> questionIds);

    AdminExamResponse submitReview(Long examId);

    AdminExamResponse publish(Long currentUserId, Long examId);

    AdminExamResponse hide(Long examId);

    AdminExamResponse archive(Long examId);

    AdminExamResponse rejectReview(Long examId);

    AdminExamResponse returnToDraft(Long examId);
}
