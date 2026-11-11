package com.vsatcompass.api.service;

import com.vsatcompass.api.dto.request.BlueprintUpsertRequest;
import com.vsatcompass.api.dto.response.BlueprintResponse;

import java.util.List;

public interface AdminBlueprintService {
    List<BlueprintResponse> listBlueprints(Long subjectId);
    BlueprintResponse getBlueprint(Long id);
    BlueprintResponse createBlueprint(BlueprintUpsertRequest request);
    BlueprintResponse updateBlueprint(Long id, BlueprintUpsertRequest request);
    void deleteBlueprint(Long id);
}
