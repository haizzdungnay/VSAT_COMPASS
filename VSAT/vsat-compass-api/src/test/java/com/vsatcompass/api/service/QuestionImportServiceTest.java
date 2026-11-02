package com.vsatcompass.api.service;

import com.vsatcompass.api.dto.response.QuestionImportResultResponse;
import com.vsatcompass.api.entity.Question;
import com.vsatcompass.api.entity.QuestionGroup;
import com.vsatcompass.api.entity.QuestionOption;
import com.vsatcompass.api.entity.Subject;
import com.vsatcompass.api.entity.Topic;
import com.vsatcompass.api.exception.AppException;
import com.vsatcompass.api.repository.*;
import com.vsatcompass.api.service.impl.QuestionImportServiceImpl;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("QuestionImportService - Excel import and template generation")
class QuestionImportServiceTest {

    @Mock SubjectRepository subjectRepository;
    @Mock TopicRepository topicRepository;
    @Mock SubtopicRepository subtopicRepository;
    @Mock QuestionGroupRepository questionGroupRepository;
    @Mock QuestionRepository questionRepository;
    @Mock QuestionOptionRepository questionOptionRepository;

    @InjectMocks QuestionImportServiceImpl importService;

    @Test
    @DisplayName("generateTemplate: produces valid Excel workbook with 2 styled sheets")
    void generateTemplate_createsWorkbookWithTwoSheets() throws Exception {
        byte[] bytes = importService.generateTemplate();

        assertThat(bytes).isNotEmpty();
        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            assertThat(wb.getNumberOfSheets()).isEqualTo(2);
            assertThat(wb.getSheetAt(0).getSheetName()).isEqualTo("Single Questions");
            assertThat(wb.getSheetAt(1).getSheetName()).isEqualTo("Question Groups");
            assertThat(wb.getSheetAt(0).getPhysicalNumberOfRows()).isGreaterThanOrEqualTo(2);
            assertThat(wb.getSheetAt(1).getPhysicalNumberOfRows()).isGreaterThanOrEqualTo(3);
        }
    }

    @Test
    @DisplayName("importQuestions: dryRun=true validates rows from generated template without DB write")
    void importQuestions_dryRun_validatesWithoutSaving() {
        byte[] templateBytes = importService.generateTemplate();
        MockMultipartFile file = new MockMultipartFile(
                "file", "template.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", templateBytes);

        Subject math = Subject.builder().id(1L).code("MATH").name("Toán").build();
        Topic topic = Topic.builder().id(1L).subjectId(1L).code("MATH_ALG").name("Đại số").build();

        when(subjectRepository.findById(1L)).thenReturn(Optional.of(math));
        when(topicRepository.findById(1L)).thenReturn(Optional.of(topic));

        QuestionImportResultResponse result = importService.importQuestions(file, true, 99L);

        assertThat(result.isDryRun()).isTrue();
        assertThat(result.getTotalRows()).isEqualTo(3);
        assertThat(result.getValidCount()).isEqualTo(3);
        assertThat(result.getErrorCount()).isZero();
        assertThat(result.getImportedQuestions()).isZero();
        verify(questionRepository, never()).save(any());
        verify(questionOptionRepository, never()).save(any());
        verify(questionGroupRepository, never()).save(any());
    }

    @Test
    @DisplayName("importQuestions: dryRun=false saves questions, options and creates question group")
    void importQuestions_commit_savesToDatabase() {
        byte[] templateBytes = importService.generateTemplate();
        MockMultipartFile file = new MockMultipartFile(
                "file", "template.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", templateBytes);

        Subject math = Subject.builder().id(1L).code("MATH").name("Toán").build();
        Topic topic = Topic.builder().id(1L).subjectId(1L).code("MATH_ALG").name("Đại số").build();
        QuestionGroup savedGroup = QuestionGroup.builder().id(100L).code("GRP_01").subjectId(1L).build();
        Question savedQuestion = Question.builder().id(501L).build();

        when(subjectRepository.findById(1L)).thenReturn(Optional.of(math));
        when(topicRepository.findById(1L)).thenReturn(Optional.of(topic));
        when(questionGroupRepository.findByCode("GRP_01")).thenReturn(Optional.empty());
        when(questionGroupRepository.save(any(QuestionGroup.class))).thenReturn(savedGroup);
        when(questionRepository.save(any(Question.class))).thenReturn(savedQuestion);
        when(questionOptionRepository.save(any(QuestionOption.class))).thenAnswer(inv -> inv.getArgument(0));

        QuestionImportResultResponse result = importService.importQuestions(file, false, 99L);

        assertThat(result.isDryRun()).isFalse();
        assertThat(result.getValidCount()).isEqualTo(3);
        assertThat(result.getImportedQuestions()).isEqualTo(3);
        assertThat(result.getCreatedGroups()).isEqualTo(1);
        verify(questionGroupRepository).save(any(QuestionGroup.class));
        verify(questionRepository, times(3)).save(any(Question.class));
        verify(questionOptionRepository, atLeast(6)).save(any(QuestionOption.class));
    }

    @Test
    @DisplayName("importQuestions: dryRun=false reuses existing QuestionGroup by code")
    void importQuestions_commit_reusesExistingGroup() {
        byte[] templateBytes = importService.generateTemplate();
        MockMultipartFile file = new MockMultipartFile(
                "file", "template.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", templateBytes);

        Subject math = Subject.builder().id(1L).code("MATH").name("Toán").build();
        Topic topic = Topic.builder().id(1L).subjectId(1L).code("MATH_ALG").name("Đại số").build();
        QuestionGroup existingGroup = QuestionGroup.builder().id(100L).code("GRP_01").subjectId(1L).build();
        Question savedQuestion = Question.builder().id(501L).build();

        when(subjectRepository.findById(1L)).thenReturn(Optional.of(math));
        when(topicRepository.findById(1L)).thenReturn(Optional.of(topic));
        when(questionGroupRepository.findByCode("GRP_01")).thenReturn(Optional.of(existingGroup));
        when(questionRepository.save(any(Question.class))).thenReturn(savedQuestion);
        when(questionOptionRepository.save(any(QuestionOption.class))).thenAnswer(inv -> inv.getArgument(0));

        QuestionImportResultResponse result = importService.importQuestions(file, false, 99L);

        assertThat(result.getCreatedGroups()).isZero(); // Reused, not created
        assertThat(result.getImportedQuestions()).isEqualTo(3);
        verify(questionGroupRepository, never()).save(any(QuestionGroup.class));
    }

    @Test
    @DisplayName("importQuestions: dryRun=false with errors throws BAD_REQUEST and saves zero questions")
    void importQuestions_commitWithErrors_throwsBadRequest() {
        byte[] templateBytes = importService.generateTemplate();
        MockMultipartFile file = new MockMultipartFile(
                "file", "template.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", templateBytes);

        // Subject not found in DB -> produces validation error
        when(subjectRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> importService.importQuestions(file, false, 99L))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Không thể nạp vào CSDL vì còn");

        verify(questionRepository, never()).save(any());
        verify(questionOptionRepository, never()).save(any());
        verify(questionGroupRepository, never()).save(any());
    }

    @Test
    @DisplayName("importQuestions: corrupt or fake xlsx throws BAD_REQUEST")
    void importQuestions_fakeXlsx_throwsBadRequest() {
        MockMultipartFile fakeFile = new MockMultipartFile(
                "file", "fake.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "this is not an excel file content".getBytes());

        assertThatThrownBy(() -> importService.importQuestions(fakeFile, true, 99L))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Không thể đọc file Excel");
    }

    @Test
    @DisplayName("importQuestions: file exceeding 10MB limit throws BAD_REQUEST before parsing")
    void importQuestions_oversizedFile_throwsBadRequest() {
        MockMultipartFile bigFile = new MockMultipartFile(
                "file", "large.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[10]) {
            @Override
            public long getSize() {
                return 11 * 1024 * 1024L; // 11MB
            }
        };

        assertThatThrownBy(() -> importService.importQuestions(bigFile, true, 99L))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Kích thước file tối đa");
    }
}
