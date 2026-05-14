package com.example.v_sat_compass.ui.admin.exam;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.v_sat_compass.data.model.admin.AdminExamAddQuestionRequest;
import com.example.v_sat_compass.data.model.admin.AdminExamCreateRequest;
import com.example.v_sat_compass.data.model.admin.AdminExamReorderQuestionsRequest;
import com.example.v_sat_compass.data.model.admin.AdminExamResponse;
import com.example.v_sat_compass.data.model.admin.AdminExamSummaryResponse;
import com.example.v_sat_compass.data.model.admin.AdminExamUpdateRequest;
import com.example.v_sat_compass.data.model.admin.PageResponse;
import com.example.v_sat_compass.data.repository.AdminExamRepository;
import com.example.v_sat_compass.data.repository.Resource;

public class AdminExamViewModel extends ViewModel {

    private final AdminExamRepository repository;

    private final MutableLiveData<Resource<PageResponse<AdminExamSummaryResponse>>> listState =
            new MutableLiveData<>();
    private final MutableLiveData<Resource<AdminExamResponse>> detailState = new MutableLiveData<>();
    private final MutableLiveData<Resource<AdminExamResponse>> createState = new MutableLiveData<>();
    private final MutableLiveData<Resource<AdminExamResponse>> updateState = new MutableLiveData<>();
    private final MutableLiveData<Resource<AdminExamResponse>> addQuestionState =
            new MutableLiveData<>();
    private final MutableLiveData<Resource<AdminExamResponse>> reorderState = new MutableLiveData<>();

    public AdminExamViewModel() {
        this(new AdminExamRepository());
    }

    public AdminExamViewModel(AdminExamRepository repository) {
        this.repository = repository;
    }

    public LiveData<Resource<PageResponse<AdminExamSummaryResponse>>> getListState() {
        return listState;
    }

    public LiveData<Resource<AdminExamResponse>> getDetailState() {
        return detailState;
    }

    public LiveData<Resource<AdminExamResponse>> getCreateState() {
        return createState;
    }

    public LiveData<Resource<AdminExamResponse>> getUpdateState() {
        return updateState;
    }

    public LiveData<Resource<AdminExamResponse>> getAddQuestionState() {
        return addQuestionState;
    }

    public LiveData<Resource<AdminExamResponse>> getReorderState() {
        return reorderState;
    }

    public void loadExams(String status, Long subjectId, int page, int size) {
        listState.setValue(Resource.loading());
        repository.listExams(status, subjectId, page, size,
                new AdminExamRepository.RepositoryCallback<PageResponse<AdminExamSummaryResponse>>() {
                    @Override
                    public void onSuccess(PageResponse<AdminExamSummaryResponse> data) {
                        listState.setValue(Resource.success(data));
                    }

                    @Override
                    public void onError(AdminExamRepository.AdminExamError error) {
                        listState.setValue(Resource.error(message(error)));
                    }
                });
    }

    public void loadExam(Long id) {
        detailState.setValue(Resource.loading());
        repository.getExam(id, new AdminExamResponseCallback(detailState));
    }

    public void createExam(AdminExamCreateRequest request) {
        createState.setValue(Resource.loading());
        repository.createExam(request, new AdminExamResponseCallback(createState));
    }

    public void updateExam(Long id, AdminExamUpdateRequest request) {
        updateState.setValue(Resource.loading());
        repository.updateExam(id, request, new AdminExamResponseCallback(updateState));
    }

    public void addQuestion(Long examId, AdminExamAddQuestionRequest request) {
        addQuestionState.setValue(Resource.loading());
        repository.addQuestion(examId, request, new AdminExamResponseCallback(addQuestionState));
    }

    public void reorderQuestions(Long examId, AdminExamReorderQuestionsRequest request) {
        reorderState.setValue(Resource.loading());
        repository.reorderQuestions(examId, request, new AdminExamResponseCallback(reorderState));
    }

    private static String message(AdminExamRepository.AdminExamError error) {
        if (error == null) {
            return "Unknown error";
        }
        if (error.getMessage() != null) {
            return error.getMessage();
        }
        return error.getType().name();
    }

    private static class AdminExamResponseCallback
            implements AdminExamRepository.RepositoryCallback<AdminExamResponse> {

        private final MutableLiveData<Resource<AdminExamResponse>> state;

        AdminExamResponseCallback(MutableLiveData<Resource<AdminExamResponse>> state) {
            this.state = state;
        }

        @Override
        public void onSuccess(AdminExamResponse data) {
            state.setValue(Resource.success(data));
        }

        @Override
        public void onError(AdminExamRepository.AdminExamError error) {
            state.setValue(Resource.error(message(error)));
        }
    }
}
