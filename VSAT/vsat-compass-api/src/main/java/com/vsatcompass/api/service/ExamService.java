package com.vsatcompass.api.service;

import com.vsatcompass.api.dto.response.ExamDetailResponse;
import com.vsatcompass.api.dto.response.ExamSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ExamService {

    Page<ExamSummaryResponse> listPublishedFreeExams(Pageable pageable);

    ExamDetailResponse getPublicExam(Long id);
}
