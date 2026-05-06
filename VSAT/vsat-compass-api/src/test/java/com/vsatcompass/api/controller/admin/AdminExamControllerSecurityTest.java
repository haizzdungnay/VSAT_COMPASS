package com.vsatcompass.api.controller.admin;

import com.vsatcompass.api.dto.response.AdminExamResponse;
import com.vsatcompass.api.entity.enums.ExamStatus;
import com.vsatcompass.api.security.jwt.JwtUtils;
import com.vsatcompass.api.security.service.CustomUserDetailsService;
import com.vsatcompass.api.service.AdminExamService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.annotation.Resource;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminExamController.class)
@Import(AdminExamControllerSecurityTest.MethodSecurityConfig.class)
@DisplayName("AdminExamController - Phase C1.2b-2 security and no-body workflow")
class AdminExamControllerSecurityTest {

    @Resource
    MockMvc mockMvc;

    @MockBean
    AdminExamService adminExamService;

    @MockBean
    JwtUtils jwtUtils;

    @MockBean
    CustomUserDetailsService customUserDetailsService;

    @Test
    @DisplayName("WF-06 publish by CONTENT_ADMIN -> 403 FORBIDDEN")
    @WithMockUser(roles = "CONTENT_ADMIN")
    void wf06_publishByContentAdmin_forbidden() throws Exception {
        mockMvc.perform(post("/admin/exams/1/publish").with(csrf()))
                .andExpect(status().isForbidden());

        verify(adminExamService, never()).publish(null, 1L);
    }

    @Test
    @DisplayName("WF-12 reject-review has no request body binding; no-body request succeeds")
    @WithMockUser(roles = "CONTENT_ADMIN")
    void wf12_rejectReviewNoBody_succeeds() throws Exception {
        when(adminExamService.rejectReview(1L)).thenReturn(AdminExamResponse.builder()
                .id(1L)
                .status(ExamStatus.DRAFT)
                .build());

        mockMvc.perform(post("/admin/exams/1/reject-review").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));

        verify(adminExamService).rejectReview(1L);
    }

    @Test
    @DisplayName("discard DRAFT by CONTENT_ADMIN -> 200")
    @WithMockUser(roles = "CONTENT_ADMIN")
    void discardDraft_contentAdmin_succeeds() throws Exception {
        mockMvc.perform(delete("/admin/exams/1").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Draft exam discarded"));

        verify(adminExamService).discardDraftExam(1L);
    }

    @Test
    @DisplayName("discard DRAFT by SUPER_ADMIN -> 200")
    @WithMockUser(roles = "SUPER_ADMIN")
    void discardDraft_superAdmin_succeeds() throws Exception {
        mockMvc.perform(delete("/admin/exams/1").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Draft exam discarded"));

        verify(adminExamService).discardDraftExam(1L);
    }

    @Test
    @DisplayName("discard DRAFT by STUDENT -> 403")
    @WithMockUser(roles = "STUDENT")
    void discardDraft_student_forbidden() throws Exception {
        mockMvc.perform(delete("/admin/exams/1").with(csrf()))
                .andExpect(status().isForbidden());

        verify(adminExamService, never()).discardDraftExam(1L);
    }

    @Test
    @DisplayName("discard DRAFT anonymous -> 401")
    void discardDraft_anonymous_unauthorized() throws Exception {
        mockMvc.perform(delete("/admin/exams/1").with(csrf()))
                .andExpect(status().isUnauthorized());

        verify(adminExamService, never()).discardDraftExam(1L);
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class MethodSecurityConfig {
    }
}
