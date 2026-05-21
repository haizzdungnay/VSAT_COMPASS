package com.example.v_sat_compass.ui.collaborator;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

@Deprecated
// Legacy editor replaced by CollaboratorCreateQuestionActivity in C1.3-C1.
// Kept as redirect to avoid breaking AdminDashboardFragment +
// AdminQuestionBankFragment imports. Remove when admin scope is updated.
public class QuestionEditorActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startActivity(new Intent(this, CollaboratorCreateQuestionActivity.class));
        finish();
    }
}
