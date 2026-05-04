package com.vsatcompass.api.service;

import com.vsatcompass.api.dto.response.SubtopicResponse;
import com.vsatcompass.api.entity.Subtopic;
import com.vsatcompass.api.entity.Topic;
import com.vsatcompass.api.exception.AppException;
import com.vsatcompass.api.repository.SubjectRepository;
import com.vsatcompass.api.repository.SubtopicRepository;
import com.vsatcompass.api.repository.TopicRepository;
import com.vsatcompass.api.service.impl.SubtopicServiceImpl;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubtopicService — Phase C1.1a baseline")
class SubtopicServiceTest {

    @Mock SubjectRepository subjectRepository;
    @Mock TopicRepository topicRepository;
    @Mock SubtopicRepository subtopicRepository;

    @InjectMocks SubtopicServiceImpl subtopicService;

    private Topic algebraTopic;
    private Subtopic linearEqSubtopic;
    private Subtopic quadraticSubtopic;

    @BeforeEach
    void setUp() {
        OffsetDateTime now = OffsetDateTime.now();

        algebraTopic = Topic.builder()
                .id(10L).subjectId(1L).code("MATH_ALGEBRA").name("Đại số")
                .displayOrder(1).isActive(true)
                .createdAt(now).updatedAt(now)
                .build();

        linearEqSubtopic = Subtopic.builder()
                .id(100L).topicId(10L).code("MATH_ALGEBRA_LINEAR").name("Phương trình bậc 1")
                .displayOrder(1).isActive(true)
                .createdAt(now).updatedAt(now)
                .build();

        quadraticSubtopic = Subtopic.builder()
                .id(101L).topicId(10L).code("MATH_ALGEBRA_QUAD").name("Phương trình bậc 2")
                .displayOrder(2).isActive(true)
                .createdAt(now).updatedAt(now)
                .build();
    }

    @Test
    @DisplayName("listActiveSubtopicsBySubjectAndTopic: happy path returns ordered mapped list")
    void happyPath_returnsMapped() {
        when(subjectRepository.existsById(1L)).thenReturn(true);
        when(topicRepository.findById(10L)).thenReturn(Optional.of(algebraTopic));
        when(subtopicRepository.findByTopicIdAndIsActiveTrueOrderByDisplayOrderAscIdAsc(10L))
                .thenReturn(List.of(linearEqSubtopic, quadraticSubtopic));

        List<SubtopicResponse> result = subtopicService.listActiveSubtopicsBySubjectAndTopic(1L, 10L);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(SubtopicResponse::getId).containsExactly(100L, 101L);
        assertThat(result).extracting(SubtopicResponse::getCode)
                .containsExactly("MATH_ALGEBRA_LINEAR", "MATH_ALGEBRA_QUAD");
        assertThat(result.get(0).getTopicId()).isEqualTo(10L);
        assertThat(result.get(0).getDisplayOrder()).isEqualTo(1);
        assertThat(result.get(1).getDisplayOrder()).isEqualTo(2);
    }

    @Test
    @DisplayName("listActiveSubtopicsBySubjectAndTopic: subject not found throws 404 BEFORE topic queried")
    void subjectMissing_throws404_doesNotQueryTopic() {
        when(subjectRepository.existsById(9999L)).thenReturn(false);

        assertThatThrownBy(() -> subtopicService.listActiveSubtopicsBySubjectAndTopic(9999L, 10L))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException ae = (AppException) ex;
                    assertThat(ae.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(ae.getCode()).isEqualTo("RESOURCE_NOT_FOUND");
                    assertThat(ae.getMessage()).contains("Subject").contains("9999");
                });

        verifyNoInteractions(topicRepository);
        verifyNoInteractions(subtopicRepository);
    }

    @Test
    @DisplayName("listActiveSubtopicsBySubjectAndTopic: topic not found throws 404 BEFORE subtopic queried")
    void topicMissing_throws404_doesNotQuerySubtopics() {
        when(subjectRepository.existsById(1L)).thenReturn(true);
        when(topicRepository.findById(9999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subtopicService.listActiveSubtopicsBySubjectAndTopic(1L, 9999L))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException ae = (AppException) ex;
                    assertThat(ae.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(ae.getCode()).isEqualTo("RESOURCE_NOT_FOUND");
                    assertThat(ae.getMessage()).contains("Topic").contains("9999");
                });

        verifyNoInteractions(subtopicRepository);
    }

    @Test
    @DisplayName("listActiveSubtopicsBySubjectAndTopic: topic belongs to different subject throws 400 VALIDATION_FAILED")
    void topicMismatch_throws400Validation() {
        // algebraTopic.subjectId = 1L, but caller passes subjectId = 2L
        when(subjectRepository.existsById(2L)).thenReturn(true);
        when(topicRepository.findById(10L)).thenReturn(Optional.of(algebraTopic));

        assertThatThrownBy(() -> subtopicService.listActiveSubtopicsBySubjectAndTopic(2L, 10L))
                .isInstanceOf(AppException.class)
                .satisfies(ex -> {
                    AppException ae = (AppException) ex;
                    assertThat(ae.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(ae.getCode()).isEqualTo("VALIDATION_FAILED");
                    assertThat(ae.getMessage()).contains("Topic 10").contains("subject 2");
                });

        verifyNoInteractions(subtopicRepository);
    }

    @Test
    @DisplayName("listActiveSubtopicsBySubjectAndTopic: empty topic returns empty list, not null")
    void emptyTopic_returnsEmptyList() {
        when(subjectRepository.existsById(1L)).thenReturn(true);
        when(topicRepository.findById(10L)).thenReturn(Optional.of(algebraTopic));
        when(subtopicRepository.findByTopicIdAndIsActiveTrueOrderByDisplayOrderAscIdAsc(10L))
                .thenReturn(Collections.emptyList());

        List<SubtopicResponse> result = subtopicService.listActiveSubtopicsBySubjectAndTopic(1L, 10L);

        assertThat(result).isNotNull().isEmpty();
        verify(subtopicRepository, times(1))
                .findByTopicIdAndIsActiveTrueOrderByDisplayOrderAscIdAsc(10L);
    }

    @Test
    @DisplayName("SubtopicResponse DTO does not expose isActive/createdAt/updatedAt")
    void responseExcludesAdminOnlyFields() {
        assertThat(SubtopicResponse.class.getDeclaredFields())
                .extracting(java.lang.reflect.Field::getName)
                .doesNotContain("isActive", "createdAt", "updatedAt")
                .containsExactlyInAnyOrder("id", "topicId", "code", "name", "description", "displayOrder");
    }
}
