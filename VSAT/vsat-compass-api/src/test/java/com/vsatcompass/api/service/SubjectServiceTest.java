package com.vsatcompass.api.service;

import com.vsatcompass.api.dto.response.SubjectResponse;
import com.vsatcompass.api.dto.response.TopicResponse;
import com.vsatcompass.api.entity.Subject;
import com.vsatcompass.api.entity.Topic;
import com.vsatcompass.api.exception.AppException;
import com.vsatcompass.api.repository.SubjectRepository;
import com.vsatcompass.api.repository.TopicRepository;
import com.vsatcompass.api.service.impl.SubjectServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubjectService — Phase C1.0 baseline")
class SubjectServiceTest {

    @Mock SubjectRepository subjectRepository;
    @Mock TopicRepository topicRepository;

    @InjectMocks SubjectServiceImpl subjectService;

    private Subject mathSubject;
    private Subject engSubject;
    private Topic algebraTopic;
    private Topic geometryTopic;

    @BeforeEach
    void setUp() {
        OffsetDateTime now = OffsetDateTime.now();

        mathSubject = Subject.builder()
                .id(1L).code("MATH").name("Toán")
                .description("Môn Toán SAT")
                .iconUrl("https://cdn/math.png")
                .displayOrder(1).isActive(true)
                .createdAt(now).updatedAt(now)
                .build();

        engSubject = Subject.builder()
                .id(2L).code("ENG").name("Tiếng Anh")
                .description("Môn Tiếng Anh SAT")
                .iconUrl("https://cdn/eng.png")
                .displayOrder(2).isActive(true)
                .createdAt(now).updatedAt(now)
                .build();

        algebraTopic = Topic.builder()
                .id(10L).subjectId(1L).code("MATH_ALGEBRA").name("Đại số")
                .description("Phương trình, bất phương trình, hệ phương trình")
                .displayOrder(1).isActive(true)
                .createdAt(now).updatedAt(now)
                .build();

        geometryTopic = Topic.builder()
                .id(11L).subjectId(1L).code("MATH_GEOMETRY").name("Hình học")
                .description("Hình học phẳng và không gian")
                .displayOrder(2).isActive(true)
                .createdAt(now).updatedAt(now)
                .build();
    }

    // ===== listActiveSubjects =====

    @Test
    @DisplayName("listActiveSubjects: returns mapped list ordered by repo")
    void listActiveSubjects_returnsMappedList() {
        when(subjectRepository.findByIsActiveTrueOrderByDisplayOrderAscIdAsc())
                .thenReturn(List.of(mathSubject, engSubject));

        List<SubjectResponse> result = subjectService.listActiveSubjects();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(SubjectResponse::getId).containsExactly(1L, 2L);
        assertThat(result).extracting(SubjectResponse::getCode).containsExactly("MATH", "ENG");
        assertThat(result).extracting(SubjectResponse::getName).containsExactly("Toán", "Tiếng Anh");
        assertThat(result.get(0).getIconUrl()).isEqualTo("https://cdn/math.png");
        assertThat(result.get(0).getDescription()).isEqualTo("Môn Toán SAT");
        assertThat(result.get(0).getDisplayOrder()).isEqualTo(1);
    }

    @Test
    @DisplayName("listActiveSubjects: empty repo result returns empty list, not null")
    void listActiveSubjects_emptyRepo_returnsEmptyList() {
        when(subjectRepository.findByIsActiveTrueOrderByDisplayOrderAscIdAsc())
                .thenReturn(Collections.emptyList());

        List<SubjectResponse> result = subjectService.listActiveSubjects();

        assertThat(result).isNotNull().isEmpty();
        verifyNoInteractions(topicRepository);
    }

    @Test
    @DisplayName("listActiveSubjects: response DTO does not expose isActive/createdAt/updatedAt")
    void listActiveSubjects_responseExcludesAdminOnlyFields() {
        when(subjectRepository.findByIsActiveTrueOrderByDisplayOrderAscIdAsc())
                .thenReturn(List.of(mathSubject));

        List<SubjectResponse> result = subjectService.listActiveSubjects();

        assertThat(result).hasSize(1);
        SubjectResponse r = result.get(0);
        // SubjectResponse must NOT have getIsActive / getCreatedAt / getUpdatedAt accessors —
        // verify by reflection that those fields don't exist on the DTO class.
        assertThat(SubjectResponse.class.getDeclaredFields())
                .extracting(java.lang.reflect.Field::getName)
                .doesNotContain("isActive", "createdAt", "updatedAt")
                .containsExactlyInAnyOrder("id", "code", "name", "description", "iconUrl", "displayOrder");
        // And the populated DTO carries the public-safe fields:
        assertThat(r.getId()).isEqualTo(1L);
        assertThat(r.getCode()).isEqualTo("MATH");
    }

