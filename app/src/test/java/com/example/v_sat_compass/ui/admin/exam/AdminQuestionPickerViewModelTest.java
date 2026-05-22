package com.example.v_sat_compass.ui.admin.exam;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.LiveData;

import com.example.v_sat_compass.data.model.admin.PageResponse;
import com.example.v_sat_compass.data.model.admin.QuestionPickerItemResponse;
import com.example.v_sat_compass.data.model.enums.Difficulty;
import com.example.v_sat_compass.data.model.enums.QuestionStatus;
import com.example.v_sat_compass.data.model.enums.QuestionType;
import com.example.v_sat_compass.data.repository.AdminQuestionRepository;
import com.example.v_sat_compass.data.repository.Resource;

import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AdminQuestionPickerViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Test
    public void loadPicker_emitsLoadingThenSuccess() {
        FakeRepository repository = new FakeRepository();
        AdminQuestionPickerViewModel viewModel = new AdminQuestionPickerViewModel(repository);
        List<Resource<PageResponse<QuestionPickerItemResponse>>> values =
                observe(viewModel.getPickerState());

        viewModel.loadPicker(null, 1L, 2L, QuestionType.SINGLE_CHOICE, " Q-1 ", 0);

        assertEquals(Resource.Status.LOADING, values.get(0).getStatus());
        assertEquals(Resource.Status.SUCCESS, values.get(1).getStatus());
        assertEquals(QuestionStatus.APPROVED, repository.lastStatus);
        assertEquals(Long.valueOf(1L), repository.lastSubjectId);
        assertEquals(Long.valueOf(2L), repository.lastTopicId);
        assertEquals(QuestionType.SINGLE_CHOICE, repository.lastQuestionType);
        assertEquals("Q-1", repository.lastQ);
        assertEquals(AdminQuestionPickerViewModel.DEFAULT_PAGE_SIZE, repository.lastSize);
        assertEquals("Q-1", values.get(1).getData().getContent().get(0).getQuestionCode());
    }

    @Test
    public void loadPicker_emitsLoadingThenError() {
        FakeRepository repository = new FakeRepository();
        repository.fail = true;
        AdminQuestionPickerViewModel viewModel = new AdminQuestionPickerViewModel(repository);
        List<Resource<PageResponse<QuestionPickerItemResponse>>> values =
                observe(viewModel.getPickerState());

        viewModel.loadPicker(QuestionStatus.PENDING_REVIEW, null, null, null, null, 0);

        assertEquals(Resource.Status.LOADING, values.get(0).getStatus());
        assertEquals(Resource.Status.ERROR, values.get(1).getStatus());
        assertEquals("Bad picker request", values.get(1).getMessage());
    }

    @Test
    public void toggleSelection_addsId() {
        AdminQuestionPickerViewModel viewModel =
                new AdminQuestionPickerViewModel(new FakeRepository());

        viewModel.toggleSelection(9L);

        assertTrue(viewModel.getSelectedIds().contains(9L));
    }

    @Test
    public void toggleSelection_removesExistingId() {
        AdminQuestionPickerViewModel viewModel =
                new AdminQuestionPickerViewModel(new FakeRepository());

        viewModel.toggleSelection(9L);
        viewModel.toggleSelection(9L);

        assertEquals(0, viewModel.getSelectedCount());
    }

    @Test
    public void getSelectedCount_returnsCurrentSelectionSize() {
        AdminQuestionPickerViewModel viewModel =
                new AdminQuestionPickerViewModel(new FakeRepository());

        viewModel.toggleSelection(1L);
        viewModel.toggleSelection(2L);
        viewModel.toggleSelection(null);

        assertEquals(2, viewModel.getSelectedCount());
    }

    @Test
    public void clearSelection_emptiesSelectedIds() {
        AdminQuestionPickerViewModel viewModel =
                new AdminQuestionPickerViewModel(new FakeRepository());

        viewModel.toggleSelection(1L);
        viewModel.clearSelection();

        assertEquals(0, viewModel.getSelectedCount());
    }

    private static <T> List<Resource<T>> observe(LiveData<Resource<T>> liveData) {
        List<Resource<T>> values = new ArrayList<>();
        liveData.observeForever(values::add);
        return values;
    }

    private static class FakeRepository extends AdminQuestionRepository {
        boolean fail;
        QuestionStatus lastStatus;
        Long lastSubjectId;
        Long lastTopicId;
        QuestionType lastQuestionType;
        String lastQ;
        int lastSize;

        FakeRepository() {
            super(null);
        }

        @Override
        public void getPickerQueue(
                QuestionStatus status,
                Long subjectId,
                Long topicId,
                QuestionType questionType,
                String q,
                int page,
                int size,
                RepositoryCallback<PageResponse<QuestionPickerItemResponse>> callback
        ) {
            lastStatus = status;
            lastSubjectId = subjectId;
            lastTopicId = topicId;
            lastQuestionType = questionType;
            lastQ = q;
            lastSize = size;
            if (fail) {
                callback.onError(new AdminQuestionError(
                        AdminQuestionError.Type.HTTP,
                        400,
                        "VALIDATION_FAILED",
                        "Bad picker request"
                ));
                return;
            }
            callback.onSuccess(new PageResponse<>(
                    Collections.singletonList(question()),
                    1,
                    1,
                    page,
                    size
            ));
        }

        private static QuestionPickerItemResponse question() {
            return QuestionPickerItemResponse.builder()
                    .id(1L)
                    .questionCode("Q-1")
                    .questionTextSnippet("Snippet")
                    .subjectId(1L)
                    .topicId(2L)
                    .questionType(QuestionType.SINGLE_CHOICE)
                    .difficulty(Difficulty.MEDIUM)
                    .status(QuestionStatus.APPROVED)
                    .version(1)
                    .updatedAt("2026-05-22T00:00:00Z")
                    .build();
        }
    }
}
