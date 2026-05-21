package com.example.v_sat_compass.ui.collaborator;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.v_sat_compass.R;
import com.example.v_sat_compass.data.model.admin.PageResponse;
import com.example.v_sat_compass.data.model.enums.QuestionStatus;
import com.example.v_sat_compass.data.model.question.QuestionListItemResponse;
import com.example.v_sat_compass.data.repository.Resource;
import com.example.v_sat_compass.databinding.FragmentCollaboratorQuestionListBinding;

import java.util.Collections;
import java.util.List;

public class CollaboratorQuestionListFragment extends Fragment {

    private static final int PAGE_SIZE = 20;

    private FragmentCollaboratorQuestionListBinding binding;
    private CollaboratorQuestionViewModel viewModel;
    private CollaboratorQuestionListAdapter adapter;
    private QuestionStatus currentStatus;
    private int nextPage = 0;
    private boolean isLoading = false;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentCollaboratorQuestionListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(requireActivity()).get(CollaboratorQuestionViewModel.class);
        adapter = new CollaboratorQuestionListAdapter();
        adapter.setOnItemClickListener(question -> Toast.makeText(
                requireContext(),
                R.string.cq_row_click_placeholder,
                Toast.LENGTH_SHORT
        ).show());

        binding.rvQuestions.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvQuestions.setAdapter(adapter);

        setupFilters();
        setupActions();
        observeListState();
        loadPage(true);
    }

    public void refreshCurrentFilter() {
        loadPage(true);
    }

    private void setupFilters() {
        binding.chipAll.setOnClickListener(v -> onFilterChanged(null));
        binding.chipDraft.setOnClickListener(v -> onFilterChanged(QuestionStatus.DRAFT));
        binding.chipPendingReview.setOnClickListener(v ->
                onFilterChanged(QuestionStatus.PENDING_REVIEW));
        binding.chipNeedsRevision.setOnClickListener(v ->
                onFilterChanged(QuestionStatus.NEEDS_REVISION));
        binding.chipApproved.setOnClickListener(v -> onFilterChanged(QuestionStatus.APPROVED));
    }

    private void setupActions() {
        binding.btnLoadMore.setOnClickListener(v -> loadPage(false));
        binding.btnRetry.setOnClickListener(v -> loadPage(adapter.getItemCount() == 0));
    }

    private void observeListState() {
        viewModel.getListState().observe(getViewLifecycleOwner(), resource -> {
            if (binding == null || resource == null) {
                return;
            }
            if (resource.getStatus() == Resource.Status.LOADING) {
                isLoading = true;
                showLoading();
                return;
            }
            isLoading = false;
            if (resource.getStatus() == Resource.Status.SUCCESS) {
                handleSuccess(resource.getData());
            } else if (resource.getStatus() == Resource.Status.ERROR) {
                showError(resource.getMessage());
            }
        });
    }

    private void onFilterChanged(QuestionStatus status) {
        if (currentStatus == status) {
            return;
        }
        currentStatus = status;
        loadPage(true);
    }

    private void loadPage(boolean reset) {
        if (isLoading) {
            return;
        }
        if (reset) {
            nextPage = 0;
            adapter.clear();
            binding.btnLoadMore.setVisibility(View.GONE);
        }
        viewModel.listMyQuestions(currentStatus, nextPage, PAGE_SIZE);
    }

    private void handleSuccess(PageResponse<QuestionListItemResponse> page) {
        if (page == null) {
            showError(getString(R.string.cq_error_generic));
            return;
        }

        List<QuestionListItemResponse> content = page.getContent() != null
                ? page.getContent()
                : Collections.emptyList();
        if (page.getNumber() == 0) {
            adapter.setItems(content);
        } else {
            adapter.appendItems(content);
        }

        nextPage = page.getNumber() + 1;
        boolean hasMore = page.getNumber() < page.getTotalPages() - 1;
        showContent(adapter.getItemCount() == 0, hasMore);
    }

    private void showLoading() {
        binding.progressLoading.setVisibility(View.VISIBLE);
        binding.emptyState.setVisibility(View.GONE);
        binding.errorContainer.setVisibility(View.GONE);
        binding.rvQuestions.setVisibility(adapter.getItemCount() > 0 ? View.VISIBLE : View.GONE);
        binding.btnLoadMore.setVisibility(View.GONE);
    }

    private void showContent(boolean empty, boolean hasMore) {
        binding.progressLoading.setVisibility(View.GONE);
        binding.errorContainer.setVisibility(View.GONE);
        binding.emptyState.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.rvQuestions.setVisibility(empty ? View.GONE : View.VISIBLE);
        binding.btnLoadMore.setVisibility(!empty && hasMore ? View.VISIBLE : View.GONE);
    }

    private void showError(String message) {
        binding.progressLoading.setVisibility(View.GONE);
        binding.emptyState.setVisibility(View.GONE);
        binding.rvQuestions.setVisibility(adapter.getItemCount() > 0 ? View.VISIBLE : View.GONE);
        binding.btnLoadMore.setVisibility(View.GONE);
        binding.errorContainer.setVisibility(View.VISIBLE);
        binding.tvErrorMessage.setText(
                message != null && !message.trim().isEmpty()
                        ? message
                        : getString(R.string.cq_error_generic)
        );
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
