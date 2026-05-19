package com.example.v_sat_compass.ui.admin.exam;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.v_sat_compass.R;
import com.example.v_sat_compass.data.model.SubjectResponse;
import com.example.v_sat_compass.data.model.admin.AdminExamCreateRequest;
import com.example.v_sat_compass.data.repository.Resource;
import com.example.v_sat_compass.databinding.ActivityAdminCreateExamBinding;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Tạo đề thi mới — DRAFT hoặc PENDING_REVIEW.
 * Quyền: CONTENT_ADMIN, SUPER_ADMIN
 *
 * Luồng 2 bước:
 *   1. Điền thông tin đề (tên, mã ^[A-Z][A-Z0-9_]{2,49}$, môn từ API, thời gian, mức độ)
 *   2a. Lưu nháp → POST /admin/exams (tạo DRAFT)
 *   2b. Gửi duyệt → POST /admin/exams rồi POST /admin/exams/{id}/submit-review
 *
 * Câu hỏi/cấu trúc đề sẽ được thêm trong phase C1.2b-D.
 */
public class AdminCreateExamActivity extends AppCompatActivity {

    private static final Pattern EXAM_CODE_PATTERN =
            Pattern.compile("^[A-Z][A-Z0-9_]{2,49}$");

    private ActivityAdminCreateExamBinding binding;
    private AdminExamViewModel viewModel;
    private String selectedLevel = "MEDIUM";

    private final List<SubjectResponse> subjectList = new ArrayList<>();
    private Long selectedSubjectId = null;
    private boolean pendingSubmitReview = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminCreateExamBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(AdminExamViewModel.class);

        binding.btnBack.setOnClickListener(v -> finish());

        setupDurationSeekBar();
        setupLevelButtons();
        setupFeeSwitch();
        setupActionButtons();
        observeViewModel();

