package com.vsatcompass.api.service;

import com.vsatcompass.api.dto.request.SessionRequest;
import com.vsatcompass.api.dto.response.QuestionImportResultResponse;
import com.vsatcompass.api.dto.response.SessionResponse;
import com.vsatcompass.api.entity.ExamQuestion;
import com.vsatcompass.api.entity.ExamSession;
import com.vsatcompass.api.entity.Question;
import com.vsatcompass.api.entity.QuestionGroup;
import com.vsatcompass.api.entity.QuestionMatchingChoice;
import com.vsatcompass.api.entity.QuestionOption;
import com.vsatcompass.api.entity.Subject;
import com.vsatcompass.api.entity.Topic;
import com.vsatcompass.api.entity.enums.Difficulty;
import com.vsatcompass.api.entity.enums.QuestionStatus;
import com.vsatcompass.api.entity.enums.QuestionType;
import com.vsatcompass.api.entity.enums.SessionMode;
import com.vsatcompass.api.entity.enums.SessionStatus;
import com.vsatcompass.api.exception.AppException;
import com.vsatcompass.api.repository.*;
import com.vsatcompass.api.service.impl.QuestionImportServiceImpl;
import com.vsatcompass.api.service.impl.SessionServiceImpl;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("QuestionImportService - Excel, JSON, DOCX, TXT import and template generation")
class QuestionImportServiceTest {

    @Mock SubjectRepository subjectRepository;
    @Mock TopicRepository topicRepository;
    @Mock SubtopicRepository subtopicRepository;
    @Mock QuestionGroupRepository questionGroupRepository;
    @Mock QuestionRepository questionRepository;
    @Mock QuestionOptionRepository questionOptionRepository;
    @Mock QuestionMatchingChoiceRepository questionMatchingChoiceRepository;

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

