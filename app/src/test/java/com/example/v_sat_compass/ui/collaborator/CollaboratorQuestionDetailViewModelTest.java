package com.example.v_sat_compass.ui.collaborator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.LiveData;

import com.example.v_sat_compass.data.model.enums.Difficulty;
import com.example.v_sat_compass.data.model.enums.QuestionStatus;
import com.example.v_sat_compass.data.model.enums.QuestionType;
import com.example.v_sat_compass.data.model.question.QuestionResponse;
import com.example.v_sat_compass.data.model.question.UpdateQuestionRequest;
import com.example.v_sat_compass.data.repository.CollaboratorQuestionRepository;
import com.example.v_sat_compass.data.repository.Resource;
import com.example.v_sat_compass.data.repository.TopicRepository;

import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class CollaboratorQuestionDetailViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Test
    public void loadDetail_emitsLoadingThenSuccess() {
        FakeQuestionRepository repository = new FakeQuestionRepository();
        CollaboratorQuestionViewModel viewModel =
                new CollaboratorQuestionViewModel(repository, new TopicRepository(null));
        List<Resource<QuestionResponse>> values = observe(viewModel.getQuestionState());

        viewModel.getQuestion(42L);

        assertSuccessTransition(values);
        assertEquals(Long.valueOf(42L), repository.loadedId);
    }

    @Test
    public void loadDetail_emitsLoadingThenError() {
        FakeQuestionRepository repository = new FakeQuestionRepository();
        repository.failGet = true;
        CollaboratorQuestionViewModel viewModel =
                new CollaboratorQuestionViewModel(repository, new TopicRepository(null));
        List<Resource<QuestionResponse>> values = observe(viewModel.getQuestionState());

        viewModel.getQuestion(42L);

        assertErrorTransition(values, "Detail failed");
    }

    @Test
    public void updateQuestion_emitsLoadingThenSuccess() {
        FakeQuestionRepository repository = new FakeQuestionRepository();
        CollaboratorQuestionViewModel viewModel =
                new CollaboratorQuestionViewModel(repository, new TopicRepository(null));
        List<Resource<QuestionResponse>> values = observe(viewModel.getUpdateQuestionState());

        viewModel.updateQuestion(42L, new UpdateQuestionRequest());

        assertSuccessTransition(values);
        assertEquals(Long.valueOf(42L), repository.updatedId);
    }

    @Test
    public void updateQuestion_emitsLoadingThenError() {
        FakeQuestionRepository repository = new FakeQuestionRepository();
        repository.failUpdate = true;
        CollaboratorQuestionViewModel viewModel =
                new CollaboratorQuestionViewModel(repository, new TopicRepository(null));
        List<Resource<QuestionResponse>> values = observe(viewModel.getUpdateQuestionState());

        viewModel.updateQuestion(42L, new UpdateQuestionRequest());

        assertErrorTransition(values, "Update failed");
    }

    @Test
    public void submitForReview_emitsLoadingThenSuccess() {
        FakeQuestionRepository repository = new FakeQuestionRepository();
        CollaboratorQuestionViewModel viewModel =
                new CollaboratorQuestionViewModel(repository, new TopicRepository(null));
        List<Resource<QuestionResponse>> values = observe(viewModel.getSubmitForReviewState());

        viewModel.submitForReview(42L);

        assertSuccessTransition(values);
        assertEquals(Long.valueOf(42L), repository.submittedId);
    }

    @Test
    public void submitForReview_emitsLoadingThenError() {
        FakeQuestionRepository repository = new FakeQuestionRepository();
        repository.failSubmit = true;
        CollaboratorQuestionViewModel viewModel =
                new CollaboratorQuestionViewModel(repository, new TopicRepository(null));
        List<Resource<QuestionResponse>> values = observe(viewModel.getSubmitForReviewState());

        viewModel.submitForReview(42L);

        assertErrorTransition(values, "Submit failed");
    }

    @Test
    public void draftAndNeedsRevision_areEditableStatuses() {
        assertTrue(CollaboratorQuestionDetailActivity.isEditableStatus(QuestionStatus.DRAFT));
        assertTrue(CollaboratorQuestionDetailActivity.isEditableStatus(
                QuestionStatus.NEEDS_REVISION));
    }

    @Test
    public void nonDraftStatuses_areReadOnly() {
        assertFalse(CollaboratorQuestionDetailActivity.isEditableStatus(
                QuestionStatus.PENDING_REVIEW));
        assertFalse(CollaboratorQuestionDetailActivity.isEditableStatus(QuestionStatus.APPROVED));
        assertFalse(CollaboratorQuestionDetailActivity.isEditableStatus(QuestionStatus.PUBLISHED));
        assertFalse(CollaboratorQuestionDetailActivity.isEditableStatus(QuestionStatus.HIDDEN));
        assertFalse(CollaboratorQuestionDetailActivity.isEditableStatus(QuestionStatus.ARCHIVED));
    }

    private void assertSuccessTransition(List<Resource<QuestionResponse>> values) {
        assertEquals(Resource.Status.LOADING, values.get(0).getStatus());
        assertEquals(Resource.Status.SUCCESS, values.get(1).getStatus());
        assertEquals("Q-T2-ABC12345", values.get(1).getData().getQuestionCode());
    }

    private void assertErrorTransition(List<Resource<QuestionResponse>> values, String message) {
        assertEquals(Resource.Status.LOADING, values.get(0).getStatus());
        assertEquals(Resource.Status.ERROR, values.get(1).getStatus());
        assertEquals(message, values.get(1).getMessage());
    }

    private static <T> List<Resource<T>> observe(LiveData<Resource<T>> liveData) {
        List<Resource<T>> values = new ArrayList<>();
        liveData.observeForever(values::add);
        return values;
    }

    private static class FakeQuestionRepository extends CollaboratorQuestionRepository {
        boolean failGet;
        boolean failUpdate;
        boolean failSubmit;
        Long loadedId;
        Long updatedId;
        Long submittedId;

        FakeQuestionRepository() {
            super(null);
        }

        @Override
        public void getQuestion(Long id, RepositoryCallback<QuestionResponse> callback) {
            loadedId = id;
            if (failGet) {
                callback.onError(error(404, "Detail failed"));
                return;
            }
            callback.onSuccess(response());
        }

        @Override
        public void updateQuestion(
                Long id,
                UpdateQuestionRequest request,
                RepositoryCallback<QuestionResponse> callback
        ) {
            updatedId = id;
            if (failUpdate) {
                callback.onError(error(400, "Update failed"));
                return;
            }
            callback.onSuccess(response());
        }

        @Override
        public void submitForReview(Long id, RepositoryCallback<QuestionResponse> callback) {
            submittedId = id;
            if (failSubmit) {
                callback.onError(error(409, "Submit failed"));
                return;
            }
            callback.onSuccess(response());
        }

        private static CollaboratorQuestionError error(int statusCode, String message) {
            return new CollaboratorQuestionError(
                    CollaboratorQuestionError.Type.HTTP,
                    statusCode,
                    "ERROR",
                    message
            );
        }

        private static QuestionResponse response() {
            QuestionResponse response = new QuestionResponse();
            response.setId(42L);
            response.setQuestionCode("Q-T2-ABC12345");
            response.setSubjectId(1L);
            response.setTopicId(2L);
            response.setDifficulty(Difficulty.MEDIUM);
            response.setQuestionType(QuestionType.SINGLE_CHOICE);
            response.setQuestionText("Question text");
            response.setStatus(QuestionStatus.DRAFT);
            response.setVersion(1);
            return response;
        }
    }
}
