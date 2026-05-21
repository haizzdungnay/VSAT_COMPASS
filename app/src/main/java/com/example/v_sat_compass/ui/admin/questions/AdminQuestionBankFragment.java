package com.example.v_sat_compass.ui.admin.questions;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.v_sat_compass.R;
import com.example.v_sat_compass.data.model.admin.PageResponse;
import com.example.v_sat_compass.data.model.enums.QuestionStatus;
import com.example.v_sat_compass.data.model.question.QuestionListItemResponse;
import com.example.v_sat_compass.data.repository.Resource;
import com.example.v_sat_compass.databinding.FragmentAdminQuestionBankBinding;
import com.example.v_sat_compass.ui.collaborator.CollaboratorCreateQuestionActivity;

import java.util.ArrayList;
import java.util.List;

public class AdminQuestionBankFragment extends Fragment {

    private static final String ARG_INITIAL_FILTER = "initial_filter";

    private FragmentAdminQuestionBankBinding binding;
    private AdminReviewQueueAdapter adapter;
    private AdminReviewViewModel viewModel;
    private String activeFilter = null;
    private String searchQuery = "";
    private final List<QuestionListItemResponse> currentItems = new ArrayList<>();

    private final ActivityResultLauncher<Intent> reviewLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    loadQuestions();
                }
            }
    );

    public static AdminQuestionBankFragment newInstance(String initialFilter) {
        AdminQuestionBankFragment fragment = new AdminQuestionBankFragment();
        Bundle args = new Bundle();
        args.putString(ARG_INITIAL_FILTER, initialFilter);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        binding = FragmentAdminQuestionBankBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            activeFilter = getArguments().getString(ARG_INITIAL_FILTER, null);
        }

        viewModel = new ViewModelProvider(this).get(AdminReviewViewModel.class);
        adapter = new AdminReviewQueueAdapter();
        binding.rvQuestions.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvQuestions.setAdapter(adapter);

        adapter.setOnItemClickListener(item -> {
            if (item == null || item.getId() == null) {
                return;
            }
            Intent intent = new Intent(requireContext(), AdminReviewQuestionActivity.class);
            intent.putExtra(AdminReviewQuestionActivity.EXTRA_QUESTION_ID, item.getId());
            reviewLauncher.launch(intent);
        });

        setupTabFilters();
        setupSearch();
        observeViewModel();

        binding.swipeRefresh.setOnRefreshListener(this::loadQuestions);
        binding.fabAddQuestion.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), CollaboratorCreateQuestionActivity.class)));

        updateTabUI();
        loadQuestions();
    }

    private void setupTabFilters() {
        binding.tabAll.setOnClickListener(v -> selectFilter(null));
        binding.tabPending.setOnClickListener(v -> selectFilter("PENDING"));
        binding.tabApproved.setOnClickListener(v -> selectFilter("APPROVED"));
        binding.tabPublished.setOnClickListener(v -> selectFilter("PUBLISHED"));
        binding.tabRevision.setOnClickListener(v -> selectFilter("NEEDS_REVISION"));
    }

    private void selectFilter(String filter) {
        activeFilter = filter;
        updateTabUI();
        loadQuestions();
    }

    private void setupSearch() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().trim();
                applySearch();
            }
        });
    }

    private void observeViewModel() {
        viewModel.getQueueState().observe(getViewLifecycleOwner(), resource -> {
            if (binding == null || resource == null) {
                return;
            }
            if (resource.getStatus() == Resource.Status.LOADING) {
                binding.tvEmpty.setVisibility(View.GONE);
                binding.swipeRefresh.setRefreshing(true);
            } else if (resource.getStatus() == Resource.Status.SUCCESS) {
                binding.swipeRefresh.setRefreshing(false);
                currentItems.clear();
                PageResponse<QuestionListItemResponse> page = resource.getData();
                if (page != null && page.getContent() != null) {
                    currentItems.addAll(page.getContent());
                }
                applySearch();
            } else if (resource.getStatus() == Resource.Status.ERROR) {
                binding.swipeRefresh.setRefreshing(false);
                adapter.clear();
                binding.rvQuestions.setVisibility(View.GONE);
                binding.tvEmpty.setText(messageOrDefault(resource.getMessage()));
                binding.tvEmpty.setVisibility(View.VISIBLE);
            }
        });
    }

    private void loadQuestions() {
        viewModel.loadQueue(statusForFilter(activeFilter), 0);
    }

    private void applySearch() {
        if (binding == null) {
            return;
        }
        List<QuestionListItemResponse> filtered = new ArrayList<>();
        for (QuestionListItemResponse item : currentItems) {
            if (matchesSearch(item, searchQuery)) {
                filtered.add(item);
            }
        }
        adapter.setItems(filtered);
        boolean empty = filtered.isEmpty();
        binding.tvEmpty.setText("Không có câu hỏi nào");
        binding.tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
        binding.rvQuestions.setVisibility(empty ? View.GONE : View.VISIBLE);
    }

    private QuestionStatus statusForFilter(String filter) {
        if ("PENDING".equals(filter)) {
            return QuestionStatus.PENDING_REVIEW;
        }
        if ("APPROVED".equals(filter)) {
            return QuestionStatus.APPROVED;
        }
        if ("PUBLISHED".equals(filter)) {
            return QuestionStatus.PUBLISHED;
        }
        if ("NEEDS_REVISION".equals(filter)) {
            return QuestionStatus.NEEDS_REVISION;
        }
        return null;
    }

    private boolean matchesSearch(QuestionListItemResponse item, String query) {
        if (item == null) {
            return false;
        }
        if (query == null || query.isEmpty()) {
            return true;
        }
        String lower = query.toLowerCase();
        return contains(item.getQuestionCode(), lower)
                || contains(AdminReviewQueueAdapter.displayQuestionText(item), lower)
                || contains(enumName(item.getStatus()), lower)
                || contains(enumName(item.getDifficulty()), lower)
                || contains(enumName(item.getQuestionType()), lower);
    }

    private boolean contains(String value, String lowerQuery) {
        return value != null && value.toLowerCase().contains(lowerQuery);
    }

    private String enumName(Enum<?> value) {
        return value != null ? value.name() : null;
    }

    private String messageOrDefault(String message) {
        return message != null && !message.trim().isEmpty()
                ? message
                : "Không tải được danh sách câu hỏi";
    }

    private void updateTabUI() {
        resetTab(binding.tabAll);
        resetTab(binding.tabPending);
        resetTab(binding.tabApproved);
        resetTab(binding.tabPublished);
        resetTab(binding.tabRevision);

        TextView active;
        if ("PENDING".equals(activeFilter)) {
            active = binding.tabPending;
        } else if ("APPROVED".equals(activeFilter)) {
            active = binding.tabApproved;
        } else if ("PUBLISHED".equals(activeFilter)) {
            active = binding.tabPublished;
        } else if ("NEEDS_REVISION".equals(activeFilter)) {
            active = binding.tabRevision;
        } else {
            active = binding.tabAll;
        }

        active.setBackgroundResource(R.drawable.bg_chip_selected);
        active.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
    }

    private void resetTab(TextView tab) {
        tab.setBackgroundResource(R.drawable.bg_chip_unselected);
        tab.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
