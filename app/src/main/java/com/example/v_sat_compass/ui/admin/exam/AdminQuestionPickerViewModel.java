package com.example.v_sat_compass.ui.admin.exam;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.v_sat_compass.data.model.admin.PageResponse;
import com.example.v_sat_compass.data.model.admin.QuestionPickerItemResponse;
import com.example.v_sat_compass.data.model.enums.QuestionStatus;
import com.example.v_sat_compass.data.model.enums.QuestionType;
import com.example.v_sat_compass.data.repository.AdminQuestionRepository;
import com.example.v_sat_compass.data.repository.Resource;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class AdminQuestionPickerViewModel extends ViewModel {

    static final int DEFAULT_PAGE_SIZE = 20;

    private final AdminQuestionRepository repository;
    private final MutableLiveData<Resource<PageResponse<QuestionPickerItemResponse>>> pickerState =
            new MutableLiveData<>();
    private final Set<Long> selectedIds = new LinkedHashSet<>();

    public AdminQuestionPickerViewModel() {
        this(new AdminQuestionRepository());
    }

    public AdminQuestionPickerViewModel(AdminQuestionRepository repository) {
        this.repository = repository;
    }

    public LiveData<Resource<PageResponse<QuestionPickerItemResponse>>> getPickerState() {
        return pickerState;
    }

    public Set<Long> getSelectedIds() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(selectedIds));
    }

    public void loadPicker(
            QuestionStatus status,
            Long subjectId,
            Long topicId,
            QuestionType questionType,
            String q,
            int page
    ) {
        loadPicker(status, subjectId, topicId, questionType, q, page, DEFAULT_PAGE_SIZE);
    }

    public void loadPicker(
            QuestionStatus status,
            Long subjectId,
            Long topicId,
            QuestionType questionType,
            String q,
            int page,
            int size
    ) {
        pickerState.setValue(Resource.loading());
        QuestionStatus effectiveStatus = status != null ? status : QuestionStatus.APPROVED;
        repository.getPickerQueue(
                effectiveStatus,
                subjectId,
                topicId,
                questionType,
                normalizeQuery(q),
                page,
                size,
                new AdminQuestionRepository.RepositoryCallback<PageResponse<QuestionPickerItemResponse>>() {
                    @Override
                    public void onSuccess(PageResponse<QuestionPickerItemResponse> data) {
                        pickerState.setValue(Resource.success(data));
                    }

                    @Override
                    public void onError(AdminQuestionRepository.AdminQuestionError error) {
                        pickerState.setValue(Resource.error(message(error)));
                    }
                }
        );
    }

    public void toggleSelection(Long id) {
        if (id == null) {
            return;
        }
        if (selectedIds.contains(id)) {
            selectedIds.remove(id);
        } else {
            selectedIds.add(id);
        }
    }

    public int getSelectedCount() {
        return selectedIds.size();
    }

    public void clearSelection() {
        selectedIds.clear();
    }

    private static String normalizeQuery(String q) {
        if (q == null || q.trim().isEmpty()) {
            return null;
        }
        return q.trim();
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
}
