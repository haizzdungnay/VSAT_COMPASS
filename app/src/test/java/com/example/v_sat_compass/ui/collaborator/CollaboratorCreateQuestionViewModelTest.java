package com.example.v_sat_compass.ui.collaborator;

import static org.junit.Assert.assertEquals;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.LiveData;

import com.example.v_sat_compass.data.model.enums.Difficulty;
import com.example.v_sat_compass.data.model.enums.QuestionType;
import com.example.v_sat_compass.data.model.question.CreateQuestionRequest;
import com.example.v_sat_compass.data.model.question.QuestionResponse;
import com.example.v_sat_compass.data.repository.CollaboratorQuestionRepository;
import com.example.v_sat_compass.data.repository.Resource;
import com.example.v_sat_compass.data.repository.TopicRepository;

import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class CollaboratorCreateQuestionViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Test
    public void createQuestion_emitsLoadingThenSuccess() {
        FakeQuestionRepository repository = new FakeQuestionRepository();
        CollaboratorQuestionViewModel viewModel =
                new CollaboratorQuestionViewModel(repository, new TopicRepository(null));
        List<Resource<QuestionResponse>> values = observe(viewModel.getCreateQuestionState());

        viewModel.createQuestion(new CreateQuestionRequest());

        assertEquals(Resource.Status.LOADING, values.get(0).getStatus());
        assertEquals(Resource.Status.SUCCESS, values.get(1).getStatus());
        assertEquals(Long.valueOf(42L), values.get(1).getData().getId());
    }

    @Test
    public void createQuestion_emitsLoadingThenError() {
        FakeQuestionRepository repository = new FakeQuestionRepository();
        repository.failCreate = true;
        CollaboratorQuestionViewModel viewModel =
                new CollaboratorQuestionViewModel(repository, new TopicRepository(null));
        List<Resource<QuestionResponse>> values = observe(viewModel.getCreateQuestionState());

        viewModel.createQuestion(new CreateQuestionRequest());

        assertEquals(Resource.Status.LOADING, values.get(0).getStatus());
        assertEquals(Resource.Status.ERROR, values.get(1).getStatus());
        assertEquals("Create failed", values.get(1).getMessage());
    }

    @Test
    public void submitForReview_emitsLoadingThenSuccess() {
        FakeQuestionRepository repository = new FakeQuestionRepository();
        CollaboratorQuestionViewModel viewModel =
                new CollaboratorQuestionViewModel(repository, new TopicRepository(null));
        List<Resource<QuestionResponse>> values = observe(viewModel.getSubmitForReviewState());

        viewModel.submitForReview(42L);

        assertEquals(Resource.Status.LOADING, values.get(0).getStatus());
        assertEquals(Resource.Status.SUCCESS, values.get(1).getStatus());
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

        assertEquals(Resource.Status.LOADING, values.get(0).getStatus());
        assertEquals(Resource.Status.ERROR, values.get(1).getStatus());
        assertEquals("Submit failed", values.get(1).getMessage());
    }

    private static <T> List<Resource<T>> observe(LiveData<Resource<T>> liveData) {
        List<Resource<T>> values = new ArrayList<>();
        liveData.observeForever(values::add);
        return values;
    }

    private static class FakeQuestionRepository extends CollaboratorQuestionRepository {
        boolean failCreate;
        boolean failSubmit;
        Long submittedId;

        FakeQuestionRepository() {
            super(null);
        }

        @Override
        public void createQuestion(
                CreateQuestionRequest request,
                RepositoryCallback<QuestionResponse> callback
        ) {
            if (failCreate) {
                callback.onError(new CollaboratorQuestionError(
                        CollaboratorQuestionError.Type.HTTP,
                        400,
                        "VALIDATION_FAILED",
                        "Create failed"
                ));
                return;
            }
            callback.onSuccess(response());
        }

        @Override
        public void submitForReview(
                Long id,
                RepositoryCallback<QuestionResponse> callback
        ) {
            submittedId = id;
            if (failSubmit) {
                callback.onError(new CollaboratorQuestionError(
                        CollaboratorQuestionError.Type.SERVER,
                        500,
                        "SERVER_ERROR",
                        "Submit failed"
                ));
                return;
            }
            callback.onSuccess(response());
        }

        private static QuestionResponse response() {
            QuestionResponse response = new QuestionResponse();
            response.setId(42L);
            response.setSubjectId(1L);
            response.setTopicId(2L);
            response.setDifficulty(Difficulty.MEDIUM);
            response.setQuestionType(QuestionType.SINGLE_CHOICE);
            response.setQuestionText("Question text");
            return response;
        }
    }
}
