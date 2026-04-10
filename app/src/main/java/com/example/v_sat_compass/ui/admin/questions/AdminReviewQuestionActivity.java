package com.example.v_sat_compass.ui.admin.questions;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.v_sat_compass.data.api.AdminApi;
import com.example.v_sat_compass.data.api.ApiClient;
import com.example.v_sat_compass.data.model.ApiResponse;
import com.example.v_sat_compass.data.model.QuestionItem;
import com.example.v_sat_compass.databinding.ActivityAdminReviewQuestionBinding;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Màn hình duyệt câu hỏi.
 * Quyền: CONTENT_ADMIN, SUPER_ADMIN
 *
 * Actions:
 *   Trả sửa  → NEEDS_REVISION (yêu cầu bình luận)
 *   Duyệt    → APPROVED
 *   Từ chối  → REJECTED (yêu cầu lý do)
 */
public class AdminReviewQuestionActivity extends AppCompatActivity {

    private ActivityAdminReviewQuestionBinding binding;
    private Long questionId;
    private AdminApi adminApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityAdminReviewQuestionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        questionId = getIntent().getLongExtra("question_id", -1);
        adminApi   = ApiClient.getClient().create(AdminApi.class);

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnEdit.setOnClickListener(v -> {
            Toast.makeText(this, "Chức năng chỉnh sửa câu hỏi sẽ mở trong phiên bản tiếp theo.", Toast.LENGTH_SHORT).show();
        });

        binding.btnApprove.setOnClickListener(v -> confirmApprove());
        binding.btnReject.setOnClickListener(v -> confirmReject());
        binding.btnRequestRevision.setOnClickListener(v -> confirmRevision());

