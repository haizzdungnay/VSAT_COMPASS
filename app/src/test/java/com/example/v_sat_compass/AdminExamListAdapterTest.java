package com.example.v_sat_compass;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.example.v_sat_compass.data.model.admin.AdminExamSummaryResponse;
import com.example.v_sat_compass.ui.admin.exam.AdminExamListAdapter;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class AdminExamListAdapterTest {

    private AdminExamListAdapter adapter;

    private AdminExamSummaryResponse makeExam(Long id, String title, String status) {
        AdminExamSummaryResponse e = new AdminExamSummaryResponse();
        e.setId(id);
        e.setTitle(title);
        e.setStatus(status);
        e.setExamCode("CODE_" + id);
        e.setVersion(1);
        return e;
    }

    @Before
    public void setUp() {
        adapter = new AdminExamListAdapter();
    }

    @Test
    public void setItems_updatesItemCount() {
        List<AdminExamSummaryResponse> items = Arrays.asList(
                makeExam(1L, "Đề 1", "DRAFT"),
                makeExam(2L, "Đề 2", "PUBLISHED")
        );
        adapter.setItems(items);
        assertEquals(2, adapter.getItemCount());
    }

    @Test
    public void setStatusFilter_filtersCorrectly() {
        adapter.setItems(Arrays.asList(
                makeExam(1L, "Đề 1", "DRAFT"),
                makeExam(2L, "Đề 2", "DRAFT"),
                makeExam(3L, "Đề 3", "PUBLISHED")
        ));
        adapter.setStatusFilter("DRAFT");
        assertEquals(2, adapter.getItemCount());
    }

    @Test
    public void setStatusFilter_nullShowsAll() {
        adapter.setItems(Arrays.asList(
                makeExam(1L, "Đề 1", "DRAFT"),
                makeExam(2L, "Đề 2", "PUBLISHED")
        ));
        adapter.setStatusFilter("DRAFT");
        assertEquals(1, adapter.getItemCount());
        adapter.setStatusFilter(null);
        assertEquals(2, adapter.getItemCount());
    }

    @Test
    public void getCurrentFilter_returnsSetFilter() {
        adapter.setStatusFilter("PENDING_REVIEW");
        assertEquals("PENDING_REVIEW", adapter.getCurrentFilter());
    }

    @Test
    public void getItemAt_validIndex_returnsItem() {
        adapter.setItems(Arrays.asList(makeExam(1L, "Đề 1", "DRAFT")));
        AdminExamSummaryResponse item = adapter.getItemAt(0);
        assertNotNull(item);
        assertEquals(Long.valueOf(1L), item.getId());
    }

    @Test
    public void getItemAt_outOfBounds_returnsNull() {
        adapter.setItems(Arrays.asList(makeExam(1L, "Đề 1", "DRAFT")));
        assertNull(adapter.getItemAt(5));
    }

    @Test
    public void appendItems_addsToExistingList() {
        adapter.setItems(Arrays.asList(makeExam(1L, "Đề 1", "DRAFT")));
        adapter.appendItems(Arrays.asList(makeExam(2L, "Đề 2", "DRAFT"), makeExam(3L, "Đề 3", "DRAFT")));
        assertEquals(3, adapter.getItemCount());
    }
}
