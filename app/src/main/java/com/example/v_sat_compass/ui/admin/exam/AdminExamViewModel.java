package com.example.v_sat_compass.ui.admin.exam;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.v_sat_compass.data.model.SubjectResponse;
import com.example.v_sat_compass.data.model.admin.AdminExamAddQuestionRequest;
import com.example.v_sat_compass.data.model.admin.AdminExamCreateRequest;
import com.example.v_sat_compass.data.model.admin.AdminExamReorderQuestionsRequest;
import com.example.v_sat_compass.data.model.admin.AdminExamResponse;
import com.example.v_sat_compass.data.model.admin.AdminExamSummaryResponse;
import com.example.v_sat_compass.data.model.admin.AdminExamUpdateRequest;
import com.example.v_sat_compass.data.model.admin.PageResponse;
import com.example.v_sat_compass.data.repository.AdminExamRepository;
import com.example.v_sat_compass.data.repository.Resource;
import com.example.v_sat_compass.data.repository.SubjectRepository;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AdminExamViewModel extends ViewModel {

    private final AdminExamRepository repository;
    private final SubjectRepository subjectRepository;

    private final MutableLiveData<Resource<PageResponse<AdminExamSummaryResponse>>> listState =
            new MutableLiveData<>();
    private final MutableLiveData<Resource<AdminExamResponse>> detailState = new MutableLiveData<>();
    private final MutableLiveData<Resource<AdminExamResponse>> createState = new MutableLiveData<>();
    private final MutableLiveData<Resource<AdminExamResponse>> updateState = new MutableLiveData<>();
    private final MutableLiveData<Resource<AdminExamResponse>> addQuestionState =
            new MutableLiveData<>();
    private final MutableLiveData<Resource<AdminExamResponse>> reorderState = new MutableLiveData<>();
    private final MutableLiveData<Resource<List<SubjectResponse>>> subjectListState =
            new MutableLiveData<>();
    private final MutableLiveData<Resource<AdminExamResponse>> submitReviewState =
            new MutableLiveData<>();
    private final MutableLiveData<Resource<AdminExamResponse>> examDetailState =
            new MutableLiveData<>();
    private final MutableLiveData<Boolean> editModeState = new MutableLiveData<>(false);
    private final MutableLiveData<Resource<AdminExamResponse>> actionResultState =
            new MutableLiveData<>();

    public AdminExamViewModel() {
        this(new AdminExamRepository(), new SubjectRepository());
    }

    public AdminExamViewModel(AdminExamRepository repository) {
        this(repository, new SubjectRepository());
    }

    public AdminExamViewModel(AdminExamRepository repository, SubjectRepository subjectRepository) {
        this.repository = repository;
        this.subjectRepository = subjectRepository;
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

    public LiveData<Resource<List<SubjectResponse>>> getSubjectListState() {
        return subjectListState;
    }

    public LiveData<Resource<AdminExamResponse>> getSubmitReviewState() {
        return submitReviewState;
    }

    public LiveData<Resource<AdminExamResponse>> getExamDetailState() {
        return examDetailState;
    }

    public LiveData<Boolean> getEditModeState() {
        return editModeState;
    }

    public LiveData<Resource<AdminExamResponse>> getActionResultState() {
        return actionResultState;
    }

    public void loadDetail(Long id) {
        examDetailState.setValue(Resource.loading());
        repository.getExam(id, new AdminExamRepository.RepositoryCallback<AdminExamResponse>() {
            @Override
            public void onSuccess(AdminExamResponse data) {
                examDetailState.setValue(Resource.success(data));
            }

            @Override
            public void onError(AdminExamRepository.AdminExamError error) {
                examDetailState.setValue(Resource.error(message(error)));
            }
        });
    }

    public void enterEditMode() {
        editModeState.setValue(true);
    }

    public void cancelEditMode() {
        editModeState.setValue(false);
    }

    public void saveEdit(Long id, AdminExamUpdateRequest request) {
        examDetailState.setValue(Resource.loading());
        repository.updateExam(id, request, new AdminExamRepository.RepositoryCallback<AdminExamResponse>() {
            @Override
            public void onSuccess(AdminExamResponse data) {
                editModeState.setValue(false);
                examDetailState.setValue(Resource.success(data));
            }

            @Override
            public void onError(AdminExamRepository.AdminExamError error) {
                examDetailState.setValue(Resource.error(message(error)));
            }
        });
    }

    public void submitForReview(Long examId) {
        actionResultState.setValue(Resource.loading());
        repository.submitForReview(examId, new AdminExamRepository.RepositoryCallback<AdminExamResponse>() {
            @Override
            public void onSuccess(AdminExamResponse data) {
                actionResultState.setValue(Resource.success(data));
                examDetailState.setValue(Resource.success(data));
            }

            @Override
            public void onError(AdminExamRepository.AdminExamError error) {
                actionResultState.setValue(Resource.error(message(error)));
            }
        });
    }

    public void discardDraft(Long examId) {
        actionResultState.setValue(Resource.loading());
        repository.discardDraftExam(examId, new AdminExamRepository.RepositoryCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                actionResultState.setValue(Resource.success(null));
            }

            @Override
            public void onError(AdminExamRepository.AdminExamError error) {
                actionResultState.setValue(Resource.error(message(error)));
            }
        });
    }

    public void publishExam(Long examId) {
        actionResultState.setValue(Resource.loading());
        repository.publish(examId, new AdminExamRepository.RepositoryCallback<AdminExamResponse>() {
            @Override
            public void onSuccess(AdminExamResponse data) {
                actionResultState.setValue(Resource.success(data));
                examDetailState.setValue(Resource.success(data));
            }

            @Override
            public void onError(AdminExamRepository.AdminExamError error) {
                actionResultState.setValue(Resource.error(message(error)));
            }
        });
    }

    public void rejectReview(Long examId) {
        actionResultState.setValue(Resource.loading());
        repository.rejectReview(examId, new AdminExamRepository.RepositoryCallback<AdminExamResponse>() {
            @Override
            public void onSuccess(AdminExamResponse data) {
                actionResultState.setValue(Resource.success(data));
                examDetailState.setValue(Resource.success(data));
            }

            @Override
            public void onError(AdminExamRepository.AdminExamError error) {
                actionResultState.setValue(Resource.error(message(error)));
            }
        });
    }

    public void returnToDraft(Long examId) {
        actionResultState.setValue(Resource.loading());
        repository.returnToDraft(examId, new AdminExamRepository.RepositoryCallback<AdminExamResponse>() {
            @Override
            public void onSuccess(AdminExamResponse data) {
                actionResultState.setValue(Resource.success(data));
                examDetailState.setValue(Resource.success(data));
            }

            @Override
            public void onError(AdminExamRepository.AdminExamError error) {
                actionResultState.setValue(Resource.error(message(error)));
            }
        });
    }

    public void hideExam(Long examId) {
        actionResultState.setValue(Resource.loading());
        repository.hide(examId, new AdminExamRepository.RepositoryCallback<AdminExamResponse>() {
            @Override
            public void onSuccess(AdminExamResponse data) {
                actionResultState.setValue(Resource.success(data));
                examDetailState.setValue(Resource.success(data));
            }

            @Override
            public void onError(AdminExamRepository.AdminExamError error) {
                actionResultState.setValue(Resource.error(message(error)));
            }
        });
    }

    public void archiveExam(Long examId) {
        actionResultState.setValue(Resource.loading());
        repository.archive(examId, new AdminExamRepository.RepositoryCallback<AdminExamResponse>() {
            @Override
            public void onSuccess(AdminExamResponse data) {
                actionResultState.setValue(Resource.success(data));
                examDetailState.setValue(Resource.success(data));
            }

            @Override
            public void onError(AdminExamRepository.AdminExamError error) {
                actionResultState.setValue(Resource.error(message(error)));
            }
        });
    }

    public static List<String> availableActionsForStatus(String status) {
        if (status == null) {
            return Collections.emptyList();
        }
        switch (status) {
            case "DRAFT":
                return Arrays.asList("EDIT", "SUBMIT_FOR_REVIEW", "DISCARD");
            case "PENDING_REVIEW":
                return Arrays.asList("PUBLISH", "REJECT", "RETURN_TO_DRAFT");
            case "PUBLISHED":
                return Arrays.asList("HIDE", "ARCHIVE");
            case "HIDDEN":
                return Arrays.asList("ARCHIVE");
            case "ARCHIVED":
                return Collections.emptyList();
            default:
                return Collections.emptyList();
        }
    }

    public void submitExam(Long examId) {
        submitReviewState.setValue(Resource.loading());
        repository.submitForReview(examId, new AdminExamResponseCallback(submitReviewState));
    }

    public void loadSubjects() {
        subjectListState.setValue(Resource.loading());
        subjectRepository.getSubjects(new SubjectRepository.SubjectCallback() {
            @Override
            public void onSuccess(List<SubjectResponse> subjects) {
                subjectListState.setValue(Resource.success(subjects));
            }

            @Override
            public void onError(SubjectRepository.SubjectError error) {
                String msg = error.getMessage() != null ? error.getMessage() : error.getType().name();
                subjectListState.setValue(Resource.error(msg));
            }
        });
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
