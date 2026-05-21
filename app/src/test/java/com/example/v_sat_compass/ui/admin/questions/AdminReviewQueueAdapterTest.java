package com.example.v_sat_compass.ui.admin.questions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.example.v_sat_compass.R;
import com.example.v_sat_compass.data.model.enums.Difficulty;
import com.example.v_sat_compass.data.model.enums.QuestionStatus;
import com.example.v_sat_compass.data.model.enums.QuestionType;
import com.example.v_sat_compass.data.model.question.QuestionListItemResponse;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

public class AdminReviewQueueAdapterTest {

    private AdminReviewQueueAdapter adapter;

    @Before
    public void setUp() {
        adapter = new AdminReviewQueueAdapter();
    }

    @Test
    public void setItems_exposesQuestionCodeForBinding() {
        adapter.setItems(Arrays.asList(
                question(1L, QuestionStatus.PENDING_REVIEW, Difficulty.EASY,
                        QuestionType.SINGLE_CHOICE)
        ));

        assertEquals(1, adapter.getItemCount());
        assertEquals("Q-1", adapter.getItemAt(0).getQuestionCode());
    }

    @Test
    public void badgeLabelMappings_coverStatusDifficultyAndType() {
        assertEquals(R.string.cq_status_pending_review,
                AdminReviewQueueAdapter.statusLabelRes(QuestionStatus.PENDING_REVIEW));
        assertEquals(R.string.cq_status_needs_revision,
                AdminReviewQueueAdapter.statusLabelRes(QuestionStatus.NEEDS_REVISION));
        assertEquals(R.string.cq_difficulty_hard,
                AdminReviewQueueAdapter.difficultyLabelRes(Difficulty.HARD));
        assertEquals(R.string.cq_type_multiple_choice,
                AdminReviewQueueAdapter.questionTypeLabelRes(QuestionType.MULTIPLE_CHOICE));
    }

    @Test
    public void displayQuestionText_fallsBackToSubjectAndTopicLabels() {
        QuestionListItemResponse item = question(7L, QuestionStatus.APPROVED,
                Difficulty.MEDIUM, QuestionType.TRUE_FALSE);

        assertEquals("Subject #1 / Topic #2",
                AdminReviewQueueAdapter.displayQuestionText(item));
    }

    @Test
    public void clickListener_dispatchesSelectedItem() {
        AtomicReference<QuestionListItemResponse> clicked = new AtomicReference<>();
        adapter.setOnItemClickListener(clicked::set);
        adapter.setItems(Arrays.asList(
                question(9L, QuestionStatus.PENDING_REVIEW, Difficulty.EASY,
                        QuestionType.SINGLE_CHOICE)
        ));

        adapter.dispatchClickForTest(0);

        assertEquals(Long.valueOf(9L), clicked.get().getId());
    }

    @Test
    public void emptyListAndClear_doNotCrashAndOutOfBoundsReturnsNull() {
        adapter.setItems(null);
        assertEquals(0, adapter.getItemCount());
        assertNull(adapter.getItemAt(0));

        adapter.setItems(Arrays.asList(
                question(2L, QuestionStatus.PUBLISHED, Difficulty.VERY_HARD,
                        QuestionType.FILL_IN_BLANK)
        ));
        adapter.clear();

        assertEquals(0, adapter.getItemCount());
        assertNull(adapter.getItemAt(0));
    }

    private static QuestionListItemResponse question(
            Long id,
            QuestionStatus status,
            Difficulty difficulty,
            QuestionType type
    ) {
        return new QuestionListItemResponse(
                id,
                "Q-" + id,
                1L,
                2L,
                difficulty,
                type,
                status,
                1,
                3L,
                "2026-05-22T08:45:30Z"
        );
    }
}
