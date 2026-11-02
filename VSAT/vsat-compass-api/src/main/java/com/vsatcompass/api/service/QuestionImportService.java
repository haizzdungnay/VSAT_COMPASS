package com.vsatcompass.api.service;

import com.vsatcompass.api.dto.response.QuestionImportResultResponse;
import org.springframework.web.multipart.MultipartFile;

public interface QuestionImportService {

    byte[] generateTemplate();

    QuestionImportResultResponse importQuestions(MultipartFile file, boolean dryRun, Long currentUserId);
}
