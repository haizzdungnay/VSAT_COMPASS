package com.vsatcompass.api.service;

import com.vsatcompass.api.dto.request.AdminExamWindowRequest;
import com.vsatcompass.api.dto.response.AdminExamWindowResponse;

import java.util.List;

public interface AdminExamWindowService {

    List<AdminExamWindowResponse> listWindows(Long examId, String status);

    AdminExamWindowResponse getWindow(Long id);

    AdminExamWindowResponse createWindow(AdminExamWindowRequest request);

    AdminExamWindowResponse updateWindow(Long id, AdminExamWindowRequest request);

    AdminExamWindowResponse updateStatus(Long id, String targetStatus);

    void deleteWindow(Long id);
}
