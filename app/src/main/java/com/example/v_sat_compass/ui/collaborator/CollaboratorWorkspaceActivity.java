package com.example.v_sat_compass.ui.collaborator;

import android.content.Intent;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.v_sat_compass.R;
import com.example.v_sat_compass.databinding.ActivityCollaboratorWorkspaceBinding;

/**
 * Không gian làm việc của Cộng tác viên (CTV).
 * Hiển thị: thống kê câu hỏi + phản hồi từ Admin.
 */
public class CollaboratorWorkspaceActivity extends AppCompatActivity {

    private ActivityCollaboratorWorkspaceBinding binding;
    private ActivityResultLauncher<Intent> editorLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityCollaboratorWorkspaceBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        editorLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> refreshQuestionList()
        );

        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(
                            R.id.collaboratorQuestionListContainer,
                            new CollaboratorQuestionListFragment()
                    )
                    .commit();
        }

        binding.btnBack.setOnClickListener(v -> finish());

        binding.fabCreateQuestion.setOnClickListener(v ->
                editorLauncher.launch(new Intent(this, QuestionEditorActivity.class)));
    }

    private void refreshQuestionList() {
        Fragment fragment = getSupportFragmentManager()
                .findFragmentById(R.id.collaboratorQuestionListContainer);
        if (fragment instanceof CollaboratorQuestionListFragment) {
            ((CollaboratorQuestionListFragment) fragment).refreshCurrentFilter();
        }
    }
}
