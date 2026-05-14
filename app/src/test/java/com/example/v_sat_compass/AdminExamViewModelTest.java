package com.example.v_sat_compass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.LiveData;

import com.example.v_sat_compass.data.model.admin.AdminExamAddQuestionRequest;
import com.example.v_sat_compass.data.model.admin.AdminExamCreateRequest;
import com.example.v_sat_compass.data.model.admin.AdminExamReorderQuestionsRequest;
import com.example.v_sat_compass.data.model.admin.AdminExamResponse;
import com.example.v_sat_compass.data.model.admin.AdminExamSummaryResponse;
import com.example.v_sat_compass.data.model.admin.AdminExamUpdateRequest;
import com.example.v_sat_compass.data.model.admin.PageResponse;
import com.example.v_sat_compass.data.repository.AdminExamRepository;
import com.example.v_sat_compass.data.repository.Resource;
import com.example.v_sat_compass.ui.admin.exam.AdminExamViewModel;

import org.junit.Rule;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AdminExamViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Test
    public void loadExams_emitsLoadingThenSuccess() {
        FakeRepository repository = new FakeRepository();
        AdminExamViewModel viewModel = new AdminExamViewModel(repository);
        List<Resource<PageResponse<AdminExamSummaryResponse>>> values =
                observe(viewModel.getListState());

        viewModel.loadExams("DRAFT", 7L, 0, 20);

        assertEquals(Resource.Status.LOADING, values.get(0).getStatus());
        assertEquals(Resource.Status.SUCCESS, values.get(1).getStatus());
        assertEquals("ADM_MATH_001", values.get(1).getData().getContent().get(0).getExamCode());
    }

    @Test
    public void createExam_emitsLoadingThenError() {
        FakeRepository repository = new FakeRepository();
        repository.failCreate = true;
        AdminExamViewModel viewModel = new AdminExamViewModel(repository);
        List<Resource<AdminExamResponse>> values = observe(viewModel.getCreateState());

        viewModel.createExam(new AdminExamCreateRequest());

        assertEquals(Resource.Status.LOADING, values.get(0).getStatus());
        assertEquals(Resource.Status.ERROR, values.get(1).getStatus());
        assertEquals("Bad request", values.get(1).getMessage());
    }

    @Test
    public void detailUpdateAddQuestionAndReorder_emitSuccessStates() {
        FakeRepository repository = new FakeRepository();
        AdminExamViewModel viewModel = new AdminExamViewModel(repository);
        List<Resource<AdminExamResponse>> detailValues = observe(viewModel.getDetailState());
        List<Resource<AdminExamResponse>> updateValues = observe(viewModel.getUpdateState());
        List<Resource<AdminExamResponse>> addQuestionValues = observe(viewModel.getAddQuestionState());
        List<Resource<AdminExamResponse>> reorderValues = observe(viewModel.getReorderState());

        viewModel.loadExam(1L);
        viewModel.updateExam(1L, new AdminExamUpdateRequest());
        viewModel.addQuestion(1L, new AdminExamAddQuestionRequest(99L));
        viewModel.reorderQuestions(1L, new AdminExamReorderQuestionsRequest(Arrays.asList(99L, 100L)));

        assertSuccessTransition(detailValues);
        assertSuccessTransition(updateValues);
        assertSuccessTransition(addQuestionValues);
        assertSuccessTransition(reorderValues);
    }

    private void assertSuccessTransition(List<Resource<AdminExamResponse>> values) {
        assertEquals(Resource.Status.LOADING, values.get(0).getStatus());
        assertEquals(Resource.Status.SUCCESS, values.get(1).getStatus());
        assertNotNull(values.get(1).getData());
        assertEquals("Sample Exam", values.get(1).getData().getTitle());
    }

    private static <T> List<Resource<T>> observe(LiveData<Resource<T>> liveData) {
        List<Resource<T>> values = new ArrayList<>();
        liveData.observeForever(values::add);
        return values;
    }

    private static class FakeRepository extends AdminExamRepository {
        boolean failCreate;
        private final AdminExamResponse response = response();

        FakeRepository() {
            super(null);
        }

        @Override
        public void listExams(
                String status,
                Long subjectId,
                int page,
                int size,
                RepositoryCallback<PageResponse<AdminExamSummaryResponse>> callback
        ) {
            AdminExamSummaryResponse summary = new AdminExamSummaryResponse(
                    1L,
                    "ADM_MATH_001",
                    "Sample Exam",
                    7L,
                    10,
                    45,
                    "EASY",
                    "FREE",
                    BigDecimal.ZERO,
                    "DRAFT",
                    1,
                    "2026-05-15T10:01:00Z"
            );
            callback.onSuccess(new PageResponse<>(
                    Collections.singletonList(summary),
                    1,
                    1,
                    0,
                    20
            ));
        }

        @Override
        public void getExam(Long id, RepositoryCallback<AdminExamResponse> callback) {
            callback.onSuccess(response);
        }

        @Override
        public void createExam(
                AdminExamCreateRequest request,
                RepositoryCallback<AdminExamResponse> callback
        ) {
            if (failCreate) {
                callback.onError(new AdminExamError(
                        AdminExamError.Type.HTTP,
                        400,
                        "VALIDATION_FAILED",
                        "Bad request"
                ));
            } else {
                callback.onSuccess(response);
            }
        }

        @Override
        public void updateExam(
                Long id,
                AdminExamUpdateRequest request,
                RepositoryCallback<AdminExamResponse> callback
        ) {
            callback.onSuccess(response);
        }

        @Override
        public void addQuestion(
                Long examId,
                AdminExamAddQuestionRequest request,
                RepositoryCallback<AdminExamResponse> callback
        ) {
            callback.onSuccess(response);
        }

        @Override
        public void reorderQuestions(
                Long examId,
                AdminExamReorderQuestionsRequest request,
                RepositoryCallback<AdminExamResponse> callback
        ) {
            callback.onSuccess(response);
        }

        private static AdminExamResponse response() {
            AdminExamResponse response = new AdminExamResponse();
            response.setId(1L);
            response.setExamCode("ADM_MATH_001");
            response.setTitle("Sample Exam");
            response.setSubjectId(7L);
            response.setQuestionCount(10);
            response.setDurationMinutes(45);
            response.setDifficulty("EASY");
            response.setPricingType("FREE");
            response.setPrice(BigDecimal.ZERO);
            response.setStatus("DRAFT");
            response.setVersion(1);
            return response;
        }
    }
}
