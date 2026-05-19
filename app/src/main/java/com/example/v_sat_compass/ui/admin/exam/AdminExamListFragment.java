package com.example.v_sat_compass.ui.admin.exam;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.v_sat_compass.data.model.admin.AdminExamSummaryResponse;
import com.example.v_sat_compass.data.model.admin.PageResponse;
import com.example.v_sat_compass.databinding.FragmentAdminExamListBinding;
import com.example.v_sat_compass.data.repository.Resource;

public class AdminExamListFragment extends Fragment {

    private static final int PAGE_SIZE = 20;

    private FragmentAdminExamListBinding binding;
    private AdminExamViewModel viewModel;
    private AdminExamListAdapter adapter;
    private int currentPage = 0;
    private boolean hasMore = false;

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

        viewModel = new ViewModelProvider(requireActivity()).get(AdminExamViewModel.class);

        adapter = new AdminExamListAdapter();
        adapter.setOnItemClickListener(exam -> {
            Intent intent = new Intent(requireContext(), AdminExamDetailActivity.class);
            intent.putExtra(AdminExamDetailActivity.EXTRA_EXAM_ID, exam.getId());
            startActivity(intent);
        });
        binding.rvExams.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvExams.setAdapter(adapter);

        setupFilterChips();

        binding.fabCreateExam.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), AdminCreateExamActivity.class)));

        binding.swipeRefresh.setOnRefreshListener(() -> loadPage(true));

        binding.btnLoadMore.setOnClickListener(v -> loadPage(false));

        viewModel.getListState().observe(getViewLifecycleOwner(), resource -> {
            if (binding == null) return;
            if (resource.getStatus() == Resource.Status.LOADING) {
                if (currentPage == 0) binding.swipeRefresh.setRefreshing(true);
                return;
            }
            binding.swipeRefresh.setRefreshing(false);
            if (resource.getStatus() == Resource.Status.SUCCESS && resource.getData() != null) {
                PageResponse<AdminExamSummaryResponse> page = resource.getData();
                if (currentPage == 0) {
                    adapter.setItems(page.getContent());
                } else {
                    adapter.appendItems(page.getContent());
                }
                hasMore = (page.getNumber() + 1) < page.getTotalPages();
                binding.btnLoadMore.setVisibility(hasMore ? View.VISIBLE : View.GONE);
                currentPage = page.getNumber() + 1;
            }
        });

        loadPage(true);
    }

    private void setupFilterChips() {
        TextView[] chips = {
                binding.chipAll, binding.chipDraft, binding.chipPendingReview,
                binding.chipPublished, binding.chipHidden, binding.chipArchived
        };
        String[] statuses = {"", "DRAFT", "PENDING_REVIEW", "PUBLISHED", "HIDDEN", "ARCHIVED"};

        for (int i = 0; i < chips.length; i++) {
            final String status = statuses[i];
            chips[i].setOnClickListener(v -> {
                adapter.setStatusFilter(status.isEmpty() ? null : status);
                for (TextView chip : chips) {
                    chip.setBackgroundResource(com.example.v_sat_compass.R.drawable.bg_chip_unselected);
                    chip.setTextColor(requireContext().getResources()
                            .getColor(com.example.v_sat_compass.R.color.primary));
                }
                ((TextView) v).setBackgroundResource(com.example.v_sat_compass.R.drawable.bg_chip_selected);
                ((TextView) v).setTextColor(android.graphics.Color.WHITE);
            });
        }
    }

    private void loadPage(boolean reset) {
        if (reset) {
            currentPage = 0;
        }
        String filter = adapter != null ? adapter.getCurrentFilter() : null;
        viewModel.loadExams(filter, null, currentPage, PAGE_SIZE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}