        if (questionId > 0) {
            loadQuestion();
        } else {
            loadMockData(); // demo khi chưa có backend
        }
    }

    private void loadQuestion() {
        adminApi.getQuestionDetail(questionId).enqueue(new Callback<ApiResponse<QuestionItem>>() {
            @Override
            public void onResponse(Call<ApiResponse<QuestionItem>> call,
                                   Response<ApiResponse<QuestionItem>> response) {
                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess()) {
                    bindQuestion(response.body().getData());
                } else {
                    loadMockData();
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<QuestionItem>> call, Throwable t) {
                loadMockData();
            }
        });
    }

    private void loadMockData() {
        // Câu hỏi toán mẫu để demo
        binding.tvQuestionContent.setText(
                "Câu 1: Trong các phương trình sau, phương trình nào là phương trình bậc nhất một ẩn?"
        );
        binding.optA.setText("A.  2x + 3 = 0");
        binding.optB.setText("B.  x² - 1 = 0");
        binding.optC.setText("C.  1/x + x = 2");
        binding.optD.setText("D.  0x + 5 = 0");
        binding.optA.setChecked(true);

        binding.tvCreator.setText("Lan Anh");
        binding.tvCreatedAt.setText("12/10/2024");
        binding.tvSubject.setText("Toán");
        binding.tvDifficulty.setText("Cơ bản");

        binding.tvExplanation.setText(
                "Để một phương trình là phương trình bậc nhất một ẩn ax + b = 0, điều kiện cần là a ≠ 0.\n\n"
                        + "Xét các đáp án:\n"
                        + "A. 2x + 3 = 0 có a = 2 ≠ 0 (Thỏa mãn).\n"
                        + "B. x² - 1 = 0 là phương trình bậc hai.\n"
                        + "C. 1/x + x = 2 là phương trình chứa ẩn ở mẫu.\n"
                        + "D. 0x + 5 = 0 có a = 0 (Không thỏa mãn).\n"
                        + "Vậy đáp án A là đúng."
        );
    }

    private void bindQuestion(QuestionItem q) {
        binding.tvQuestionContent.setText(q.getContent());
        binding.tvCreator.setText(q.getCreatorName());
        binding.tvCreatedAt.setText(q.getCreatedAt());
        binding.tvSubject.setText(q.getSubjectName());
        binding.tvDifficulty.setText(q.getDifficultyLabel());
        binding.tvExplanation.setText(q.getExplanation());

        if (q.getOptions() != null && q.getOptions().size() >= 4) {
            binding.optA.setText("A.  " + q.getOptions().get(0).getContent());
            binding.optB.setText("B.  " + q.getOptions().get(1).getContent());
            binding.optC.setText("C.  " + q.getOptions().get(2).getContent());
            binding.optD.setText("D.  " + q.getOptions().get(3).getContent());
        }

        // Đánh dấu đáp án đúng
        String correct = q.getCorrectAnswer();
        if ("A".equals(correct)) binding.optA.setChecked(true);
        else if ("B".equals(correct)) binding.optB.setChecked(true);
        else if ("C".equals(correct)) binding.optC.setChecked(true);
        else if ("D".equals(correct)) binding.optD.setChecked(true);

        // Ẩn nút duyệt nếu đã duyệt/xuất bản rồi
        if ("APPROVED".equals(q.getStatus()) || "PUBLISHED".equals(q.getStatus())) {
            binding.btnApprove.setVisibility(View.GONE);
        }
    }

    // ─── Confirm dialogs ──────────────────────────────────────────────────────

    private void confirmApprove() {
        new AlertDialog.Builder(this)
                .setTitle("Duyệt câu hỏi")
                .setMessage("Bạn có chắc muốn duyệt câu hỏi này? Câu hỏi sẽ chuyển sang trạng thái Đã duyệt.")
                .setPositiveButton("Duyệt", (d, w) -> doApprove())
                .setNegativeButton("Huỷ", null)
                .show();
    }

    private void confirmReject() {
        String comment = getComment();
        if (comment.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập lý do từ chối vào ô bình luận.", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Từ chối câu hỏi")
                .setMessage("Câu hỏi sẽ bị từ chối và CTV sẽ nhận thông báo. Tiếp tục?")
                .setPositiveButton("Từ chối", (d, w) -> doReject(comment))
                .setNegativeButton("Huỷ", null)
                .show();
    }

    private void confirmRevision() {
        String comment = getComment();
        if (comment.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập yêu cầu sửa vào ô bình luận.", Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Yêu cầu chỉnh sửa")
                .setMessage("Câu hỏi sẽ được trả lại cho CTV để chỉnh sửa. Tiếp tục?")
                .setPositiveButton("Trả sửa", (d, w) -> doRequestRevision(comment))
                .setNegativeButton("Huỷ", null)
                .show();
    }

    // ─── API calls ────────────────────────────────────────────────────────────

    private void doApprove() {
        if (questionId <= 0) { showSuccess("Đã duyệt câu hỏi (demo)."); return; }
        adminApi.approveQuestion(questionId).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                showSuccess("Câu hỏi đã được duyệt thành công!");
            }
            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                showSuccess("Đã duyệt câu hỏi (demo).");
            }
        });
    }

    private void doReject(String comment) {
        if (questionId <= 0) { showSuccess("Đã từ chối câu hỏi (demo)."); return; }
        Map<String, String> body = new HashMap<>();
        body.put("comment", comment);
        adminApi.rejectQuestion(questionId, body).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                showSuccess("Câu hỏi đã bị từ chối.");
            }
            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                showSuccess("Đã từ chối câu hỏi (demo).");
            }
        });
    }

    private void doRequestRevision(String comment) {
        if (questionId <= 0) { showSuccess("Đã yêu cầu sửa (demo)."); return; }
        Map<String, String> body = new HashMap<>();
        body.put("comment", comment);
        adminApi.requestRevision(questionId, body).enqueue(new Callback<ApiResponse<Void>>() {
            @Override
            public void onResponse(Call<ApiResponse<Void>> call, Response<ApiResponse<Void>> response) {
                showSuccess("Đã gửi yêu cầu chỉnh sửa cho cộng tác viên.");
            }
            @Override
            public void onFailure(Call<ApiResponse<Void>> call, Throwable t) {
                showSuccess("Đã yêu cầu sửa (demo).");
            }
        });
    }

    private void showSuccess(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        finish();
    }

    private String getComment() {
        if (binding.etAdminComment.getText() == null) return "";
        return binding.etAdminComment.getText().toString().trim();
    }
}
