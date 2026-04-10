package com.example.v_sat_compass.ui.admin.questions;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.v_sat_compass.R;
import com.example.v_sat_compass.data.api.AdminApi;
import com.example.v_sat_compass.data.api.ApiClient;
import com.example.v_sat_compass.data.model.ApiResponse;
import com.example.v_sat_compass.data.model.QuestionItem;
import com.example.v_sat_compass.databinding.FragmentAdminQuestionBankBinding;
import com.example.v_sat_compass.ui.collaborator.QuestionEditorActivity;
import com.example.v_sat_compass.util.UserRoleHelper;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Ngân hàng câu hỏi — dùng cho cả Admin lẫn CTV.
 *
 * CONTENT_ADMIN / SUPER_ADMIN: thấy tất cả câu hỏi, có thể duyệt
 * COLLABORATOR: chỉ thấy câu hỏi của mình (API endpoint khác)
 */
public class AdminQuestionBankFragment extends Fragment {

    private static final String ARG_INITIAL_FILTER = "initial_filter";

    private FragmentAdminQuestionBankBinding binding;
    private QuestionBankAdapter adapter;
    private String activeFilter = null; // null = tất cả
    private String searchQuery  = "";

    public static AdminQuestionBankFragment newInstance(String initialFilter) {
        AdminQuestionBankFragment f = new AdminQuestionBankFragment();
        Bundle args = new Bundle();
        args.putString(ARG_INITIAL_FILTER, initialFilter);
        f.setArguments(args);
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        binding = FragmentAdminQuestionBankBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Lấy initial filter từ argument (ví dụ: mở từ Dashboard "Duyệt câu hỏi")
        if (getArguments() != null) {
            activeFilter = getArguments().getString(ARG_INITIAL_FILTER, null);
        }

        adapter = new QuestionBankAdapter();
        binding.rvQuestions.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvQuestions.setAdapter(adapter);

        adapter.setOnQuestionClickListener(q -> {
            Intent intent = new Intent(requireContext(), AdminReviewQuestionActivity.class);
            intent.putExtra("question_id", q.getId());
            startActivity(intent);
        });

        setupTabFilters();
        setupSearch();

        binding.swipeRefresh.setOnRefreshListener(this::loadQuestions);
        binding.fabAddQuestion.setOnClickListener(v ->
                startActivity(new Intent(requireContext(), QuestionEditorActivity.class)));

        // Ẩn nút thêm câu hỏi cho CONTENT_ADMIN thuần (để CTV tạo)
        // Nhưng CONTENT_ADMIN vẫn có thể tạo từ dashboard
        loadQuestions();

        updateTabUI();
    }

    private void setupTabFilters() {
        binding.tabAll.setOnClickListener(v -> { activeFilter = null; updateTabUI(); loadQuestions(); });
        binding.tabPending.setOnClickListener(v -> { activeFilter = "PENDING"; updateTabUI(); loadQuestions(); });
        binding.tabApproved.setOnClickListener(v -> { activeFilter = "APPROVED"; updateTabUI(); loadQuestions(); });
        binding.tabPublished.setOnClickListener(v -> { activeFilter = "PUBLISHED"; updateTabUI(); loadQuestions(); });
        binding.tabRevision.setOnClickListener(v -> { activeFilter = "NEEDS_REVISION"; updateTabUI(); loadQuestions(); });
    }

    private void setupSearch() {
        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString().trim();
                loadQuestions();
            }
        });
    }

    private void loadQuestions() {
        binding.tvEmpty.setVisibility(View.GONE);
        binding.swipeRefresh.setRefreshing(true);

        AdminApi api = ApiClient.getClient().create(AdminApi.class);
        api.getQuestions(activeFilter, null, searchQuery.isEmpty() ? null : searchQuery, 0, 50)
                .enqueue(new Callback<ApiResponse<List<QuestionItem>>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<List<QuestionItem>>> call,
                                           Response<ApiResponse<List<QuestionItem>>> response) {
                        if (binding == null) return;
                        binding.swipeRefresh.setRefreshing(false);

                        if (response.isSuccessful() && response.body() != null
                                && response.body().isSuccess()) {
                            List<QuestionItem> list = response.body().getData();
                            if (list == null || list.isEmpty()) {
                                binding.tvEmpty.setVisibility(View.VISIBLE);
                                binding.rvQuestions.setVisibility(View.GONE);
                            } else {
                                binding.tvEmpty.setVisibility(View.GONE);
                                binding.rvQuestions.setVisibility(View.VISIBLE);
                                adapter.setQuestions(list);
                            }
                        } else {
                            showMockData(); // fallback khi chưa có API thật
                        }
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<List<QuestionItem>>> call, Throwable t) {
                        if (binding == null) return;
                        binding.swipeRefresh.setRefreshing(false);
                        showMockData(); // fallback khi offline
                    }
                });
    }

    /** Hiển thị dữ liệu mẫu khi chưa có backend */
    private void showMockData() {
        List<QuestionItem> mock = com.example.v_sat_compass.util.MockDataHelper.getMockQuestions(activeFilter);
        if (mock.isEmpty()) {
            binding.tvEmpty.setVisibility(View.VISIBLE);
            binding.rvQuestions.setVisibility(View.GONE);
        } else {
            binding.tvEmpty.setVisibility(View.GONE);
            binding.rvQuestions.setVisibility(View.VISIBLE);
            adapter.setQuestions(mock);
        }
    }

    private void updateTabUI() {
        resetTab(binding.tabAll);
        resetTab(binding.tabPending);
        resetTab(binding.tabApproved);
        resetTab(binding.tabPublished);
        resetTab(binding.tabRevision);

        TextView active;
        if ("PENDING".equals(activeFilter))        active = binding.tabPending;
        else if ("APPROVED".equals(activeFilter))  active = binding.tabApproved;
        else if ("PUBLISHED".equals(activeFilter)) active = binding.tabPublished;
        else if ("NEEDS_REVISION".equals(activeFilter)) active = binding.tabRevision;
        else                                       active = binding.tabAll;

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
