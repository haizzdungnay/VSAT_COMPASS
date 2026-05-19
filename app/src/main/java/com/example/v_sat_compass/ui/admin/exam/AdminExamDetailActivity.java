package com.example.v_sat_compass.ui.admin.exam;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class AdminExamDetailActivity extends AppCompatActivity {

    public static final String EXTRA_EXAM_ID = "exam_id";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        long examId = getIntent().getLongExtra(EXTRA_EXAM_ID, -1L);
        Toast.makeText(this, "Chi tiết đề thi ID=" + examId + " — C1.2b-C", Toast.LENGTH_LONG).show();
        finish();
    }
}
