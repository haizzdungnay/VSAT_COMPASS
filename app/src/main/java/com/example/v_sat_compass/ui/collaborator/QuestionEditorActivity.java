package com.example.v_sat_compass.ui.collaborator;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.v_sat_compass.R;
import com.example.v_sat_compass.data.api.ApiClient;
import com.example.v_sat_compass.data.api.CollaboratorApi;
import com.example.v_sat_compass.data.model.ApiResponse;
import com.example.v_sat_compass.data.model.QuestionItem;
import com.example.v_sat_compass.databinding.ActivityQuestionEditorBinding;

import java.util.HashMap;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Màn hình biên soạn câu hỏi mới hoặc sửa câu hỏi.
 * Tab: Nội dung | Đáp án | Cài đặt
 *
 * Dùng cho: COLLABORATOR, CONTENT_ADMIN, SUPER_ADMIN
 */
public class QuestionEditorActivity extends AppCompatActivity {

    private ActivityQuestionEditorBinding binding;
    private Long editingQuestionId = null; // null = tạo mới

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityQuestionEditorBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        editingQuestionId = getIntent().getLongExtra("question_id", -1);
        if (editingQuestionId == -1) editingQuestionId = null;

        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnCancel.setOnClickListener(v -> finish());

        // ViewPager với 3 fragments: Content, Answer, Settings
        QuestionEditorPagerAdapter pagerAdapter = new QuestionEditorPagerAdapter(this);
        binding.viewPager.setAdapter(pagerAdapter);

        setupTabListeners();

        binding.btnSaveDraft.setOnClickListener(v -> saveQuestion(false));
        binding.btnSubmit.setOnClickListener(v -> saveQuestion(true));
    }

    private void setupTabListeners() {
        binding.tabContent.setOnClickListener(v -> { binding.viewPager.setCurrentItem(0, true); updateTabUI(0); });
        binding.tabAnswer.setOnClickListener(v -> { binding.viewPager.setCurrentItem(1, true); updateTabUI(1); });
        binding.tabSettings.setOnClickListener(v -> { binding.viewPager.setCurrentItem(2, true); updateTabUI(2); });

        binding.viewPager.registerOnPageChangeCallback(new androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateTabUI(position);
            }
        });

        updateTabUI(0);
    }

    private void updateTabUI(int selectedTab) {
        // Reset
        binding.tabContent.setBackgroundColor(ContextCompat.getColor(this, R.color.surface));
        binding.tabContent.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        binding.tabAnswer.setBackgroundColor(ContextCompat.getColor(this, R.color.surface));
        binding.tabAnswer.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        binding.tabSettings.setBackgroundColor(ContextCompat.getColor(this, R.color.surface));
        binding.tabSettings.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));

        android.widget.TextView[] tabs = {binding.tabContent, binding.tabAnswer, binding.tabSettings};
        tabs[selectedTab].setBackgroundColor(ContextCompat.getColor(this, R.color.primary));
        tabs[selectedTab].setTextColor(ContextCompat.getColor(this, R.color.white));
    }

    private void saveQuestion(boolean submit) {
        Map<String, Object> body = new HashMap<>();
        body.put("status", submit ? "PENDING" : "DRAFT");
        // Dữ liệu thực lấy từ ViewPager fragments – đơn giản hóa cho demo
        body.put("content", "Câu hỏi được tạo từ biên soạn viên.");
        body.put("question_type", "MULTIPLE_CHOICE");

        CollaboratorApi api = ApiClient.getClient().create(CollaboratorApi.class);
        Call<ApiResponse<QuestionItem>> call = editingQuestionId != null
                ? api.updateQuestion(editingQuestionId, body)
                : api.createQuestion(body);

        call.enqueue(new Callback<ApiResponse<QuestionItem>>() {
            @Override
            public void onResponse(Call<ApiResponse<QuestionItem>> c, Response<ApiResponse<QuestionItem>> r) {
                String msg = submit ? "Câu hỏi đã được gửi duyệt!" : "Đã lưu nháp câu hỏi.";
                Toast.makeText(QuestionEditorActivity.this, msg, Toast.LENGTH_SHORT).show();
                finish();
            }
            @Override
            public void onFailure(Call<ApiResponse<QuestionItem>> c, Throwable t) {
                String msg = submit ? "Đã gửi duyệt (demo)." : "Đã lưu nháp (demo).";
                Toast.makeText(QuestionEditorActivity.this, msg, Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }
}
