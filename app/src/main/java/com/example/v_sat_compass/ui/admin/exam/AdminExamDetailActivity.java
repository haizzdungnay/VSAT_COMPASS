package com.example.v_sat_compass.ui.admin.exam;

import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.v_sat_compass.R;
import com.example.v_sat_compass.data.model.SubjectResponse;
import com.example.v_sat_compass.data.model.admin.AdminExamResponse;
import com.example.v_sat_compass.data.model.admin.AdminExamUpdateRequest;
import com.example.v_sat_compass.data.repository.Resource;

import java.util.List;

public class AdminExamDetailActivity extends AppCompatActivity {

    public static final String EXTRA_EXAM_ID = "exam_id";

    private AdminExamViewModel viewModel;
    private long examId = -1L;

    private TextView tvTitle;
    private TextView tvCode;
    private TextView tvSubject;
    private TextView tvDescription;
    private TextView tvVersion;
    private TextView tvStatus;
    private TextView tvUpdatedAt;
    private TextView tvQuestionCount;
    private TextView tvError;

    private EditText etTitle;
    private EditText etDescription;
    private Spinner spSubject;

    private View readOnlyGroup;
    private View editGroup;

    private Button btnEdit;
    private Button btnSubmitReview;
    private Button btnDiscard;
    private Button btnPublish;
    private Button btnReject;
    private Button btnReturnToDraft;
    private Button btnHide;
    private Button btnArchive;
    private Button btnSave;
    private Button btnCancel;