    @Test
    @DisplayName("listActiveSubjects: calls findByIsActiveTrueOrderByDisplayOrderAscIdAsc exactly once")
    void listActiveSubjects_callsCorrectRepoMethod() {
        when(subjectRepository.findByIsActiveTrueOrderByDisplayOrderAscIdAsc())
                .thenReturn(List.of(mathSubject));

        subjectService.listActiveSubjects();

        verify(subjectRepository, times(1)).findByIsActiveTrueOrderByDisplayOrderAscIdAsc();
        verify(subjectRepository, never()).findAll();
        verify(subjectRepository, never()).findById(anyLong());
        verifyNoInteractions(topicRepository);
    }

    // ===== listActiveTopicsBySubjectId =====

    @Test
    @DisplayName("listActiveTopicsBySubjectId: happy path returns mapped list")
    void listActiveTopicsBySubjectId_happyPath_returnsMapped() {
        when(subjectRepository.existsById(1L)).thenReturn(true);
        when(topicRepository.findBySubjectIdAndIsActiveTrueOrderByDisplayOrderAscIdAsc(1L))
                .thenReturn(List.of(algebraTopic, geometryTopic));

        List<TopicResponse> result = subjectService.listActiveTopicsBySubjectId(1L);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(TopicResponse::getId).containsExactly(10L, 11L);
        assertThat(result).extracting(TopicResponse::getCode).containsExactly("MATH_ALGEBRA", "MATH_GEOMETRY");
        assertThat(result).extracting(TopicResponse::getName).containsExactly("Đại số", "Hình học");
        assertThat(result.get(0).getDescription()).isEqualTo("Phương trình, bất phương trình, hệ phương trình");
        assertThat(result.get(0).getDisplayOrder()).isEqualTo(1);
        assertThat(result.get(1).getDisplayOrder()).isEqualTo(2);
    }

    @Test
    @DisplayName("listActiveTopicsBySubjectId: subject does not exist throws 404 AppException")
    void listActiveTopicsBySubjectId_subjectNotFound_throws404() {
        when(subjectRepository.existsById(9999L)).thenReturn(false);

        assertThatThrownBy(() -> subjectService.listActiveTopicsBySubjectId(9999L))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException ae = (AppException) ex;
                    assertThat(ae.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(ae.getCode()).isEqualTo("RESOURCE_NOT_FOUND");
                    assertThat(ae.getMessage()).contains("Subject").contains("9999");
                });
    }

    @Test
    @DisplayName("listActiveTopicsBySubjectId: subject exists but no active topics returns empty list")
    void listActiveTopicsBySubjectId_noActiveTopics_returnsEmpty() {
        when(subjectRepository.existsById(1L)).thenReturn(true);
        when(topicRepository.findBySubjectIdAndIsActiveTrueOrderByDisplayOrderAscIdAsc(1L))
                .thenReturn(Collections.emptyList());

        List<TopicResponse> result = subjectService.listActiveTopicsBySubjectId(1L);

        assertThat(result).isNotNull().isEmpty();
        verify(topicRepository, times(1)).findBySubjectIdAndIsActiveTrueOrderByDisplayOrderAscIdAsc(1L);
    }

    @Test
    @DisplayName("listActiveTopicsBySubjectId: 404 thrown BEFORE topic repo is queried")
    void listActiveTopicsBySubjectId_subjectMissing_doesNotQueryTopics() {
        when(subjectRepository.existsById(9999L)).thenReturn(false);

        assertThatThrownBy(() -> subjectService.listActiveTopicsBySubjectId(9999L))
                .isInstanceOf(AppException.class);

        verifyNoInteractions(topicRepository);
    }

    @Test
    @DisplayName("listActiveTopicsBySubjectId: response includes subjectId for client-side grouping")
    void listActiveTopicsBySubjectId_responseIncludesSubjectId() {
        when(subjectRepository.existsById(1L)).thenReturn(true);
        when(topicRepository.findBySubjectIdAndIsActiveTrueOrderByDisplayOrderAscIdAsc(1L))
                .thenReturn(List.of(algebraTopic));

        List<TopicResponse> result = subjectService.listActiveTopicsBySubjectId(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSubjectId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("listActiveTopicsBySubjectId: response DTO does not expose isActive/createdAt/updatedAt")
    void listActiveTopicsBySubjectId_responseExcludesAdminOnlyFields() {
        assertThat(TopicResponse.class.getDeclaredFields())
                .extracting(java.lang.reflect.Field::getName)
                .doesNotContain("isActive", "createdAt", "updatedAt")
                .containsExactlyInAnyOrder("id", "subjectId", "code", "name", "description", "displayOrder");
    }
}
