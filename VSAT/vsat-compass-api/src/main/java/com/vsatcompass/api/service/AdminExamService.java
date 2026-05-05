package com.vsatcompass.api.service;

import com.vsatcompass.api.dto.request.AdminExamCreateRequest;
import com.vsatcompass.api.dto.request.AdminExamUpdateRequest;
import com.vsatcompass.api.dto.response.AdminExamResponse;
import com.vsatcompass.api.dto.response.AdminExamSummaryResponse;
import com.vsatcompass.api.entity.enums.ExamStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AdminExamService {

    Page<AdminExamSummaryResponse> listAdminExams(
            ExamStatus status,
            Long subjectId,
            Pageable pageable);

    AdminExamResponse getAdminExam(Long id);

    AdminExamResponse createExam(Long currentUserId, AdminExamCreateRequest request);

    AdminExamResponse updateExam(Long id, AdminExamUpdateRequest request);
}