        viewModel.loadSubjects();
    }

    private void observeViewModel() {
        viewModel.getSubjectListState().observe(this, resource -> {
            if (resource == null) return;
            if (resource.getStatus() == Resource.Status.SUCCESS && resource.getData() != null) {
                populateSubjectDropdown(resource.getData());
            } else if (resource.getStatus() == Resource.Status.ERROR) {
                binding.actvSubject.setHint("Lỗi tải môn học");
            }
        });

        viewModel.getCreateState().observe(this, resource -> {
            if (resource == null) return;
            switch (resource.getStatus()) {
                case LOADING:
                    binding.btnSaveDraft.setEnabled(false);
                    binding.btnSubmitForReview.setEnabled(false);
                    break;
                case SUCCESS:
                    binding.btnSaveDraft.setEnabled(true);
                    binding.btnSubmitForReview.setEnabled(true);
                    if (resource.getData() != null) {
                        Long examId = resource.getData().getId();
                        if (pendingSubmitReview && examId != null) {
                            pendingSubmitReview = false;
                            viewModel.submitExam(examId);
                        } else {
                            pendingSubmitReview = false;
                            Toast.makeText(AdminCreateExamActivity.this,
                                    "Đã lưu nháp đề thi.", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }
                    break;
                case ERROR:
                    binding.btnSaveDraft.setEnabled(true);
                    binding.btnSubmitForReview.setEnabled(true);
                    pendingSubmitReview = false;
                    showError(resource.getMessage());
                    break;
            }
        });

        viewModel.getSubmitReviewState().observe(this, resource -> {
            if (resource == null) return;
            if (resource.getStatus() == Resource.Status.SUCCESS) {
                Toast.makeText(AdminCreateExamActivity.this,
                        "Đề thi đã được gửi duyệt thành công!", Toast.LENGTH_SHORT).show();
                finish();
            } else if (resource.getStatus() == Resource.Status.ERROR) {
                showError(resource.getMessage());
            }
        });
    }

    private void populateSubjectDropdown(List<SubjectResponse> subjects) {
        subjectList.clear();
        subjectList.addAll(subjects);
        List<String> names = new ArrayList<>();
        for (SubjectResponse s : subjects) {
            names.add(s.getName() != null ? s.getName() : s.getCode());
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, names);
        binding.actvSubject.setAdapter(adapter);
        if (!subjects.isEmpty()) {
            binding.actvSubject.setText(names.get(0), false);
            selectedSubjectId = subjects.get(0).getId();
        }
        binding.actvSubject.setOnItemClickListener((parent, view, position, id) -> {
            if (position < subjectList.size()) {
                selectedSubjectId = subjectList.get(position).getId();
            }
        });
    }

    private void setupDurationSeekBar() {
        binding.seekDuration.setProgress(90);
        binding.tvDuration.setText("90");
        binding.seekDuration.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                int value = Math.max(30, p);
                binding.tvDuration.setText(String.valueOf(value));
            }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });
    }

    private void setupLevelButtons() {
        binding.btnLevelEasy.setOnClickListener(v -> selectLevel("EASY"));
        binding.btnLevelMedium.setOnClickListener(v -> selectLevel("MEDIUM"));
        binding.btnLevelHard.setOnClickListener(v -> selectLevel("HARD"));
        selectLevel("MEDIUM");
    }

    private void selectLevel(String level) {
        selectedLevel = level;
        resetLevelButton(binding.btnLevelEasy);
        resetLevelButton(binding.btnLevelMedium);
        resetLevelButton(binding.btnLevelHard);

        android.widget.TextView selected = "EASY".equals(level) ? binding.btnLevelEasy
                : "HARD".equals(level) ? binding.btnLevelHard
                : binding.btnLevelMedium;
        selected.setBackgroundResource(R.drawable.bg_chip_selected);
        selected.setTextColor(ContextCompat.getColor(this, R.color.white));
    }

    private void resetLevelButton(android.widget.TextView btn) {
        btn.setBackgroundResource(R.drawable.bg_chip_unselected);
        btn.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
    }

    private void setupFeeSwitch() {
        binding.switchPaid.setChecked(false);
        binding.layoutPrice.setVisibility(android.view.View.GONE);
        binding.switchPaid.setOnCheckedChangeListener((btn, checked) ->
                binding.layoutPrice.setVisibility(checked
                        ? android.view.View.VISIBLE : android.view.View.GONE));
    }

    private void setupActionButtons() {
        binding.btnSaveDraft.setOnClickListener(v -> saveExam(false));
        binding.btnSubmitForReview.setOnClickListener(v -> saveExam(true));
    }

    private void saveExam(boolean submit) {
        String title = binding.etExamTitle.getText() != null
                ? binding.etExamTitle.getText().toString().trim() : "";
        if (title.isEmpty()) {
            binding.etExamTitle.setError("Vui lòng nhập tên đề");
            return;
        }

        String examCode = binding.etExamCode.getText() != null
                ? binding.etExamCode.getText().toString().trim() : "";
        if (!examCode.isEmpty() && !EXAM_CODE_PATTERN.matcher(examCode).matches()) {
            binding.etExamCode.setError("Mã đề phải khớp: ^[A-Z][A-Z0-9_]{2,49}$");
            return;
        }

        if (selectedSubjectId == null) {
            Toast.makeText(this, "Vui lòng chọn môn học.", Toast.LENGTH_SHORT).show();
            return;
        }

        int duration = Math.max(30, binding.seekDuration.getProgress());

        boolean isPaid = binding.switchPaid.isChecked();
        String pricingType = isPaid ? "PAID_ONCE" : "FREE";
        BigDecimal price = BigDecimal.ZERO;
        if (isPaid && binding.etPrice.getText() != null
                && !TextUtils.isEmpty(binding.etPrice.getText().toString().trim())) {
            try {
                price = new BigDecimal(binding.etPrice.getText().toString().trim());
            } catch (NumberFormatException ignored) {
                price = BigDecimal.ZERO;
            }
        }

        AdminExamCreateRequest request = new AdminExamCreateRequest(
                examCode.isEmpty() ? null : examCode,
                title,
                selectedSubjectId,
                "",
                duration,
                selectedLevel,
                pricingType,
                price,
                null
        );

        pendingSubmitReview = submit;
        viewModel.createExam(request);
    }

    private void showError(String message) {
        new AlertDialog.Builder(this)
                .setTitle("Lỗi")
                .setMessage(message != null ? message : "Đã xảy ra lỗi. Vui lòng thử lại.")
                .setPositiveButton("OK", null)
                .show();
    }
}
