package com.example.v_sat_compass.ui.admin.exam;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.v_sat_compass.R;
import com.example.v_sat_compass.data.api.AdminApi;
import com.example.v_sat_compass.data.api.ApiClient;
import com.example.v_sat_compass.data.model.ApiResponse;
import com.example.v_sat_compass.data.model.Exam;
import com.example.v_sat_compass.data.model.ExamStructureQuestion;
import com.example.v_sat_compass.databinding.ActivityAdminCreateExamBinding;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Tạo đề thi mới.
 * Quyền: CONTENT_ADMIN, SUPER_ADMIN
 *
 * Luồng:
 *   1. Điền thông tin đề (tên, mã, môn, thời gian, mức độ)
 *   2. Thêm câu hỏi từ ngân hàng
 *   3. Thiết lập phí (miễn phí / trả phí)
 *   4. Lưu nháp hoặc Gửi duyệt
 *
 * Lưu ý VSAT Toán: 50 câu × 3 điểm = 150 điểm, 90 phút.
 */
public class AdminCreateExamActivity extends AppCompatActivity {

    private ActivityAdminCreateExamBinding binding;
    private ExamStructureAdapter structureAdapter;
    private String selectedLevel = "MEDIUM";

    // Điểm mỗi câu theo môn:
    // Toán: 3 điểm/câu (tổng 150 = 50 câu × 3)
    // Tiếng Anh: 3 điểm/câu (tổng 150 = 50 câu × 3)
    private static final int POINTS_PER_QUESTION = 3;
    private static final int MAX_SCORE = 150;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminCreateExamBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());

        setupSubjectDropdown();
        setupDurationSeekBar();
        setupLevelButtons();
        setupStructureList();
        setupFeeSwitch();
        setupActionButtons();
    }

    private void setupSubjectDropdown() {
        String[] subjects = {"Toán học", "Tiếng Anh", "Vật lý", "Hóa học", "Sinh học"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, subjects);
        binding.actvSubject.setAdapter(adapter);
        binding.actvSubject.setText("Toán học", false);
    }

    private void setupDurationSeekBar() {
        binding.seekDuration.setProgress(90); // default 90 phút (VSAT chuẩn)
        binding.tvDuration.setText("90");
        binding.seekDuration.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean fromUser) {
                int value = Math.max(30, p); // min 30 phút
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

    private void setupStructureList() {
        structureAdapter = new ExamStructureAdapter();
        binding.rvStructure.setLayoutManager(new LinearLayoutManager(this));
        binding.rvStructure.setAdapter(structureAdapter);
        binding.rvStructure.setNestedScrollingEnabled(false);

        structureAdapter.setOnRemoveListener(position -> {
            structureAdapter.removeAt(position);
            updateTotalPoints();
        });

        // Nút thêm câu hỏi: mở dialog pick câu từ ngân hàng
        binding.btnAddQuestion.setOnClickListener(v -> showAddQuestionDialog());
        updateTotalPoints();
    }

    private void showAddQuestionDialog() {
        int current = structureAdapter.getTotalQuestions();
        if (current >= 50) {
            Toast.makeText(this, "Đề VSAT tối đa 50 câu.", Toast.LENGTH_SHORT).show();
            return;
        }

        // TODO: Mở màn hình chọn câu từ ngân hàng (phase 2)
        // Tạm thời thêm câu mẫu để demo
        ExamStructureQuestion demo = new ExamStructureQuestion(
                null,
                "MATH-V5-" + String.format("%03d", current + 1),
                "Câu " + (current + 1) + ": Phương trình bậc ha...",
                "MULTIPLE_CHOICE",
                current + 1,
                POINTS_PER_QUESTION
        );
        structureAdapter.addQuestion(demo);
        updateTotalPoints();
    }

    private void updateTotalPoints() {
        int count  = structureAdapter.getTotalQuestions();
        int points = count * POINTS_PER_QUESTION;
        binding.tvTotalPoints.setText(points + " / " + MAX_SCORE + " điểm  (" + count + " câu)");

        if (points > MAX_SCORE) {
            binding.tvTotalPoints.setTextColor(ContextCompat.getColor(this, R.color.error));
        } else if (points == MAX_SCORE) {
            binding.tvTotalPoints.setTextColor(ContextCompat.getColor(this, R.color.success));
        } else {
            binding.tvTotalPoints.setTextColor(ContextCompat.getColor(this, R.color.primary));
        }
    }

    private void setupFeeSwitch() {
        binding.switchPaid.setChecked(true);
        binding.layoutPrice.setVisibility(android.view.View.VISIBLE);
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

        String subject = binding.actvSubject.getText().toString();
        int duration = binding.seekDuration.getProgress();
        boolean isPaid = binding.switchPaid.isChecked();

        List<ExamStructureQuestion> questions = structureAdapter.getItems();
        if (submit && questions.isEmpty()) {
            Toast.makeText(this, "Vui lòng thêm ít nhất 1 câu hỏi trước khi gửi duyệt.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> body = new HashMap<>();
        body.put("title", title);
        body.put("exam_code", binding.etExamCode.getText() != null
                ? binding.etExamCode.getText().toString() : "");
        body.put("subject_name", subject);
        body.put("duration_minutes", duration);
        body.put("difficulty", selectedLevel);
        body.put("is_paid", isPaid);
        body.put("status", submit ? "PENDING" : "DRAFT");
        body.put("total_questions", questions.size());
        body.put("max_score", MAX_SCORE);

        AdminApi api = ApiClient.getClient().create(AdminApi.class);
        api.createExam(body).enqueue(new Callback<ApiResponse<Exam>>() {
            @Override
            public void onResponse(Call<ApiResponse<Exam>> call, Response<ApiResponse<Exam>> response) {
                String msg = submit
                        ? "Đề thi đã được gửi duyệt thành công!"
                        : "Đã lưu nháp đề thi.";
                Toast.makeText(AdminCreateExamActivity.this, msg, Toast.LENGTH_SHORT).show();
                finish();
            }
            @Override
            public void onFailure(Call<ApiResponse<Exam>> call, Throwable t) {
                // Giả lập thành công khi offline
                String msg = submit ? "Đề thi đã được gửi duyệt (demo)." : "Đã lưu nháp (demo).";
                Toast.makeText(AdminCreateExamActivity.this, msg, Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
}
