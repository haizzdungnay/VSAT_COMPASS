package com.example.v_sat_compass.ui.collaborator;

import static org.junit.Assert.assertEquals;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.LiveData;

import com.example.v_sat_compass.data.model.admin.PageResponse;
import com.example.v_sat_compass.data.model.enums.Difficulty;
import com.example.v_sat_compass.data.model.enums.QuestionStatus;
import com.example.v_sat_compass.data.model.enums.QuestionType;
import com.example.v_sat_compass.data.model.question.QuestionListItemResponse;
import com.example.v_sat_compass.data.repository.CollaboratorQuestionRepository;
import com.example.v_sat_compass.data.repository.Resource;
import com.example.v_sat_compass.data.repository.TopicRepository;

import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CollaboratorQuestionListViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Test
    public void listMyQuestions_passesFilterAndPagingParams() {
        FakeQuestionRepository repository = new FakeQuestionRepository();
        CollaboratorQuestionViewModel viewModel =
                new CollaboratorQuestionViewModel(repository, new TopicRepository(null));

        viewModel.listMyQuestions(QuestionStatus.NEEDS_REVISION, 2, 20);

        assertEquals(QuestionStatus.NEEDS_REVISION, repository.lastStatus);
        assertEquals(2, repository.lastPage);
        assertEquals(20, repository.lastSize);
    }

    @Test
    public void listMyQuestions_emitsLoadingThenSuccessForPage() {
        FakeQuestionRepository repository = new FakeQuestionRepository();
        CollaboratorQuestionViewModel viewModel =
                new CollaboratorQuestionViewModel(repository, new TopicRepository(null));
        List<Resource<PageResponse<QuestionListItemResponse>>> values =
                observe(viewModel.getListState());

        viewModel.listMyQuestions(null, 0, 20);

        assertEquals(Resource.Status.LOADING, values.get(0).getStatus());
        assertEquals(Resource.Status.SUCCESS, values.get(1).getStatus());
        assertEquals(0, values.get(1).getData().getNumber());
        assertEquals(2, values.get(1).getData().getTotalPages());
        assertEquals("Q-1", values.get(1).getData().getContent().get(0).getQuestionCode());
    }

    @Test
    public void listMyQuestions_errorIsExposed() {
        FakeQuestionRepository repository = new FakeQuestionRepository();
        repository.failList = true;
        CollaboratorQuestionViewModel viewModel =
                new CollaboratorQuestionViewModel(repository, new TopicRepository(null));
        List<Resource<PageResponse<QuestionListItemResponse>>> values =
                observe(viewModel.getListState());

        viewModel.listMyQuestions(QuestionStatus.DRAFT, 0, 20);

        assertEquals(Resource.Status.LOADING, values.get(0).getStatus());
        assertEquals(Resource.Status.ERROR, values.get(1).getStatus());
        assertEquals("Server unavailable", values.get(1).getMessage());
    }

    private static <T> List<Resource<T>> observe(LiveData<Resource<T>> liveData) {
        List<Resource<T>> values = new ArrayList<>();
        liveData.observeForever(values::add);
        return values;
    }

    private static class FakeQuestionRepository extends CollaboratorQuestionRepository {
        QuestionStatus lastStatus;
        int lastPage = -1;
        int lastSize = -1;
        boolean failList;

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
            lastStatus = status;
            lastPage = page;
            lastSize = size;

            if (failList) {
                callback.onError(new CollaboratorQuestionError(
                        CollaboratorQuestionError.Type.SERVER,
                        503,
                        "SERVER_ERROR",
                        "Server unavailable"
                ));
                return;
            }

            callback.onSuccess(new PageResponse<>(
                    Collections.singletonList(question(page + 1L)),
                    21,
                    2,
                    page,
                    size
            ));
        }

        private static QuestionListItemResponse question(Long id) {
            return new QuestionListItemResponse(
                    id,
                    "Q-" + id,
                    1L,
                    2L,
                    Difficulty.MEDIUM,
                    QuestionType.SINGLE_CHOICE,
                    QuestionStatus.DRAFT,
                    1,
                    3L,
                    "2026-05-22T08:45:30Z"
            );
        }
    }
}