        assertThat(result.getCreatedGroups()).isZero();
        assertThat(result.getImportedQuestions()).isEqualTo(3);
        verify(questionGroupRepository, never()).save(any(QuestionGroup.class));
    }

    @Test
    @DisplayName("importQuestions: dryRun=false with errors throws BAD_REQUEST and saves zero questions")
    void importQuestions_commitWithErrors_throwsBadRequest() {
        byte[] templateBytes = importService.generateTemplate();
        MockMultipartFile file = new MockMultipartFile(
                "file", "template.xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", templateBytes);

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
                return 11 * 1024 * 1024L;
            }
        };

        assertThatThrownBy(() -> importService.importQuestions(bigFile, true, 99L))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Kích thước file tối đa");
    }

    @Test
    @DisplayName("importQuestions: unsupported extensions (.pdf, .exe) throw BAD_REQUEST")
    void importQuestions_unsupportedExtensions_throwsBadRequest() {
        MockMultipartFile pdfFile = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", new byte[]{1, 2, 3});
        assertThatThrownBy(() -> importService.importQuestions(pdfFile, true, 99L))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Định dạng file không hỗ trợ");

        MockMultipartFile exeFile = new MockMultipartFile(
                "file", "danger.exe", "application/octet-stream", new byte[]{1, 2, 3});
        assertThatThrownBy(() -> importService.importQuestions(exeFile, true, 99L))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("Định dạng file không hỗ trợ");
    }

    @Test
    @DisplayName("importQuestions: valid JSON array format parsed successfully")
    void importQuestions_jsonArray_parsesSuccessfully() {
        String json = """
                [
                  {
                    "subject": "MATH",
                    "topic": "1",
                    "questionType": "SINGLE_CHOICE",
                    "difficulty": "TH",
                    "questionText": "Hàm số nào đồng biến trên R?",
                    "options": ["y = x^3 + x", "y = -x^3", "y = x^2", "y = 1/x"],
                    "correctAnswer": "A",
                    "explanation": "Đạo hàm y' = 3x^2 + 1 > 0 với mọi x"
                  }
                ]
                """;
        MockMultipartFile file = new MockMultipartFile(
                "file", "questions.json", "application/json", json.getBytes(StandardCharsets.UTF_8));

        Subject math = Subject.builder().id(1L).code("MATH").name("Toán").build();
        Topic topic = Topic.builder().id(1L).subjectId(1L).code("MATH_ALG").name("Đại số").build();

        when(subjectRepository.findByCode("MATH")).thenReturn(Optional.of(math));
        when(topicRepository.findById(1L)).thenReturn(Optional.of(topic));

        QuestionImportResultResponse result = importService.importQuestions(file, true, 99L);

        assertThat(result.isDryRun()).isTrue();
        assertThat(result.getTotalRows()).isEqualTo(1);
        assertThat(result.getValidCount()).isEqualTo(1);
        assertThat(result.getErrorCount()).isZero();
        assertThat(result.getItems().get(0).getQuestionType()).isEqualTo(QuestionType.SINGLE_CHOICE);
        assertThat(result.getItems().get(0).getDifficulty()).isEqualTo(Difficulty.MEDIUM);
    }

    @Test
    @DisplayName("importQuestions: valid JSON object format with questions and group audio parsed successfully")
    void importQuestions_jsonObjectWithGroup_parsesSuccessfully() {
        String json = """
                {
                  "questions": [
                    {
                      "groupCode": "GRP_ENG_01",
                      "groupTitle": "Listening Section 1",
                      "passageText": "Listen to the conversation between two students.",
                      "audioUrl": "https://cdn.example.com/audio/eng01.mp3",
                      "maxPlays": 2,
                      "subject": "ENG",
                      "topic": "10",
                      "questionType": "SINGLE_CHOICE",
                      "difficulty": "NB",
                      "questionText": "What time does the library close?",
                      "options": ["8:00 PM", "9:00 PM", "10:00 PM", "11:00 PM"],
                      "correctAnswer": "B",
                      "explanation": "The speaker explicitly mentions 9:00 PM."
                    }
                  ]
                }
                """;
        MockMultipartFile file = new MockMultipartFile(
                "file", "questions_group.json", "application/json", json.getBytes(StandardCharsets.UTF_8));

        Subject eng = Subject.builder().id(2L).code("ENG").name("Tiếng Anh").build();
        Topic topic = Topic.builder().id(10L).subjectId(2L).code("ENG_LISTENING").name("Nghe hiểu").build();

        when(subjectRepository.findByCode("ENG")).thenReturn(Optional.of(eng));
        when(topicRepository.findById(10L)).thenReturn(Optional.of(topic));

        QuestionImportResultResponse result = importService.importQuestions(file, true, 99L);

        assertThat(result.getTotalRows()).isEqualTo(1);
        assertThat(result.getValidCount()).isEqualTo(1);
        assertThat(result.getItems().get(0).getGroupCode()).isEqualTo("GRP_ENG_01");
        assertThat(result.getItems().get(0).getAudioUrl()).isEqualTo("https://cdn.example.com/audio/eng01.mp3");
        assertThat(result.getItems().get(0).getMaxPlays()).isEqualTo(2);
    }

    @Test
    @DisplayName("importQuestions: valid DOCX file parsed successfully")
    void importQuestions_docx_parsesSuccessfully() throws Exception {
        byte[] docxBytes;
        try (XWPFDocument doc = new XWPFDocument(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            doc.createParagraph().createRun().setText("[Q]");
            doc.createParagraph().createRun().setText("[SUBJECT] MATH");
            doc.createParagraph().createRun().setText("[TOPIC] 1");
            doc.createParagraph().createRun().setText("[TYPE] SINGLE_CHOICE");
            doc.createParagraph().createRun().setText("[DIFF] NB");
            doc.createParagraph().createRun().setText("[STEM] Số đo của góc vuông là bao nhiêu độ?");
            doc.createParagraph().createRun().setText("[A] 45 độ");
            doc.createParagraph().createRun().setText("[B] 60 độ");
            doc.createParagraph().createRun().setText("[C] 90 độ");
            doc.createParagraph().createRun().setText("[D] 180 độ");
            doc.createParagraph().createRun().setText("[KEY] C");
            doc.createParagraph().createRun().setText("[EXPLANATION] Góc vuông có số đo 90 độ");
            doc.write(bos);
            docxBytes = bos.toByteArray();
        }

        MockMultipartFile file = new MockMultipartFile(
                "file", "sample.docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", docxBytes);

        Subject math = Subject.builder().id(1L).code("MATH").name("Toán").build();
        Topic topic = Topic.builder().id(1L).subjectId(1L).code("MATH_ALG").name("Đại số").build();

        when(subjectRepository.findByCode("MATH")).thenReturn(Optional.of(math));
        when(topicRepository.findById(1L)).thenReturn(Optional.of(topic));

        QuestionImportResultResponse result = importService.importQuestions(file, true, 99L);

        assertThat(result.getTotalRows()).isEqualTo(1);
        assertThat(result.getValidCount()).isEqualTo(1);
        assertThat(result.getItems().get(0).getQuestionText()).isEqualTo("Số đo của góc vuông là bao nhiêu độ?");
        assertThat(result.getItems().get(0).getCorrectAnswer()).isEqualTo("C");
    }

    @Test
    @DisplayName("importQuestions: valid TXT file with MATCHING type parsed successfully")
    void importQuestions_txt_parsesMatchingSuccessfully() {
        String txt = """
                [Q]
                [SUBJECT] MATH
                [TOPIC] 1
                [TYPE] MATCHING
                [DIFF] VD
                [STEM] Ghép nối các mệnh đề tương ứng sau
                [A] Hà Nội
                [B] Huế
                [C] Đà Nẵng
                [D] Sài Gòn
                [KEY] 1-C,2-A,3-D,4-B
                [EXPLANATION] Ghép cặp theo địa danh
                """;

        MockMultipartFile file = new MockMultipartFile(
                "file", "questions.txt", "text/plain", txt.getBytes(StandardCharsets.UTF_8));

        Subject math = Subject.builder().id(1L).code("MATH").name("Toán").build();
        Topic topic = Topic.builder().id(1L).subjectId(1L).code("MATH_ALG").name("Đại số").build();

        when(subjectRepository.findByCode("MATH")).thenReturn(Optional.of(math));
        when(topicRepository.findById(1L)).thenReturn(Optional.of(topic));

        QuestionImportResultResponse result = importService.importQuestions(file, true, 99L);

        assertThat(result.getTotalRows()).isEqualTo(1);
        assertThat(result.getValidCount()).isEqualTo(1);
        assertThat(result.getItems().get(0).getQuestionType()).isEqualTo(QuestionType.MATCHING);
        assertThat(result.getItems().get(0).getDifficulty()).isEqualTo(Difficulty.HARD);
    }

    @Test
    @DisplayName("importQuestions: rejects unknown type, MCQ multi-key, and TF 3 options (VF-D02/D03/D04)")
    void importQuestions_vsatValidationErrors() {
        String json = """
                [
                  {
                    "subject": "MATH", "topic": "1", "type": "ABC",
                    "questionText": "Unknown type",
                    "options": ["A", "B", "C", "D"], "correctAnswer": "A"
                  },
                  {
                    "subject": "MATH", "topic": "1", "type": "SINGLE_CHOICE",
                    "questionText": "MCQ multi key",
                    "options": ["A", "B", "C", "D"], "correctAnswer": "A,B"
                  },
                  {
                    "subject": "MATH", "topic": "1", "type": "TRUE_FALSE",
                    "questionText": "TF 3 options",
                    "options": ["M1", "M2", "M3"], "correctAnswer": "A,C"
                  }
                ]
                """;
        MockMultipartFile file = new MockMultipartFile(
                "file", "bad.json", "application/json", json.getBytes(StandardCharsets.UTF_8));

        Subject math = Subject.builder().id(1L).code("MATH").name("Toán").build();
        Topic topic = Topic.builder().id(1L).subjectId(1L).code("MATH_ALG").name("Đại số").build();
        when(subjectRepository.findByCode("MATH")).thenReturn(Optional.of(math));
        when(topicRepository.findById(1L)).thenReturn(Optional.of(topic));

        QuestionImportResultResponse result = importService.importQuestions(file, true, 99L);

        assertThat(result.getTotalRows()).isEqualTo(3);
        assertThat(result.getValidCount()).isZero();
        assertThat(result.getErrorCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("importQuestions: commit saves PENDING_REVIEW, SHORT_ANSWER option, and MATCHING options + choices")
    void importQuestions_commit_vsatTypesAndPendingReview() {
        String json = """
                [
                  {
                    "subject": "MATH", "topic": "1", "type": "TRUE_FALSE",
                    "questionText": "TF question",
                    "options": ["M1", "M2", "M3", "M4"], "correctAnswer": "A,C"
                  },
                  {
                    "subject": "MATH", "topic": "1", "type": "SHORT_ANSWER",
                    "questionText": "SA question",
                    "answer": "12.5"
                  },
                  {
                    "subject": "MATH", "topic": "1", "type": "MATCHING",
                    "questionText": "Match question",
                    "options": ["Ha Noi", "Hue", "Da Nang", "Sai Gon"],
                    "correctAnswer": "1-C,2-A,3-D,4-B"
                  }
                ]
                """;
        MockMultipartFile file = new MockMultipartFile(
                "file", "vsat.json", "application/json", json.getBytes(StandardCharsets.UTF_8));

        Subject math = Subject.builder().id(1L).code("MATH").name("Toán").build();
        Topic topic = Topic.builder().id(1L).subjectId(1L).code("MATH_ALG").name("Đại số").build();
        when(subjectRepository.findByCode("MATH")).thenReturn(Optional.of(math));
        when(topicRepository.findById(1L)).thenReturn(Optional.of(topic));
        when(questionRepository.save(any(Question.class))).thenAnswer(inv -> {
            Question q = inv.getArgument(0);
            q.setId(q.getQuestionType().ordinal() + 100L);
            return q;
        });
        when(questionOptionRepository.save(any(QuestionOption.class))).thenAnswer(inv -> inv.getArgument(0));
        when(questionMatchingChoiceRepository.save(any(QuestionMatchingChoice.class))).thenAnswer(inv -> inv.getArgument(0));

        QuestionImportResultResponse result = importService.importQuestions(file, false, 99L);

        assertThat(result.getErrorCount()).isZero();
        assertThat(result.getImportedQuestions()).isEqualTo(3);

        ArgumentCaptor<Question> qCap = ArgumentCaptor.forClass(Question.class);
        verify(questionRepository, times(3)).save(qCap.capture());
        assertThat(qCap.getAllValues()).allSatisfy(q -> assertThat(q.getStatus()).isEqualTo(QuestionStatus.PENDING_REVIEW));

        ArgumentCaptor<QuestionOption> optCap = ArgumentCaptor.forClass(QuestionOption.class);
        verify(questionOptionRepository, times(9)).save(optCap.capture());
        List<QuestionOption> savedOpts = optCap.getAllValues();
        assertThat(savedOpts).anySatisfy(o -> {
            assertThat(o.getOptionLabel()).isEqualTo("A");
            assertThat(o.getOptionText()).isEqualTo("12.5");
            assertThat(o.getIsCorrect()).isTrue();
        });
        assertThat(savedOpts).anySatisfy(o -> {
            assertThat(o.getOptionLabel()).isEqualTo("1");
            assertThat(o.getOptionText()).isEqualTo("Mục 1 ⇄ C");
            assertThat(o.getIsCorrect()).isTrue();
        });

        ArgumentCaptor<QuestionMatchingChoice> choiceCap = ArgumentCaptor.forClass(QuestionMatchingChoice.class);
        verify(questionMatchingChoiceRepository, times(4)).save(choiceCap.capture());
        assertThat(choiceCap.getAllValues()).extracting(QuestionMatchingChoice::getChoiceKey)
                .containsExactly("A", "B", "C", "D");
    }

    @Test
    @DisplayName("Imported MATCHING and SHORT_ANSWER questions score 6.0 pts each in SessionService")
    void importedMatchingAndShortAnswer_scoreSixPointsInSession() {
        String json = """
                [
                  {
                    "subject": "MATH",
                    "topicId": 1,
                    "difficulty": "MEDIUM",
                    "questionType": "MATCHING",
                    "questionText": "Ghep cap tinh thanh",
                    "options": ["Ha Noi", "Hue", "Da Nang", "Can Tho"],
                    "correctAnswer": "1-C,2-A,3-D,4-B"
                  },
                  {
                    "subject": "MATH",
                    "topicId": 1,
                    "difficulty": "MEDIUM",
                    "questionType": "SHORT_ANSWER",
                    "questionText": "Tinh 25 / 2 = ?",
                    "correctAnswer": "12.5"
                  }
                ]
                """;
        MockMultipartFile file = new MockMultipartFile(
                "file", "vsat.json", "application/json", json.getBytes(StandardCharsets.UTF_8));

        Subject math = Subject.builder().id(1L).code("MATH").name("Toán").build();
        Topic topic = Topic.builder().id(1L).subjectId(1L).code("MATH_ALG").name("Đại số").build();
        when(subjectRepository.findByCode("MATH")).thenReturn(Optional.of(math));
        when(topicRepository.findById(1L)).thenReturn(Optional.of(topic));

        java.util.List<Question> savedQuestions = new java.util.ArrayList<>();
        java.util.List<QuestionOption> savedOptions = new java.util.ArrayList<>();
        when(questionRepository.save(any(Question.class))).thenAnswer(inv -> {
            Question q = inv.getArgument(0);
            q.setId((long) (savedQuestions.size() + 10));
            savedQuestions.add(q);
            return q;
        });
        when(questionOptionRepository.save(any(QuestionOption.class))).thenAnswer(inv -> {
            QuestionOption opt = inv.getArgument(0);
            opt.setId((long) (savedOptions.size() + 100));
            savedOptions.add(opt);
            return opt;
        });
        when(questionMatchingChoiceRepository.save(any(QuestionMatchingChoice.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        QuestionImportResultResponse importResult = importService.importQuestions(file, false, 99L);
        assertThat(importResult.getImportedQuestions()).isEqualTo(2);

        Question matchingQ = savedQuestions.get(0);
        Question shortQ = savedQuestions.get(1);
        List<QuestionOption> matchingOpts = savedOptions.stream()
                .filter(o -> matchingQ.getId().equals(o.getQuestionId()))
                .toList();
        List<QuestionOption> shortOpts = savedOptions.stream()
                .filter(o -> shortQ.getId().equals(o.getQuestionId()))
                .toList();

        ExamSessionRepository examSessionRepo = org.mockito.Mockito.mock(ExamSessionRepository.class);
        ExamQuestionRepository examQuestionRepo = org.mockito.Mockito.mock(ExamQuestionRepository.class);
        SessionAnswerRepository sessionAnswerRepo = org.mockito.Mockito.mock(SessionAnswerRepository.class);
        SessionServiceImpl sessionService = new SessionServiceImpl(
                examSessionRepo, examQuestionRepo, questionRepository,
                questionOptionRepository, sessionAnswerRepo, questionGroupRepository,
                org.mockito.Mockito.mock(ExamWindowRepository.class));

        ExamSession session = ExamSession.builder()
                .id(500L).userId(7L).examId(20L).mode(SessionMode.MOCK_EXAM).status(SessionStatus.IN_PROGRESS).build();
        when(examSessionRepo.findById(500L)).thenReturn(Optional.of(session));
        when(examSessionRepo.save(any(ExamSession.class))).thenAnswer(inv -> inv.getArgument(0));
        when(examQuestionRepo.findByExamIdOrderByQuestionOrderAscIdAsc(20L)).thenReturn(List.of(
                ExamQuestion.builder().id(1L).examId(20L).questionId(matchingQ.getId()).questionOrder(1)
                        .maxRawScore(java.math.BigDecimal.valueOf(6.0)).build(),
                ExamQuestion.builder().id(2L).examId(20L).questionId(shortQ.getId()).questionOrder(2)
                        .maxRawScore(java.math.BigDecimal.valueOf(6.0)).build()
        ));
        when(questionRepository.findById(matchingQ.getId())).thenReturn(Optional.of(matchingQ));
        when(questionRepository.findById(shortQ.getId())).thenReturn(Optional.of(shortQ));
        when(questionOptionRepository.findByQuestionIdOrderByDisplayOrderAscIdAsc(matchingQ.getId()))
                .thenReturn(matchingOpts);
        when(questionOptionRepository.findByQuestionIdOrderByDisplayOrderAscIdAsc(shortQ.getId()))
                .thenReturn(shortOpts);

        SessionRequest.ClientSubmit.ClientSubmitAnswer a1 = new SessionRequest.ClientSubmit.ClientSubmitAnswer();
        a1.setQuestionId(matchingQ.getId());
        a1.setQuestionOrder(1);
        a1.setAnswerPayload(java.util.Map.of("1", "C", "2", "A", "3", "D", "4", "B"));

        SessionRequest.ClientSubmit.ClientSubmitAnswer a2 = new SessionRequest.ClientSubmit.ClientSubmitAnswer();
        a2.setQuestionId(shortQ.getId());
        a2.setQuestionOrder(2);
        a2.setAnswerPayload(java.util.Map.of("value", "12.5"));

        SessionRequest.ClientSubmit req = new SessionRequest.ClientSubmit();
        req.setAnswers(List.of(a1, a2));
        req.setTotalQuestions(2);

        SessionResponse.SessionInfo info = sessionService.clientSubmit(7L, 500L, req);

        assertThat(info.getCorrectCount()).isEqualTo(2);
        assertThat(info.getRawScore()).isEqualByComparingTo("12.0");
    }

    @Test
    @DisplayName("Invalid maxPlays in JSON and TXT fails row instead of falling back to 2")
    void invalidMaxPlays_failsRowInsteadOfFallback() {
        String json = """
                [
                  {
                    "subject": "MATH",
                    "topicId": 1,
                    "difficulty": "EASY",
                    "questionType": "SINGLE_CHOICE",
                    "questionText": "Bad maxPlays",
                    "options": ["A", "B", "C", "D"],
                    "correctAnswer": "A",
                    "maxPlays": "abc"
                  },
                  {
                    "subject": "MATH",
                    "topicId": 1,
                    "difficulty": "EASY",
                    "questionType": "SINGLE_CHOICE",
                    "questionText": "Zero maxPlays",
                    "options": ["A", "B", "C", "D"],
                    "correctAnswer": "A",
                    "maxPlays": 0
                  }
                ]
                """;
        MockMultipartFile jsonFile = new MockMultipartFile(
                "file", "bad-maxplays.json", "application/json", json.getBytes(StandardCharsets.UTF_8));

        QuestionImportResultResponse jsonPreview = importService.importQuestions(jsonFile, true, 99L);
        assertThat(jsonPreview.getErrorCount()).isEqualTo(2);
        assertThat(jsonPreview.getValidCount()).isZero();

        String txt = """
                [SUBJECT]: MATH
                [TOPIC]: 1
                [TYPE]: SINGLE_CHOICE
                [STEM]: Bad TXT maxPlays
                [A]: 1
                [B]: 2
                [C]: 3
                [D]: 4
                [KEY]: A
                [MAXPLAYS]: invalid
                """;
        MockMultipartFile txtFile = new MockMultipartFile(
                "file", "bad-maxplays.txt", "text/plain", txt.getBytes(StandardCharsets.UTF_8));

        QuestionImportResultResponse txtPreview = importService.importQuestions(txtFile, true, 99L);
        assertThat(txtPreview.getErrorCount()).isEqualTo(1);
        assertThat(txtPreview.getValidCount()).isZero();
    }
}
