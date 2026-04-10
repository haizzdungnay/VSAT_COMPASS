package com.example.v_sat_compass.ui.collaborator;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.textfield.TextInputLayout;

/** Tab "Cài đặt" — môn, chủ đề, mức độ, loại câu hỏi */
public class QuestionSettingsFragment extends Fragment {

    public static QuestionSettingsFragment newInstance() { return new QuestionSettingsFragment(); }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Layout đơn giản tạo inline
        android.widget.ScrollView sv = new android.widget.ScrollView(requireContext());
        android.widget.LinearLayout ll = new android.widget.LinearLayout(requireContext());
        ll.setOrientation(android.widget.LinearLayout.VERTICAL);
        ll.setPadding(48, 48, 48, 48);

        // Môn học
        TextInputLayout subjectLayout = new TextInputLayout(requireContext(),
                null, com.google.android.material.R.style.Widget_MaterialComponents_TextInputLayout_OutlinedBox_ExposedDropdownMenu);
        subjectLayout.setHint("Môn học");
        AutoCompleteTextView subjectAc = new AutoCompleteTextView(requireContext());
        subjectAc.setText("Toán học", false);
        ArrayAdapter<String> subjectAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                new String[]{"Toán học", "Tiếng Anh", "Vật lý", "Hóa học"});
        subjectAc.setAdapter(subjectAdapter);
        subjectLayout.addView(subjectAc);
        android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(
                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = 32;
        ll.addView(subjectLayout, lp);

        // Mức độ
        TextInputLayout levelLayout = new TextInputLayout(requireContext(),
                null, com.google.android.material.R.style.Widget_MaterialComponents_TextInputLayout_OutlinedBox_ExposedDropdownMenu);
        levelLayout.setHint("Mức độ");
        AutoCompleteTextView levelAc = new AutoCompleteTextView(requireContext());
        levelAc.setText("Trung bình", false);
        ArrayAdapter<String> levelAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                new String[]{"Dễ", "Trung bình", "Khó"});
        levelAc.setAdapter(levelAdapter);
        levelLayout.addView(levelAc);
        ll.addView(levelLayout, lp);

        // Loại câu hỏi
        TextInputLayout typeLayout = new TextInputLayout(requireContext(),
                null, com.google.android.material.R.style.Widget_MaterialComponents_TextInputLayout_OutlinedBox_ExposedDropdownMenu);
        typeLayout.setHint("Loại câu hỏi");
        AutoCompleteTextView typeAc = new AutoCompleteTextView(requireContext());
        typeAc.setText("Trắc nghiệm", false);
        ArrayAdapter<String> typeAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                new String[]{"Trắc nghiệm", "Tự luận"});
        typeAc.setAdapter(typeAdapter);
        typeLayout.addView(typeAc);
        ll.addView(typeLayout, lp);

        sv.addView(ll);
        return sv;
    }
}
