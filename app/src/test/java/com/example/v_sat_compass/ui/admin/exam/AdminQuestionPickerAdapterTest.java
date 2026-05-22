package com.example.v_sat_compass.ui.admin.exam;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.example.v_sat_compass.data.model.admin.QuestionPickerItemResponse;
import com.example.v_sat_compass.data.model.enums.Difficulty;
import com.example.v_sat_compass.data.model.enums.QuestionStatus;
import com.example.v_sat_compass.data.model.enums.QuestionType;

import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

public class AdminQuestionPickerAdapterTest {

    private AdminQuestionPickerAdapter adapter;

    @Before
    public void setUp() {
        adapter = new AdminQuestionPickerAdapter();
    }

    @Test
    public void setItems_exposesQuestionCodeForBinding() {
        adapter.setItems(Collections.singletonList(question()));

        assertEquals(1, adapter.getItemCount());
        assertEquals("Q-T1-PICK", adapter.getItemAt(0).getQuestionCode());
    }

    @Test
    public void displaySnippet_bindsQuestionTextSnippet() {
        assertEquals("Short snippet", AdminQuestionPickerAdapter.displaySnippet(
                questionBuilder().questionTextSnippet("Short snippet").build()));
    }

    @Test
    public void difficultyLabel_bindsDifficultyBadge() {
        assertEquals("Trung bình", AdminQuestionPickerAdapter.difficultyLabel(Difficulty.MEDIUM));
    }

    @Test
    public void questionTypeLabel_bindsQuestionTypeBadge() {
        assertEquals("Nhiều đáp án",
                AdminQuestionPickerAdapter.questionTypeLabel(QuestionType.MULTIPLE_CHOICE));
    }

    @Test
    public void updateSelectionState_marksSelectedIds() {
        adapter.setItems(Collections.singletonList(question()));
        adapter.updateSelectionState(Collections.singleton(9L));

        assertTrue(adapter.isSelectedForTest(9L));
    }

    @Test
    public void click_dispatchesSelectionToggle() {
        AtomicReference<Long> selected = new AtomicReference<>();
        adapter.setItems(Collections.singletonList(question()));
        adapter.setOnSelectionToggleListener(selected::set);

        adapter.dispatchClickForTest(0);

        assertEquals(Long.valueOf(9L), selected.get());
    }

    private static QuestionPickerItemResponse question() {
        return questionBuilder().build();
    }

    private static QuestionPickerItemResponse.Builder questionBuilder() {
        return QuestionPickerItemResponse.builder()
                .id(9L)
                .questionCode("Q-T1-PICK")
                .questionTextSnippet("Short snippet")
                .subjectId(1L)
                .topicId(2L)
                .questionType(QuestionType.MULTIPLE_CHOICE)
                .difficulty(Difficulty.MEDIUM)
                .status(QuestionStatus.APPROVED)
                .version(1)
                .updatedAt("2026-05-22T00:00:00Z");
    }
}
