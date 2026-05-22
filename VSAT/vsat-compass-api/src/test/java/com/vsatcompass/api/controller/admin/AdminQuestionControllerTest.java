package com.vsatcompass.api.controller.admin;

import com.vsatcompass.api.dto.response.QuestionPickerItemResponse;
import com.vsatcompass.api.entity.enums.Difficulty;
import com.vsatcompass.api.entity.enums.QuestionStatus;
import com.vsatcompass.api.entity.enums.QuestionType;
import com.vsatcompass.api.security.jwt.JwtUtils;
import com.vsatcompass.api.security.service.CustomUserDetailsService;
import com.vsatcompass.api.service.QuestionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.annotation.Resource;
import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminQuestionController.class)
@Import(AdminQuestionControllerTest.MethodSecurityConfig.class)
@DisplayName("AdminQuestionController - question picker")
class AdminQuestionControllerTest {

    @Resource
    MockMvc mockMvc;

    @MockBean
    QuestionService questionService;

    @MockBean
    JwtUtils jwtUtils;

    @MockBean
    CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("GET /admin/questions/picker with no filters delegates APPROVED default")
    @WithMockUser(roles = "CONTENT_ADMIN")
    void getQuestionPicker_noFilters_delegatesApprovedDefault() throws Exception {
        stubPickerPage();

        mockMvc.perform(get("/admin/questions/picker"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Success"))
                .andExpect(jsonPath("$.data.content[0].id").value(1))
                .andExpect(jsonPath("$.data.content[0].questionCode").value("Q-T10-ABCD1234"));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(questionService).findForPicker(
                eq(QuestionStatus.APPROVED),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(20);
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("updatedAt").isDescending()).isTrue();
    }

    @Test
    @DisplayName("GET /admin/questions/picker accepts explicit status for SUPER_ADMIN")
    @WithMockUser(roles = "SUPER_ADMIN")
    void getQuestionPicker_statusParam_delegatesPendingReview() throws Exception {
        stubPickerPage();

        mockMvc.perform(get("/admin/questions/picker")
                        .param("status", "PENDING_REVIEW"))
                .andExpect(status().isOk());

        verify(questionService).findForPicker(
                eq(QuestionStatus.PENDING_REVIEW),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                any(Pageable.class));
    }

    @Test
    @DisplayName("GET /admin/questions/picker delegates subjectId and topicId")
    @WithMockUser(roles = "CONTENT_ADMIN")
    void getQuestionPicker_subjectAndTopic_delegatesBothFilters() throws Exception {
        stubPickerPage();

        mockMvc.perform(get("/admin/questions/picker")
                        .param("subjectId", "1")
                        .param("topicId", "10"))
                .andExpect(status().isOk());

        verify(questionService).findForPicker(
                eq(QuestionStatus.APPROVED),
                eq(1L),
                eq(10L),
                eq(null),
                eq(null),
                any(Pageable.class));
    }

    @Test
    @DisplayName("GET /admin/questions/picker delegates questionType")
    @WithMockUser(roles = "CONTENT_ADMIN")
    void getQuestionPicker_questionType_delegatesQuestionType() throws Exception {
        stubPickerPage();

        mockMvc.perform(get("/admin/questions/picker")
                        .param("questionType", "MULTIPLE_CHOICE"))
                .andExpect(status().isOk());

        verify(questionService).findForPicker(
                eq(QuestionStatus.APPROVED),
                eq(null),
                eq(null),
                eq(QuestionType.MULTIPLE_CHOICE),
                eq(null),
                any(Pageable.class));
    }

    @Test
    @DisplayName("GET /admin/questions/picker delegates q keyword")
    @WithMockUser(roles = "CONTENT_ADMIN")
    void getQuestionPicker_qKeyword_delegatesQ() throws Exception {
        stubPickerPage();

        mockMvc.perform(get("/admin/questions/picker")
                        .param("q", "linear"))
                .andExpect(status().isOk());

        verify(questionService).findForPicker(
                eq(QuestionStatus.APPROVED),
                eq(null),
                eq(null),
                eq(null),
                eq("linear"),
                any(Pageable.class));
    }

    @Test
    @DisplayName("GET /admin/questions/picker delegates combined filters")
    @WithMockUser(roles = "CONTENT_ADMIN")
    void getQuestionPicker_combinedFilters_delegatesAllFilters() throws Exception {
        stubPickerPage();

        mockMvc.perform(get("/admin/questions/picker")
                        .param("status", "PENDING_REVIEW")
                        .param("subjectId", "1")
                        .param("topicId", "10")
                        .param("questionType", "TRUE_FALSE")
                        .param("q", "fuel"))
                .andExpect(status().isOk());

        verify(questionService).findForPicker(
                eq(QuestionStatus.PENDING_REVIEW),
                eq(1L),
                eq(10L),
                eq(QuestionType.TRUE_FALSE),
                eq("fuel"),
                any(Pageable.class));
    }

    @Test
    @DisplayName("GET /admin/questions/picker size=100 is allowed")
    @WithMockUser(roles = "CONTENT_ADMIN")
    void getQuestionPicker_size100_allowed() throws Exception {
        stubPickerPage();

        mockMvc.perform(get("/admin/questions/picker")
                        .param("size", "100"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(questionService).findForPicker(
                eq(QuestionStatus.APPROVED),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(100);
    }

    @Test
    @DisplayName("GET /admin/questions/picker size=101 is capped to 100")
    @WithMockUser(roles = "CONTENT_ADMIN")
    void getQuestionPicker_size101_capsTo100() throws Exception {
        stubPickerPage();

        mockMvc.perform(get("/admin/questions/picker")
                        .param("page", "2")
                        .param("size", "101"))
                .andExpect(status().isOk());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(questionService).findForPicker(
                eq(QuestionStatus.APPROVED),
                eq(null),
                eq(null),
                eq(null),
                eq(null),
                pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(2);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(100);
    }

    @Test
    @DisplayName("GET /admin/questions/picker by STUDENT returns 403")
    @WithMockUser(roles = "STUDENT")
    void getQuestionPicker_student_forbidden() throws Exception {
        mockMvc.perform(get("/admin/questions/picker"))
                .andExpect(status().isForbidden());

        verify(questionService, never()).findForPicker(any(), any(), any(), any(), any(), any(Pageable.class));
    }

    @Test
    @DisplayName("GET /admin/questions/picker anonymous returns 401")
    void getQuestionPicker_anonymous_unauthorized() throws Exception {
        mockMvc.perform(get("/admin/questions/picker"))
                .andExpect(status().isUnauthorized());

        verify(questionService, never()).findForPicker(any(), any(), any(), any(), any(), any(Pageable.class));
    }

    private void stubPickerPage() {
        when(questionService.findForPicker(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenAnswer(inv -> pickerPage(inv.getArgument(5)));
    }

    private Page<QuestionPickerItemResponse> pickerPage(Pageable pageable) {
        QuestionPickerItemResponse item = QuestionPickerItemResponse.builder()
                .id(1L)
                .questionCode("Q-T10-ABCD1234")
                .questionTextSnippet("Find x")
                .subjectId(1L)
                .topicId(10L)
                .subtopicId(100L)
                .questionType(QuestionType.SINGLE_CHOICE)
                .difficulty(Difficulty.EASY)
                .status(QuestionStatus.APPROVED)
                .version(1)
                .updatedAt(OffsetDateTime.parse("2026-05-22T00:00:00Z"))
                .build();
        return new PageImpl<>(List.of(item), pageable, 1);
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityConfig {
    }
}