    private List<SubjectResponse> subjects;
    private Long selectedSubjectId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_exam_detail);

        examId = getIntent().getLongExtra(EXTRA_EXAM_ID, -1L);
        if (examId == -1L) {
            Toast.makeText(this, getString(R.string.detail_error_invalid_id), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        viewModel = new ViewModelProvider(this).get(AdminExamViewModel.class);
        bindViews();
        observeViewModel();
        viewModel.loadDetail(examId);
        viewModel.loadSubjects();
    }

    private void bindViews() {
        tvTitle = findViewById(R.id.tv_detail_title);
        tvCode = findViewById(R.id.tv_detail_code);
        tvSubject = findViewById(R.id.tv_detail_subject);
        tvDescription = findViewById(R.id.tv_detail_description);
        tvVersion = findViewById(R.id.tv_detail_version);
        tvStatus = findViewById(R.id.tv_detail_status);
        tvUpdatedAt = findViewById(R.id.tv_detail_updated_at);
        tvQuestionCount = findViewById(R.id.tv_detail_question_count);
        tvError = findViewById(R.id.tv_detail_error);

        etTitle = findViewById(R.id.et_detail_title);
        etDescription = findViewById(R.id.et_detail_description);
        spSubject = findViewById(R.id.sp_detail_subject);

        readOnlyGroup = findViewById(R.id.group_readonly);
        editGroup = findViewById(R.id.group_edit);

        btnEdit = findViewById(R.id.btn_edit);
        btnSubmitReview = findViewById(R.id.btn_submit_review);
        btnDiscard = findViewById(R.id.btn_discard);
        btnPublish = findViewById(R.id.btn_publish);
        btnReject = findViewById(R.id.btn_reject);
        btnReturnToDraft = findViewById(R.id.btn_return_to_draft);
        btnHide = findViewById(R.id.btn_hide);
        btnArchive = findViewById(R.id.btn_archive);
        btnSave = findViewById(R.id.btn_save);
        btnCancel = findViewById(R.id.btn_cancel_edit);

        btnEdit.setOnClickListener(v -> viewModel.enterEditMode());
        btnSave.setOnClickListener(v -> saveEdits());
        btnCancel.setOnClickListener(v -> {
            viewModel.cancelEditMode();
            viewModel.loadDetail(examId);
        });

        btnSubmitReview.setOnClickListener(v -> viewModel.submitForReview(examId));
        btnDiscard.setOnClickListener(v -> confirmAndDiscard());
        btnPublish.setOnClickListener(v -> viewModel.publishExam(examId));
        btnReject.setOnClickListener(v -> confirmAndReject());
        btnReturnToDraft.setOnClickListener(v -> viewModel.returnToDraft(examId));
        btnHide.setOnClickListener(v -> viewModel.hideExam(examId));
        btnArchive.setOnClickListener(v -> confirmAndArchive());
    }

    private void observeViewModel() {
        viewModel.getExamDetailState().observe(this, resource -> {
            if (resource.getStatus() == Resource.Status.LOADING) {
                tvError.setVisibility(View.GONE);
                return;
            }
            if (resource.getStatus() == Resource.Status.SUCCESS && resource.getData() != null) {
                tvError.setVisibility(View.GONE);
                populateDetail(resource.getData());
                applyActionButtons(resource.getData().getStatus());
            } else if (resource.getStatus() == Resource.Status.ERROR) {
                tvError.setText(resource.getMessage());
                tvError.setVisibility(View.VISIBLE);
            }
        });

        viewModel.getEditModeState().observe(this, inEdit -> {
            if (inEdit != null && inEdit) {
                readOnlyGroup.setVisibility(View.GONE);
                editGroup.setVisibility(View.VISIBLE);
            } else {
                readOnlyGroup.setVisibility(View.VISIBLE);
                editGroup.setVisibility(View.GONE);
            }
        });

        viewModel.getActionResultState().observe(this, resource -> {
            if (resource == null) return;
            if (resource.getStatus() == Resource.Status.SUCCESS) {
                if (resource.getData() == null) {
                    Toast.makeText(this, getString(R.string.detail_action_success_discard), Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(this, getString(R.string.detail_action_success), Toast.LENGTH_SHORT).show();
                }
            } else if (resource.getStatus() == Resource.Status.ERROR) {
                Toast.makeText(this, resource.getMessage(), Toast.LENGTH_LONG).show();
            }
        });

        viewModel.getSubjectListState().observe(this, resource -> {
            if (resource.getStatus() == Resource.Status.SUCCESS && resource.getData() != null) {
                subjects = resource.getData();
                setupSubjectSpinner();
            }
        });
    }

    private void populateDetail(AdminExamResponse exam) {
        tvTitle.setText(exam.getTitle() != null ? exam.getTitle() : "");
        tvCode.setText(exam.getExamCode() != null ? exam.getExamCode() : "");
        tvDescription.setText(exam.getDescription() != null ? exam.getDescription() : "");
        tvVersion.setText(getString(R.string.detail_version_format, exam.getVersion() != null ? exam.getVersion() : 1));
        tvStatus.setText(exam.getStatus() != null ? exam.getStatus() : "");
        tvUpdatedAt.setText(exam.getUpdatedAt() != null ? exam.getUpdatedAt() : "");
        int count = exam.getQuestionCount() != null ? exam.getQuestionCount() : 0;
        tvQuestionCount.setText(getString(R.string.detail_question_count_format, count));

        if (exam.getSubjectCode() != null) {
            tvSubject.setText(exam.getSubjectCode());
        } else if (exam.getSubjectId() != null) {
            tvSubject.setText(getString(R.string.detail_subject_id_fallback, exam.getSubjectId()));
        } else {
            tvSubject.setText("");
        }

        etTitle.setText(exam.getTitle() != null ? exam.getTitle() : "");
        etDescription.setText(exam.getDescription() != null ? exam.getDescription() : "");
        selectedSubjectId = exam.getSubjectId();
        syncSubjectSpinnerSelection();
    }

    private void setupSubjectSpinner() {
        if (subjects == null || subjects.isEmpty()) return;
        String[] names = new String[subjects.size()];
        for (int i = 0; i < subjects.size(); i++) {
            SubjectResponse s = subjects.get(i);
            names[i] = s.getName() != null ? s.getName() : String.valueOf(s.getId());
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, names);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spSubject.setAdapter(adapter);
        spSubject.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedSubjectId = subjects.get(position).getId();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        syncSubjectSpinnerSelection();
    }

    private void syncSubjectSpinnerSelection() {
        if (subjects == null || selectedSubjectId == null) return;
        for (int i = 0; i < subjects.size(); i++) {
            if (selectedSubjectId.equals(subjects.get(i).getId())) {
                spSubject.setSelection(i);
                return;
            }
        }
    }

    private void applyActionButtons(String status) {
        List<String> actions = AdminExamViewModel.availableActionsForStatus(status);
        btnEdit.setVisibility(actions.contains("EDIT") ? View.VISIBLE : View.GONE);
        btnSubmitReview.setVisibility(actions.contains("SUBMIT_FOR_REVIEW") ? View.VISIBLE : View.GONE);
        btnDiscard.setVisibility(actions.contains("DISCARD") ? View.VISIBLE : View.GONE);
        btnPublish.setVisibility(actions.contains("PUBLISH") ? View.VISIBLE : View.GONE);
        btnReject.setVisibility(actions.contains("REJECT") ? View.VISIBLE : View.GONE);
        btnReturnToDraft.setVisibility(actions.contains("RETURN_TO_DRAFT") ? View.VISIBLE : View.GONE);
        btnHide.setVisibility(actions.contains("HIDE") ? View.VISIBLE : View.GONE);
        btnArchive.setVisibility(actions.contains("ARCHIVE") ? View.VISIBLE : View.GONE);
    }

    private void saveEdits() {
        String title = etTitle.getText().toString().trim();
        String description = etDescription.getText().toString().trim();
        AdminExamUpdateRequest request = new AdminExamUpdateRequest(
                title.isEmpty() ? null : title,
                selectedSubjectId,
                description.isEmpty() ? null : description,
                null, null, null, null, null
        );
        viewModel.saveEdit(examId, request);
    }

    private void confirmAndDiscard() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.detail_confirm_discard_title)
                .setMessage(R.string.detail_confirm_discard_msg)
                .setPositiveButton(R.string.detail_confirm_yes, (d, w) -> viewModel.discardDraft(examId))
                .setNegativeButton(R.string.detail_confirm_no, null)
                .show();
    }

    private void confirmAndReject() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.detail_confirm_reject_title)
                .setMessage(R.string.detail_confirm_reject_msg)
                .setPositiveButton(R.string.detail_confirm_yes, (d, w) -> viewModel.rejectReview(examId))
                .setNegativeButton(R.string.detail_confirm_no, null)
                .show();
    }

    private void confirmAndArchive() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.detail_confirm_archive_title)
                .setMessage(R.string.detail_confirm_archive_msg)
                .setPositiveButton(R.string.detail_confirm_yes, (d, w) -> viewModel.archiveExam(examId))
                .setNegativeButton(R.string.detail_confirm_no, null)
                .show();
    }
}
