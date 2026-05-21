package com.example.v_sat_compass.ui.admin.questions;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.v_sat_compass.data.model.admin.PageResponse;
import com.example.v_sat_compass.data.model.enums.QuestionStatus;
import com.example.v_sat_compass.data.model.question.QuestionListItemResponse;
import com.example.v_sat_compass.data.model.question.QuestionResponse;
import com.example.v_sat_compass.data.repository.AdminQuestionRepository;
import com.example.v_sat_compass.data.repository.Resource;

public class AdminReviewViewModel extends ViewModel {

    static final int DEFAULT_PAGE_SIZE = 20;
    static final int MAX_COMMENT_LENGTH = 2000;
    static final String ERROR_COMMENT_REQUIRED = "Comment is required";
    static final String ERROR_COMMENT_TOO_LONG = "Comment must be 2000 characters or fewer";

    private final AdminQuestionRepository repository;

    private final MutableLiveData<Resource<PageResponse<QuestionListItemResponse>>> queueState =
            new MutableLiveData<>();
    private final MutableLiveData<Resource<QuestionResponse>> detailState =
            new MutableLiveData<>();
    private final MutableLiveData<Resource<QuestionResponse>> actionState =
            new MutableLiveData<>();

    public AdminReviewViewModel() {
        this(new AdminQuestionRepository());
    }

    public AdminReviewViewModel(AdminQuestionRepository repository) {
        this.repository = repository;
    }

    public LiveData<Resource<PageResponse<QuestionListItemResponse>>> getQueueState() {
        return queueState;
    }

    public LiveData<Resource<QuestionResponse>> getDetailState() {
        return detailState;
    }

    public LiveData<Resource<QuestionResponse>> getActionState() {
        return actionState;
    }

    public void loadQueue(QuestionStatus status, int page) {
        queueState.setValue(Resource.loading());
        repository.getReviewQueue(
                status,
                page,
                DEFAULT_PAGE_SIZE,
                new AdminQuestionRepository.RepositoryCallback<PageResponse<QuestionListItemResponse>>() {
                    @Override
                    public void onSuccess(PageResponse<QuestionListItemResponse> data) {
                        queueState.setValue(Resource.success(data));
                    }

                    @Override
                    public void onError(AdminQuestionRepository.AdminQuestionError error) {
                        queueState.setValue(Resource.error(message(error)));
                    }
                }
        );
    }

    public void loadDetail(Long id) {
        detailState.setValue(Resource.loading());
        repository.getQuestionDetail(id, new QuestionResponseCallback(detailState));
    }

    public void approve(Long id, @Nullable String comment) {
        String normalized = optionalCommentOrError(comment);
        if (normalized == null && comment != null && comment.length() > MAX_COMMENT_LENGTH) {
            return;
        }
        actionState.setValue(Resource.loading());
        repository.approve(id, normalized, new QuestionResponseCallback(actionState));
    }

    public void requestRevision(Long id, String comment) {
        String normalized = requiredCommentOrError(comment);
        if (normalized == null) {
            return;
        }
        actionState.setValue(Resource.loading());
        repository.requestRevision(id, normalized, new QuestionResponseCallback(actionState));
    }

    public void reject(Long id, String comment) {
        String normalized = requiredCommentOrError(comment);
        if (normalized == null) {
            return;
        }
        actionState.setValue(Resource.loading());
        repository.reject(id, normalized, new QuestionResponseCallback(actionState));
    }

    private String optionalCommentOrError(@Nullable String comment) {
        if (comment == null) {
            return null;
        }
        if (comment.length() > MAX_COMMENT_LENGTH) {
            actionState.setValue(Resource.error(ERROR_COMMENT_TOO_LONG));
            return null;
        }
        String trimmed = comment.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String requiredCommentOrError(String comment) {
        if (comment == null || comment.trim().isEmpty()) {
            actionState.setValue(Resource.error(ERROR_COMMENT_REQUIRED));
            return null;
        }
        if (comment.length() > MAX_COMMENT_LENGTH) {
            actionState.setValue(Resource.error(ERROR_COMMENT_TOO_LONG));
            return null;
        }
        return comment.trim();
    }

    private static String message(AdminQuestionRepository.AdminQuestionError error) {
        if (error == null) {
            return "Unknown error";
        }
        if (error.getMessage() != null) {
            return error.getMessage();
        }
        return error.getType().name();
    }

    private static class QuestionResponseCallback
            implements AdminQuestionRepository.RepositoryCallback<QuestionResponse> {

        private final MutableLiveData<Resource<QuestionResponse>> state;

        QuestionResponseCallback(MutableLiveData<Resource<QuestionResponse>> state) {
            this.state = state;
        }

        @Override
        public void onSuccess(QuestionResponse data) {
            state.setValue(Resource.success(data));
        }

        @Override
        public void onError(AdminQuestionRepository.AdminQuestionError error) {
            state.setValue(Resource.error(message(error)));
        }
    }
}
