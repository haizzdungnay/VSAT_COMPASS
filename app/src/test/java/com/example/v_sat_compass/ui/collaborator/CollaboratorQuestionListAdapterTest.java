package com.example.v_sat_compass.ui.collaborator;

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

public class CollaboratorQuestionListAdapterTest {

    private CollaboratorQuestionListAdapter adapter;

    @Before
    public void setUp() {
        adapter = new CollaboratorQuestionListAdapter();
    }

    @Test
    public void setItemsAndAppendItems_updatesItemCount() {
        adapter.setItems(Arrays.asList(
                question(1L, QuestionStatus.DRAFT, Difficulty.EASY, QuestionType.SINGLE_CHOICE),
                question(2L, QuestionStatus.APPROVED, Difficulty.MEDIUM, QuestionType.TRUE_FALSE)
        ));

        adapter.appendItems(Arrays.asList(
                question(3L, QuestionStatus.NEEDS_REVISION, Difficulty.HARD,
                        QuestionType.FILL_IN_BLANK)
        ));

        assertEquals(3, adapter.getItemCount());
        assertEquals("Q-3", adapter.getItemAt(2).getQuestionCode());
    }

    @Test
    public void clear_removesItemsAndOutOfBoundsReturnsNull() {
        adapter.setItems(Arrays.asList(
                question(1L, QuestionStatus.DRAFT, Difficulty.EASY, QuestionType.SINGLE_CHOICE)
        ));

        adapter.clear();

        assertEquals(0, adapter.getItemCount());
        assertNull(adapter.getItemAt(0));
    }

    @Test
    public void exposedStatusLabels_mapToVietnameseResources() {
        assertEquals(R.string.cq_status_draft,
                CollaboratorQuestionListAdapter.statusLabelRes(QuestionStatus.DRAFT));
        assertEquals(R.string.cq_status_pending_review,
                CollaboratorQuestionListAdapter.statusLabelRes(QuestionStatus.PENDING_REVIEW));
        assertEquals(R.string.cq_status_needs_revision,
                CollaboratorQuestionListAdapter.statusLabelRes(QuestionStatus.NEEDS_REVISION));
        assertEquals(R.string.cq_status_approved,
                CollaboratorQuestionListAdapter.statusLabelRes(QuestionStatus.APPROVED));
    }

    @Test
    public void difficultyLabelsAndColors_coverAllDifficultyValues() {
        assertEquals(R.string.cq_difficulty_easy,
                CollaboratorQuestionListAdapter.difficultyLabelRes(Difficulty.EASY));
        assertEquals(R.color.cq_difficulty_easy_bg,
                CollaboratorQuestionListAdapter.difficultyColorRes(Difficulty.EASY));
        assertEquals(R.string.cq_difficulty_medium,
                CollaboratorQuestionListAdapter.difficultyLabelRes(Difficulty.MEDIUM));
        assertEquals(R.color.cq_difficulty_medium_bg,
                CollaboratorQuestionListAdapter.difficultyColorRes(Difficulty.MEDIUM));
        assertEquals(R.string.cq_difficulty_hard,
                CollaboratorQuestionListAdapter.difficultyLabelRes(Difficulty.HARD));
        assertEquals(R.color.cq_difficulty_hard_bg,
                CollaboratorQuestionListAdapter.difficultyColorRes(Difficulty.HARD));
        assertEquals(R.string.cq_difficulty_very_hard,
                CollaboratorQuestionListAdapter.difficultyLabelRes(Difficulty.VERY_HARD));
        assertEquals(R.color.cq_difficulty_very_hard_bg,
                CollaboratorQuestionListAdapter.difficultyColorRes(Difficulty.VERY_HARD));
    }

    @Test
    public void questionTypeLabels_coverAllQuestionTypes() {
        assertEquals(R.string.cq_type_single_choice,
                CollaboratorQuestionListAdapter.questionTypeLabelRes(QuestionType.SINGLE_CHOICE));
        assertEquals(R.string.cq_type_multiple_choice,
                CollaboratorQuestionListAdapter.questionTypeLabelRes(QuestionType.MULTIPLE_CHOICE));
        assertEquals(R.string.cq_type_true_false,
                CollaboratorQuestionListAdapter.questionTypeLabelRes(QuestionType.TRUE_FALSE));
        assertEquals(R.string.cq_type_fill_in_blank,
                CollaboratorQuestionListAdapter.questionTypeLabelRes(QuestionType.FILL_IN_BLANK));
    }

    @Test
    public void formatUpdatedAt_usesSafeIsoFallback() {
        assertEquals("2026-05-22 08:45",
                CollaboratorQuestionListAdapter.formatUpdatedAt("2026-05-22T08:45:30Z"));
        assertEquals("", CollaboratorQuestionListAdapter.formatUpdatedAt(null));
    }

    @Test
    public void clickListener_dispatchesSelectedQuestion() {
        AtomicReference<QuestionListItemResponse> clicked = new AtomicReference<>();
        adapter.setOnItemClickListener(clicked::set);
        adapter.setItems(Arrays.asList(
                question(7L, QuestionStatus.DRAFT, Difficulty.EASY, QuestionType.SINGLE_CHOICE)
        ));

        adapter.dispatchClickForTest(0);

        assertEquals(Long.valueOf(7L), clicked.get().getId());
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
