package com.example.v_sat_compass.ui.admin.exam;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.v_sat_compass.R;
import com.example.v_sat_compass.data.model.admin.PageResponse;
import com.example.v_sat_compass.data.model.admin.QuestionPickerItemResponse;
import com.example.v_sat_compass.data.model.enums.QuestionStatus;
import com.example.v_sat_compass.data.model.enums.QuestionType;
import com.example.v_sat_compass.data.repository.Resource;

import java.util.Set;

public class AdminQuestionPickerActivity extends AppCompatActivity {

    public static final String EXTRA_EXAM_ID = "examId";
    public static final String EXTRA_SELECTED_IDS = "selectedIds";

    private static final int PICKER_PAGE_SIZE = 50;

    private AdminQuestionPickerViewModel viewModel;
    private AdminQuestionPickerAdapter adapter;
    private QuestionType selectedType;

    private EditText editSearch;
    private TextView textError;
    private ProgressBar progressLoading;
    private Button buttonAdd;
    private TextView chipAll;
    private TextView chipSingleChoice;
    private TextView chipMultipleChoice;
    private TextView chipTrueFalse;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_question_picker);

        viewModel = new ViewModelProvider(this).get(AdminQuestionPickerViewModel.class);
        adapter = new AdminQuestionPickerAdapter();

        bindViews();
        setupRecyclerView();
        setupFilters();
        observeViewModel();
        loadFirstPage();
    }

    private void bindViews() {
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        editSearch = findViewById(R.id.editSearch);
        textError = findViewById(R.id.textError);
        progressLoading = findViewById(R.id.progressLoading);
        buttonAdd = findViewById(R.id.btnAddSelected);
        chipAll = findViewById(R.id.chipAll);
        chipSingleChoice = findViewById(R.id.chipSingleChoice);
        chipMultipleChoice = findViewById(R.id.chipMultipleChoice);
        chipTrueFalse = findViewById(R.id.chipTrueFalse);

        findViewById(R.id.btnSearch).setOnClickListener(v -> loadFirstPage());
        editSearch.setOnEditorActionListener((v, actionId, event) -> {
            boolean submitted = actionId == EditorInfo.IME_ACTION_SEARCH
                    || (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER);
            if (submitted) {
                loadFirstPage();
                return true;
            }
            return false;
        });

        buttonAdd.setOnClickListener(v -> finishWithSelection());
        updateAddButton();
    }

    private void setupRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.recyclerQuestions);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
        adapter.setOnSelectionToggleListener(id -> {
            viewModel.toggleSelection(id);
            adapter.updateSelectionState(viewModel.getSelectedIds());
            updateAddButton();
        });
    }

    private void setupFilters() {
        chipAll.setOnClickListener(v -> selectType(null, chipAll));
        chipSingleChoice.setOnClickListener(v ->
                selectType(QuestionType.SINGLE_CHOICE, chipSingleChoice));
        chipMultipleChoice.setOnClickListener(v ->
                selectType(QuestionType.MULTIPLE_CHOICE, chipMultipleChoice));
        chipTrueFalse.setOnClickListener(v -> selectType(QuestionType.TRUE_FALSE, chipTrueFalse));
        selectType(null, chipAll);
    }

    private void selectType(QuestionType type, TextView selectedChip) {
        selectedType = type;
        TextView[] chips = {chipAll, chipSingleChoice, chipMultipleChoice, chipTrueFalse};
        for (TextView chip : chips) {
            chip.setBackgroundResource(R.drawable.bg_chip_unselected);
            chip.setTextColor(ContextCompat.getColor(this, R.color.primary));
        }
        selectedChip.setBackgroundResource(R.drawable.bg_chip_selected);
        selectedChip.setTextColor(ContextCompat.getColor(this, R.color.white));
        loadFirstPage();
    }

    private void observeViewModel() {
        viewModel.getPickerState().observe(this, resource -> {
            if (resource == null) {
                return;
            }
            if (resource.getStatus() == Resource.Status.LOADING) {
                progressLoading.setVisibility(View.VISIBLE);
                textError.setVisibility(View.GONE);
                return;
            }
            progressLoading.setVisibility(View.GONE);
            if (resource.getStatus() == Resource.Status.SUCCESS && resource.getData() != null) {
                PageResponse<QuestionPickerItemResponse> page = resource.getData();
                adapter.setItems(page.getContent());
                adapter.updateSelectionState(viewModel.getSelectedIds());
            } else if (resource.getStatus() == Resource.Status.ERROR) {
                textError.setText(resource.getMessage() != null
                        ? resource.getMessage()
                        : "Không tải được câu hỏi.");
                textError.setVisibility(View.VISIBLE);
            }
        });
    }

    private void loadFirstPage() {
        String q = editSearch == null || editSearch.getText() == null
                ? null
                : editSearch.getText().toString();
        viewModel.loadPicker(QuestionStatus.APPROVED, null, null, selectedType, q, 0,
                PICKER_PAGE_SIZE);
    }

    private void updateAddButton() {
        int count = viewModel.getSelectedCount();
        buttonAdd.setEnabled(count > 0);
        buttonAdd.setText("Thêm " + count + " câu hỏi");
    }

    private void finishWithSelection() {
        Set<Long> ids = viewModel.getSelectedIds();
        long[] selectedIds = new long[ids.size()];
        int index = 0;
        for (Long id : ids) {
            if (id != null) {
                selectedIds[index++] = id;
            }
        }
        if (index != selectedIds.length) {
            long[] compact = new long[index];
            System.arraycopy(selectedIds, 0, compact, 0, index);
            selectedIds = compact;
        }
        if (selectedIds.length == 0) {
            Toast.makeText(this, "Chưa chọn câu hỏi", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent data = new Intent();
        data.putExtra(EXTRA_SELECTED_IDS, selectedIds);
        setResult(RESULT_OK, data);
        finish();
    }
}
