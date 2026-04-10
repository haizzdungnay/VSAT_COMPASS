package com.example.v_sat_compass.ui.collaborator;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.v_sat_compass.databinding.ActivityCollaboratorWorkspaceBinding;

/**
 * Không gian làm việc của Cộng tác viên (CTV).
 * Hiển thị: thống kê câu hỏi + phản hồi từ Admin.
 */
public class CollaboratorWorkspaceActivity extends AppCompatActivity {

    private ActivityCollaboratorWorkspaceBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCollaboratorWorkspaceBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnBack.setOnClickListener(v -> finish());

        binding.fabCreateQuestion.setOnClickListener(v ->
                startActivity(new Intent(this, QuestionEditorActivity.class)));
    }
}
