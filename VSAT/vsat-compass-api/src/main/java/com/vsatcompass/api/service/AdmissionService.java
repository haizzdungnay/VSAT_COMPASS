package com.vsatcompass.api.service;

import com.vsatcompass.api.dto.request.AdmissionPredictionRequest;
import com.vsatcompass.api.dto.response.AdmissionPredictionResponse;
import com.vsatcompass.api.dto.response.UniversityResponse;

import java.util.List;

public interface AdmissionService {

    List<UniversityResponse> getUniversities();

    List<String> getSubjectCombos();

    AdmissionPredictionResponse predictAdmission(AdmissionPredictionRequest request);
}
