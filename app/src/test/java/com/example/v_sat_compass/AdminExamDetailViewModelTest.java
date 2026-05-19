package com.example.v_sat_compass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.LiveData;

import com.example.v_sat_compass.data.model.admin.AdminExamResponse;
import com.example.v_sat_compass.data.model.admin.AdminExamUpdateRequest;
import com.example.v_sat_compass.data.repository.AdminExamRepository;
import com.example.v_sat_compass.data.repository.Resource;
import com.example.v_sat_compass.ui.admin.exam.AdminExamViewModel;

import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class AdminExamDetailViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    // ── loadDetail state machine ──────────────────────────────────────────────

    @Test
    public void loadDetail_emitsLoadingThenSuccess() {
        FakeDetailRepo repo = new FakeDetailRepo();
        AdminExamViewModel vm = new AdminExamViewModel(repo);
        List<Resource<AdminExamResponse>> values = observe(vm.getExamDetailState());

        vm.loadDetail(1L);

        assertEquals(Resource.Status.LOADING, values.get(0).getStatus());
        assertEquals(Resource.Status.SUCCESS, values.get(1).getStatus());
        assertEquals("Detail Exam", values.get(1).getData().getTitle());
    }

    @Test
    public void loadDetail_emitsLoadingThenError() {
        FakeDetailRepo repo = new FakeDetailRepo();
        repo.failGetExam = true;
        AdminExamViewModel vm = new AdminExamViewModel(repo);
        List<Resource<AdminExamResponse>> values = observe(vm.getExamDetailState());

        vm.loadDetail(1L);

        assertEquals(Resource.Status.LOADING, values.get(0).getStatus());
        assertEquals(Resource.Status.ERROR, values.get(1).getStatus());
        assertEquals("Not found", values.get(1).getMessage());
    }

    // ── editModeState ─────────────────────────────────────────────────────────

    @Test
    public void enterEditMode_setsEditModeTrue() {
        FakeDetailRepo repo = new FakeDetailRepo();
        AdminExamViewModel vm = new AdminExamViewModel(repo);
        List<Boolean> values = observe(vm.getEditModeState());

        vm.enterEditMode();

        assertTrue(values.get(values.size() - 1));
    }

    @Test
    public void cancelEditMode_setsEditModeFalse() {
        FakeDetailRepo repo = new FakeDetailRepo();
        AdminExamViewModel vm = new AdminExamViewModel(repo);
        List<Boolean> values = observe(vm.getEditModeState());

        vm.enterEditMode();
        vm.cancelEditMode();

        assertFalse(values.get(values.size() - 1));
    }

    @Test
    public void saveEdit_success_exitsEditModeAndUpdatesDetail() {
        FakeDetailRepo repo = new FakeDetailRepo();
        AdminExamViewModel vm = new AdminExamViewModel(repo);
        List<Boolean> editValues = observe(vm.getEditModeState());
        List<Resource<AdminExamResponse>> detailValues = observe(vm.getExamDetailState());

        vm.enterEditMode();
        vm.saveEdit(1L, new AdminExamUpdateRequest());

        assertFalse(editValues.get(editValues.size() - 1));
        Resource<AdminExamResponse> last = detailValues.get(detailValues.size() - 1);
        assertEquals(Resource.Status.SUCCESS, last.getStatus());
    }

    @Test
    public void saveEdit_error_keepsErrorState() {
        FakeDetailRepo repo = new FakeDetailRepo();
        repo.failUpdate = true;
        AdminExamViewModel vm = new AdminExamViewModel(repo);
        List<Resource<AdminExamResponse>> detailValues = observe(vm.getExamDetailState());

        vm.saveEdit(1L, new AdminExamUpdateRequest());

        Resource<AdminExamResponse> last = detailValues.get(detailValues.size() - 1);
        assertEquals(Resource.Status.ERROR, last.getStatus());
        assertEquals("Update failed", last.getMessage());
    }

    // ── actionResultState ─────────────────────────────────────────────────────

    @Test
    public void submitForReview_success_updatesActionAndDetailState() {
        FakeDetailRepo repo = new FakeDetailRepo();
        AdminExamViewModel vm = new AdminExamViewModel(repo);
        List<Resource<AdminExamResponse>> actionValues = observe(vm.getActionResultState());

        vm.submitForReview(1L);

        assertEquals(Resource.Status.SUCCESS, actionValues.get(actionValues.size() - 1).getStatus());
    }

    @Test
    public void submitForReview_error_setsActionErrorState() {
        FakeDetailRepo repo = new FakeDetailRepo();
        repo.failAction = true;
        AdminExamViewModel vm = new AdminExamViewModel(repo);
        List<Resource<AdminExamResponse>> actionValues = observe(vm.getActionResultState());

        vm.submitForReview(1L);

        Resource<AdminExamResponse> last = actionValues.get(actionValues.size() - 1);
        assertEquals(Resource.Status.ERROR, last.getStatus());
        assertEquals("Action failed", last.getMessage());
    }

    // ── status action availability mapping ───────────────────────────────────

    @Test
    public void availableActionsForStatus_draft() {
        List<String> actions = AdminExamViewModel.availableActionsForStatus("DRAFT");
        assertTrue(actions.contains("EDIT"));
        assertTrue(actions.contains("SUBMIT_FOR_REVIEW"));
        assertTrue(actions.contains("DISCARD"));
        assertEquals(3, actions.size());
    }

    @Test
    public void availableActionsForStatus_pendingReview() {
        List<String> actions = AdminExamViewModel.availableActionsForStatus("PENDING_REVIEW");
        assertTrue(actions.contains("PUBLISH"));
        assertTrue(actions.contains("REJECT"));
        assertTrue(actions.contains("RETURN_TO_DRAFT"));
        assertEquals(3, actions.size());
    }

    @Test
    public void availableActionsForStatus_published() {
        List<String> actions = AdminExamViewModel.availableActionsForStatus("PUBLISHED");
        assertTrue(actions.contains("HIDE"));
        assertTrue(actions.contains("ARCHIVE"));
        assertEquals(2, actions.size());
    }

    @Test
    public void availableActionsForStatus_hidden() {
        List<String> actions = AdminExamViewModel.availableActionsForStatus("HIDDEN");
        assertTrue(actions.contains("ARCHIVE"));
        assertEquals(1, actions.size());
    }

    @Test
    public void availableActionsForStatus_archived() {
        List<String> actions = AdminExamViewModel.availableActionsForStatus("ARCHIVED");
        assertTrue(actions.isEmpty());
    }

    @Test
    public void availableActionsForStatus_null() {
        List<String> actions = AdminExamViewModel.availableActionsForStatus(null);
        assertTrue(actions.isEmpty());
    }

    @Test
    public void availableActionsForStatus_unknown() {
        List<String> actions = AdminExamViewModel.availableActionsForStatus("UNKNOWN_STATE");
        assertTrue(actions.isEmpty());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private static <T> List<T> observe(LiveData<T> liveData) {
        List<T> values = new ArrayList<>();
        liveData.observeForever(values::add);
        return values;
    }

    private static class FakeDetailRepo extends AdminExamRepository {
        boolean failGetExam;
        boolean failUpdate;
        boolean failAction;

        FakeDetailRepo() {
            super(null);
        }

        private AdminExamResponse makeResponse() {
            AdminExamResponse r = new AdminExamResponse();
            r.setId(1L);
            r.setTitle("Detail Exam");
            r.setExamCode("DETAIL_001");
            r.setStatus("DRAFT");
            r.setVersion(1);
            return r;
        }

        @Override
        public void getExam(Long id, RepositoryCallback<AdminExamResponse> callback) {
            if (failGetExam) {
                callback.onError(new AdminExamError(AdminExamError.Type.HTTP, 404, "NOT_FOUND", "Not found"));
            } else {
                callback.onSuccess(makeResponse());
            }
        }

        @Override
        public void updateExam(Long id, AdminExamUpdateRequest request,
                RepositoryCallback<AdminExamResponse> callback) {
            if (failUpdate) {
                callback.onError(new AdminExamError(AdminExamError.Type.HTTP, 400, "UPDATE_FAILED", "Update failed"));
            } else {
                callback.onSuccess(makeResponse());
            }
        }

        @Override
        public void submitForReview(Long examId, RepositoryCallback<AdminExamResponse> callback) {
            if (failAction) {
                callback.onError(new AdminExamError(AdminExamError.Type.HTTP, 409, "INVALID_STATE", "Action failed"));
            } else {
                callback.onSuccess(makeResponse());
            }
        }

        @Override
        public void publish(Long examId, RepositoryCallback<AdminExamResponse> callback) {
            if (failAction) {
                callback.onError(new AdminExamError(AdminExamError.Type.HTTP, 409, "INVALID_STATE", "Action failed"));
            } else {
                callback.onSuccess(makeResponse());
            }
        }

        @Override
        public void rejectReview(Long examId, RepositoryCallback<AdminExamResponse> callback) {
            if (failAction) {
                callback.onError(new AdminExamError(AdminExamError.Type.HTTP, 409, "INVALID_STATE", "Action failed"));
            } else {
                callback.onSuccess(makeResponse());
            }
        }

        @Override
        public void returnToDraft(Long examId, RepositoryCallback<AdminExamResponse> callback) {
            if (failAction) {
                callback.onError(new AdminExamError(AdminExamError.Type.HTTP, 409, "INVALID_STATE", "Action failed"));
            } else {
                callback.onSuccess(makeResponse());
            }
        }

        @Override
        public void hide(Long examId, RepositoryCallback<AdminExamResponse> callback) {
            if (failAction) {
                callback.onError(new AdminExamError(AdminExamError.Type.HTTP, 409, "INVALID_STATE", "Action failed"));
            } else {
                callback.onSuccess(makeResponse());
            }
        }

        @Override
        public void archive(Long examId, RepositoryCallback<AdminExamResponse> callback) {
            if (failAction) {
                callback.onError(new AdminExamError(AdminExamError.Type.HTTP, 409, "INVALID_STATE", "Action failed"));
            } else {
                callback.onSuccess(makeResponse());
            }
        }

        @Override
        public void discardDraftExam(Long examId, RepositoryCallback<Void> callback) {
            if (failAction) {
                callback.onError(new AdminExamError(AdminExamError.Type.HTTP, 409, "INVALID_STATE", "Action failed"));
            } else {
                callback.onSuccess(null);
            }
        }
    }
}
