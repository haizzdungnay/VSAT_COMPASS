package com.example.v_sat_compass.ui.exam;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.v_sat_compass.data.api.ApiClient;
import com.example.v_sat_compass.data.api.ExamApi;
import com.example.v_sat_compass.data.model.ApiResponse;
import com.example.v_sat_compass.data.model.Exam;
import com.example.v_sat_compass.databinding.FragmentExamBinding;
import com.example.v_sat_compass.ui.exam.adapter.ExamAdapter;
import com.example.v_sat_compass.ui.exam.session.ExamSessionActivity;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ExamFragment extends Fragment {

    private FragmentExamBinding binding;
    private ExamAdapter adapter;

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

        adapter.setOnExamClickListener(exam -> {
            Intent intent = new Intent(requireContext(), ExamSessionActivity.class);
            intent.putExtra("exam_id", exam.getId());
            intent.putExtra("exam_title", exam.getTitle());
            intent.putExtra("duration_minutes", exam.getDurationMinutes());
            intent.putExtra("total_questions", exam.getTotalQuestions());
            startActivity(intent);
        });

        binding.swipeRefresh.setOnRefreshListener(this::loadExams);
        loadExams();
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
                        adapter.setExams(exams);
                        binding.tvEmpty.setVisibility(View.GONE);
                    } else {
                        binding.tvEmpty.setVisibility(View.VISIBLE);
                    }
                } else {
                    binding.tvEmpty.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<List<Exam>>> call, Throwable t) {
                if (binding == null) return;
                binding.progressBar.setVisibility(View.GONE);
                binding.swipeRefresh.setRefreshing(false);
                binding.tvEmpty.setVisibility(View.VISIBLE);
                Toast.makeText(requireContext(), "Loi ket noi", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
