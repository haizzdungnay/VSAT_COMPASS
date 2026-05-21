package com.example.v_sat_compass.ui.admin.questions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.LiveData;

import com.example.v_sat_compass.data.model.admin.PageResponse;
import com.example.v_sat_compass.data.model.enums.Difficulty;
import com.example.v_sat_compass.data.model.enums.QuestionStatus;
import com.example.v_sat_compass.data.model.enums.QuestionType;
import com.example.v_sat_compass.data.model.question.QuestionListItemResponse;
import com.example.v_sat_compass.data.model.question.QuestionResponse;
import com.example.v_sat_compass.data.repository.AdminQuestionRepository;
import com.example.v_sat_compass.data.repository.Resource;

import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AdminReviewViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Test
    public void loadQueue_emitsLoadingThenSuccess() {
        FakeRepository repository = new FakeRepository();
        AdminReviewViewModel viewModel = new AdminReviewViewModel(repository);
        List<Resource<PageResponse<QuestionListItemResponse>>> values =
                observe(viewModel.getQueueState());

        assertNull(viewModel.getQueueState().getValue());
        viewModel.loadQueue(QuestionStatus.PENDING_REVIEW, 0);

        assertEquals(Resource.Status.LOADING, values.get(0).getStatus());
        assertEquals(Resource.Status.SUCCESS, values.get(1).getStatus());
        assertEquals("Q-T2-ABC12345",
                values.get(1).getData().getContent().get(0).getQuestionCode());
        assertEquals(AdminReviewViewModel.DEFAULT_PAGE_SIZE, repository.lastSize);
    }

    @Test
    public void loadQueue_emitsLoadingThenError() {
        FakeRepository repository = new FakeRepository();
        repository.failQueue = true;
        AdminReviewViewModel viewModel = new AdminReviewViewModel(repository);
        List<Resource<PageResponse<QuestionListItemResponse>>> values =
                observe(viewModel.getQueueState());

        viewModel.loadQueue(QuestionStatus.APPROVED, 0);

        assertEquals(Resource.Status.LOADING, values.get(0).getStatus());
        assertEquals(Resource.Status.ERROR, values.get(1).getStatus());
        assertEquals("Bad request", values.get(1).getMessage());
    }

    @Test
    public void loadDetail_emitsLoadingThenSuccess() {
        FakeRepository repository = new FakeRepository();
        AdminReviewViewModel viewModel = new AdminReviewViewModel(repository);
        List<Resource<QuestionResponse>> values = observe(viewModel.getDetailState());

        viewModel.loadDetail(9L);

        assertEquals(Resource.Status.LOADING, values.get(0).getStatus());
        assertEquals(Resource.Status.SUCCESS, values.get(1).getStatus());
        assertEquals("Q-T2-ABC12345", values.get(1).getData().getQuestionCode());
    }

    @Test
    public void approve_emitsLoadingThenSuccess() {
        FakeRepository repository = new FakeRepository();
        AdminReviewViewModel viewModel = new AdminReviewViewModel(repository);
        List<Resource<QuestionResponse>> values = observe(viewModel.getActionState());

        viewModel.approve(9L, "Looks good");

        assertEquals(Resource.Status.LOADING, values.get(0).getStatus());
        assertEquals(Resource.Status.SUCCESS, values.get(1).getStatus());
        assertEquals(1, repository.approveCalls);
        assertEquals("Looks good", repository.lastComment);
    }

    @Test
    public void requestRevisionAndReject_blankComment_emitValidationErrorWithoutRepositoryCall() {
        FakeRepository repository = new FakeRepository();
        AdminReviewViewModel viewModel = new AdminReviewViewModel(repository);
        List<Resource<QuestionResponse>> values = observe(viewModel.getActionState());

        viewModel.requestRevision(9L, " ");
        viewModel.reject(9L, null);

        assertEquals(Resource.Status.ERROR, values.get(0).getStatus());
        assertEquals(AdminReviewViewModel.ERROR_COMMENT_REQUIRED, values.get(0).getMessage());
        assertEquals(Resource.Status.ERROR, values.get(1).getStatus());
        assertEquals(0, repository.requestRevisionCalls);
        assertEquals(0, repository.rejectCalls);
    }

    @Test
    public void overLengthComment_emitsValidationErrorWithoutRepositoryCall() {
        FakeRepository repository = new FakeRepository();
        AdminReviewViewModel viewModel = new AdminReviewViewModel(repository);
        List<Resource<QuestionResponse>> values = observe(viewModel.getActionState());

        viewModel.approve(9L, repeat('a', AdminReviewViewModel.MAX_COMMENT_LENGTH + 1));

        assertEquals(Resource.Status.ERROR, values.get(0).getStatus());
        assertEquals(AdminReviewViewModel.ERROR_COMMENT_TOO_LONG, values.get(0).getMessage());
        assertEquals(0, repository.approveCalls);
    }

    private static <T> List<Resource<T>> observe(LiveData<Resource<T>> liveData) {
        List<Resource<T>> values = new ArrayList<>();
        liveData.observeForever(values::add);
        return values;
    }

    private static String repeat(char value, int count) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < count; i++) {
            builder.append(value);
        }
        return builder.toString();
    }

    private static class FakeRepository extends AdminQuestionRepository {
        boolean failQueue;
        int lastSize;
        int approveCalls;
        int requestRevisionCalls;
        int rejectCalls;
        String lastComment;
        private final QuestionResponse response = response();

        FakeRepository() {
            super(null);
        }

        @Override
        public void getReviewQueue(
                QuestionStatus status,
                int page,
                int size,
                RepositoryCallback<PageResponse<QuestionListItemResponse>> callback
        ) {
            lastSize = size;
            if (failQueue) {
                callback.onError(error());
                return;
            }
            QuestionListItemResponse item = new QuestionListItemResponse(
                    9L,
                    "Q-T2-ABC12345",
                    1L,
                    2L,
                    Difficulty.MEDIUM,
                    QuestionType.SINGLE_CHOICE,
                    QuestionStatus.PENDING_REVIEW,
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
        public void getQuestionDetail(Long id, RepositoryCallback<QuestionResponse> callback) {
            callback.onSuccess(response);
        }

        @Override
        public void approve(Long id, String comment, RepositoryCallback<QuestionResponse> callback) {
            approveCalls++;
            lastComment = comment;
            callback.onSuccess(response);
        }

        @Override
        public void requestRevision(
                Long id,
                String comment,
                RepositoryCallback<QuestionResponse> callback
        ) {
            requestRevisionCalls++;
            lastComment = comment;
            callback.onSuccess(response);
        }

        @Override
        public void reject(Long id, String comment, RepositoryCallback<QuestionResponse> callback) {
            rejectCalls++;
            lastComment = comment;
            callback.onSuccess(response);
        }

        private AdminQuestionError error() {
            return new AdminQuestionError(
                    AdminQuestionError.Type.HTTP,
                    400,
                    "VALIDATION_FAILED",
                    "Bad request"
            );
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
            response.setStatus(QuestionStatus.PENDING_REVIEW);
            response.setVersion(1);
            return response;
        }
    }
}
