package com.example.v_sat_compass.ui.exam;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.v_sat_compass.R;
import com.example.v_sat_compass.data.api.ApiClient;
import com.example.v_sat_compass.data.api.ExamApi;
import com.example.v_sat_compass.data.local.LocalExamDataSource;
import com.example.v_sat_compass.data.model.ApiResponse;
import com.example.v_sat_compass.data.model.Exam;
import com.example.v_sat_compass.databinding.FragmentExamBinding;
import com.example.v_sat_compass.ui.exam.adapter.ExamAdapter;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ExamFragment extends Fragment {

    private FragmentExamBinding binding;
    private ExamAdapter adapter;
    private List<Exam> allExams = new ArrayList<>();
    private String selectedSubject = null; // null = all
    private String searchQuery = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentExamBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adapter = new ExamAdapter();
        binding.rvExams.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvExams.setAdapter(adapter);

        adapter.setOnExamClickListener(exam -> navigateToDetail(exam));

        setupChipFilters();
        setupSearch();

        binding.swipeRefresh.setOnRefreshListener(this::loadExams);
        loadExams();
    }

    private void setupChipFilters() {
        binding.chipAll.setOnClickListener(v -> selectChip(null));
        binding.chipMath.setOnClickListener(v -> selectChip("Toán"));
        binding.chipEnglish.setOnClickListener(v -> selectChip("Tiếng Anh"));
        binding.chipPhysics.setOnClickListener(v -> selectChip("Vật lí"));
        binding.chipChemistry.setOnClickListener(v -> selectChip("Hóa học"));
    }

    private void selectChip(String subject) {
        selectedSubject = subject;

        // Reset all chips to unselected style
        updateChipStyle(binding.chipAll, subject == null);
        updateChipStyle(binding.chipMath, "Toán".equals(subject));
        updateChipStyle(binding.chipEnglish, "Tiếng Anh".equals(subject));
        updateChipStyle(binding.chipPhysics, "Vật lí".equals(subject));
        updateChipStyle(binding.chipChemistry, "Hóa học".equals(subject));

        applyFilters();
    }

    private void updateChipStyle(TextView chip, boolean selected) {
        if (selected) {
            chip.setBackgroundResource(R.drawable.bg_chip_selected);
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
        } else {
            chip.setBackgroundResource(R.drawable.bg_chip_unselected);
            chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
        }
    }

    private void setupSearch() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().trim().toLowerCase();
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void applyFilters() {
        List<Exam> filtered = new ArrayList<>();
        for (Exam exam : allExams) {
            // Subject filter
            boolean subjectMatch = selectedSubject == null ||
                    (exam.getSubjectName() != null &&
                            exam.getSubjectName().toLowerCase().contains(selectedSubject.toLowerCase()));

            // Search filter
            boolean searchMatch = searchQuery.isEmpty() ||
                    (exam.getTitle() != null && exam.getTitle().toLowerCase().contains(searchQuery)) ||
                    (exam.getSubjectName() != null && exam.getSubjectName().toLowerCase().contains(searchQuery));

            if (subjectMatch && searchMatch) {
                filtered.add(exam);
            }
        }

        adapter.setExams(filtered);

        if (filtered.isEmpty()) {
            binding.tvEmpty.setVisibility(View.VISIBLE);
            binding.tvEmpty.setText(searchQuery.isEmpty() ? "Không có đề thi nào" : "Không tìm thấy kết quả");
        } else {
            binding.tvEmpty.setVisibility(View.GONE);
        }
    }

    private void loadExams() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.tvEmpty.setVisibility(View.GONE);

        ExamApi api = ApiClient.getClient().create(ExamApi.class);
        api.getPublishedExams(null).enqueue(new Callback<ApiResponse<List<Exam>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Exam>>> call, Response<ApiResponse<List<Exam>>> response) {
                if (binding == null) return;
                binding.progressBar.setVisibility(View.GONE);
                binding.swipeRefresh.setRefreshing(false);

                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    List<Exam> exams = response.body().getData();
                    if (exams != null && !exams.isEmpty()) {
                        allExams = exams;
                        applyFilters();
                    } else {
                        allExams = LocalExamDataSource.getInstance().getPublishedExams(requireContext());
                        applyFilters();
                    }
                } else {
                    allExams = LocalExamDataSource.getInstance().getPublishedExams(requireContext());
                    applyFilters();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Exam>>> call, Throwable t) {
                if (binding == null) return;
                binding.progressBar.setVisibility(View.GONE);
                binding.swipeRefresh.setRefreshing(false);
                allExams = LocalExamDataSource.getInstance().getPublishedExams(requireContext());
                applyFilters();
                Toast.makeText(requireContext(), "Đang dùng dữ liệu đề thi cục bộ", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void navigateToDetail(Exam exam) {
        Intent intent = new Intent(requireContext(), ExamDetailActivity.class);
        intent.putExtra("exam_id", exam.getId());
        intent.putExtra("exam_title", exam.getTitle());
        intent.putExtra("exam_description", exam.getDescription());
        intent.putExtra("exam_subject", exam.getSubjectName());
        intent.putExtra("total_questions", exam.getTotalQuestions());
        intent.putExtra("duration_minutes", exam.getDurationMinutes());
        startActivity(intent);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
