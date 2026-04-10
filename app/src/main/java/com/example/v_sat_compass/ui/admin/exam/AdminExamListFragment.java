package com.example.v_sat_compass.ui.admin.exam;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.v_sat_compass.data.api.AdminApi;
import com.example.v_sat_compass.data.api.ApiClient;
import com.example.v_sat_compass.data.model.ApiResponse;
import com.example.v_sat_compass.data.model.Exam;
import com.example.v_sat_compass.databinding.FragmentAdminExamListBinding;
import com.example.v_sat_compass.ui.exam.adapter.ExamAdapter;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminExamListFragment extends Fragment {

    private FragmentAdminExamListBinding binding;
    private ExamAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminExamListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adapter = new ExamAdapter();
        binding.rvExams.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvExams.setAdapter(adapter);

        binding.fabCreateExam.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AdminCreateExamActivity.class)));

        binding.swipeRefresh.setOnRefreshListener(this::loadExams);
        loadExams();
    }

    private void loadExams() {
        binding.swipeRefresh.setRefreshing(true);
        AdminApi api = ApiClient.getClient().create(AdminApi.class);
        api.getAdminExams(null, null, 0, 50).enqueue(new Callback<ApiResponse<List<Exam>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<Exam>>> call,
                                   Response<ApiResponse<List<Exam>>> response) {
                if (binding == null) return;
                binding.swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null
                        && response.body().isSuccess()) {
                    adapter.setExams(response.body().getData());
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<List<Exam>>> call, Throwable t) {
                if (binding != null) binding.swipeRefresh.setRefreshing(false);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
