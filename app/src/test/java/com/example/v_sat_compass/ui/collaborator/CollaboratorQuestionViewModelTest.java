package com.example.v_sat_compass.ui.collaborator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.LiveData;

import com.example.v_sat_compass.data.model.admin.PageResponse;
import com.example.v_sat_compass.data.model.enums.Difficulty;
import com.example.v_sat_compass.data.model.enums.QuestionStatus;
import com.example.v_sat_compass.data.model.enums.QuestionType;
import com.example.v_sat_compass.data.model.question.CreateQuestionRequest;
import com.example.v_sat_compass.data.model.question.QuestionListItemResponse;
import com.example.v_sat_compass.data.model.question.QuestionResponse;
import com.example.v_sat_compass.data.model.question.UpdateQuestionRequest;
import com.example.v_sat_compass.data.repository.CollaboratorQuestionRepository;
import com.example.v_sat_compass.data.repository.Resource;
import com.example.v_sat_compass.data.repository.TopicRepository;

import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CollaboratorQuestionViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Test
    public void listMyQuestions_emitsLoadingThenSuccess() {
        FakeQuestionRepository repository = new FakeQuestionRepository();
        CollaboratorQuestionViewModel viewModel =
                new CollaboratorQuestionViewModel(repository, new TopicRepository(null));
        List<Resource<PageResponse<QuestionListItemResponse>>> values =
                observe(viewModel.getListState());

        assertNull(viewModel.getListState().getValue());
        viewModel.listMyQuestions(QuestionStatus.DRAFT, 0, 20);

        assertEquals(Resource.Status.LOADING, values.get(0).getStatus());
        assertEquals(Resource.Status.SUCCESS, values.get(1).getStatus());
        assertEquals("Q-T2-ABC12345",
                values.get(1).getData().getContent().get(0).getQuestionCode());
    }

    @Test
    public void createQuestion_emitsLoadingThenError() {
        FakeQuestionRepository repository = new FakeQuestionRepository();
        repository.failCreate = true;
        CollaboratorQuestionViewModel viewModel =
                new CollaboratorQuestionViewModel(repository, new TopicRepository(null));
        List<Resource<QuestionResponse>> values = observe(viewModel.getCreateQuestionState());

        assertNull(viewModel.getCreateQuestionState().getValue());
        viewModel.createQuestion(new CreateQuestionRequest());

        assertEquals(Resource.Status.LOADING, values.get(0).getStatus());
        assertEquals(Resource.Status.ERROR, values.get(1).getStatus());
        assertEquals("Bad request", values.get(1).getMessage());
    }

    @Test
    public void getUpdateAndSubmit_emitSuccessStates() {
        FakeQuestionRepository repository = new FakeQuestionRepository();
        CollaboratorQuestionViewModel viewModel =
                new CollaboratorQuestionViewModel(repository, new TopicRepository(null));
        List<Resource<QuestionResponse>> questionValues = observe(viewModel.getQuestionState());
        List<Resource<QuestionResponse>> updateValues = observe(viewModel.getUpdateQuestionState());
        List<Resource<QuestionResponse>> submitValues = observe(viewModel.getSubmitForReviewState());

        viewModel.getQuestion(9L);
        viewModel.updateQuestion(9L, new UpdateQuestionRequest());
        viewModel.submitForReview(9L);

        assertSuccessTransition(questionValues);
        assertSuccessTransition(updateValues);
        assertSuccessTransition(submitValues);
    }

    private void assertSuccessTransition(List<Resource<QuestionResponse>> values) {
        assertEquals(Resource.Status.LOADING, values.get(0).getStatus());
        assertEquals(Resource.Status.SUCCESS, values.get(1).getStatus());
        assertEquals("Q-T2-ABC12345", values.get(1).getData().getQuestionCode());
    }

    private static <T> List<Resource<T>> observe(LiveData<Resource<T>> liveData) {
        List<Resource<T>> values = new ArrayList<>();
        liveData.observeForever(values::add);
        return values;
    }

    private static class FakeQuestionRepository extends CollaboratorQuestionRepository {
        boolean failCreate;
        private final QuestionResponse response = response();

        FakeQuestionRepository() {
            super(null);
        }

        @Override
        public void listMyQuestions(
                QuestionStatus status,
                int page,
                int size,
                RepositoryCallback<PageResponse<QuestionListItemResponse>> callback
        ) {
            QuestionListItemResponse item = new QuestionListItemResponse(
                    9L,
                    "Q-T2-ABC12345",
                    1L,
                    2L,
                    Difficulty.MEDIUM,
                    QuestionType.SINGLE_CHOICE,
                    QuestionStatus.DRAFT,
                    1,
                    3L,
                    "2026-05-15T10:01:00Z"
            );
            callback.onSuccess(new PageResponse<>(
                    Collections.singletonList(item),
                    1,
                    1,
                    0,
                    20
            ));
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
                        "Bad request"
                ));
            } else {
                callback.onSuccess(response);
            }
        }

        @Override
        public void getQuestion(Long id, RepositoryCallback<QuestionResponse> callback) {
            callback.onSuccess(response);
        }

        @Override
        public void updateQuestion(
                Long id,
                UpdateQuestionRequest request,
                RepositoryCallback<QuestionResponse> callback
        ) {
            callback.onSuccess(response);
        }

        @Override
        public void submitForReview(Long id, RepositoryCallback<QuestionResponse> callback) {
            callback.onSuccess(response);
        }

        private static QuestionResponse response() {
            QuestionResponse response = new QuestionResponse();
            response.setId(9L);
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
