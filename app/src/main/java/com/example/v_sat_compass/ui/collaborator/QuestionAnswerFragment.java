package com.example.v_sat_compass.ui.collaborator;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.v_sat_compass.R;

/** Tab "Đáp án" - chọn đáp án đúng và nhập lời giải */
public class QuestionAnswerFragment extends Fragment {
    public static QuestionAnswerFragment newInstance() { return new QuestionAnswerFragment(); }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Reuse layout trang nội dung (có phần đáp án) — đơn giản hoá cho phase 1
        return inflater.inflate(R.layout.fragment_question_editor_content, container, false);
    }
}
