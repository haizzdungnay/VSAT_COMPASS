package com.vsatcompass.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionImportResultResponse {
    private int totalRows;
    private int validCount;
    private int errorCount;
    private int importedQuestions;
    private int createdGroups;
    private boolean dryRun;
    private List<QuestionImportItemDto> items;
    private List<String> errors;
}
